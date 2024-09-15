package org.paul.core.context;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import lombok.Data;

@Data
public class HttpRequestWrapper {
    // 属性：fullHttpRequest
    private FullHttpRequest request;
    // ctx:ChannelHandlerContext
    private ChannelHandlerContext ctx;
}
