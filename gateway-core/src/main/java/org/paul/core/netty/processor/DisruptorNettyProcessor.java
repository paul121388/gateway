package org.paul.core.netty.processor;

import com.lmax.disruptor.dsl.ProducerType;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import lombok.extern.slf4j.Slf4j;
import org.paul.common.enums.ResponseCode;
import org.paul.core.Config;
import org.paul.core.context.HttpRequestWrapper;
import org.paul.core.disruptor.EventListener;
import org.paul.core.disruptor.ParallelQueueHandler;
import org.paul.core.helper.ResponseHelper;

@Slf4j
public class DisruptorNettyProcessor implements NettyProcessor {
    private static final String THREAD_NAME_PREFIX = "gateway-disruptor-netty-processor-";

    private Config config;

    private NettyCoreProcessor nettyCoreProcessor;

    private ParallelQueueHandler<HttpRequestWrapper> parallelQueueHandler;

    public DisruptorNettyProcessor(Config config, NettyCoreProcessor nettyCoreProcessor) {
        this.config = config;
        this.nettyCoreProcessor = nettyCoreProcessor;

        ParallelQueueHandler.Builder<HttpRequestWrapper> builder = new ParallelQueueHandler.Builder<HttpRequestWrapper>()
                .setBufferSize(config.getBufferSize())
                .setThreads(config.getProcessThreadNum())
                .setNamePrefix(THREAD_NAME_PREFIX)
                .setProducerType(ProducerType.MULTI)
                .setWaitStrategy(config.getWaitStrategy());

        BatchEventListenerProcessor batchEventListenerProcessor = new BatchEventListenerProcessor();
        builder.setEventListener(batchEventListenerProcessor);
        this.parallelQueueHandler = builder.build();
    }

    @Override
    public void process(HttpRequestWrapper httpRequestWrapper) {

    }

    public class BatchEventListenerProcessor implements EventListener<HttpRequestWrapper> {
        @Override
        public void onEvent(HttpRequestWrapper event) {
            nettyCoreProcessor.process(event);
        }

        @Override
        public void onException(Throwable ex, long sequence, HttpRequestWrapper event) {
            HttpRequest httpRequest = event.getRequest();
            ChannelHandlerContext context = event.getCtx();

            try {
                log.error("BatchEventListenerProcessor onException failed, request:{}, errorMsg:{}", httpRequest, ex.getMessage(), ex);
                FullHttpResponse fullHttpResponse = ResponseHelper.getHttpResponse(ResponseCode.INTERNAL_ERROR);
                if (!HttpUtil.isKeepAlive(fullHttpResponse)) {
                    context.writeAndFlush(fullHttpResponse).addListener(ChannelFutureListener.CLOSE);
                } else {
                    fullHttpResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                    context.writeAndFlush(fullHttpResponse);
                }
            } catch (Exception e) {
                log.error("BatchEventListenerProcessor onException failed, request:{}, errorMsg:{}", httpRequest, e.getMessage(), e);
            }


        }
    }
}
