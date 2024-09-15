package org.paul.core.netty.processor;

import org.paul.core.context.HttpRequestWrapper;

public interface NettyProcessor {
    // 核心处理器，定义方法：入参：包装器
    void process(HttpRequestWrapper httpRequestWrapper);

}
