package org.paul.core.filter.loadbalance;

import lombok.extern.slf4j.Slf4j;
import org.paul.common.config.DynamicConfigManager;
import org.paul.common.config.ServiceInstance;
import org.paul.common.enums.ResponseCode;
import org.paul.common.exception.NotFoundException;
import org.paul.core.context.GatewayContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
public class RandomLoadBalanceRule implements IGatewayLoadBalanceRule {
    private final String serviceId;
    private Set<ServiceInstance> serviceInstanceSet;

    public RandomLoadBalanceRule(String serviceId) {
        this.serviceId = serviceId;
    }

    private static ConcurrentHashMap<String, RandomLoadBalanceRule> serviceMap = new ConcurrentHashMap<>();

    public static RandomLoadBalanceRule getInstance(String serviceId) {
        RandomLoadBalanceRule loadBalanceRule = serviceMap.get(serviceId);
        if (loadBalanceRule == null) {
            loadBalanceRule = new RandomLoadBalanceRule(serviceId);
            serviceMap.put(serviceId, loadBalanceRule);
        }
        return loadBalanceRule;
    }

    @Override
    public ServiceInstance choose(GatewayContext ctx) {
        String serviceId = ctx.getUniqueId();
        return choose(serviceId, ctx.isGray());
    }

    @Override
    public ServiceInstance choose(String serviceId, boolean gray) {
        //可能存在延迟加载，因为是每秒去轮询获取注册中的实例，所以这里再次加载
        serviceInstanceSet = DynamicConfigManager.getInstance().getServiceInstanceByUniqueId(serviceId, gray);

        if (serviceInstanceSet.isEmpty()) {
            //注册中心真的没有对应的service实例
            log.warn("No instance available for: {}", serviceId);
            throw new NotFoundException(ResponseCode.SERVICE_INSTANCE_NOT_FOUND);
        }
        List<ServiceInstance> instances = new ArrayList<ServiceInstance>(serviceInstanceSet);

        //在所有service Instance中随机选择一个
        int index = ThreadLocalRandom.current().nextInt(instances.size());
        ServiceInstance serviceInstance = (ServiceInstance) instances.get(index);
        return serviceInstance;
    }
}
