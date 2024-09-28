package org.paul.core.context;

import io.micrometer.core.instrument.Timer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import lombok.Getter;
import lombok.Setter;
import org.paul.common.utils.AssertUtil;
import org.paul.core.response.GatewayResponse;
import org.paul.core.request.GatewayRequest;
import org.paul.common.config.Rule;

/**
 * @PROJECT_NAME: api-gateway
 * @DESCRIPTION: 核心上下文基础类
 */
public class GatewayContext extends BaseContext {
    // 请求体
    public GatewayRequest request;

    // 响应体
    public GatewayResponse response;

    // 规则
    public Rule rule;

    //当前重试次数
    private int currentRetryTimes;

    //当前是否为灰度请求
    @Getter
    @Setter
    private boolean gray;

    //普罗米修斯工具包中提供了时间采集指标的相关类
    @Getter
    @Setter
    private Timer.Sample timeSample;

    public GatewayContext(String protocol, boolean keepAlive, ChannelHandlerContext nettyCtx, GatewayRequest request, Rule rule, int currentRetryTimes) {
        super(protocol, keepAlive, nettyCtx);
        this.request = request;
        this.rule = rule;
        this.currentRetryTimes = currentRetryTimes;
    }

    // 建造者类
    public static class Builder {
        // 定义协议
        // netty上下文
        // 网关请求对象
        // 规则
        // 是否长连接
        private String protocol;
        private ChannelHandlerContext nettyCtx;
        private GatewayRequest request;
        private Rule rule;
        private boolean keepAlive;

        // 提供对应的get set方法，返回builder本身

        public Builder getProtocol() {
            this.protocol = protocol;
            return this;
        }

        public Builder getNettyCtx() {
            this.nettyCtx = nettyCtx;
            return this;
        }

        public Builder getRequest() {
            this.request = request;
            return this;
        }

        public Builder getRule() {
            this.rule = rule;
            return this;
        }

        public Builder isKeepAlive() {
            this.keepAlive = keepAlive;
            return this;
        }


        // GatewayContext构建方法
        public GatewayContext build() {
            // protocol，nettyCtx，request，rule不能为空
            AssertUtil.notNull(protocol, "protocol 不能为空");
            AssertUtil.notNull(request, "request 不能为空");
            AssertUtil.notNull(nettyCtx, "nettyCtx 不能为空");
            AssertUtil.notNull(rule, "rule 不能为空");
            // 构建GatewayContext对象
            return new GatewayContext(protocol, keepAlive, nettyCtx, request, rule, 0);
        }
    }


    // 根据key获取必要的上下文参数
    public <T> T getRequireAttribute(String key) {
        T value = getAttribute(key);
        AssertUtil.notNull(value, "缺乏必要参数");
        return value;
    }

    // 获取指定key的上下文参数，如果没有返回默认值
    public <T> T getOptionalAttribute(String key, T defaultValue) {
        return (T) attributes.getOrDefault(key, defaultValue);
    }

    // 获取filterId指定的过滤器配置信息
    public Rule.FilterConfig getFilterConfig(String filterId) {
        return rule.getFilterConfig(filterId);
    }

    // 获取服务唯一Id，针对一个请求，服务唯一Id相同
    public String getUniqueId() {
        return request.getUniqueId();
    }

    // 重写父类，释放资源
    public void releaseRequest() {
        // 判断是否释放，使用CAS
        if (requestReleased.compareAndSet(false, true)) {
            // 释放原始请求对象，使用netty自带的工具类
            ReferenceCountUtil.release(request.getFullHttpRequest());
        }
    }

    // 获取原始请求对象
    public GatewayRequest getOriginRequest() {
        return request;
    }

    // 本身类的get set方法
    @Override
    public GatewayRequest getRequest() {
        return request;
    }

    public void setRequest(GatewayRequest request) {
        this.request = request;
    }

    @Override
    public GatewayResponse getResponse() {
        return response;
    }

    public void setResponse(Object response) {
        this.response = (GatewayResponse) response;
    }

    public Rule getRule() {
        return rule;
    }

    public void setRule(Rule rule) {
        this.rule = rule;
    }

    public int getCurrentRetryTimes() {
        return currentRetryTimes;
    }

    public void setCurrentRetryTimes(int currentRetryTimes) {
        this.currentRetryTimes = currentRetryTimes;
    }
}
