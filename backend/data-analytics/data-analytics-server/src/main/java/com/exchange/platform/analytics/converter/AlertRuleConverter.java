package com.exchange.platform.analytics.converter;

import com.alibaba.fastjson.JSON;
import com.exchange.platform.analytics.api.response.AlertRuleView;
import com.exchange.platform.analytics.entity.AlertRule;


public class AlertRuleConverter {

    private AlertRuleConverter() {
    }

    public static AlertRuleView toAlertRuleView(AlertRule entity) {
        if (entity == null) {
            return null;
        }
        AlertRuleView view = new AlertRuleView();
        view.setRuleCode(entity.getRuleCode());
        view.setRuleName(entity.getRuleName());
        view.setRuleType(entity.getRuleType());
        view.setTargetCode(entity.getTargetCode());
        view.setGroupCode(entity.getGroupCode());
        view.setMetricCode(entity.getMetricCode());
        view.setCondition(JSON.parse(entity.getConditionJson()));
        view.setNoticeChannels(JSON.parse(entity.getNoticeChannelsJson()));
        view.setEffectiveStartTime(entity.getEffectiveStartTime());
        view.setEffectiveEndTime(entity.getEffectiveEndTime());
        view.setStatus(entity.getStatus());
        view.setDescription(entity.getDescription());
        view.setCreatedAt(entity.getCreatedAt());
        view.setUpdatedAt(entity.getUpdatedAt());
        return view;
    }
}