package org.paul.core.filter;

import org.apache.commons.lang3.StringUtils;
import org.paul.common.config.Rule;
import org.paul.common.constants.FilterConst;
import org.paul.core.context.GatewayContext;
import org.paul.core.filter.router.RouterFilter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 过滤器工厂实现类
 */
public class GatewayFilterChainFactory implements FilterFactory {
    //单例模式饿汉式实现，保证线程安全
    private static class SingletonInstance {
        private static final GatewayFilterChainFactory INSTANCE = new GatewayFilterChainFactory();
    }
    public static GatewayFilterChainFactory getInstance(){
        return SingletonInstance.INSTANCE;
    }

    public Map<String, Filter> processorFilterIdMap = new ConcurrentHashMap<>();

    public GatewayFilterChainFactory(){
        //加载所有的filter类，遍历filter类，放到当前工厂中的map
        ServiceLoader<Filter> serviceLoader = ServiceLoader.load(Filter.class);
        serviceLoader.stream().forEach(filterProvider -> {
            Filter filter = filterProvider.get();
            FilterAspect annotation = filter.getClass().getAnnotation(FilterAspect.class);
            if(annotation != null){
                String filterId = annotation.id();

                //如果注解中没有配置filterId，就以类名为filterId
                if(StringUtils.isEmpty(filterId)){
                    filterId = filter.getClass().getName();
                }
                processorFilterIdMap.put(filterId, filter);
            }
        });
    }

    /**
     * 构建过滤器链条
     * @param ctx
     * @return
     * @throws Exception
     */
    @Override
    public GatewayFilterChain buildFilterChain(GatewayContext ctx) throws Exception {
        GatewayFilterChain gatewayFilterChain = new GatewayFilterChain();
        List<Filter> filters = new ArrayList<>();

        filters.add(getFilterInfo(FilterConst.GRAY_FILTER_ID));

        //默认添加前后两个监控过滤器
        filters.add(getFilterInfo(FilterConst.MONITOR_FILTER_ID));
        filters.add(getFilterInfo(FilterConst.MONITOR_END_FILTER_ID));

        //GatewayContext中的Rule定义了规则
        //Rule中有了FilterConfig的集合
        Rule rule = ctx.getRule();
        if(rule != null){
            //获取filterConfig的set
            Set<Rule.FilterConfig> filterConfigs = rule.getFilterConfigs();
            Iterator iterator = filterConfigs.iterator();

            //遍历filterConfigs
            Rule.FilterConfig filterConfig;
            while (iterator.hasNext()){
                //尝试获取过滤器配置
                filterConfig = (Rule.FilterConfig)iterator.next();
                if(filterConfig == null){
                    continue;
                }
                //根据过滤器配置获取过滤器id
                String filterId = filterConfig.getId();

                //根据过滤器id获取过滤器，并添加到过滤器链条的list中
                if(StringUtils.isNotEmpty(filterId) && getFilterInfo(filterId) != null){
                    Filter filter = getFilterInfo(filterId);
                    filters.add(filter);
                }
            }
        }
        //添加路由过滤器，最后一步
        filters.add(new RouterFilter());

        //根据order对暂存的过滤器根据order进行排序
        filters.sort(Comparator.comparingInt(Filter::getOrder));

        //将所有获取到的过滤器添加到GatewayFilterChain中
        gatewayFilterChain.addFilterList(filters);
        return gatewayFilterChain;
    }

    /**
     * 用map存filterid和filter，通过get方法根据filterId获取对应的filter
     * @param filterId
     * @return
     * @throws Exception
     */
    @Override
    public Filter getFilterInfo(String filterId) throws Exception {
        return processorFilterIdMap.get(filterId);
    }
}
