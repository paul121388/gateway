package org.paul.core.disruptor;

/**
 * 监听器的接口
 */
public interface EventListener<E> {

    //正常处理
    void onEvent(E event);

    //异常处理，sequence是RingBuffer中的次序
    void onException(Throwable ex, long sequence, E event);
}
