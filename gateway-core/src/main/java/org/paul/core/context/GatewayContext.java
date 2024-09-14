package org.paul.core.context;

import io.netty.channel.ChannelHandlerContext;
import org.paul.core.reponse.GatewayResponse;
import org.paul.core.request.GatewayRequest;

import javax.naming.Context;

/**
 * @PROJECT_NAME: api-gateway
 * @DESCRIPTION: 核心上下文基础类
 */
public class GatewayContext extends BaseContext{
    // 请求体
    public GatewayRequest request;

    // 响应体
    public GatewayResponse response;

    // 规则
    public Rule rule;

    public GatewayContext(String protocol, boolean keepAlive, ChannelHandlerContext nettyCtx) {
        super(protocol, keepAlive, nettyCtx);
    }
}
