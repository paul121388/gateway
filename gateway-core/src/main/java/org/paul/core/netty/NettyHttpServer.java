package org.paul.core.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.paul.common.utils.RemotingUtil;
import org.paul.core.Config;
import org.paul.core.LifeCycle;
import org.paul.core.netty.processor.NettyProcessor;

import java.net.InetSocketAddress;

@Slf4j
public class NettyHttpServer implements LifeCycle {
    /**
     * 定义属性
     */
    private final Config config;

    /**
     * 封装netty的属性，
     * 线程组:用于处理网络事件
     * 启动器:Netty 中用于启动服务端的辅助类
     * 核心处理器（自定义）:用于处理业务逻辑
     */
    private ServerBootstrap serverBootstrap;
    private EventLoopGroup eventLoopGroupBoss;
    private EventLoopGroup eventLoopGroupWorker;

    private NettyProcessor nettyProcessor;

    /**
     * 实现NettyHttpServer构造方法
     * config初始化
     * 调用init
     */
    public NettyHttpServer(Config config) {
        this.config = config;
        init();
    }


    /**
     * 实现生命周期接口的几个方法
     */

    /**
     * init方法，初始化
     * 如果支持epoll，使用epoll对应的api
     * new serverBootstrap 作为netty服务的启动
     * new eventLoopGroupBoss，构造时从config传入配置，定义线程工厂，给线程池起名字
     * new eventLoopGroupWorker，构造时从config传入配置，定义线程工厂，给线程池起名字
     */
    @Override
    public void init() {
        if (useEpoll()) {
            this.serverBootstrap = new ServerBootstrap();
            this.eventLoopGroupBoss = new EpollEventLoopGroup(config.getEventLoopGroupBossNum(),
                    new DefaultThreadFactory("netty-boss-nio"));
            this.eventLoopGroupWorker = new EpollEventLoopGroup(config.getEventLoopGroupWorkerNum(),
                    new DefaultThreadFactory("netty-worker-nio"));
        } else {
            this.serverBootstrap = new ServerBootstrap();
            this.eventLoopGroupBoss = new NioEventLoopGroup(config.getEventLoopGroupBossNum(),
                    new DefaultThreadFactory("netty-boss-nio"));
            this.eventLoopGroupWorker = new NioEventLoopGroup(config.getEventLoopGroupWorkerNum(),
                    new DefaultThreadFactory("netty-worker-nio"));
        }
    }

    /**
     * 判断系统支持epoll
     * netty优化，减少GC等
     */
    public boolean useEpoll() {
        // 是否linux系统，是否支持epoll
        return RemotingUtil.isLinuxPlatform() && Epoll.isAvailable();
    }

    /**
     * start方法，启动过程逻辑
     *      使用启动器，指定group
     *      指定channel，判断是否支持epoll，是：使用epoll的api，否则使用默认
     *      配置端口
     *      配置channelhandler
     *      初始化channel：添加一系列处理器
     *          netty提供的http编码器
     *          netty提供的http对象聚合器，参数：最大长度
     *          链接管理器：netty生命周期的管理，已经封装好了
     *          核心处理逻辑
     * try
     *      启动：bind.sync
     *      添加日志：打印端口
     */
    @Override
    public void start() {
        this.serverBootstrap
                .group(eventLoopGroupBoss, eventLoopGroupWorker)
                .channel(useEpoll()? EpollServerSocketChannel.class: NioServerSocketChannel.class)
                .localAddress(new InetSocketAddress(config.getPort()))

                // 调用 childHandler 方法来设置一个 ChannelInitializer，这是一个特殊的处理器，用于配置新创建的 Channel 的 ChannelPipeline
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel channel) throws Exception {
                        channel.pipeline().addLast(
                                // 这是一个 Netty 提供的编解码器，用于处理 HTTP 请求和响应。它将字节流转换为 HttpRequest 和 HttpResponse 对象，反之亦然
                                new HttpServerCodec(),

                                // 聚合器（Aggregator）：由于 HTTP 请求可能分布在多个 TCP 数据包中，
                                // Netty 使用了一个叫做 HttpObjectAggregator 的处理器来将这些片段聚合成一个完整的 HTTP 请求。
                                // 这个处理器确保了即使请求是分块传输的，应用程序也会接收到一个完整的 FullHttpRequest 对象
                                new HttpObjectAggregator(config.getHttpMaxContentLength()),
                                new NettyServerConnectManagerHandler(),
                                new NettyHttpServerHandler(nettyProcessor)
                        );
                    }
                });
        try {
            // 绑定服务器到指定端口并同步等待绑定完成
            this.serverBootstrap.bind().sync();
            log.info("server startup on port {}", config.getPort());
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    /**
     * 优雅停机shutdown
     *      两个线程组是否为空
     *      调用api停止
     */
    @Override
    public void shutdown() {
        if(eventLoopGroupBoss != null){
            eventLoopGroupBoss.shutdownGracefully();
        }
        if(eventLoopGroupWorker != null){
            eventLoopGroupWorker.shutdownGracefully();
        }
    }
}
