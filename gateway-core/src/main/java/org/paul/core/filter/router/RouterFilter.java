package org.paul.core.filter.router;

import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;
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

import java.util.Objects;
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
     * 路由函数
     *      获取request对象（真正发送时使用
     *      调用自定义AsyncHttpHelper，首先获取实例，然后执行request，返回Future对象
     *
     *      拿到配置：通过configLoader获取config的单双异步配置信息
     *      如果单异步
     *          调用future的whencomplete，传入闭包，参数时response，throwable
     *              封装方法complete，参数：请求，响应，throwable，gatewaycontext上下文
     *      双异步类似
     *          闭包逻辑一致，改为异步调用whencomplete异步方法
     */
    @Override
    public void doFilter(GatewayContext gatewayContext) throws Exception {
        Request request = gatewayContext.getRequest().build();
        CompletableFuture<Response> future = AsyncHttpHelper.getInstance().executeRequest(request);

        boolean whenComplete = ConfigLoader.getConfig().isWhenComplete();
        if(whenComplete){
            future.whenComplete((response, throwable) -> {
                complete(request, response, throwable, gatewayContext);
            });
        }else{
            future.whenCompleteAsync((response, throwable) -> {
                complete(request, response, throwable, gatewayContext);
            });
        }
    }

    /**
     * complete方法：参数：请求，响应，throwable，gatewayContext上下文
     *       catch
     *          在gatewayContext中记录信息，打印日志
     *       finally
     *          修改gatewayContext的状态
     *          调用辅助类responseHelper，写回数据
     */
    void complete(Request request, Response response, Throwable throwable, GatewayContext gatewayContext){
        //释放资源
        gatewayContext.releaseRequest();

        try {
            //判断是否有异常
            if(Objects.nonNull(throwable)){
                String url = request.getUrl();
                //超时异常：打印日志（url），设置gatewayContext的throwable，记录异常code
                if(throwable instanceof TimeoutException){
                    log.warn("complete time out {}", url);
                    gatewayContext.setThrowable(new ResponseException(ResponseCode.REQUEST_TIMEOUT));
                    gatewayContext.setResponse(GatewayResponse.buildGatewayResponse(ResponseCode.REQUEST_TIMEOUT));
                }else{
                    //如果时其他异常，记录必要信息：唯一id，url，响应码
                    gatewayContext.setThrowable(new ConnectException(throwable, gatewayContext.getUniqueId(), url, ResponseCode.HTTP_RESPONSE_ERROR));
                    gatewayContext.setResponse(GatewayResponse.buildGatewayResponse(ResponseCode.HTTP_RESPONSE_ERROR));
                }
            }else{
                //正常的话，往上下文写入response
                gatewayContext.setResponse(GatewayResponse.buildGatewayResponse(response));
            }
        } catch (Throwable t) {
            gatewayContext.setThrowable(new ResponseException(ResponseCode.INTERNAL_ERROR));
            gatewayContext.setResponse(GatewayResponse.buildGatewayResponse(ResponseCode.INTERNAL_ERROR));
            log.error("complete error", t);
        }finally {
            gatewayContext.writtened();
            ResponseHelper.writeResponse(gatewayContext);
        }
    }
//    @Override
//    public int getOrder() {
//        return Filter.super.getOrder();
//    }
}
