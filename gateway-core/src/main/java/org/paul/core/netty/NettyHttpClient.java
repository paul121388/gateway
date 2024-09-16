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

    @Override
    public void init() {
        // new 默认的AsyncHttpClientConfig的builder对象，并添加工作线程组
        // 添加超时配置，从config中获取链接超时异常，请求超时异常，最大重试次数，
        // 设置一个池化的ByteBuffer分配器，设置压缩，设置最大连接数，设置每台服务最大连接数，设置池化连接超时
        DefaultAsyncHttpClientConfig.Builder builder = new DefaultAsyncHttpClientConfig.Builder()
                .setEventLoopGroup(eventLoopGroupWorker)
                .setConnectTimeout(config.getHttpConnectTimeout())
                .setRequestTimeout(config.getHttpRequestTimeout())
                .setMaxRedirects(config.getHttpMaxRequestRetry())
                .setAllocator(PooledByteBufAllocator.DEFAULT)
                .setCompressionEnforced(true)
                .setMaxConnections(config.getHttpMaxConnections())
                .setMaxConnectionsPerHost(config.getHttpConnectionsPerHost())
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
