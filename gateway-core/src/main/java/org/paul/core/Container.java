package org.paul.core;

import lombok.extern.slf4j.Slf4j;
import org.paul.core.netty.NettyHttpClient;
import org.paul.core.netty.NettyHttpServer;
import org.paul.core.netty.processor.NettyCoreProcessor;
import org.paul.core.netty.processor.NettyProcessor;

/**
 * 目的：整合之前的netty容器，比如接收http请求的NettyHttpServer，转发请求的NettyHttpClient，和两者之间的核心处理逻辑NettyCoreProcessor
 * 实现LifeCycle
 * 封装属性，实现构造方法
 * 实现LifeCycle中的init，start，shutdown方法
 */
@Slf4j
public class Container implements LifeCycle{
    // 封装属性
    private final Config config;

    //接收http请求
    private NettyHttpServer nettyHttpServer;

    //转发请求
    private NettyHttpClient nettyHttpClient;

    //核心逻辑，处理转发
    private NettyCoreProcessor nettyCoreProcessor;

    public Container(Config config) {
        this.config = config;
        init();
    }
    @Override
    public void init() {
        this.nettyCoreProcessor = new NettyCoreProcessor();

        this.nettyHttpServer = new NettyHttpServer(config, nettyCoreProcessor);

        this.nettyHttpClient = new NettyHttpClient(config, nettyHttpServer.getEventLoopGroupWorker());
    }

    @Override
    public void start() {
        nettyHttpServer.start();
        nettyHttpClient.start();
        log.info("api gateway started successfully");
    }

    @Override
    public void shutdown() {
        nettyHttpServer.shutdown();
        nettyHttpClient.shutdown();
    }
}
