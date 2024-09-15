package org.paul.core;

/**
 * 定义组件的生命周期
 */
public interface LifeCycle {
    /**
     * 初始化
     */
    void init();

    /**
     * 启动
     */

    void start();

    /**
     * 关闭
     */

    void shutdown();
}
