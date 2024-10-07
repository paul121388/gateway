package org.paul.core;

import lombok.extern.slf4j.Slf4j;
import org.paul.core.netty.NettyHttpClient;
import org.paul.core.netty.NettyHttpServer;
import org.paul.core.netty.processor.DisruptorNettyProcessor;
import org.paul.core.netty.processor.NettyCoreProcessor;
import org.paul.core.netty.processor.NettyProcessor;

import static org.paul.common.constants.GatewayConst.BUFFER_TYPE_PARALLEL;

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
    private NettyProcessor nettyProcessor;

    public Container(Config config) {
        this.config = config;
        init();
    }
    @Override
    public void init() {
        //根据不同的配置使用不同的处理类
        NettyCoreProcessor nettyCoreProcessor = new NettyCoreProcessor();
        if(config.getBufferType().equals(BUFFER_TYPE_PARALLEL)){
            this.nettyProcessor = new DisruptorNettyProcessor(config, nettyCoreProcessor);
        }else{
            //如果为串行处理，则使用原来的processor
            this.nettyProcessor = nettyCoreProcessor;
        }

        this.nettyHttpServer = new NettyHttpServer(config, nettyProcessor);

        this.nettyHttpClient = new NettyHttpClient(config, nettyHttpServer.getEventLoopGroupWorker());
    }

    @Override
    public void start() {
        nettyProcessor.start();
        nettyHttpServer.start();
        nettyHttpClient.start();
        log.info("api gateway started successfully");
    }

    @Override
    public void shutdown() {
        nettyProcessor.shutDown();
        nettyHttpServer.shutdown();
        nettyHttpClient.shutdown();
    }
}
