package org.paul.gateway.client.core;

import java.lang.annotation.*;

// 定义核心注解，用于描述下游服务
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiService {
    // 服务id
    String serviceId();

    // 版本号
    String version() default "1.0.0";

    // 协议
    ApiProtocol protocol();

    // 匹配路径
    String patternPath();
}
