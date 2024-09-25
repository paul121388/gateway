package org.paul.common.constants;

/**
 * 负载均衡常量类
 */
public interface FilterConst {
    String LOAD_BALANCE_FILTER_ID = "load_balance_filter";
    String LOAD_BALANCE_FILTER_NAME = "load_balance_filter";
    int LOAD_BALANCE_FILTER_ORDER = 100;

    String LOAD_BALANCE_STRATEGY_RANDOM = "random";
    String LOAD_BALANCE_STRATEGY_ROUND_ROBIN = "roundRobin";
    String LOAD_BALANCE_KEY = "load_balance";

    String ROUTER_FILTER_ID = "router_filter";
    String ROUTER_FILTER_NAME = "router_filter";
    int ROUTER_BALANCE_FILTER_ORDER = Integer.MAX_VALUE;
}
