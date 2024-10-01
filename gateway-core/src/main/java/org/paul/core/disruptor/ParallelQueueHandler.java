package org.paul.core.disruptor;

import com.google.common.base.Preconditions;
import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.ProducerType;

import java.util.concurrent.ExecutorService;

/**
 * 基于Disruptor的多生产者多消费者无锁队列处理类
 */
public class ParallelQueueHandler<E> implements ParallelQueue {

    //disruptor中的缓存
    private RingBuffer<Holder> ringBuffer;

    //事件监听器
    private EventListener<E> eventListener;

    //workerPool是用来管理一组workerProcessor存在的，它被作为一个消费者对待
    private WorkerPool<E> workerPool;

    //自定义线程池
    private ExecutorService executorService;

    //参数传递类，用于向RingBuffer中放入参数的转换类
    private EventTranslatorOneArg<Holder, E> eventTranslator;

    public ParallelQueueHandler() {
    }

    public ParallelQueueHandler(RingBuffer<Holder> ringBuffer,
                                EventListener<E> eventListener,
                                WorkerPool<E> workerPool,
                                ExecutorService executorService,
                                EventTranslatorOneArg<Holder, E> eventTranslator) {
        // todo 构造这里面成员变量
        this.ringBuffer = ringBuffer;
        this.eventListener = eventListener;
        this.workerPool = workerPool;
        this.executorService = executorService;
        this.eventTranslator = eventTranslator;
    }

    @Override
    public void add(Object event) {

    }

    @Override
    public void add(Object[] event) {

    }

    @Override
    public boolean tryAdd(Object event) {
        return false;
    }

    @Override
    public boolean tryAdd(Object[] event) {
        return false;
    }

    @Override
    public void start() {

    }

    @Override
    public void shutDown() {

    }

    @Override
    public boolean isShutdown() {
        return false;
    }

    //使用建造者模式构建
    public static class Builder<E> {
        //设置生产者模式为多生产者
        private ProducerType producerType = ProducerType.MULTI;

        //定义RingBuffer的大小
        private int bufferSize = 1024 * 16;

        //工作线程数
        private int threads = 1;

        //命名前缀
        String namePrefix = "";

        //等待策略：生产者和消费者之间的平衡
        private WaitStrategy waitStrategy = new BlockingWaitStrategy();

        //事件监听器
        private EventListener<E> eventListener;


        //建造者模式：返回Builder，方便链式调用，最后再build，建立当前对象
        //要求对参数进行校验
        public Builder<E> setProducerType(ProducerType producerType) {
            Preconditions.checkNotNull(producerType);
            this.producerType = producerType;
            return this;
        }

        public Builder<E> setBufferSize(int bufferSize) {
            Preconditions.checkArgument(Integer.bitCount(bufferSize) == 1);
            this.bufferSize = bufferSize;
            return this;
        }

        public Builder<E> setThreads(int threads) {
            Preconditions.checkArgument(threads > 0);
            this.threads = threads;
            return this;
        }

        public Builder<E> setNamePrefix(String namePrefix) {
            Preconditions.checkNotNull(namePrefix);
            this.namePrefix = namePrefix;
            return this;
        }

        public Builder<E> setWaitStrategy(WaitStrategy waitStrategy) {
            Preconditions.checkNotNull(waitStrategy);
            this.waitStrategy = waitStrategy;
            return this;
        }

        public Builder<E> setEventListener(EventListener<E> eventListener) {
            Preconditions.checkNotNull(eventListener);
            this.eventListener = eventListener;
            return this;
        }

        public ParallelQueueHandler<E> build(){
            return new ParallelQueueHandler<E>();
        }
    }


    public class Holder {
        //放入RingBuffer的事件（消息)
        private E event;

        public void setEvent(E event) {
            this.event = event;
        }

        @Override
        public String toString() {
            return "Holder{" +
                    "event=" + event +
                    '}';
        }
    }
}
