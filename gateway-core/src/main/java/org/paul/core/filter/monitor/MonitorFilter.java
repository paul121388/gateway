package org.paul.core.filter.monitor;

import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.paul.core.context.GatewayContext;
import org.paul.core.filter.Filter;
import org.paul.core.filter.FilterAspect;

import static org.paul.common.constants.FilterConst.*;

//因为对网关全过程的监控，所以需要对入网关和出网关都需要监控
@Slf4j
@FilterAspect(id = MONITOR_FILTER_ID,
        name = MONITOR_FILTER_NAME,
        order = MONITOR_FILTER_ORDER)
public class MonitorFilter implements Filter {
    @Override
    public void doFilter(GatewayContext ctx) throws Exception {
        //开始采集
        ctx.setTimeSample(Timer.start());
    }
}
