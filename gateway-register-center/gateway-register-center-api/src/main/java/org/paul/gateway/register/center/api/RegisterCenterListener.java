package org.paul.gateway.register.center.api;

import org.paul.common.config.ServiceDefinition;
import org.paul.common.config.ServiceInstance;

import java.util.Set;

public interface RegisterCenterListener {
    // 回调方法， 参数：服务定义，服务实例的列表
    void onChange(ServiceDefinition serviceDefinition,
                  Set<ServiceInstance> serviceInstanceSet);
}
