package org.paul.core.request;

import org.asynchttpclient.Request;
import org.asynchttpclient.cookie.Cookie;

/**
 * @PROJECT_NAME: api-gateway
 * @DESCRIPTION: 提供可修改的Request参数操作接口
 */
public interface IGatewayRequest {
    /**
     * 修改目标服务地址，将host替换为真正的ip地址
     */
    void setModifyHost(String host);

    /**
     * 获取目标服务地址
     */
    String getModifyHost();

    /**
     * 修改目标服务路径
     *
     * @param path
     */
    void setModifyPath(String path);

    /**
     * 获取目标服务路径
     */
    String getModifyPath();

    /**
     * 添加Header
     */
    void addHeader(CharSequence key, String value);

    /**
     * 设置Header信息
     */
    void setHeader(CharSequence key, String value);

    /**
     * 添加get请求参数
     */
    void addQueryParam(CharSequence key, String value);

    /**
     * 添加post请求参数
     */
    void addFormParam(CharSequence key, String value);

    /**
     * 请求下游服务，添加cookie
     */
    void addOrReplaceCookie(Cookie cookie);

    /**
     * 设置超时时间
     */
    void setRequestTimeout(int requestTimeout);

    /**
     * 获取最终请求路径，包含请求参数 http://127.0.0.1:8080/test?name=paul
     */
    void getFinalUrl();

    /**
     * 构建最终的请求对象
     */
    Request build();

}
