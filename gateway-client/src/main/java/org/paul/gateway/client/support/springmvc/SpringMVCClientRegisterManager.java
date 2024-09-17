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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class SpringMVCClientRegisterManager extends AbstractClientRegisterManager implements ApplicationListener<ApplicationEvent>, ApplicationContextAware {
    // 封装的属性
    private ApplicationContext applicationContext;

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
            try {
                doRegisterSpringMvc();
            } catch (Exception e) {
                log.error("doRegisterSpringMvc error", e);
                throw new RuntimeException(e);
            }

            log.info("springmvc api started");
        }
    }

    private void doRegisterSpringMvc() {
        Map<String, RequestMappingHandlerMapping> allRequestMappings = BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, RequestMappingHandlerMapping.class,
                true, false);
        for (RequestMappingHandlerMapping handleMapping : allRequestMappings.values()) {
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

                // 重新扫描环境
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
