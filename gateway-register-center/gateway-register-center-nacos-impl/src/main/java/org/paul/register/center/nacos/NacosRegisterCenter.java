package org.paul.register.center.nacos;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingMaintainFactory;
import com.alibaba.nacos.api.naming.NamingMaintainService;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.ServiceInfo;
import com.alibaba.nacos.common.executor.NameThreadFactory;
import com.alibaba.nacos.common.utils.CollectionUtils;
import lombok.extern.slf4j.Slf4j;
import org.paul.common.config.ServiceDefinition;
import org.paul.common.config.ServiceInstance;
import org.paul.common.constants.GatewayConst;
import org.paul.gateway.register.center.api.RegisterCenter;
import org.paul.gateway.register.center.api.RegisterCenterListener;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class NacosRegisterCenter implements RegisterCenter {
    // 封装必要的属性
    // 注册中心的地址
    private String registerAddress;

    // 环境
    private String env;

    // Nacos封装好的两个实例，用于维护服务实例信息和服务定义信息
    private NamingService namingService;
    private NamingMaintainService namingMaintainService;

    // 维护一个list，存放注册中心的监听器
    private List<RegisterCenterListener> registerCenterListenerList;

    @Override
    public void init(String registerAddress, String env) {
        // 首先进行赋值操作
        this.registerAddress = registerAddress;
        this.env = env;

        // 设置两个实例
        try {
            // 工厂模式创建
            this.namingMaintainService = NamingMaintainFactory.createMaintainService(registerAddress);
            this.namingService = NamingFactory.createNamingService(registerAddress);
        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void register(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance) {
        try {
            // 构造Nacos的实例信息
            Instance nacosInstance = new Instance();
            nacosInstance.setInstanceId(serviceInstance.getServiceInstanceId());
            nacosInstance.setIp(serviceInstance.getIp());
            nacosInstance.setPort(serviceInstance.getPort());

            // 设置元信息，将实例的信息序列化后放在里面
            nacosInstance.setMetadata(Map.of(GatewayConst.META_DATA_KEY, JSON.toJSONString(serviceInstance)));

            // 注册，调用nacos的api，传入实例id，环境,nacos的实例
            namingService.registerInstance(serviceDefinition.getServiceId(), env, nacosInstance);

            // 更新服务，将服务的信息放在map中，key为meta，value为序列化的服务定义
            namingMaintainService.updateService(serviceDefinition.getServiceId(), env, 0,
                    Map.of(GatewayConst.META_DATA_KEY, JSON.toJSONString(serviceDefinition)));
            log.info("register {} {}", serviceDefinition, serviceInstance);

        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deregister(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance) {
        try {
            namingService.registerInstance(serviceDefinition.getServiceId(),
                    env, serviceInstance.getIp(), serviceInstance.getPort());
        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void subscribeAllServices(RegisterCenterListener registerCenterListener) {
        // 添加监听器
        registerCenterListenerList.add(registerCenterListener);
        doSubcribeAllService();

        // 如果新加入的服务，我们不知道，为了将这些任务假如，因此需要定时任务，循环执行上述订阅服务的方法
        ScheduledExecutorService scheduledThreadPool = Executors
                .newScheduledThreadPool(1, new NameThreadFactory("doSubcribeAllService"));
        scheduledThreadPool.scheduleWithFixedDelay(()->doSubcribeAllService(), 10, 10, TimeUnit.SECONDS);
    }

    private void doSubcribeAllService(){
        try{
            // 拿到已经够订阅的服务，只需要服务名
            Set<String> subscribedServices = namingService.getSubscribeServices().stream()
                    .map(ServiceInfo::getName).collect(Collectors.toSet());

            // 分页，拿到所有服务
            int pageNo = 1;
            int pageSize = 100;
            // 拿到服务列表
            List<String> serviceList = namingService.getServicesOfServer(pageNo, pageSize, env).getData();

            // 定义eventListener
            EventListener eventListener = new NacosRegisterListener();

            // 服务列表不为空，一致循环
            while (CollectionUtils.isNotEmpty(serviceList)){
                log.info("service list size{}", serviceList.size());

                // 对拿到的列表进行循环
                for(String serviceName : serviceList){
                    // 判断是否订阅过
                    if(subscribedServices.contains(serviceName)){
                        continue;
                    }
                    // 没有定义，就进行订阅，需要service和nacos包中的eventListener，打印日志
                    namingService.subscribe(serviceName, eventListener);
                    log.info("subsribe {} {}", serviceName, env);
                }

                // 获取下一页
                serviceList = namingService.getServicesOfServer(++pageNo, pageSize, env).getData();
            }


        }catch (NacosException e){
            throw new RuntimeException(e);
        }
    }

    public class NacosRegisterListener implements EventListener{

        @Override
        public void onEvent(Event event) {

        }
    }
}
