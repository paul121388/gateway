package org.paul.core.disruptor;

/**
 * 多生产者多消费者处理接口
 */
public interface ParallelQueue<E> {

    /**
     * 添加元素
     * @param event
     */
    void add(E event);
    void add(E... event);

    /**
     * 判断是否添加成功
     * @param event
     * @return
     */
    boolean tryAdd(E event);
    boolean tryAdd(E... event);

    /**
     * 启动
     */
    void start();

    /**
     * 销毁
     */
    void shutDown();

    /**
     * 判断是否已经被销毁
     * @return
     */
    boolean isShutdown();
}
