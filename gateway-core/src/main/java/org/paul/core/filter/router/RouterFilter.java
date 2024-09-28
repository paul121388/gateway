package org.paul.core.filter.router;

import com.netflix.hystrix.*;
import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;
import org.paul.common.config.Rule;
import org.paul.common.enums.ResponseCode;
import org.paul.common.exception.ConnectException;
import org.paul.common.exception.ResponseException;
import org.paul.core.ConfigLoader;
import org.paul.core.context.GatewayContext;
import org.paul.core.filter.Filter;
import org.paul.core.filter.FilterAspect;
import org.paul.core.helper.AsyncHttpHelper;
import org.paul.core.helper.ResponseHelper;
import org.paul.core.response.GatewayResponse;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import static org.paul.common.constants.FilterConst.*;

/**
 * 路由过滤器
 */
@FilterAspect(id = ROUTER_FILTER_ID,
        name = ROUTER_FILTER_NAME,
        order = ROUTER_BALANCE_FILTER_ORDER
)
@Slf4j
public class RouterFilter implements Filter {

    /**
     * 执行过滤，处理异步请求结果
     *
     * @param gatewayContext
     * @throws Exception
     */
    @Override
    public void doFilter(GatewayContext gatewayContext) throws Exception {
        //使用自定义的方法获取rule中的HystrixConfig熔断配置，使用Optional进行包装
        Optional<Rule.HystrixConfig> hystrixConfig = getHystrixConfig(gatewayContext);
        //不一定每个接口都要走熔断，首先判断HystrixConfig熔断配置是否存在，存在：走有熔断的方法，不存在：走正常的重试
        if (hystrixConfig.isPresent()) {
            routeWithHystrix(gatewayContext, hystrixConfig);
        } else {
            route(gatewayContext, hystrixConfig);
        }
    }


    /**
     * 定义route方法，没有熔断配置时，就是原来的路由转发的代码
     * 在这段代码中还需要再次判断是否有熔断配置，没有才进行重试
     *
     * @param gatewayContext
     * @param hystrixConfig
     */
    private CompletableFuture<Response> route(GatewayContext gatewayContext, Optional<Rule.HystrixConfig> hystrixConfig) {
        //获取request对象（真正发送给下游服务时使用）
        Request request = gatewayContext.getRequest().build();
        //调用自定义AsyncHttpHelper，首先获取实例，然后执行request，返回Future对象
        CompletableFuture<Response> future = AsyncHttpHelper.getInstance().executeRequest(request);

        //拿到配置：通过configLoader获取config的单双异步配置信息
        boolean whenComplete = ConfigLoader.getConfig().isWhenComplete();

        //单异步
        if (whenComplete) {
            //调用future的whencomplete，传入闭包，参数时response，throwable
            //封装方法complete，参数：请求，响应，throwable，gatewaycontext上下文
            //在这段代码中还需要再次判断是否有熔断配置，没有才进行重试
            future.whenComplete((response, throwable) -> {
                complete(request, response, throwable, gatewayContext, hystrixConfig);
            });
            //双异步
        } else {
            //调用future的whencomplete，传入闭包，参数时response，throwable
            //封装方法complete，参数：请求，响应，throwable，gatewaycontext上下文
            future.whenCompleteAsync((response, throwable) -> {
                complete(request, response, throwable, gatewayContext, hystrixConfig);
            });
        }
        return future;
    }

    /**
     * 获取熔断配置
     *
     * @param gatewayContext
     * @return
     */
    private static Optional<Rule.HystrixConfig> getHystrixConfig(GatewayContext gatewayContext) {
        //从上下文中获取rule，从rule中获取熔断配置
        Rule rule = gatewayContext.getRule();
        //使用stream进行过滤，判断hystrixConfig中的path是否和上下文请求中的path相同，获取第一个，进行返回
        Optional<Rule.HystrixConfig> hystrixConfig = rule.getHystrixConfigs().stream().filter(c -> c.getPath().equals(gatewayContext.getRequest().getPath())).findFirst();
        return hystrixConfig;
    }


    /**
     * 有熔断配置时的路由转发
     *
     * @param gatewayContext
     * @param hystrixConfig
     */
    private void routeWithHystrix(GatewayContext gatewayContext, Optional<Rule.HystrixConfig> hystrixConfig) {
        //使用原生的HystrixCommand，定义一组的key，第一个key时上下文中的唯一id；定义CommandKey，key为上下文的请求中的path
        //设置Hystrix命令组的键。所有的Hystrix命令都会属于一个命令组，这个组通常是根据功能或服务来划分的。命令组用于统计、监控和配置的目的。
        HystrixCommand.Setter setter = HystrixCommand.Setter.withGroupKey(HystrixCommandGroupKey
                        .Factory
                        .asKey(gatewayContext.getUniqueId()))
                //设置Hystrix命令的键。每个Hystrix命令都有一个唯一的键，这个键用于区分不同的命令实例，特别是在监控和日志记录中。
                .andCommandKey(HystrixCommandKey.Factory
                        .asKey(gatewayContext.getRequest().getPath()))
                //设置线程池大小，从配置中获取
                .andThreadPoolPropertiesDefaults(HystrixThreadPoolProperties.Setter()
                        .withCoreSize(hystrixConfig.get().getThreadCoreSize()))
                //配置CommandProperties
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                        //定义隔离策略：线程隔离的方式，这意味着每个Hystrix命令将在单独的线程中执行，以隔离不同的依赖调用，防止一个慢调用或失败的依赖影响到其他依赖。
                        .withExecutionIsolationStrategy(HystrixCommandProperties.ExecutionIsolationStrategy.THREAD)
                        //设置超时时间，超过阈值，Hystrix将中断命令的执行
                        .withExecutionTimeoutInMilliseconds(hystrixConfig.get().getTimeoutInMilliseconds())
                        //发生异常时，Hystrix尝试执行中断
                        .withExecutionIsolationThreadInterruptOnTimeout(true)
                        //开启超时时间配置，超时直接报错
                        .withExecutionTimeoutEnabled(true));

        //new 原生的HystrixCommand
        new HystrixCommand<Object>(setter) {
            //执行原来的route
            @Override
            protected Object run() throws Exception {
                route(gatewayContext, hystrixConfig).get();
                return null;
            }

            //定义fallback，设置上下文中的response和上下文的状态
            @Override
            protected Object getFallback() {
                gatewayContext.setResponse(hystrixConfig);
                gatewayContext.writtened();
                return null;
            }
        }.execute();
    }


    /**
     * 处理异步的向下游发送请求后的结果
     *
     * @param request
     * @param response
     * @param throwable
     * @param gatewayContext
     */
    void complete(Request request, Response response, Throwable throwable, GatewayContext gatewayContext, Optional<Rule.HystrixConfig> hystrixConfig) {
        //释放资源
        gatewayContext.releaseRequest();

        //获取当前上下文的重试次数
        Rule rule = gatewayContext.getRule();

        //当前重试的次数
        int currentRetryTimes = gatewayContext.getCurrentRetryTimes();

        //应该重试的次数，配置在规则中
        int configRetryTimes = rule.getRetryConfig().getTimes();

        //重试应该在路由转发后，下游服务器返回失败后执行
        //并且熔断配置不存在时，才进行重试
        if ((throwable instanceof TimeoutException || throwable instanceof IOException)
                && currentRetryTimes <= configRetryTimes
                && !hystrixConfig.isPresent()) {
            doRetry(gatewayContext, currentRetryTimes);
            return;
        }

        try {
            //判断是否有异常
            if (Objects.nonNull(throwable)) {
                String url = request.getUrl();
                //超时异常：打印日志（url），设置gatewayContext的throwable，记录异常code
                if (throwable instanceof TimeoutException) {
                    log.warn("complete time out {}", url);
                    gatewayContext.setThrowable(new ResponseException(ResponseCode.REQUEST_TIMEOUT));
                    gatewayContext.setResponse(GatewayResponse.buildGatewayResponse(ResponseCode.REQUEST_TIMEOUT));
                } else {
                    //如果时其他异常，记录必要信息：唯一id，url，响应码
                    gatewayContext.setThrowable(new ConnectException(throwable, gatewayContext.getUniqueId(), url, ResponseCode.HTTP_RESPONSE_ERROR));
                    gatewayContext.setResponse(GatewayResponse.buildGatewayResponse(ResponseCode.HTTP_RESPONSE_ERROR));
                }
            } else {
                //正常的话，往上下文写入response
                gatewayContext.setResponse(GatewayResponse.buildGatewayResponse(response));
            }
        } catch (Throwable t) {
            gatewayContext.setThrowable(new ResponseException(ResponseCode.INTERNAL_ERROR));
            gatewayContext.setResponse(GatewayResponse.buildGatewayResponse(ResponseCode.INTERNAL_ERROR));
            log.error("complete error", t);
        } finally {
            gatewayContext.writtened();
            ResponseHelper.writeResponse(gatewayContext);
        }
    }

    /**
     * 执行重试
     *
     * @param gatewayContext
     * @param currentRetryTimes
     */
    private void doRetry(GatewayContext gatewayContext, int currentRetryTimes) {
        System.out.println("当前重试次数为：" + currentRetryTimes);

        //设置重试次数+1
        gatewayContext.setCurrentRetryTimes(currentRetryTimes + 1);
        try {
            //重新执行route路由过滤器的过滤动作，重新向下游服务器发送请求
            doFilter(gatewayContext);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
