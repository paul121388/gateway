package org.paul.core.filter.loadbalance;

import org.paul.common.config.ServiceInstance;
import org.paul.core.context.GatewayContext;

/**
 * 负载均衡顶级接口
 */
public interface IGatewayLoadBalanceRule {
    /**
     * 根据GatewayContext上下文获取service实例
     * @param ctx
     * @return
     */
    ServiceInstance choose(GatewayContext ctx);

    /**
     * 根据服务id获取service实例
     * @param serviceId
     * @param gray
     * @return
     */
    ServiceInstance choose(String serviceId, boolean gray);
}
