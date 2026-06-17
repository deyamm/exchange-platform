package com.exchange.platform.analytics.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exchange.platform.analytics.api.enums.CommonStatus;
import com.exchange.platform.analytics.api.enums.RuleType;
import com.exchange.platform.analytics.api.request.AlertRuleSaveRequest;
import com.exchange.platform.analytics.api.response.AlertRuleView;
import com.exchange.platform.analytics.entity.AlertRule;
import com.exchange.platform.common.core.page.PageResult;


public interface AlertRuleService extends IService<AlertRule> {

    PageResult<AlertRuleView> getAlertRuleList(String ruleName, String ruleCode, RuleType ruleType, String targetCode, String groupCode, String metricCode, CommonStatus status, Integer pageNo, Integer pageSize);

    AlertRuleView saveOrUpdateAlertRule(AlertRuleSaveRequest request);

    void deleteAlertRule(String ruleCode);
}