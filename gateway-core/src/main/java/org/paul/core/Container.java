package org.paul.core;

import lombok.extern.slf4j.Slf4j;
import org.paul.core.netty.NettyHttpClient;
import org.paul.core.netty.NettyHttpServer;
import org.paul.core.netty.processor.NettyCoreProcessor;
import org.paul.core.netty.processor.NettyProcessor;

@Slf4j
public class Container implements LifeCycle{
    // 封装属性
    private final Config config;

    private NettyHttpServer nettyHttpServer;

    private NettyHttpClient nettyHttpClient;

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
