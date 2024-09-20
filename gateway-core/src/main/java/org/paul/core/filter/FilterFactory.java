package org.paul.core.filter;

import org.paul.core.context.GatewayContext;

/**
 * 过滤器工厂
 */
public interface FilterFactory {
    /**
     * 构造过滤器链条
     * @param ctx
     * @return
     * @throws Exception
     */
    GatewayFilterChain buildFilterChain(GatewayContext ctx) throws Exception;

    /**
     * 根据过滤器id获取过滤器
     * @param filterId
     * @param <T>
     * @return
     * @throws Exception
     */
    <T> T getFilterInfo(String filterId) throws Exception;
}
