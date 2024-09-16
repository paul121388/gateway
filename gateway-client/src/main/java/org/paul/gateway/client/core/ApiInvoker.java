package org.paul.gateway.client.core;

import java.lang.annotation.*;

// 服务调用的注解，对应的每一个方法或者接口
// 必须在方法上强制声明
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiInvoker {
    // 路径
    String path();
}
