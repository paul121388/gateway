package org.paul.core.netty.processor;

import org.paul.core.context.HttpRequestWrapper;

public class NettyCoreProcessor implements NettyProcessor{
    @Override
    public void process(HttpRequestWrapper httpRequestWrapper) {
        /**
         * 从httpRequestWrapper中获取参数：完整的httpRequest和context
         *
         * try
         *      转换为内部GatewayContext对象
         *      路由转发
         * catch已知异常
         *      日志，code和message
         *      根据辅助类，获取响应结果
         *      写入并释放doWriteAndRelease
         * catch处理未知异常
         *      打印日志
         *      获取响应结果，指定code
         *      写入并释放doWriteAndRelease
         */

        /**
         * doWriteAndRelease
         *  写入response，添加监听器，由监听器关闭channel
         *  调用ReferenceCountUtil，释放request中的缓冲
         */

        /**
         * 路由函数
         *      获取request对象（真正发送时使用
         *      调用自定义AsyncHttpHelper，首先获取实例，然后执行request，返回Future对象
         *
         *      拿到配置：通过configLoader获取config的单双异步配置信息
         *      如果单异步
         *          调用future的whencomplete，传入闭包，参数时response，throwable
         *              封装方法complete，参数：请求，响应，throwable，gatewaycontext上下文
         *      双异步类似
         *          闭包逻辑一致，改为异步调用whencomplete异步方法
         */

        /**
         * complete方法：参数：请求，响应，throwable，gatewayContext上下文
         *      释放资源
         *      try
         *          判断是否由异常
         *          获取url
         *              对异常进行处理
         *                  超时异常：打印日志（url），设置gatewayContext的throwable，记录异常code
         *                  如果时其他异常，记录必要信息：唯一id，url，响应码
         *          没有异常
         *              正常响应
         *       catch
         *          在gatewayContext中记录信息，打印日志
         *       finally
         *          修改gatewayContext的状态
         *          调用辅助类responseHelper，写回数据
         */
    }
}
