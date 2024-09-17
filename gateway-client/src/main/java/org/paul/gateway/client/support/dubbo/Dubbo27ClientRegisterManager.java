package org.paul.gateway.client.support.dubbo;

import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.spring.ServiceBean;
import org.apache.dubbo.config.spring.context.event.ServiceBeanExportedEvent;
import org.paul.common.config.ServiceDefinition;
import org.paul.common.config.ServiceInstance;
import org.paul.common.constants.BasicConst;
import org.paul.common.constants.GatewayConst;
import org.paul.common.utils.NetUtils;
import org.paul.common.utils.TimeUtil;
import org.paul.gateway.client.core.ApiAnnotationScanner;
import org.paul.gateway.client.core.ApiProperties;
import org.paul.gateway.client.support.AbstractClientRegisterManager;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class Dubbo27ClientRegisterManager extends AbstractClientRegisterManager implements ApplicationListener<ApplicationEvent> {
    // 保存处理过的
    private Set<Object> set = new HashSet<>();

    public Dubbo27ClientRegisterManager(ApiProperties apiProperties) {
        super(apiProperties);
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if(applicationEvent instanceof ServiceBeanExportedEvent){
            try {
                ServiceBean serviceBean = ((ServiceBeanExportedEvent) applicationEvent).getServiceBean();
                doRegisterDubbo(serviceBean);
            } catch (Exception e) {
                log.error("doRegisterDubbo error", e);
                throw new RuntimeException(e);
            }
        }else if(applicationEvent instanceof ApplicationStartedEvent){
            log.info("dubbo api started");
        }
    }

    private void doRegisterDubbo(ServiceBean serviceBean) {
        Object ref = serviceBean.getRef();
        if(set.contains(ref)){
            return;
        }

        ServiceDefinition serviceDefinition = ApiAnnotationScanner.getInstance().scanner(ref, serviceBean);
        if(serviceDefinition == null){
            return;
        }

        serviceDefinition.setEnvType(getApiProperties().getEnv());

        // 服务实例
        ServiceInstance serviceInstance = new ServiceInstance();
        String localIp = NetUtils.getLocalIp();
        int port = serviceBean.getProtocol().getPort();
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
