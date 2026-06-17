package com.exchange.platform.analytics.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exchange.platform.analytics.api.enums.CommonStatus;
import com.exchange.platform.analytics.api.enums.RuleType;
import com.exchange.platform.analytics.api.request.AlertRuleSaveRequest;
import com.exchange.platform.analytics.api.response.AlertRuleView;
import com.exchange.platform.analytics.converter.AlertRuleConverter;
import com.exchange.platform.analytics.entity.AlertRule;
import com.exchange.platform.analytics.mapper.AlertRuleMapper;
import com.exchange.platform.analytics.service.AlertRuleService;
import com.exchange.platform.common.core.page.PageResult;


@Service
public class AlertRuleServiceImpl extends ServiceImpl<AlertRuleMapper, AlertRule> implements AlertRuleService {

    private static final Logger log = LoggerFactory.getLogger(AlertRuleServiceImpl.class);

    @Override
    @Transactional(readOnly = true)
    public PageResult<AlertRuleView> getAlertRuleList(String ruleName, String ruleCode, RuleType ruleType, String targetCode, String groupCode, String metricCode, CommonStatus status, Integer pageNo, Integer pageSize) {
        log.info("[Service] getAlertRuleList ruleName={}, ruleCode={}, ruleType={}, targetCode={}, groupCode={}, metricCode={}, status={}, pageNo={}, pageSize={}", ruleName, ruleCode, ruleType, targetCode, groupCode, metricCode, status, pageNo, pageSize);

        Integer safePageNo = (pageNo == null || pageNo < 1) ? 1 : pageNo;
        Integer safePageSize = (pageSize == null || pageSize < 1) ? 20 : Math.min(200, pageSize);

        LambdaQueryWrapper<AlertRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(ruleName), AlertRule::getRuleName, ruleName)
            .eq(StringUtils.hasText(ruleCode), AlertRule::getRuleCode, ruleCode)
            .eq(ruleType != null, AlertRule::getRuleType, ruleType)
            .eq(StringUtils.hasText(targetCode), AlertRule::getTargetCode, targetCode)
            .eq(StringUtils.hasText(groupCode), AlertRule::getGroupCode, groupCode)
            .eq(StringUtils.hasText(metricCode), AlertRule::getMetricCode, metricCode)
            .eq(status != null, AlertRule::getStatus, status);

        Page<AlertRule> page = this.page(new Page<>(safePageNo, safePageSize), wrapper);
        List<AlertRuleView> viewList = page.getRecords().stream()
            .map(AlertRuleConverter::toAlertRuleView)
            .collect(Collectors.toList());
        return new PageResult<>(safePageNo, safePageSize, page.getTotal(), viewList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AlertRuleView saveOrUpdateAlertRule(AlertRuleSaveRequest request) {
        log.info("[Service] saveOrUpdateAlertRule request={}", request);

        AlertRule entity = this.getOne(
            new LambdaQueryWrapper<AlertRule>()
                .eq(AlertRule::getRuleCode, request.ruleCode())
        );

        if (entity == null) {
            entity = new AlertRule();
            entity.setRuleCode(request.ruleCode());
        }

        entity.setRuleName(request.ruleName())
            .setRuleType(request.ruleType())
            .setTargetCode(request.targetCode())
            .setGroupCode(request.groupCode())
            .setMetricCode(request.metricCode())
            .setConditionJson(JSON.toJSONString(request.condition()))
            .setNoticeChannelsJson(JSON.toJSONString(request.noticeChannels()))
            .setEffectiveStartTime(request.effectiveStartTime())
            .setEffectiveEndTime(request.effectiveEndTime())
            .setStatus(request.status())
            .setDescription(request.description());

        this.saveOrUpdate(entity);
        return AlertRuleConverter.toAlertRuleView(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAlertRule(String ruleCode) {
        log.info("[Service] deleteAlertRule ruleCode={}", ruleCode);

        this.remove(new LambdaQueryWrapper<AlertRule>().eq(AlertRule::getRuleCode, ruleCode));
    }
}