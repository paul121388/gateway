package org.paul.common.config;

import org.checkerframework.checker.units.qual.A;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 动态服务缓存配置管理类
 */
public class DynamicConfigManager {

    //	服务的定义集合：uniqueId代表服务的唯一标识
    private ConcurrentHashMap<String /* uniqueId */ , ServiceDefinition>  serviceDefinitionMap = new ConcurrentHashMap<>();

    //	服务的实例集合：uniqueId与一对服务实例对应
    private ConcurrentHashMap<String /* uniqueId */ , Set<ServiceInstance>>  serviceInstanceMap = new ConcurrentHashMap<>();

    //	规则集合
    private ConcurrentHashMap<String /* ruleId */ , Rule>  ruleMap = new ConcurrentHashMap<>();

    //路径规则map
    private ConcurrentHashMap<String /* 路径 */, Rule> pathRuleMap = new ConcurrentHashMap<>();

    //服务id，规则的list map
    private ConcurrentHashMap<String /* 服务id */, List<Rule>> serviceIdRuleMap = new ConcurrentHashMap<>();

    private DynamicConfigManager() {
    }

    private static class SingletonHolder {
        private static final DynamicConfigManager INSTANCE = new DynamicConfigManager();
    }

    public static DynamicConfigManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /***************** 	对服务定义缓存进行操作的系列方法 	***************/
    public void putServiceDefinition(String uniqueId,
                                     ServiceDefinition serviceDefinition) {

        serviceDefinitionMap.put(uniqueId, serviceDefinition);;
    }

    public ServiceDefinition getServiceDefinition(String uniqueId) {
        return serviceDefinitionMap.get(uniqueId);
    }

    public void removeServiceDefinition(String uniqueId) {
        serviceDefinitionMap.remove(uniqueId);
    }

    public ConcurrentHashMap<String, ServiceDefinition> getServiceDefinitionMap() {
        return serviceDefinitionMap;
    }

    /***************** 	对服务实例缓存进行操作的系列方法 	***************/

    public Set<ServiceInstance> getServiceInstanceByUniqueId(String uniqueId){
        return serviceInstanceMap.get(uniqueId);
    }

    public void addServiceInstance(String uniqueId, ServiceInstance serviceInstance) {
        Set<ServiceInstance> set = serviceInstanceMap.get(uniqueId);
        set.add(serviceInstance);
    }

    public void addServiceInstance(String uniqueId, Set<ServiceInstance> serviceInstanceSet) {
        serviceInstanceMap.put(uniqueId, serviceInstanceSet);
    }

    public void updateServiceInstance(String uniqueId, ServiceInstance serviceInstance) {
        Set<ServiceInstance> set = serviceInstanceMap.get(uniqueId);
        Iterator<ServiceInstance> it = set.iterator();
        while(it.hasNext()) {
            ServiceInstance is = it.next();
            if(is.getServiceInstanceId().equals(serviceInstance.getServiceInstanceId())) {
                it.remove();
                break;
            }
        }
        set.add(serviceInstance);
    }

    public void removeServiceInstance(String uniqueId, String serviceInstanceId) {
        Set<ServiceInstance> set = serviceInstanceMap.get(uniqueId);
        Iterator<ServiceInstance> it = set.iterator();
        while(it.hasNext()) {
            ServiceInstance is = it.next();
            if(is.getServiceInstanceId().equals(serviceInstanceId)) {
                it.remove();
                break;
            }
        }
    }

    public void removeServiceInstancesByUniqueId(String uniqueId) {
        serviceInstanceMap.remove(uniqueId);
    }


    /***************** 	对规则缓存进行操作的系列方法 	***************/

    public void putRule(String ruleId, Rule rule) {
        ruleMap.put(ruleId, rule);
    }

    public void putAllRule(List<Rule> ruleList) {
        //规则id，规则的map
        ConcurrentHashMap<String /* ruleId */, Rule> newRuleMap = new ConcurrentHashMap<>();
        //路径规则map
        ConcurrentHashMap<String /* 路径 */, Rule> newPathRuleMap = new ConcurrentHashMap<>();
        //服务id，规则map
        ConcurrentHashMap<String /* 服务id */, List<Rule>> newServiceIdRuleMap = new ConcurrentHashMap<>();

        //将rule放入上述map中
        for(Rule rule: ruleList){
            //添加到ruleMap
            newRuleMap.put(rule.getId(), rule);

            //添加到serviceIdRuleMap
            List<Rule> rules = newServiceIdRuleMap.get(rule.getServiceId());
            if(rules == null){
                rules = new ArrayList<>();
            }
            rules.add(rule);
            newServiceIdRuleMap.put(rule.getServiceId(), rules);

            //添加到pathRuleMap
            List<String> paths = rule.getPaths();
            for(String path: paths){
                String key = rule.getServiceId() + "." + path;
                newPathRuleMap.put(key, rule);
            }
        }
        ruleMap = newRuleMap;
        pathRuleMap = newPathRuleMap;
        serviceIdRuleMap = newServiceIdRuleMap;
    }

    public Rule getRule(String ruleId) {
        return ruleMap.get(ruleId);
    }

    public void removeRule(String ruleId) {
        ruleMap.remove(ruleId);
    }

    public ConcurrentHashMap<String, Rule> getRuleMap() {
        return ruleMap;
    }

    /**
     * 根据path获取rule
     * @param path
     * @return
     */
    public Rule getRuleByPath(String path){
        return pathRuleMap.get(path);
    }

    public List<Rule> getRuleByServiceId(String serviceId){
        return serviceIdRuleMap.get(serviceId);
    }
}