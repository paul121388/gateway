package org.paul.gateway.config.center.api;

import org.paul.common.config.Rule;

import java.util.List;

public interface RulesChangeListener {
    //规则变更时，触发这个方法的使用
    void onRulesChanged(List<Rule> ruleList);
}
