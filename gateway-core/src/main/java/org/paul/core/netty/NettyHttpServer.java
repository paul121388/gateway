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

import java.net.InetSocketAddress;

@Slf4j
public class NettyHttpServer implements LifeCycle {
    /**
     * 定义属性
     */
    private final Config config;

    /**
     * 封装netty的属性，线程组，启动器
     */
    private ServerBootstrap serverBootstrap;
    private EventLoopGroup eventLoopGroupBoss;
    private EventLoopGroup eventLoopGroupWorker;

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
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel channel) throws Exception {
                        channel.pipeline().addLast(
                                new HttpServerCodec(),
                                new HttpObjectAggregator(config.getHttpMaxContentLength()),
                                new NettyServerConnectManagerHandler(),
                                new NettyHttpServerHandler()
                        );
                    }
                });
        try {
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
