package org.paul.core;

public class Config {
    // 网关对外暴露的端口
    private int port = 9888;

    // 主流服务都是微服务架构，需要进行服务发现，需要微服务名称/服务唯一id
    private String applicationName = "api-gateway";

    // 注册中心的地址
    private String registryAddress = "http://127.0.0.1:8848";

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

}
