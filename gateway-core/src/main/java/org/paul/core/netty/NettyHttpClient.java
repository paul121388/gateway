package org.paul.core.netty;

import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.EventLoopGroup;
import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.paul.core.Config;
import org.paul.core.LifeCycle;
import org.paul.core.helper.AsyncHttpHelper;

import java.io.IOException;

/**
 * 主要用于下游服务请求转发
 * 实现LifeCycle
 * 封装属性
 * 实现LifeCycle中的init，start，shutdown方法
 */
@Slf4j
public class NettyHttpClient implements LifeCycle {
    // 封装属性
    private final Config config;

    private final EventLoopGroup eventLoopGroupWorker;

    private AsyncHttpClient asyncHttpClient;

    public NettyHttpClient(Config config, EventLoopGroup eventLoopGroupWorker) {
        this.config = config;
        this.eventLoopGroupWorker = eventLoopGroupWorker;
        init();
    }

    /**
     * 初始化AsyncHttpClient asyncHttpClient
     */
    @Override
    public void init() {
        // new 默认的AsyncHttpClientConfig的builder对象，并添加工作线程组
        // 添加配置，从config中获取
        DefaultAsyncHttpClientConfig.Builder builder = new DefaultAsyncHttpClientConfig.Builder()
                .setEventLoopGroup(eventLoopGroupWorker)
                //链接超时时间
                .setConnectTimeout(config.getHttpConnectTimeout())
                //请求超时时间
                .setRequestTimeout(config.getHttpRequestTimeout())
                //最大重试次数
                .setMaxRedirects(config.getHttpMaxRequestRetry())
                //池化的ByteBuf分配器
                .setAllocator(PooledByteBufAllocator.DEFAULT)
                //能否对信息压缩
                .setCompressionEnforced(true)
                //设置最大连接数
                .setMaxConnections(config.getHttpMaxConnections())
                //设置每台服务最大连接数
                .setMaxConnectionsPerHost(config.getHttpConnectionsPerHost())
                //设置池化连接超时
                .setPooledConnectionIdleTimeout(config.getHttpPooledConnectionIdleTimeout());

        // 通过builder初始化AsyncHttpClient
        this.asyncHttpClient = new DefaultAsyncHttpClient(builder.build());
    }

    @Override
    public void start() {
        // AsyncHttpHelper的单例方法初始化
        AsyncHttpHelper.getInstance().initialized(asyncHttpClient);
    }

    @Override
    public void shutdown() {
        // AsyncHttpClient不为空
        if (asyncHttpClient != null) {
            try {// 优雅的关闭
                this.asyncHttpClient.close();
            }// 捕获IO异常
            catch (IOException e) {
                log.error("NettyHttpClient shutdown exception", e);
            }
        }
    }
}
