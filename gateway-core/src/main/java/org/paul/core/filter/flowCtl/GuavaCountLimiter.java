package org.paul.core.filter.flowCtl;

import com.google.common.util.concurrent.RateLimiter;
import org.apache.commons.lang3.StringUtils;
import org.paul.common.config.Rule;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 单机实现限流
 */
public class GuavaCountLimiter {
    /**
     * 属性
     */
    //RateLimiter，产生令牌的类
    private RateLimiter rateLimiter;
    //最大请求
    private int maxPermits;

    //构造函数：最大限流

    public GuavaCountLimiter(int maxPermits) {
        this.maxPermits = maxPermits;
        rateLimiter = RateLimiter.create(maxPermits);
    }

    //构造函数：预先生产一定的令牌，最大限流数，预热时间，时间单位
    public GuavaCountLimiter(int maxPermits, long warmUpPeriodAsSecond) {
        this.maxPermits = maxPermits;
        rateLimiter = RateLimiter.create(maxPermits, warmUpPeriodAsSecond, TimeUnit.SECONDS);
    }

    //定义一个缓存获取当前实例，放在Map中，key为serviceId+限流对象（ip/path/serviceId）
    public static ConcurrentHashMap<String, GuavaCountLimiter> resourceRateLimiterMap = new ConcurrentHashMap<>();

    //对外暴露获取当前实例的方法，参数为serviceId和配置对象
    public static GuavaCountLimiter getInstance(String serviceId, Rule.FlowCtlConfig flowCtlConfig) {
        //如果配置为空或者配置中任意参数为空，直接返回null
        if (StringUtils.isEmpty(serviceId)
                || flowCtlConfig == null
                || StringUtils.isEmpty(flowCtlConfig.getValue())
                || StringUtils.isEmpty(flowCtlConfig.getConfig())
                || StringUtils.isEmpty(flowCtlConfig.getType())) {
            return null;
        }
        //如果当前实例为空，需要new一个，并放入Map中
        StringBuffer buffer = new StringBuffer();
        String key = buffer.append(serviceId).append(".").append(flowCtlConfig.getValue()).toString();

        GuavaCountLimiter guavaCountLimiter = resourceRateLimiterMap.get(key);
        if(guavaCountLimiter == null){
            //todo 应该改为从配置中心中获取
            guavaCountLimiter = new GuavaCountLimiter(50);
            resourceRateLimiterMap.putIfAbsent(key, guavaCountLimiter);
        }
        return guavaCountLimiter;
    }


    //提供方法，判断能不能获取这么多令牌
    public boolean acquire(int permits){
        //通过rateLimiter尝试获取的结果判断
        boolean success = rateLimiter.tryAcquire(permits);
        if(success){
            return true;
        }
        return false;
    }
}
