package org.paul.core.disruptor;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.ProducerType;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 基于Disruptor的多生产者多消费者无锁队列处理类
 */
public class ParallelQueueHandler<E> implements ParallelQueue {

    //disruptor中的缓存
    private RingBuffer<Holder> ringBuffer;

    //事件监听器
    private EventListener<E> eventListener;

    //workerPool是用来管理一组workerProcessor存在的，它被作为一个消费者对待
    private WorkerPool<Holder> workerPool;

    //自定义线程池
    private ExecutorService executorService;

    //参数传递类，用于向RingBuffer中放入参数的转换类
    private EventTranslatorOneArg<Holder, E> eventTranslator;


    public ParallelQueueHandler(Builder<E> builder) {
        this.executorService = Executors.newFixedThreadPool(builder.threads,
                new ThreadFactoryBuilder().setNameFormat("ParallelQueueHandler" + builder.namePrefix + "-pool-%d").build());

        this.eventListener = builder.eventListener;

        this.eventTranslator = new HolderEventTranslator();

        RingBuffer<Holder> ringBuffer = RingBuffer.create(builder.producerType,
                new HolderEventFactory(),
                builder.bufferSize,
                builder.waitStrategy);

        //创建RingBuffer的屏障
        SequenceBarrier sequenceBarrier = ringBuffer.newBarrier();

        //创建消费者组
        WorkHandler<Holder>[] workHandlers = new WorkHandler[builder.threads];
        //循环填充
        for (int i = 0; i < workHandlers.length; i++) {
            workHandlers[i] = new HolderWorkHandler();
        }

        //给消费者构建线程池
        WorkerPool<Holder> workerPool = new WorkerPool<>(ringBuffer,
                sequenceBarrier,
                new HolderExceptionHandler(),
                workHandlers);

        //设置消费者的Sequence序号
        ringBuffer.addGatingSequences(workerPool.getWorkerSequences());


        this.workerPool = workerPool;
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

        public ParallelQueueHandler<E> build() {
            return new ParallelQueueHandler<E>(this);
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

    private class HolderEventTranslator implements EventTranslatorOneArg<Holder, E> {
        @Override
        public void translateTo(Holder holder, long l, E e) {
            holder.setEvent(e);
        }
    }

    private class HolderEventFactory implements EventFactory<Holder> {
        @Override
        public Holder newInstance() {
            return new Holder();
        }
    }

    //消费者
    private class HolderWorkHandler implements WorkHandler<Holder> {
        //事件发生，调用eventListener
        @Override
        public void onEvent(Holder holder) throws Exception {
            eventListener.onEvent(holder.event);
            holder.setEvent(null);
        }
    }

    private class HolderExceptionHandler implements ExceptionHandler<Holder> {
        @Override
        public void handleEventException(Throwable throwable, long l, Holder holder) {
            try {
                //调用eventListener
                eventListener.onException(throwable, l, holder.event);
            } catch (Exception e) {

            } finally {
                holder.setEvent(null);
            }

        }

        @Override
        public void handleOnStartException(Throwable throwable) {
            throw new UnsupportedOperationException(throwable);
        }

        @Override
        public void handleOnShutdownException(Throwable throwable) {
            throw new UnsupportedOperationException(throwable);
        }
    }
}
