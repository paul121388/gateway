package org.paul.gateway.client.support;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.paul.common.config.ServiceDefinition;
import org.paul.common.config.ServiceInstance;
import org.paul.gateway.client.core.ApiProperties;
import org.paul.gateway.register.center.api.RegisterCenter;

import java.util.ServiceLoader;

// 用于支持不同的协议
@Slf4j
public abstract class AbstractClientRegisterManager {
    // 公共的属性封装在抽象类种

    //封装了注册中心地址registerAddress和环境env
    @Getter
    private ApiProperties apiProperties;

    // 注册中心的客户端，为一个接口，具体实现的由jdk的spi机制载入
    private RegisterCenter registerCenter;

    // 构造方法protected，本类及子类使用
    protected AbstractClientRegisterManager(ApiProperties apiProperties) {
        this.apiProperties = apiProperties;

        // 初始化注册中心对象，因为使用了java的spi，所以需要添加配置，
        // 在resource中添加META_INF文件夹，添加service目录
        // 里面添加文件，文件名为api的名字:org.paul.gateway.register.center.api.RegisterCenter
        // 文件内容为具体实现org.paul.register.center.nacos.NacosRegisterCenter
        ServiceLoader<RegisterCenter> serviceLoader = ServiceLoader.load(RegisterCenter.class);
        // 通过serviceLoader拿到具体实现
        registerCenter = serviceLoader.findFirst().orElseThrow(()->{
            // 拿不到，抛出异常
            log.error("not found registerCenter impl");
            throw new RuntimeException("not found registerCenter impl");
        });
        registerCenter.init(apiProperties.getRegisterAddress(), apiProperties.getEnv());
    }

    // 提供注册方法，参数：服务定义，服务实例
    protected void register(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance){
        //直接调用注册中心的方法
        registerCenter.register(serviceDefinition, serviceInstance);
    }

}
