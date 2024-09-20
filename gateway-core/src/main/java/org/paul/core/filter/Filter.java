package org.paul.core.filter;

import org.paul.core.context.GatewayContext;

/**
 * 过滤器顶级接口
 */
public interface Filter {
    /**
     * 过滤器执行拦截
     * @param ctx
     * @throws Exception
     */
    void doFilter(GatewayContext ctx) throws Exception;

    /**
     * 获取等级，为了一个地方写了，别的地方就不用再写，可以把属性封装到注解中，通过注解获取
     */
    default int getOrder(){
        FilterAspect annotation = this.getClass().getAnnotation(FilterAspect.class);
        if(annotation != null){
            return annotation.order();
        }
        return Integer.MAX_VALUE;
    };
}
