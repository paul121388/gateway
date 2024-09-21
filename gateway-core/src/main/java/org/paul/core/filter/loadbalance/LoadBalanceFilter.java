package org.paul.core.filter.loadbalance;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.paul.common.config.Rule;
import org.paul.common.config.ServiceInstance;
import org.paul.common.enums.ResponseCode;
import org.paul.common.exception.NotFoundException;
import org.paul.core.context.GatewayContext;
import org.paul.core.filter.Filter;
import org.paul.core.filter.FilterAspect;
import org.paul.core.request.GatewayRequest;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.paul.common.constants.FilterConst.*;

/**
 * 负载均衡过滤器
 */
@Slf4j
@FilterAspect(id = Load_BALANCE_FILTER_ID,
        name = Load_BALANCE_FILTER_NAME,
        order = Load_BALANCE_FILTER_ORDER)
public class LoadBalanceFilter implements Filter {
    @Override
    public void doFilter(GatewayContext ctx) throws Exception {
        //先拿到服务id，再获取对应的负载均衡rule
        String serviceId = ctx.getUniqueId();
        IGatewayLoadBalanceRule gatewayLoadBalanceRule = getLoadBalanceRule(ctx);


        //根据上面的rule获取对应的服务器，设置GatewayContext的modifyHost，用于构建发向下游的请求
        ServiceInstance serviceInstance = gatewayLoadBalanceRule.choose(serviceId);

        //获取发向网关的请求
        GatewayRequest request = ctx.getRequest();

        //判断GatewayRequest和ServiceInstance都不为空，才去构建发向下游服务的request
        if(serviceInstance != null && request != null){
            String modifyHost = serviceInstance.getIp() + ": "+ serviceInstance.getPort();
            request.setModifyHost(modifyHost);
        }else{
            log.warn("No instance available for: {}", serviceId);
            throw new NotFoundException(ResponseCode.SERVICE_INSTANCE_NOT_FOUND);
        }

    }

    /**
     * 根据配置获取负载均衡器
     * @param ctx
     * @return
     */
    private IGatewayLoadBalanceRule getLoadBalanceRule(GatewayContext ctx) {
        IGatewayLoadBalanceRule gatewayLoadBalanceRule = null;
        Rule configRule = ctx.getRule();
        if(configRule != null){
            //遍历所有rule中的过滤器配置，从中根据id获取负载均衡过滤器
            Set<Rule.FilterConfig> filterConfigs = configRule.getFilterConfigs();
            Rule.FilterConfig filterConfig;
            Iterator iterator = filterConfigs.iterator();
            while(iterator.hasNext()){
                filterConfig = (Rule.FilterConfig)iterator.next();

                //如果过滤器的配置为空，直接跳过
                if(filterConfig == null){
                    continue;
                }

                //判断过滤器是不是负载均衡过滤器
                if(filterConfig.getId().equals(Load_BALANCE_FILTER_ID)){
                    String config = filterConfig.getConfig();

                    //负载均衡策略：随机/轮询，默认为随机
                    String strategy = LOAD_BALANCE_STRATEGY_RANDOM;

                    //判断config配置是否为空，不为空就将里面配置使用JSON工具转化为Map
                    if(StringUtils.isNotEmpty(config)){
                        Map<String, String> mapTypeMap = JSON.parseObject(config, Map.class);
                        //获取strategy
                        strategy = mapTypeMap.getOrDefault(LOAD_BALANCE_KEY, strategy);
                    }
                    switch (strategy){
                        case LOAD_BALANCE_STRATEGY_RANDOM:
                            gatewayLoadBalanceRule = new RandomLoadBalanceRule(ctx.getUniqueId());
                            break;
                        case LOAD_BALANCE_STRATEGY_ROUND_ROBIN:
                            gatewayLoadBalanceRule = new RoundRobinLoadBalanceRule(new AtomicInteger(1), ctx.getUniqueId());
                            break;
                        default:
                            break;
                    }
                }
            }
        }
        return gatewayLoadBalanceRule;
    }
}
