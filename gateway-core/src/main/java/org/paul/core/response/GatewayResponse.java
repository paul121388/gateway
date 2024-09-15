package org.paul.core.response;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.handler.codec.http.*;
import lombok.Data;
import org.asynchttpclient.Response;
import org.paul.common.enums.ResponseCode;
import org.paul.common.utils.JSONUtil;

/**
 * @author paul
 * @date 2019/8/3
 * @description 网关回复消息对象
 */
@Data
public class GatewayResponse {
    /**
     * 响应头
     */
    private HttpHeaders responseHeaders = new DefaultHttpHeaders();

    /**
     * 额外的响应头信息
     */
    private HttpHeaders extraResponseHeaders = new DefaultHttpHeaders();

    /**
     * 响应体
     */
    private String content;

    /**
     * 返回状态响应码
     */
    private HttpResponseStatus httpResponseStatus;

    /**
     * 异步返回对象
     */
    private Response futureResponse;

    public GatewayResponse() {}

    /**
     * 设置响应头信息
     */
    public void putHeader(CharSequence key, CharSequence value) {
        responseHeaders.add(key, value);
    }

    /**
     * 构建异步响应对象
     * @param futureResponse
     * @return
     */
    public static GatewayResponse buildGatewayResponse(Response futureResponse) {
        GatewayResponse gatewayResponse = new GatewayResponse();
        gatewayResponse.setFutureResponse(futureResponse);
        gatewayResponse.setHttpResponseStatus(HttpResponseStatus.valueOf(futureResponse.getStatusCode()));
        return gatewayResponse;
    }

    /**
     * 返回Json类型的响应信息，失败时使用
     * @param code args
     * @return
     */
    public static GatewayResponse buildGatewayResponse(ResponseCode code, Object... args){
        ObjectNode objectNode = JSONUtil.createObjectNode();
        objectNode.put(JSONUtil.STATUS, code.getStatus().code());
        objectNode.put(JSONUtil.CODE, code.getCode());
        objectNode.put(JSONUtil.MESSAGE, code.getMessage());


        GatewayResponse gatewayResponse = new GatewayResponse();

        gatewayResponse.setHttpResponseStatus(code.getStatus());
        gatewayResponse.putHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON + ";charset=UTF-8");
        gatewayResponse.setContent(JSONUtil.toJSONString(objectNode));

        return gatewayResponse;
    }

    /**
     * 返回Json类型的响应信息，成功时使用
     * @param data
     * @return
     */
    public static GatewayResponse buildGatewayResponse(Object data){
        ObjectNode objectNode = JSONUtil.createObjectNode();
        objectNode.put(JSONUtil.STATUS, ResponseCode.SUCCESS.getStatus().code());
        objectNode.put(JSONUtil.CODE, ResponseCode.SUCCESS.getCode());
        objectNode.putPOJO(JSONUtil.DATA, data);


        GatewayResponse gatewayResponse = new GatewayResponse();

        gatewayResponse.setHttpResponseStatus(ResponseCode.SUCCESS.getStatus());
        gatewayResponse.putHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON + ";charset=UTF-8");
        gatewayResponse.setContent(JSONUtil.toJSONString(objectNode));

        return gatewayResponse;
    }
}
