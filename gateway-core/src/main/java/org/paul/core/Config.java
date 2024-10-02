package org.paul.core;

import com.lmax.disruptor.*;
import lombok.Data;

@Data
//网关的配置
public class Config {
    // 网关对外暴露的端口
    private int port = 9888;

    //暴露给普罗米修斯注册表的接口
    private int prometheusPort = 18100;

    // 主流服务都是微服务架构，需要进行服务发现，需要微服务名称/服务唯一id
    private String applicationName = "api-gateway";

    // 注册中心的地址
    private String registryAddress = "127.0.0.1:8848";

    // 多环境配置
    private String env = "dev";

    // 网关依赖netty，需要netty的配置
    // Boss线程池的线程数
    private int eventLoopGroupBossNum= 1;

    // Worker线程池的线程数，默认去CPU核心数
    private int eventLoopGroupWorkerNum= Runtime.getRuntime().availableProcessors();

    // 因为接收http请求，http报文大小有限制
    private int httpMaxContentLength = 1024 * 1024 * 64;

    // 单双异步的配置,默认单异步
    private boolean whenComplete = true;

    //	连接超时时间
    private int httpConnectTimeout = 30 * 1000;

    //	请求超时时间
    private int httpRequestTimeout = 30 * 1000;

    //	客户端请求重试次数
    private int httpMaxRequestRetry = 2;

    //	客户端请求最大连接数
    private int httpMaxConnections = 10000;

    //	客户端每个地址支持的最大连接数
    private int httpConnectionsPerHost = 8000;

    //	客户端空闲连接超时时间, 默认60秒
    private int httpPooledConnectionIdleTimeout = 60 * 1000;

    //缓存类型
    private String bufferType = "parallel";

    //定义队列大小
    private int bufferSize = 1 * 1024 * 16;

    //线程数
    private int processThreadNum = Runtime.getRuntime().availableProcessors();

    //等待策略名称
    private String waitStrategy = "blocking";

    //根据上述名称，new对应的等待策略
    public WaitStrategy getWaitStrategy() {
        switch (waitStrategy) {
            case "blocking":
                return new BlockingWaitStrategy();
            case "busySpin":
                return new BusySpinWaitStrategy();
            case "yielding":
                return new YieldingWaitStrategy();
            case "sleeping":
                return new SleepingWaitStrategy();
            default:
                return new BlockingWaitStrategy();
        }
    }

    // 扩展。。。


}
