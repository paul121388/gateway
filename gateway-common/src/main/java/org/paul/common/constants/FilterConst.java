package org.paul.common.constants;

/**
 * 负载均衡常量类
 */
public interface FilterConst {
    String LOAD_BALANCE_FILTER_ID = "load_balancer_filter";
    String LOAD_BALANCE_FILTER_NAME = "load_balancer_filter";
    int LOAD_BALANCE_FILTER_ORDER = 100;

    String LOAD_BALANCE_STRATEGY_RANDOM = "Random";
    String LOAD_BALANCE_STRATEGY_ROUND_ROBIN = "RoundRobin";
    String LOAD_BALANCE_KEY = "load_balance";

    String ROUTER_FILTER_ID = "router_filter";
    String ROUTER_FILTER_NAME = "router_filter";
    int ROUTER_BALANCE_FILTER_ORDER = Integer.MAX_VALUE;
}
