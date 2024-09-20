package org.paul.core.filter;

/**
 * 过滤器注解类
 */
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
