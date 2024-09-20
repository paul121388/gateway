package org.paul.core.helper;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.apache.commons.lang3.StringUtils;
import org.paul.common.config.*;
import org.paul.common.constants.BasicConst;
import org.paul.common.constants.GatewayConst;
import org.paul.common.constants.GatewayProtocol;
import org.paul.common.enums.ResponseCode;
import org.paul.common.exception.ResponseException;
import org.paul.core.context.GatewayContext;
import org.paul.core.request.GatewayRequest;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;


public class RequestHelper {

    public static GatewayContext doContext(FullHttpRequest request, ChannelHandlerContext ctx) {

        //	构建请求对象GatewayRequest
        GatewayRequest gateWayRequest = doRequest(request, ctx);

        //	根据请求对象里的uniqueId，获取资源服务信息(也就是服务定义信息)
        ServiceDefinition serviceDefinition = ServiceDefinition.builder()
                //后台服务的id
                .serviceId(gateWayRequest.getUniqueId())
                .enable(true)
                .version("v1")
                .patternPath("**")
                .envType("dev")
                .protocol(GatewayProtocol.HTTP)
                .build();


        //	根据请求对象获取服务定义对应的方法调用，然后获取对应的规则
        ServiceInvoker serviceInvoker = new HttpServiceInvoker();
        serviceInvoker.setInvokerPath(gateWayRequest.getPath());
        serviceInvoker.setTimeout(500);

        //根据请求对象返回rule
        Rule rule = getRule(gateWayRequest);

        //	构建我们而定GateWayContext对象
        GatewayContext gatewayContext = new GatewayContext(
                serviceDefinition.getProtocol(),
                HttpUtil.isKeepAlive(request),
                ctx,
                gateWayRequest,
                rule);


        //后续服务发现做完，这里都要改成动态的
        gatewayContext.getRequest().setModifyHost("127.0.0.1:8083");

        return gatewayContext;
    }



    /**
     * 构建Request请求对象
     */
    private static GatewayRequest doRequest(FullHttpRequest fullHttpRequest, ChannelHandlerContext ctx) {

        HttpHeaders headers = fullHttpRequest.headers();
        //	从header头获取必须要传入的关键属性 uniqueId
        String uniqueId = headers.get(GatewayConst.UNIQUE_ID);

        String host = headers.get(HttpHeaderNames.HOST);
        HttpMethod method = fullHttpRequest.method();
        String uri = fullHttpRequest.uri();
        String clientIp = getClientIp(ctx, fullHttpRequest);
        String contentType = HttpUtil.getMimeType(fullHttpRequest) == null ? null : HttpUtil.getMimeType(fullHttpRequest).toString();
        Charset charset = HttpUtil.getCharset(fullHttpRequest, StandardCharsets.UTF_8);

        GatewayRequest gatewayRequest = new GatewayRequest(uniqueId,
                charset,
                clientIp,
                host,
                uri,
                method,
                contentType,
                headers,
                fullHttpRequest);

        return gatewayRequest;
    }

    /**
     * 获取客户端ip
     */
    private static String getClientIp(ChannelHandlerContext ctx, FullHttpRequest request) {
        String xForwardedValue = request.headers().get(BasicConst.HTTP_FORWARD_SEPARATOR);

        String clientIp = null;
        if (StringUtils.isNotEmpty(xForwardedValue)) {
            List<String> values = Arrays.asList(xForwardedValue.split(", "));
            if (values.size() >= 1 && StringUtils.isNotBlank(values.get(0))) {
                clientIp = values.get(0);
            }
        }
        if (clientIp == null) {
            InetSocketAddress inetSocketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
            clientIp = inetSocketAddress.getAddress().getHostAddress();
        }
        return clientIp;
    }

    /**
     * 根据请求对象获取rule
     * @param gateWayRequest
     * @return
     */
    private static Rule getRule(GatewayRequest gateWayRequest) {
        //从配置中心获取rule的数据，根据DynamicConfigManager中的rule集合获取
        String key = gateWayRequest.getUniqueId() + "." + gateWayRequest.getPath();
        Rule ruleByPath = DynamicConfigManager.getInstance().getRuleByPath(key);

        //如果有根据path配置rule，直接返回
        if(ruleByPath != null){
            return ruleByPath;
        }

        //否则，根据请求的服务id和path返回
        return DynamicConfigManager.getInstance().getRuleByServiceId(gateWayRequest.getUniqueId())
                .stream().filter(r -> gateWayRequest.getPath().startsWith(r.getPrefix()))
                .findAny().orElseThrow(() -> new ResponseException(ResponseCode.PATH_NO_MATCHED));
    }
}