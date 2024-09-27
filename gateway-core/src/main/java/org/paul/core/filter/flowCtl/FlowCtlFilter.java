package org.paul.core.filter.flowCtl;

import org.paul.core.context.GatewayContext;
import org.paul.core.filter.Filter;

/**
 * 限流过滤器，一般的算法实现有漏桶算法和令牌桶
 */
public class FlowCtlFilter implements Filter {
    @Override
    public void doFilter(GatewayContext ctx) throws Exception {

    }
}
