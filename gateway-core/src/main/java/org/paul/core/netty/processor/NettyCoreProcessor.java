package org.paul.core.netty.processor;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;
import org.paul.common.enums.ResponseCode;
import org.paul.common.exception.BaseException;
import org.paul.common.exception.ConnectException;
import org.paul.common.exception.ResponseException;
import org.paul.core.ConfigLoader;
import org.paul.core.context.GatewayContext;
import org.paul.core.context.HttpRequestWrapper;
import org.paul.core.helper.AsyncHttpHelper;
import org.paul.core.helper.RequestHelper;
import org.paul.core.helper.ResponseHelper;
import org.paul.core.response.GatewayResponse;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

@Slf4j
public class NettyCoreProcessor implements NettyProcessor {
    @Override
    public void process(HttpRequestWrapper httpRequestWrapper) {

        // 从httpRequestWrapper中获取参数：完整的httpRequest和context
        FullHttpRequest request = httpRequestWrapper.getRequest();
        ChannelHandlerContext ctx = httpRequestWrapper.getCtx();

        try {
            // request转换为内部GatewayContext对象
            GatewayContext gatewayContext = RequestHelper.doContext(request, ctx);
            // 路由转发
            route(gatewayContext);
        }// catch已知异常
        catch (BaseException e){
            // 日志，code和message
            log.error("process error{} {}", e.getCode(), e.getMessage());
            // 根据辅助类，获取响应结果
            FullHttpResponse httpResponse = ResponseHelper.getHttpResponse(e.getCode());
            // 写入并释放doWriteAndRelease
            doWriteAndRelease(ctx, request, httpResponse);
        }// catch处理未知异常
        catch (Throwable t){
            // 打印日志
            log.error("process unknown error", t);
            // 获取响应结果，指定code
            FullHttpResponse httpResponse = ResponseHelper.getHttpResponse(ResponseCode.INTERNAL_ERROR);
            // 写入并释放doWriteAndRelease
            doWriteAndRelease(ctx, request, httpResponse);
        }
    }

    /**
     * doWriteAndRelease
     *  写入response，添加监听器，由监听器关闭channel
     *  调用ReferenceCountUtil，释放request中的缓冲
     */
    private void doWriteAndRelease(ChannelHandlerContext ctx, FullHttpRequest request, FullHttpResponse response) {
        ctx.writeAndFlush(request)
                .addListener(ChannelFutureListener.CLOSE);
        ReferenceCountUtil.release(request);
    }

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
    private void route(GatewayContext gatewayContext){
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
     *      释放资源
     *      try
     *          判断是否由异常
     *          获取url
     *              对异常进行处理
     *                  超时异常：打印日志（url），设置gatewayContext的throwable，记录异常code
     *                  如果时其他异常，记录必要信息：唯一id，url，响应码
     *          没有异常
     *              正常响应
     *       catch
     *          在gatewayContext中记录信息，打印日志
     *       finally
     *          修改gatewayContext的状态
     *          调用辅助类responseHelper，写回数据
     */
    void complete(Request request, Response response, Throwable throwable, GatewayContext gatewayContext){
        gatewayContext.releaseRequest();

        try {
            if(Objects.nonNull(throwable)){
                String url = request.getUrl();
                if(throwable instanceof TimeoutException){
                    log.warn("complete time out {}", url);
                    gatewayContext.setThrowable(new ResponseException(ResponseCode.REQUEST_TIMEOUT));
                }else{
                    gatewayContext.setThrowable(new ConnectException(throwable, gatewayContext.getUniqueId(), url, ResponseCode.HTTP_RESPONSE_ERROR));
                }
            }else{
                gatewayContext.setResponse(GatewayResponse.buildGatewayResponse(response));
            }
        } catch (Throwable t) {
            gatewayContext.setThrowable(new ResponseException(ResponseCode.INTERNAL_ERROR));
            log.error("complete error", t);
        }finally {
            gatewayContext.writtened();
            ResponseHelper.writeResponse(gatewayContext);
        }
    }
}
