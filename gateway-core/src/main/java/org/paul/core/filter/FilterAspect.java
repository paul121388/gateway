package org.paul.core.filter;

import java.lang.annotation.*;

/**
 * 过滤器注解类
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface FilterAspect {
    /**
     * 过滤器id
     * @return
     */
    String id();

    /**
     * 过滤器名称
     * @return
     */
    String name() default "";

    /**
     * 过滤器order顺序
     * @return
     */
    int order() default 0;
}
