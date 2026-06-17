package com.exchange.platform.analytics.service.impl;

import java.time.LocalDateTime;
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
import com.exchange.platform.analytics.api.enums.ResultType;
import com.exchange.platform.analytics.api.response.AppResultView;
import com.exchange.platform.analytics.converter.AppResultConverter;
import com.exchange.platform.analytics.entity.AppResult;
import com.exchange.platform.analytics.mapper.AppResultMapper;
import com.exchange.platform.analytics.service.AppResultService;
import com.exchange.platform.common.core.page.PageResult;

import cn.hutool.core.util.IdUtil;

@Service
public class AppResultServiceImpl extends ServiceImpl<AppResultMapper, AppResult> implements AppResultService {
    
    private static final Logger log = LoggerFactory.getLogger(AppResultServiceImpl.class);

    @Override
    @Transactional(readOnly = true)
    public PageResult<AppResultView> pageResults(ResultType resultType, String targetCode, String sceneCode,
            String metricCode, LocalDateTime startTime, LocalDateTime endTime, Integer pageNo, Integer pageSize) {

        log.info("[Service] pageResults resultType={}, targetCode={}, sceneCode={}, metricCode={}",
                resultType, targetCode, sceneCode, metricCode);

        LambdaQueryWrapper<AppResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(resultType != null, AppResult::getResultType, resultType)
                .eq(StringUtils.hasText(targetCode), AppResult::getTargetCode, targetCode)
                .eq(StringUtils.hasText(sceneCode), AppResult::getSceneCode, sceneCode)
                .eq(StringUtils.hasText(metricCode), AppResult::getMetricCode, metricCode)
                .ge(startTime != null, AppResult::getCreatedAt, startTime)
                .le(endTime != null, AppResult::getCreatedAt, endTime)
                .orderByDesc(AppResult::getCreatedAt);

        int safePageNo = (pageNo == null || pageNo < 1) ? 1 : pageNo;
        int safePageSize = (pageSize == null || pageSize < 1) ? 20 : Math.min(200, pageSize);

        Page<AppResult> page = this.page(new Page<>(safePageNo, safePageSize), wrapper);

        List<AppResultView> views = page.getRecords().stream()
                .map(AppResultConverter::toView)
                .collect(Collectors.toList());
        return new PageResult<>(safePageNo, safePageSize, page.getTotal(), views);
    }

    @Override
    @Transactional(readOnly = true)
    public AppResultView getLatest(ResultType resultType, String targetCode, String sceneCode) {
        LambdaQueryWrapper<AppResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(resultType != null, AppResult::getResultType, resultType)
                .eq(StringUtils.hasText(targetCode), AppResult::getTargetCode, targetCode)
                .eq(StringUtils.hasText(sceneCode), AppResult::getSceneCode, sceneCode)
                .orderByDesc(AppResult::getCreatedAt)
                .last("LIMIT 1");
        return AppResultConverter.toView(this.getOne(wrapper));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppResultView saveResult(ResultType resultType, String sceneCode, String targetCode,
            String metricCode, String relatedObjectType, String relatedObjectId, Object resultSummary,
            LocalDateTime sourceDataTime) {

        AppResult result = new AppResult()
                .setResultId("AR" + IdUtil.getSnowflakeNextIdStr())
                .setResultType(resultType)
                .setSceneCode(sceneCode)
                .setTargetCode(targetCode)
                .setMetricCode(metricCode)
                .setRelatedObjectType(relatedObjectType)
                .setRelatedObjectId(relatedObjectId)
                .setResultSummaryJson(JSON.toJSONString(resultSummary))
                .setSourceDataTime(sourceDataTime == null ? LocalDateTime.now() : sourceDataTime);

        this.save(result);
        log.info("[Service] saveResult saved resultId={}", result.getResultId());
        return AppResultConverter.toView(result);
    }
}
