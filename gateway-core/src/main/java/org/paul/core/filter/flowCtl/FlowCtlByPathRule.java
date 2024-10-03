package org.paul.core.filter.flowCtl;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.paul.common.config.Rule;
import org.paul.core.redis.JedisUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.paul.common.constants.FilterConst.*;

/**
 * 根据路径进行流控
 */
public class FlowCtlByPathRule implements IGatewayFlowCtlRule {
    private String serviceId;

    private String path;

    private RedisCountLimiter redisCountLimiter;

    private static final String LIMIT_MESSAGE = "请求过于频繁，请稍后重试";

    //redis限流对象，添加到构造函数中；暴露方法也要设置

    public FlowCtlByPathRule(String serviceId, String path, RedisCountLimiter redisCountLimiter) {
        this.serviceId = serviceId;
        this.path = path;
        this.redisCountLimiter = redisCountLimiter;
    }

    //将实例放在concurrentHashMap中
    private static ConcurrentHashMap<String /*path+serviceId*/, FlowCtlByPathRule> servicePathMap = new ConcurrentHashMap<>();

    //对外暴露获取当前实例的方法，根据当前路径和serviceId获取，提前组装好，不用重复组装
    public static FlowCtlByPathRule getInstance(String serviceId, String path) {
        StringBuffer buffer = new StringBuffer();
        String key = buffer.append(serviceId).append(".").append(path).toString();

        //尝试根据key获取，获取不到就new一个，放入Map中，并返回
        FlowCtlByPathRule flowCtlByPathRule = servicePathMap.get(key);
        if (flowCtlByPathRule == null) {
            flowCtlByPathRule = new FlowCtlByPathRule(serviceId, path, new RedisCountLimiter(new JedisUtil()));
            servicePathMap.put(key, flowCtlByPathRule);
        }
        return flowCtlByPathRule;
    }


    /**
     * 根据路径执行流控
     *
     * @param flowCtlConfig
     * @param serviceId
     */
    @Override
    public void doFlowCtlFilter(Rule.FlowCtlConfig flowCtlConfig, String serviceId) {
        //如果流控规则为空 或 serviceId  或  配置中的规则为空，直接返回
        if (flowCtlConfig == null || StringUtils.isEmpty(serviceId) || StringUtils.isEmpty(flowCtlConfig.getConfig())) {
            return;
        }

        //流控指的是某一分钟内执行多少次，具体为duration/permits，将这个值放到Map中，key为两个对应的描述的字符串
        //需要从flowCtlConfig中解析上述两个参数
        Map<String, Integer> configMap = JSON.parseObject(flowCtlConfig.getConfig(), Map.class);
        //只要一个参数为空，直接返回，不做流控
        if (!configMap.containsKey(FLOW_CTL_LIMIT_DURATION) || !configMap.containsKey(FLOW_CTL_LIMIT_PERMITS)) {
            return;
        }
        //从上述Map中获取两个参数
        double duration = configMap.get(FLOW_CTL_LIMIT_DURATION);
        double permits = configMap.get(FLOW_CTL_LIMIT_PERMITS);

        StringBuffer buffer = new StringBuffer();
        String key = buffer.append(serviceId).append(".").append(path).toString();

        /******具体流控******/
        boolean flag = true;
        //区分是分布式还是单机，分布式就使用redis，单机就使用guava中的rateLimiter
        if (flowCtlConfig.getModel().equalsIgnoreCase(FLOW_CTL_MODEL_DISRTIBUTED)) {
            // redis中的限流，根据限流结果设置flag
            flag = redisCountLimiter.doFlowCtl(key, (int) permits, (int) duration);
        } else {
            //guava中的rateLimiter时，如果不能获取对应的对象，直接抛出异常
            GuavaCountLimiter guavaCountLimiter = GuavaCountLimiter.getInstance(serviceId, flowCtlConfig, (int) permits);
            if (guavaCountLimiter == null) {
                throw new RuntimeException("获取单机限流工具类失败");
            }
            //尝试获取对应数量的令牌，判断是否被限流，设置flag
//            double count = Math.ceil(permits / duration);
            flag = guavaCountLimiter.acquire(1);
        }

        //使用flag表示是否被流控了，流控了就直接抛出异常
        if (!flag) {
            throw new RuntimeException(LIMIT_MESSAGE);
        }
    }
}
