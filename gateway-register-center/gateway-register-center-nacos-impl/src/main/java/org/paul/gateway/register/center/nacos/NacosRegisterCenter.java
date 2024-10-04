package org.paul.gateway.register.center.nacos;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingMaintainFactory;
import com.alibaba.nacos.api.naming.NamingMaintainService;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.Service;
import com.alibaba.nacos.api.naming.pojo.ServiceInfo;
import com.alibaba.nacos.common.executor.NameThreadFactory;
import com.alibaba.nacos.common.utils.CollectionUtils;
import lombok.extern.slf4j.Slf4j;
import org.paul.common.config.ServiceDefinition;
import org.paul.common.config.ServiceInstance;
import org.paul.common.constants.GatewayConst;
import org.paul.gateway.register.center.api.RegisterCenter;
import org.paul.gateway.register.center.api.RegisterCenterListener;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
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

    // Nacos封装好的两个实例，用于维护服务实例信息
    private NamingService namingService;
    //服务定义信息
    private NamingMaintainService namingMaintainService;

    // 维护一个list，存放注册中心的监听器
    private List<RegisterCenterListener> registerCenterListenerList= new CopyOnWriteArrayList<>();

    @Override
    public void init(String registerAddress, String env) {
        // 首先进行赋值操作
        this.registerAddress = registerAddress;
        this.env = env;

        // 设置两个维护服务实例信息和服务定义信息的实例
        try {
            // 工厂模式创建，参数为注册中心的地址
            this.namingMaintainService = NamingMaintainFactory.createMaintainService(registerAddress);
            this.namingService = NamingFactory.createNamingService(registerAddress);
        } catch (NacosException e) {
            log.info("create failed");
            throw new RuntimeException(e);
        }
    }

    @Override
    public void register(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance) {
        try {
            // 构造服务在Nacos中的信息
            Instance nacosInstance = new Instance();
            nacosInstance.setInstanceId(serviceInstance.getServiceInstanceId());
            nacosInstance.setIp(serviceInstance.getIp());
            nacosInstance.setPort(serviceInstance.getPort());

            // 设置元信息，将实例的信息序列化后放在里面
            nacosInstance.setMetadata(Map.of(GatewayConst.META_DATA_KEY, JSON.toJSONString(serviceInstance)));

            // 注册，调用nacos的api，传入实例id，环境,服务在Nacos中的信息
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
            namingService.deregisterInstance(serviceDefinition.getServiceId(),
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

        // 如果新加入的服务，我们不知道，为了将这些任务加入，因此需要定时任务，循环执行上述订阅服务的方法
        ScheduledExecutorService scheduledThreadPool = Executors
                .newScheduledThreadPool(1, new NameThreadFactory("doSubcribeAllService"));
        scheduledThreadPool.scheduleWithFixedDelay(()->doSubcribeAllService(), 10, 10, TimeUnit.SECONDS);
    }

    private void doSubcribeAllService(){
        try{
            // 拿到已经够订阅的服务，只需要服务名，
            // getSubscribeServices方法返回客户端当前主动订阅的服务列表，也就是那些客户端关心并实时跟踪状态变化的服务。
            Set<String> subscribedServices = namingService.getSubscribeServices().stream()
                    .map(ServiceInfo::getName).collect(Collectors.toSet());

            // 分页，拿到所有服务
            int pageNo = 1;
            int pageSize = 100;
            // 拿到服务列表，
            // getServicesOfServer：这个方法返回的是当前 Nacos 服务器 上注册的所有服务，可能包含客户端未订阅的服务。
            // 该方法一般用于全局性地查询服务注册情况，并通过分页返回，以避免一次性加载太多服务列表。
            List<String> serviceList = namingService.getServicesOfServer(pageNo, pageSize, env).getData();



            // 服务列表不为空，一致循环
            while (CollectionUtils.isNotEmpty(serviceList)){
                log.info("service list size{}", serviceList.size());

                // 对拿到的列表进行循环
                for(String serviceName : serviceList){
                    // 判断是否订阅过
                    if(subscribedServices.contains(serviceName)){
                        continue;
                    }
                    // 定义eventListener，服务变更的事件监听器。当服务的实例状态变化时，listener 会触发并执行相应的回调
                    EventListener eventListener = new NacosRegisterListener();
                    // 没有定义，就进行订阅，需要service和nacos包中的eventListener，打印日志
                    eventListener.onEvent(new NamingEvent(serviceName, null));
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


    // 监听到事件后，做回调处理
    public class NacosRegisterListener implements EventListener{

        @Override
        public void onEvent(Event event) {
            // 首先判断event的类型，如果时注册中心的event
            if(event instanceof NamingEvent){
                // 强转event
                NamingEvent namingEvent = (NamingEvent) event;
                // 获取服务名称
                String serviceName = namingEvent.getServiceName();

                try{
                    // 获取服务定义信息，通过nacos的api获取，传入服务名和环境
                    Service service = namingMaintainService.queryService(serviceName, env);

                    // 获取元信息，即JSON数据
                    String s = service.getMetadata().get(GatewayConst.META_DATA_KEY);
                    // 反序列化，获得自己写的服务定义
                    ServiceDefinition serviceDefinition = JSON.parseObject(s, ServiceDefinition.class);

                    // 通过nacos的api获取服务实例信息
                    List<Instance> allInstances = namingService.getAllInstances(serviceName, env);
                    // new一个set，接收上述信息
                    Set<ServiceInstance> set = new HashSet<>();
                    // 循环进行反序列化
                    for(Instance instance: allInstances){
                        ServiceInstance serviceInstance = JSON.parseObject(instance.getMetadata().get(GatewayConst.META_DATA_KEY), ServiceInstance.class);
                        set.add(serviceInstance);
                    }

                    // 调用监听器
                    registerCenterListenerList.stream()
                            .forEach(l -> l.onChange(serviceDefinition, set));
                }catch (NacosException e){
                    throw new RuntimeException(e);
                }

            }

        }
    }
}
