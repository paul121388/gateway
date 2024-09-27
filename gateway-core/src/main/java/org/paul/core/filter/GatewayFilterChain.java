package org.paul.core.filter;

import lombok.extern.slf4j.Slf4j;
import org.paul.core.context.GatewayContext;

import java.util.ArrayList;
import java.util.List;

/**
 * 过滤器链条类
 */
@Slf4j
public class GatewayFilterChain {
    /**
     * 存放过滤器的list
     */
    private List<Filter> filters = new ArrayList<>();

    /**
     * 将过滤器添加到过滤器链条种
     *
     * @param filter
     * @return
     */
    public GatewayFilterChain addFilte(Filter filter) {
        filters.add(filter);
        return this;
    }

    public GatewayFilterChain addFilterList(List<Filter> tempFilterList){
        filters.addAll(tempFilterList);
        return this;
    }

    /**
     * 过滤器链条执行
     *
     * @param ctx
     * @return
     */
    public GatewayContext doFilter(GatewayContext ctx) throws Exception{
        // 过滤器链条为空，不执行过滤动作，直接返回
        if (filters.isEmpty()) {
            return ctx;
        }
        try {
            for (Filter filter : filters) {
                //遍历过滤器，每个过滤器执行对应的过滤动作
                filter.doFilter(ctx);
            }
        } catch (Exception e) {
            log.error("执行过滤器发生异常，异常信息：{}", e.getMessage());
            throw e;
        }
        return ctx;
    }

}
