package org.paul.gateway.register.center.api;

import org.paul.common.config.ServiceDefinition;
import org.paul.common.config.ServiceInstance;

public interface RegisterCenter {

    /**
     * 初始化方法
     * @param registerAddress
     * @param env
     */
    void init(String registerAddress, String env);


    /**
     * 注册
     * @param serviceDefinition
     * @param serviceInstance
     */
    void register(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance);

    /**
     * 注销
     * @param serviceDefinition
     * @param serviceInstance
     */
    void deregister(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance);

    /**
     * 订阅所有服务变更
     * @param registerCenterListener
     */
    void subscribeAllServices(RegisterCenterListener registerCenterListener);
}
