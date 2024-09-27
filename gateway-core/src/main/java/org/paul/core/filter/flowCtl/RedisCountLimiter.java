package org.paul.core.filter.flowCtl;

/**
 * 使用redis实现分布式限流
 */
public class RedisCountLimiter {
    //引入reids对应的工具包，还需要配置properties

    //定义两个对应的常量，判断是成功还是失败

    //执行限流：放入对应的key，limit，过期时间；原理是根据过期时间
        //执行对应的lua脚本保证线程安全
            //判断返回结果，如果为空，返回true
            //如果成功，返回1
            //如果失败，返回0，
        //try/catch执行，抛出异常
}
