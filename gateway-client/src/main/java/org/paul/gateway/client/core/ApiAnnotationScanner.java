package org.paul.gateway.client.core;

import com.google.protobuf.Api;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.spring.ServiceBean;
import org.paul.common.config.DubboServiceInvoker;
import org.paul.common.config.HttpServiceInvoker;
import org.paul.common.config.ServiceDefinition;
import org.paul.common.config.ServiceInvoker;
import org.paul.common.constants.BasicConst;
import org.paul.gateway.client.support.dubbo.DubboConstants;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class ApiAnnotationScanner {
    // 实现单例
    private ApiAnnotationScanner() {

    }

    private static class SingletonHolder {
        static final ApiAnnotationScanner INSTANCE = new ApiAnnotationScanner();
    }

    public static ApiAnnotationScanner getInstance() {
        return SingletonHolder.INSTANCE;
    }


    /**
     * 扫描传入的bean对象，返回服务定义
     * @param bean
     * @param args
     * @return
     */
    public ServiceDefinition scanner(Object bean, Object... args) {
        // 获取bean的class对象
        Class<?> aClass = bean.getClass();
        // 判断是否加载了ApiService注解
        if (!aClass.isAnnotationPresent(ApiService.class)) {
            // 如果没有，直接返回空
            return null;
        } else {
            // 如果有，会拿到注解的相关数据
            ApiService apiService = aClass.getAnnotation(ApiService.class);
            // 获取apiService中的数据
            String serviceId = apiService.serviceId();
            ApiProtocol protocol = apiService.protocol();
            String patternPath = apiService.patternPath();
            String version = apiService.version();

            ServiceDefinition serviceDefinition = new ServiceDefinition();

            // 定义Map，用于存放ServiceInvoker
            Map<String, ServiceInvoker> invokerMap = new HashMap<String, ServiceInvoker>();

            // 获取class的所有方法；遍历所有方法
            Method[] methods = aClass.getMethods();
            if (methods != null && methods.length > 0) {
                for (Method method : methods) {
                    // 获取添加了ApiInvoker注解的方法，获得ApiInvoker的path
                    ApiInvoker annotation = method.getAnnotation(ApiInvoker.class);
                    if (annotation == null) {
                        // 如果为空，跳过
                        continue;
                    }
                    String path = annotation.path();
                    // 判断是什么协议
                    switch (protocol) {
                        case HTTP:
                            HttpServiceInvoker httpServiceInvoker = createHttpServiceInvoker(path);
                            invokerMap.put(path, httpServiceInvoker);
                            break;
                        case DUBBO:
                            ServiceBean<?> serviceBean = (ServiceBean<?>) args[0];
                            DubboServiceInvoker dubboServiceInvoker = createDubboServiceInvoker(path, serviceBean, method);

                            // 特殊处理，获取版本号
                            String dubboVersion = dubboServiceInvoker.getVersion();
                            // 如果版本号不为空，将当前服务定义的版本号设置为此版本号
                            if (!StringUtils.isBlank(dubboVersion)) {
                                version = dubboVersion;
                            }
                            // 放入invokerMap种
                            invokerMap.put(path, dubboServiceInvoker);
                            break;
                        default:
                            break;
                    }
                }
            }
            // 设置服务定义对应的属性
            serviceDefinition.setUniqueId(serviceId + BasicConst.COLON_SEPARATOR + version);
            serviceDefinition.setServiceId(serviceId);
            serviceDefinition.setVersion(version);
            serviceDefinition.setProtocol(protocol.getCode());
            serviceDefinition.setPatternPath(patternPath);
            // 服务启用
            serviceDefinition.setEnable(true);
            serviceDefinition.setInvokerMap(invokerMap);

            return serviceDefinition;
        }
    }


    /**
     * 构建HttpServiceInvoker对象
     */
    private HttpServiceInvoker createHttpServiceInvoker(String path) {
        HttpServiceInvoker httpServiceInvoker = new HttpServiceInvoker();
        httpServiceInvoker.setInvokerPath(path);
        return httpServiceInvoker;
    }

    /**
     * 构建DubboServiceInvoker对象
     */
    private DubboServiceInvoker createDubboServiceInvoker(String path, ServiceBean<?> serviceBean, Method method) {
        DubboServiceInvoker dubboServiceInvoker = new DubboServiceInvoker();
        dubboServiceInvoker.setInvokerPath(path);

        String methodName = method.getName();
        String registerAddress = serviceBean.getRegistry().getAddress();
        String interfaceClass = serviceBean.getInterface();

        dubboServiceInvoker.setRegisterAddress(registerAddress);
        dubboServiceInvoker.setMethodName(methodName);
        dubboServiceInvoker.setInterfaceClass(interfaceClass);

        String[] parameterTypes = new String[method.getParameterCount()];
        Class<?>[] classes = method.getParameterTypes();
        for (int i = 0; i < classes.length; i++) {
            parameterTypes[i] = classes[i].getName();
        }
        dubboServiceInvoker.setParameterTypes(parameterTypes);

        Integer seriveTimeout = serviceBean.getTimeout();
        if (seriveTimeout == null || seriveTimeout.intValue() == 0) {
            ProviderConfig providerConfig = serviceBean.getProvider();
            if (providerConfig != null) {
                Integer providerTimeout = providerConfig.getTimeout();
                if (providerTimeout == null || providerTimeout.intValue() == 0) {
                    seriveTimeout = DubboConstants.DUBBO_TIMEOUT;
                } else {
                    seriveTimeout = providerTimeout;
                }
            }
        }
        dubboServiceInvoker.setTimeout(seriveTimeout);

        String dubboVersion = serviceBean.getVersion();
        dubboServiceInvoker.setVersion(dubboVersion);

        return dubboServiceInvoker;
    }
}
