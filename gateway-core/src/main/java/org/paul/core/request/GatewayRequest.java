package org.paul.core.request;

import com.google.common.collect.Lists;
import com.jayway.jsonpath.JsonPath;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.asynchttpclient.Request;
import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.cookie.Cookie;
import org.paul.common.constants.BasicConst;
import org.paul.common.utils.TimeUtil;

import java.nio.charset.Charset;
import java.util.*;

@Slf4j
public class GatewayRequest implements IGatewayRequest {
    /**
     * 服务唯一id
     */
    @Getter
    private final String uniqueId;

    /**
     * 进入网关的开始时间
     */
    @Getter
    private final long beginTime;

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
    private final HttpMethod method;

    /**
     * 请求内容格式
     */
    @Getter
    private final String contentType;

    /**
     * 请求头
     */
    @Getter
    private final HttpHeaders headers;

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
                          long endTime,
                          Charset charset,
                          String clientIp,
                          String host,
                          String path,
                          String uri,
                          HttpMethod method,
                          String contentType,
                          HttpHeaders headers,
                          QueryStringDecoder queryStringDecoder,
                          FullHttpRequest fullHttpRequest,
                          RequestBuilder requestBuilder) {
        this.uniqueId = uniqueId;
        this.beginTime = TimeUtil.currentTimeMillis();
        this.endTime = endTime;
        this.charset = charset;
        this.clientIp = clientIp;
        this.host = host;
        this.path = path;
        this.uri = uri;
        this.contentType = contentType;
        this.method = method;
        this.headers = headers;
        this.queryStringDecoder = new QueryStringDecoder(uri, this.charset);
        this.fullHttpRequest = fullHttpRequest;
        this.requestBuilder = new RequestBuilder();

        this.modifyHost = host;
        this.modifyPath = path;
        this.modifyScheme = BasicConst.HTTP_PREFIX_SEPARATOR;
        this.requestBuilder.setMethod(getMethod().name());
        this.requestBuilder.setHeaders(getHeaders());
        this.requestBuilder.setQueryParams(getQueryStringDecoder().parameters());

        ByteBuf contentBuffer = fullHttpRequest.content();
        if (Objects.nonNull(contentBuffer)) {
            this.requestBuilder.setBody(contentBuffer.toString(this.charset));
        }

    }

    /**
     * 获取请求体
     *
     * @return
     */
    public String getBody() {
        if (StringUtils.isEmpty(body)) {
            body = fullHttpRequest.content().toString(charset);
        }
        return body;
    }

    /**
     * 获取cookie
     *
     * @param name
     * @return
     */
    public Cookie getCookie(String name) {
        if (cookieMap == null) {
            cookieMap = new HashMap<String, Cookie>();
            String cookieStr = headers.get(HttpHeaderNames.COOKIE);
            Set<io.netty.handler.codec.http.cookie.Cookie> cookies = ServerCookieDecoder.STRICT.decode(cookieStr);
            for (io.netty.handler.codec.http.cookie.Cookie cookie : cookies) {
                cookieMap.put(cookie.name(), (Cookie) cookie);
            }
        }
        return cookieMap.get(name);
    }

    /**
     * 获取指定名字的参数值
     *
     * @param name
     * @return
     */
    public List<String> getQueryParamsMultiple(String name) {
        return queryStringDecoder.parameters().get(name);
    }

    /**
     * 获取post参数值
     * @param name
     * @return
     */
    public List<String> getPostParamsMultiple(String name) {
        String body = getBody();
        if (isFormPost()) {
            if (postParams == null) {
                QueryStringDecoder queryStringDecoder = new QueryStringDecoder(body, charset);
                postParams = queryStringDecoder.parameters();
            }

            if (postParams == null || !postParams.isEmpty()) {
                return null;
            } else {
                return postParams.get(name);
            }
        } else if (isJsonPost()) {
//            try {
                return Lists.newArrayList(JsonPath.read(body, name).toString());
//            } catch (Exception e) {
//                log.error("JsonPath解析失败，JsonPath:{},Body:{},", name, body, e);
//            }
        }
        return null;
    }

    /**
     * 是否是表单提交
     * @return
     */
    public boolean isFormPost() {
        return HttpMethod.POST.equals(method) &&
                (contentType.startsWith(HttpHeaderValues.FORM_DATA.toString())
                || contentType.startsWith(HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.toString()));
    }

    /**
     * 是否是json提交
     * @return
     */
    public boolean isJsonPost() {
        return HttpMethod.POST.equals(method) &&
                contentType.startsWith(HttpHeaderValues.APPLICATION_JSON.toString());
    }

    @Override
    public void setModifyHost(String host) {
        this.modifyHost = host;
    }

    @Override
    public String getModifyHost() {
        return modifyHost;
    }

    @Override
    public void setModifyPath(String path) {
        this.modifyPath = path;
    }

    @Override
    public String getModifyPath() {
        return path;
    }

    @Override
    public void addHeader(CharSequence name, String value) {
        requestBuilder.addHeader(name, value);
    }

    @Override
    public void setHeader(CharSequence name, String value) {
        requestBuilder.setHeader(name, value);
    }

    @Override
    public void addQueryParam(String name, String value) {
        requestBuilder.addQueryParam(name, value);
    }

    @Override
    public void addFormParam(String name, String value) {
        if(isFormPost()){
            requestBuilder.addFormParam(name, value);
        }
    }

    @Override
    public void addOrReplaceCookie(Cookie cookie) {
        requestBuilder.addOrReplaceCookie(cookie);
    }

    @Override
    public void setRequestTimeout(int requestTimeout) {
        requestBuilder.setRequestTimeout(requestTimeout);
    }

    @Override
    public String getFinalUrl() {
        return modifyScheme + modifyHost + modifyPath;
    }

    @Override
    public Request build() {
        requestBuilder.setUrl(getFinalUrl());
        return requestBuilder.build();
    }
}
