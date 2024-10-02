package org.paul.core.netty.processor;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.paul.common.enums.ResponseCode;
import org.paul.common.exception.BaseException;
import org.paul.core.context.GatewayContext;
import org.paul.core.context.HttpRequestWrapper;
import org.paul.core.filter.FilterFactory;
import org.paul.core.filter.GatewayFilterChainFactory;
import org.paul.core.helper.RequestHelper;
import org.paul.core.helper.ResponseHelper;

/**
 * 定义接口
 * 最小可用的版本
 * 路由函数实现
 * 获取异步配置，实现complete犯法
 * 异常处理
 * 写回响应信息并释放资源
 */
@Slf4j
public class NettyCoreProcessor implements NettyProcessor {

    //拿到工厂类
    private FilterFactory filterFactory = GatewayFilterChainFactory.getInstance();

    @Override
    public void process(HttpRequestWrapper httpRequestWrapper) {

        // 从httpRequestWrapper中获取参数：完整的httpRequest和context
        FullHttpRequest request = httpRequestWrapper.getRequest();
        ChannelHandlerContext ctx = httpRequestWrapper.getCtx();

        try {
            // request转换为内部GatewayContext对象
            GatewayContext gatewayContext = RequestHelper.doContext(request, ctx);

            //执行过滤器逻辑
            filterFactory.buildFilterChain(gatewayContext).doFilter(gatewayContext);

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

    @Override
    public void start() {

    }

    @Override
    public void shutDown() {

    }

    /**
     * doWriteAndRelease
     *  写入response，添加监听器，由监听器关闭channel
     *  调用ReferenceCountUtil，释放request中的缓冲
     */
    private void doWriteAndRelease(ChannelHandlerContext ctx, FullHttpRequest request, FullHttpResponse response) {
        ctx.writeAndFlush(response)
                //关闭channel
                .addListener(ChannelFutureListener.CLOSE);
        //释放request
        ReferenceCountUtil.release(request);
    }
}
