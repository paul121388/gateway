package org.paul.core.filter.flowCtl;

import org.paul.common.config.Rule;
import org.paul.core.context.GatewayContext;

/**
 * 执行限流的接口
 */
public interface IGatewayFlowCtlRule {
    void doFlowCtlFilter(Rule.FlowCtlConfig flowCtlConfig, String serviceId);
}
