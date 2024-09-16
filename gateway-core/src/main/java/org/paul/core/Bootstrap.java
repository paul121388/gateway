package org.paul.core;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.paul.common.config.DynamicConfigManager;
import org.paul.common.config.ServiceDefinition;
import org.paul.common.config.ServiceInstance;
import org.paul.common.utils.NetUtils;
import org.paul.common.utils.TimeUtil;
import org.paul.gateway.register.center.api.RegisterCenter;
import org.paul.gateway.register.center.api.RegisterCenterListener;

import java.util.Map;
import java.util.Set;

import static org.paul.common.constants.BasicConst.COLON_SEPARATOR;

/**
 * API网关启动类
 */
@Slf4j
public class Bootstrap {
    public static void main(String[] args) {
        //加载网关核心静态配置
        Config config = ConfigLoader.getInstance().load(args);
        System.out.println(config.getPort());

        //插件初始化
        //配置中心管理器初始化，连接配置中心，监听配置的新增、修改、删除
        //启动容器
        Container container = new Container(config);
        container.start();

        final RegisterCenter registerCenter = registerAndSubscribe(config);

        //服务优雅关机，收到kill信号时调用
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                registerCenter.deregister(buildGatewayServiceDefinition(config), buildGatewayServiceInstance(config));
            }
        });
    }

    private static RegisterCenter registerAndSubscribe(Config config) {
        //连接注册中心，将注册中心的实例加载到本地
        final RegisterCenter registerCenter = null;
        // 构造网关的服务定义和服务实例
        ServiceDefinition serviceDefinition = buildGatewayServiceDefinition(config);
        ServiceInstance serviceInstance = buildGatewayServiceInstance(config);

        // 注册
        registerCenter.register(serviceDefinition, serviceInstance);

        // 订阅
        registerCenter.subscribeAllServices(new RegisterCenterListener() {
            @Override
            public void onChange(ServiceDefinition serviceDefinition, Set<ServiceInstance> serviceInstanceSet) {
                log.info("refresh service and instance:{} {}", serviceInstance.getUniqueId(), JSON.toJSON(serviceInstanceSet));
                // 更新服务定义和服务缓存
                DynamicConfigManager manager = DynamicConfigManager.getInstance();
                manager.addServiceInstance(serviceInstance.getUniqueId(), serviceInstanceSet);
            }
        });
        return registerCenter;
    }

    private static ServiceDefinition buildGatewayServiceDefinition(Config config) {
        ServiceDefinition serviceDefinition = new ServiceDefinition();
        serviceDefinition.setInvokerMap(Map.of());
        serviceDefinition.setUniqueId(config.getApplicationName());
        serviceDefinition.setServiceId(config.getApplicationName());
        serviceDefinition.setEnvType(config.getEnv());
        return serviceDefinition;
    }

    private static ServiceInstance buildGatewayServiceInstance(Config config) {
        String localIp = NetUtils.getLocalIp();
        int port = config.getPort();
        ServiceInstance serviceInstance = new ServiceInstance();
        serviceInstance.setServiceInstanceId(localIp + COLON_SEPARATOR + port);
        serviceInstance.setIp(localIp);
        serviceInstance.setPort(port);
        serviceInstance.setRegisterTime(TimeUtil.currentTimeMillis());
        return serviceInstance;
    }
}