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
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class RoundRobinLoadBalanceRule implements IGatewayLoadBalanceRule{
    //当前轮询到的位置
    private AtomicInteger position = new AtomicInteger(1);

    private final String serviceId;
    private Set<ServiceInstance> serviceInstanceSet;

    private RoundRobinLoadBalanceRule( String serviceId) {
        this.serviceId = serviceId;
    }

    private static ConcurrentHashMap<String,RoundRobinLoadBalanceRule> serviceMap = new ConcurrentHashMap<>();

    public static RoundRobinLoadBalanceRule getInstance(String serviceId){
        RoundRobinLoadBalanceRule loadBalanceRule = serviceMap.get(serviceId);
        if(loadBalanceRule == null){
            loadBalanceRule = new RoundRobinLoadBalanceRule(serviceId);
            serviceMap.put(serviceId,loadBalanceRule);
        }
        return loadBalanceRule;
    }

    @Override
    public ServiceInstance choose(GatewayContext ctx) {
        return choose(ctx.getUniqueId(), ctx.isGray());
    }

    @Override
    public ServiceInstance choose(String serviceId, boolean gray) {
        //可能存在延迟加载，因为是每秒去轮询获取注册中的实例，所以这里再次加载
        serviceInstanceSet = DynamicConfigManager.getInstance().getServiceInstanceByUniqueId(serviceId, gray);

        if(serviceInstanceSet.isEmpty()){
            //注册中心真的没有对应的service实例
            log.warn("No instance available for: {}", serviceId);
            throw new NotFoundException(ResponseCode.SERVICE_INSTANCE_NOT_FOUND);
        }
        List<ServiceInstance> instances = new ArrayList<ServiceInstance>(serviceInstanceSet);
        if(instances.isEmpty()){
            return null;
        }else {
            int pos = Math.abs(this.position.incrementAndGet());
            return instances.get(pos % instances.size());
        }
    }
}
