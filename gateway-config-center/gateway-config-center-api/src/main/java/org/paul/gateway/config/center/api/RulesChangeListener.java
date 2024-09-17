package org.paul.gateway.config.center.api;

import org.paul.common.config.Rule;

import java.util.List;

public interface RulesChangeListener {
    void onRulesChanged(List<Rule> ruleList);
}
