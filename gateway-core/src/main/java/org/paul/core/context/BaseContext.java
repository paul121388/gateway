package org.paul.core.context;

import io.netty.channel.ChannelHandlerContext;

import javax.naming.Context;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * @PROJECT_NAME: api-gateway
 * @DESCRIPTION: 核心上下文基础类
 */
public class BaseContext implements IContext {
    // 转发协议
    protected final String protocol;

    // 状态，多线程考虑使用volatile
    protected volatile int status = IContext.Running;

    // Netty上下文
    protected final ChannelHandlerContext nettyCtx;

    // 请求处理过程中存储和传递额外信息
    protected final Map<String, Object> attributes = new HashMap<String, Object>();

    // 请求过程中发生的异常
    protected Throwable throwable;

    // 是否长连接
    protected final boolean keepAlive;

    // 存放回调函数集合
    protected List<Consumer<IContext>> completedCallbacks;

    // 资源是否已经释放
    protected final AtomicBoolean requestReleased = new AtomicBoolean(false);

    public BaseContext(String protocol, boolean keepAlive, ChannelHandlerContext nettyCtx) {
        this.protocol = protocol;
        this.keepAlive = keepAlive;
        this.nettyCtx = nettyCtx;
    }

    @Override
    public void runned() {
        this.status = IContext.Running;
    }

    @Override
    public void writtened() {
        this.status = IContext.Written;
    }

    @Override
    public void completed() {
        this.status = IContext.Completed;
    }

    @Override
    public void terminated() {
        this.status = IContext.Terminated;
    }

    @Override
    public boolean isRunning() {
        return this.status == Running;
    }

    @Override
    public boolean isWritten() {
        return this.status == Written;
    }

    @Override
    public boolean isCompleted() {
        return this.status == Completed;
    }

    @Override
    public boolean isTerminted() {
        return this.status == Terminated;
    }

    @Override
    public String getProtocol() {
        return this.protocol;
    }

    @Override
    public Object getRequest() {
        return null;
    }

    @Override
    public Object getResponse() {
        return null;
    }

    @Override
    public void setResponse(Object response) {

    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T putAttribute(String key, T value) {
        return (T) attributes.put(key, value);
    }

    @Override
    public Throwable getThrowable() {
        return this.throwable;
    }

    @Override
    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }

    @Override
    public ChannelHandlerContext getNettyCtx() {
        return this.nettyCtx;
    }

    @Override
    public boolean isKeepAlive() {
        return this.keepAlive;
    }

    @Override
    public void releaseRequest() {
        this.requestReleased.compareAndSet(false, true);
    }

    @Override
    public void setcompletedCallBack(Consumer<IContext> consumer) {
        if (this.completedCallbacks == null) {
            this.completedCallbacks = new ArrayList<>();
        }
        this.completedCallbacks.add(consumer);
    }

    @Override
    public void invokeCompletedCallBack() {
        if (this.completedCallbacks != null) {
            for (Consumer<IContext> consumer : this.completedCallbacks) {
                consumer.accept(this);
            }
        }
    }
}
