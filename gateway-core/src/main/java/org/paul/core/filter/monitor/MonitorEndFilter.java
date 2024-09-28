package org.paul.core.filter.monitor;

import lombok.extern.slf4j.Slf4j;
import org.paul.core.context.GatewayContext;
import org.paul.core.filter.Filter;
import org.paul.core.filter.FilterAspect;

import static org.paul.common.constants.FilterConst.*;

//请求结束的时候走到这里
@Slf4j
@FilterAspect(id = MONITOR_END_FILTER_ID,
        name = MONITOR_END_FILTER_NAME,
        order = MONITOR_END_FILTER_ORDER)
public class MonitorEndFilter implements Filter {
    @Override
    public void doFilter(GatewayContext ctx) throws Exception {

    }
}
