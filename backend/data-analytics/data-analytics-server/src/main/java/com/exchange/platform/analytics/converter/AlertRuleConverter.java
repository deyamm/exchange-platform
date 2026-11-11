package com.exchange.platform.analytics.converter;

import com.alibaba.fastjson.JSON;
import com.exchange.platform.analytics.api.response.AlertRuleView;
import com.exchange.platform.analytics.entity.AlertRule;
import com.exchange.platform.common.core.enums.AssetType;
import java.math.BigDecimal;
import java.util.Map;


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
        view.setTargetType(AssetType.fromCode(entity.getTargetType()));
        view.setGroupCode(entity.getGroupCode());
        view.setMetricCode(entity.getMetricCode());
        Object condition = JSON.parse(entity.getConditionJson());
        view.setCondition(condition);
        if (condition instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) condition;
            Object operator = map.get("operator");
            if (operator == null) operator = map.get("compareOperator");
            view.setOperator(operator == null ? null : String.valueOf(operator));
            Object threshold = map.get("threshold");
            if (threshold == null) threshold = map.get("thresholdValue");
            if (threshold != null) try { view.setThreshold(new BigDecimal(String.valueOf(threshold))); } catch (NumberFormatException ignored) { }
        }
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
