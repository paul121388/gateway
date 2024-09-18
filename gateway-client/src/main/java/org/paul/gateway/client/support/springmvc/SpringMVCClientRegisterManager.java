package org.paul.gateway.client.support.springmvc;

import lombok.extern.slf4j.Slf4j;
import org.paul.common.config.ServiceDefinition;
import org.paul.common.config.ServiceInstance;
import org.paul.common.constants.BasicConst;
import org.paul.common.constants.GatewayConst;
import org.paul.common.utils.NetUtils;
import org.paul.common.utils.TimeUtil;
import org.paul.gateway.client.core.ApiAnnotationScanner;
import org.paul.gateway.client.core.ApiProperties;
import org.paul.gateway.client.support.AbstractClientRegisterManager;
import org.paul.gateway.register.center.api.RegisterCenter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.annotation.Resource;
import javax.annotation.Resources;
import javax.swing.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class SpringMVCClientRegisterManager extends AbstractClientRegisterManager implements ApplicationListener<ApplicationEvent>, ApplicationContextAware {
    // 封装的属性
    private ApplicationContext applicationContext;

    //需要服务的配置，比如端口，由spring封装好了
    @Resource
    private ServerProperties serverProperties;

    // 保存处理过的
    private Set<Object> set = new HashSet<>();

    public SpringMVCClientRegisterManager(ApiProperties apiProperties) {
        super(apiProperties);
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) throws BeansException {
        if(applicationEvent instanceof ApplicationStartedEvent){
            //如果是启动事件
            try {
                //具体实现，把下游服务注册到服务中心
                doRegisterSpringMvc();
            } catch (Exception e) {
                log.error("doRegisterSpringMvc error", e);
                throw new RuntimeException(e);
            }

            log.info("springmvc api started");
        }
    }

    //具体注册方法的实现
    private void doRegisterSpringMvc() {
        //目的是获取当前 Spring 应用程序上下文及其祖先上下文中所有 RequestMappingHandlerMapping 类型的 bean，并将它们存储在一个 Map 中
        Map<String, RequestMappingHandlerMapping> allRequestMappings = BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext,
                RequestMappingHandlerMapping.class, true, false);

        //RequestMappingHandlerMapping 是 Spring 框架中的一个接口，它定义了用于处理 HTTP 请求的映射器（Handler Mapping）应该实现的方法。
        // 这个接口是 Spring MVC 框架的一部分，将 HTTP 请求映射到控制器（Controller）的 @RequestMapping 注解方法
        for (RequestMappingHandlerMapping handleMapping : allRequestMappings.values()) {
            //键是 RequestMappingInfo 对象，它包含了请求的详细信息，如 URL 模式、HTTP 方法等
            //HandlerMethod 对象是一个用于表示 Spring MVC 控制器方法的对象，它包含了与控制器方法相关的所有信息，包括方法本身、控制器类以及方法执行时所需的参数类型等
            Map<RequestMappingInfo, HandlerMethod> handlerMethods = handleMapping.getHandlerMethods();

            for (Map.Entry<RequestMappingInfo, HandlerMethod> methodEntry : handlerMethods.entrySet()) {
                HandlerMethod handlerMethod = methodEntry.getValue();
                Class<?> clazz = handlerMethod.getBeanType();

                Object bean = applicationContext.getBean(clazz);
                if(set.contains(bean)){
                    // 如果处理过了，跳过
                    continue;
                }

                // 注解扫描
                ServiceDefinition serviceDefinition = ApiAnnotationScanner.getInstance().scanner(bean);
                if(serviceDefinition == null){
                    // 拿不到服务定义，直接跳过
                    continue;
                }
                set.add(bean);

                // 重新设置ServiceDefinition环境
                serviceDefinition.setEnvType(getApiProperties().getEnv());

                // 服务实例
                ServiceInstance serviceInstance = new ServiceInstance();

                String localIp = NetUtils.getLocalIp();
                int port = serverProperties.getPort();
                String serviceInstanceId = localIp + BasicConst.COLON_SEPARATOR + port;
                String uniqueId = serviceDefinition.getUniqueId();
                String version = serviceDefinition.getVersion();

                serviceInstance.setServiceInstanceId(serviceInstanceId);
                serviceInstance.setUniqueId(uniqueId);
                serviceInstance.setIp(localIp);
                serviceInstance.setPort(port);
                serviceInstance.setRegisterTime(TimeUtil.currentTimeMillis());
                serviceInstance.setVersion(version);
                serviceInstance.setWeight(GatewayConst.DEFAULT_WEIGHT);

                // 下游服务注册到服务中心
                register(serviceDefinition, serviceInstance);
            }
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
