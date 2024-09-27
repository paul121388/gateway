package org.paul.core.filter.flowCtl;

import org.paul.core.redis.JedisUtil;

/**
 * 使用redis实现分布式限流
 */
public class RedisCountLimiter {
    //引入reids对应的工具包，还需要配置properties
    protected JedisUtil jedisUtil;

    public RedisCountLimiter(JedisUtil jedisUtil) {
        this.jedisUtil = jedisUtil;
    }

    //定义两个对应的常量，判断是成功还是失败
    private static final int SUCCESS_RESULT = 1;
    private static final int failed_RESULT = 0;

    //执行限流：放入对应的key，limit，过期时间；原理是根据过期时间
    public boolean doFlowCtl(String key, int limit, int expire){
        try{//try/catch执行，抛出异常
            //执行对应的lua脚本保证线程安全
            Object object = jedisUtil.executeScript(key, limit, expire);
            //判断返回结果，如果为空，返回true
            if (object == null) {
                return true;
            }
            //如果成功，返回1
            Long result = Long.valueOf(object.toString());
            if (result == SUCCESS_RESULT) {
                return true;
            }
            //如果失败，返回0，
            else {
                return false;
            }
        }catch (Exception e){
            throw new RuntimeException("分布式限流错误");
        }
    }
}
