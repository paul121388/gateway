package org.paul.core.filter.flowCtl;

import lombok.extern.slf4j.Slf4j;
import org.paul.common.config.Rule;
import org.paul.core.context.GatewayContext;
import org.paul.core.filter.Filter;
import org.paul.core.filter.FilterAspect;

import java.util.Iterator;
import java.util.Set;

import static org.paul.common.constants.FilterConst.*;

/**
 * 限流过滤器，一般的算法实现有漏桶算法和令牌桶
 */
@Slf4j
@FilterAspect(id = FLOW_CTL_FILTER_ID,
        name = FLOW_CTL_FILTER_NAME,
        order = FLOW_CTL_FILTER_ORDER)
public class FlowCtlFilter implements Filter {
    @Override
    public void doFilter(GatewayContext ctx) throws Exception {
        //从上下文获取rule，如果不为空，拿到流控过滤器的配置集合
        Rule rule = ctx.getRule();
        if(rule != null){
            //使用迭代器遍历上述集合，如果为空，就迭代下一个
            Set<Rule.FlowCtlConfig> flowCtlConfigs = rule.getFlowCtlConfigs();
            Iterator iterator = flowCtlConfigs.iterator();
            Rule.FlowCtlConfig flowCtlConfig;
            while (iterator.hasNext()){
                flowCtlConfig = (Rule.FlowCtlConfig)iterator.next();
                if(flowCtlConfig == null){
                    continue;
                }
                //限流规则变量，在下面根据限流类型设置不同的限流规则
                IGatewayFlowCtlRule flowCtlRule = null;
                //获取请求路径path
                String path = ctx.getRequest().getPath();
                //如果限流类型是根据path限流，且上下文ctx中的path和配置中的path相同，需要执行限流
                if(flowCtlConfig.getType().equalsIgnoreCase(FLOW_CTL_TYPE_PATH) && path.equals(flowCtlConfig.getValue())){
                    //获取对应的流控规则
                    flowCtlRule = FlowCtlByPathRule.getInstance(rule.getServiceId(), path);
                } //如果限流类型是根据服务限流
                else if (flowCtlConfig.getType().equalsIgnoreCase(FLOW_CTL_TYPE_SERVICE)){
                    //todo 获取对应的流控规则
                }


                //如果流控规则不为空，执行对应的流控方法，传入对应的流控规则+服务id
                if(flowCtlRule != null){
                    flowCtlRule.doFlowCtlFilter(flowCtlConfig, rule.getServiceId());
                }
            }


        }




    }
}
