package org.paul.core.request;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.QueryStringDecoder;
import lombok.Getter;
import org.asynchttpclient.Request;
import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.cookie.Cookie;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

public class GatewayRequest implements IGatewayRequest{
    /**
     * 服务唯一id
     */
    @Getter
    private final String uniqueId;

    /**
     * 进入网关的开始时间
     */
    @Getter
    private final  long beginTime;

    /**
     * 进入网关的结束时间
     */
    @Getter
    private final long endTime;

    /**
     * 请求的charset字符集
     */
    @Getter
    private final Charset charset;

    /**
     * 客户端的ip
     */
    @Getter
    private final String clientIp;

    /**
     * 目标服务器的主机名
     */
    @Getter
    private final String host;

    /**
     * 服务端请求路径，http://127.0.0.1:8080/api-gatewai/,ip后面的那段
     */
    @Getter
    private final String path;

    /**
     * 统一资源标识符 /XXX/YYY?a=1&b=2
     */
    @Getter
    private final String uri;

    /**
     * 请求方式 post/get/put/delete
     */
    @Getter
    private final HttpMethod httpMethod;

    /**
     * 请求内容格式
     */
    @Getter
    private final HttpMethod contentType;

    /**
     * 请求头
     */
    @Getter
    private final HttpHeaders httpHeaders;

    /**
     * 参数解析器，用于解析uri中的参数
     */
    @Getter
    private final QueryStringDecoder queryStringDecoder;

    /**
     * fullhttpRequest是否完整的请求
     */
    @Getter
    private final FullHttpRequest fullHttpRequest;

    /**
     * 请求体，post
     */
    private String body;

    /**
     * 不同服务的cookie
     */
    private Map<String, Cookie> cookieMap;

    /**
     * post请求参数
     */
    private Map<String, List<String>> postParams;

    /**
     * 可修改的Scheme，默认http
     */
    private String modifyScheme;

    /**
     * 可修改的主机名host
     */
    private String modifyHost;

    /**
     * 可修改的路径path
     */
    private String modifyPath;

    /**
     * 构造下游请求时的http请求构建器
     */
    @Getter
    private final RequestBuilder requestBuilder;

    public GatewayRequest(String uniqueId,
                          long beginTime,
                          long endTime,
                          Charset charset,
                          String clientIp,
                          String host,
                          String path,
                          String uri,
                          HttpMethod httpMethod,
                          HttpMethod contentType,
                          HttpHeaders httpHeaders,
                          QueryStringDecoder queryStringDecoder,
                          FullHttpRequest fullHttpRequest,
                          RequestBuilder requestBuilder) {
        this.uniqueId = uniqueId;
        this.beginTime = beginTime;
        this.endTime = endTime;
        this.charset = charset;
        this.clientIp = clientIp;
        this.host = host;
        this.path = path;
        this.uri = uri;
        this.httpMethod = httpMethod;
        this.contentType = contentType;
        this.httpHeaders = httpHeaders;
        this.queryStringDecoder = queryStringDecoder;
        this.fullHttpRequest = fullHttpRequest;
        this.requestBuilder = requestBuilder;
    }

    @Override
    public void setModifyHost(String host) {

    }

    @Override
    public String getModifyHost() {
        return null;
    }

    @Override
    public void setModifyPath(String path) {

    }

    @Override
    public String getModifyPath() {
        return null;
    }

    @Override
    public void addHeader(CharSequence key, String value) {

    }

    @Override
    public void setHeader(CharSequence key, String value) {

    }

    @Override
    public void addQueryParam(CharSequence key, String value) {

    }

    @Override
    public void addFormParam(CharSequence key, String value) {

    }

    @Override
    public void addOrReplaceCookie(Cookie cookie) {

    }

    @Override
    public void setRequestTimeout(int requestTimeout) {

    }

    @Override
    public void getFinalUrl() {

    }

    @Override
    public Request build() {
        return null;
    }
}
