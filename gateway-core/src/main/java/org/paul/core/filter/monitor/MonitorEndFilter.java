package org.paul.core.filter.monitor;
import com.alibaba.nacos.client.naming.utils.RandomUtils;


import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import lombok.extern.slf4j.Slf4j;
import com.sun.net.httpserver.HttpServer;
import org.paul.core.ConfigLoader;
import org.paul.core.context.GatewayContext;
import org.paul.core.filter.Filter;
import org.paul.core.filter.FilterAspect;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.paul.common.constants.FilterConst.*;

//请求结束的时候走到这里
@Slf4j
@FilterAspect(id = MONITOR_END_FILTER_ID,
        name = MONITOR_END_FILTER_NAME,
        order = MONITOR_END_FILTER_ORDER)
public class MonitorEndFilter implements Filter {
    //普罗米修斯的注册表
    private final PrometheusMeterRegistry prometheusMeterRegistry;

    public MonitorEndFilter() {
        this.prometheusMeterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

        try{
            //暴露接口，提供给普罗米修斯拉取数据
            HttpServer server = HttpServer.create(new InetSocketAddress(ConfigLoader.getConfig().getPrometheusPort()), 0);
            server.createContext("/prometheus", exchange -> {
                //数据从注册表中取，获取指标数据的文本内容
                String scrape = prometheusMeterRegistry.scrape();

                //指标数据返回
                exchange.sendResponseHeaders(200, scrape.getBytes().length);
                try(OutputStream outputStream = exchange.getResponseBody()){
                    outputStream.write(scrape.getBytes());
                }
            });

            new Thread(server::start).start();
        }catch (IOException e){
            log.error("prometheus http server start error", e);
            throw new RuntimeException(e);
        }
        log.info("prometheus http server start successful, port:{}", ConfigLoader.getConfig().getPrometheusPort());

        /*//mock
        Executors.newScheduledThreadPool(1000).scheduleAtFixedRate(() -> {
            Timer.Sample sample = Timer.start();
            try {
                Thread.sleep(RandomUtils.nextInt(100));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            Timer timer = prometheusMeterRegistry.timer("gateway_request",
                    "uniqueId", "backend-http-server:1.0.0",
                    "protocol", "http",
                    "path", "/http-server/ping" + RandomUtils.nextInt(10));
            sample.stop(timer);
        },200, 100, TimeUnit.MILLISECONDS);*/

    }

    @Override
    public void doFilter(GatewayContext ctx) throws Exception {
        //定义指标的名字和tag（关键字段）
        Timer timer = prometheusMeterRegistry.timer("gateway_request",
                "uniqueId", ctx.getUniqueId(),
                "protocol", ctx.getProtocol(),
                "path", ctx.getRequest().getPath());
        //停止采集
        ctx.getTimeSample().stop(timer);
    }
}
