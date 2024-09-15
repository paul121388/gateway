package org.paul.core.netty;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import org.paul.core.context.HttpRequestWrapper;

public class NettyHttpServerHandler extends ChannelInboundHandlerAdapter {
    // netty封装好的适配器

    // 定义final属性 NettyProcessor
    private final NettyProcessor nettyProcessor;
    // 构造
    public NettyHttpServerHandler(NettyProcessor nettyProcessor){
        this.nettyProcessor = nettyProcessor;
    }

    // 重写了 channelRead 方法，该方法会在 Netty 读取到网络数据时被调用。
    // ChannelHandlerContext 参数用于与 Netty 的 ChannelPipeline 进行交互，而 Object msg 参数则是读取到的数据
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 对消息进去强转，转为httpRequest，因为NettyHttpServer中HttpObjectAggregator前面对http报文做了聚合
        // 当 msg 被强制转换为 FullHttpRequest 类型时，这表明 HttpObjectAggregator 已经完成了它的任务，
        // 并且 msg 现在包含了整个 HTTP 请求的所有信息，包括请求行、请求头和请求体。
        // 这样，NettyHttpServerHandler 就可以处理这个完整的 HTTP 请求，而无需担心请求数据的碎片化问题
        FullHttpRequest request = (FullHttpRequest) msg;

        // 定义包装器，包装request和context的数据
        // new 包装器，设置对应的属性
        HttpRequestWrapper httpRequestWrapper = new HttpRequestWrapper();
        httpRequestWrapper.setRequest(request);
        httpRequestWrapper.setCtx(ctx);

        // 将核心逻辑委托给另一个对象
        nettyProcessor.process(httpRequestWrapper);
    }
}
