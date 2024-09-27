package org.paul.common.config;

import lombok.Data;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @author paul
 * @description 规则对象
 */
@Data
public class Rule implements Comparable<Rule>, Serializable {
    /**
     * 全局唯一规则Id
     */
    private String id;

    /**
     * 规则名称
     */
    private String name;

    /**
     * 协议类型
     */
    private String protocol;

    /**
     * 规则优先级
     */
    private Integer order;

    /**
     * 后端服务id
     */
    private String serviceId;

    /**
     * 请求前缀
     */
    private String prefix;

    /**
     * 请求路径
     */
    private List<String> paths;

    /**
     * 过滤器配置集合
     */
    private Set<FilterConfig> filterConfigs = new HashSet<>();

    /**
     * 限流过滤器配置集合
     */
    private Set<FlowCtlConfig> flowCtlConfigs = new HashSet<>();

    private RetryConfig retryConfig = new RetryConfig();

    public RetryConfig getRetryConfig() {
        return retryConfig;
    }

    public void setRetryConfig(RetryConfig retryConfig) {
        this.retryConfig = retryConfig;
    }

    public Rule() {
        super();
    }

    public Rule(String id, String name, String protocol, Integer order, String serviceId, String prefix, List<String> paths, Set<FilterConfig> filterConfigs) {
        this.id = id;
        this.name = name;
        this.protocol = protocol;
        this.order = order;
        this.serviceId = serviceId;
        this.prefix = prefix;
        this.paths = paths;
        this.filterConfigs = filterConfigs;
    }

    public static class FilterConfig {
        /**
         * 过滤器规则Id
         */
        private String id;


        /**
         * 过滤器配置信息
         */
        private String config;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getConfig() {
            return config;
        }

        public void setConfig(String config) {
            this.config = config;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FilterConfig that = (FilterConfig) o;
            return id.equals(that.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }

    /**
     * 限流规则配置，一般根据ip/路径/后端服务
     */
    public static class FlowCtlConfig{
        /**
         * 限流类型，根据ip/路径/后端服务
         */
        private String type;

        /**
         * 限流对象，比如是ip，路径，或后端服务id
         */
        private String value;

        /**
         * 限流模式，分布式/单机限流
         */
        private String model;

        /**
         * 限流规则，配置在nacos配置中心，通常是两个参数：duration内permits流量，JSON格式
         */
        private String config;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getConfig() {
            return config;
        }

        public void setConfig(String config) {
            this.config = config;
        }
    }

    /**
     * 限流规则配置，一般根据ip/路径/后端服务
     */
    public static class FlowCtlConfig{
        /**
         * 限流类型，根据ip/路径/后端服务
         */
        private String type;

        /**
         * 限流对象，比如是ip，路径，或后端服务id
         */
        private String value;

        /**
         * 限流模式，分布式/单机限流
         */
        private String model;

        /**
         * 限流规则，配置在nacos配置中心，通常是两个参数：duration内permits流量，JSON格式
         */
        private String config;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getConfig() {
            return config;
        }

        public void setConfig(String config) {
            this.config = config;
        }
    }

    //对外暴露流控过滤器配置的获取和修改
    public Set<FlowCtlConfig> getFlowCtlConfigs() {
        return flowCtlConfigs;
    }

    public void setFlowCtlConfigs(Set<FlowCtlConfig> flowCtlConfigs) {
        this.flowCtlConfigs = flowCtlConfigs;
    }

    /**
     * 向规则里面提供新增配置的方法
     * @param filterConfig
     * @return
     */
    public boolean addFilterConfig(FilterConfig filterConfig) {
        return filterConfigs.add(filterConfig);
    }

    /**
     * 通过指定的id获取指定的配置信息
     * @param id
     * @return
     */
    public FilterConfig getFilterConfig(String id) {
        for (FilterConfig filterConfig : filterConfigs) {
            if (filterConfig.getId().equalsIgnoreCase(id)) {
                return filterConfig;
            }
        }
        return null;
    }

    /**
     * 重试的配置，一般在路由转发中进行重试
     */
    public static class RetryConfig{
        //重试次数
        private int times;

        public int getTimes() {
            return times;
        }

        public void setTimes(int times) {
            this.times = times;
        }
    }

    /**
     * 根据Filterid，判断FilterConfig是否存在
     * @param filterId
     * @return
     */
    public boolean hashId(String filterId) {
        for (FilterConfig filterConfig : filterConfigs) {
            if (filterConfig.getId().equalsIgnoreCase(filterId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int compareTo(Rule o) {
        int compareOrder = Integer.compare(this.getOrder(), o.getOrder());
        if(compareOrder == 0){
            return this.getId().compareTo(o.getId());
        }
        return compareOrder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rule rule = (Rule) o;
        return id.equals(rule.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
