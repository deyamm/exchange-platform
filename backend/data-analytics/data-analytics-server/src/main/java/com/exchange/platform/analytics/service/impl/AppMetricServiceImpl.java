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
import com.exchange.platform.analytics.api.request.AppMetricSaveRequest;
import com.exchange.platform.analytics.api.response.AppMetricView;
import com.exchange.platform.analytics.converter.MetricConverter;
import com.exchange.platform.analytics.entity.AppMetric;
import com.exchange.platform.analytics.mapper.MetricMapper;
import com.exchange.platform.analytics.service.AppMetricService;
import com.exchange.platform.common.core.page.PageResult;


@Service
public class AppMetricServiceImpl extends ServiceImpl<MetricMapper, AppMetric> implements AppMetricService {

    private static final Logger log = LoggerFactory.getLogger(AppMetricServiceImpl.class);

    @Override
    @Transactional(readOnly = true)
    public PageResult<AppMetricView> getMetricList(String metricName, String metricCode, String metricCategory, String dataSourceCode, CommonStatus status, Integer pageNo, Integer pageSize) {

        log.info("[Service] getMetricList metricName={}, metricCode={}, metricCategory={}, dataSourceCode={}, status={}, pageNo={}, pageSize={}", metricName, metricCode, metricCategory, dataSourceCode, status, pageNo, pageSize);

        Integer safePageNo = (pageNo == null || pageNo < 1) ? 1 : pageNo;
        Integer safePageSize = (pageSize == null || pageSize < 1) ? 20 : Math.min(200, pageSize);

        LambdaQueryWrapper<AppMetric> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(metricName), AppMetric::getMetricName, metricName)
            .eq(StringUtils.hasText(metricCode), AppMetric::getMetricCode, metricCode)
            .eq(StringUtils.hasText(metricCategory), AppMetric::getMetricCategory, metricCategory)
            .eq(StringUtils.hasText(dataSourceCode), AppMetric::getDataSourceCode, dataSourceCode)
            .eq(status != null, AppMetric::getStatus, status);

        Page<AppMetric> page = this.page(new Page<>(safePageNo, safePageSize), wrapper);
        List<AppMetricView> viewList = page.getRecords().stream()
            .map(MetricConverter::toMetricView)
            .collect(Collectors.toList());
        return new PageResult<>(safePageNo, safePageSize, page.getTotal(), viewList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppMetricView saveOrUpdateMetric(AppMetricSaveRequest request) {
        log.info("[Service] saveOrUpdateMetric request={}", request);

        AppMetric entity = this.getOne(
            new LambdaQueryWrapper<AppMetric>()
                .eq(AppMetric::getMetricCode, request.metricCode())
        );

        if (entity == null) {
            entity = new AppMetric();
            entity.setMetricCode(request.metricCode());
        }
        entity.setMetricName(request.metricName())
            .setMetricCategory(request.metricCategory())
            .setUnit(request.unit())
            .setDisplayFormat(request.displayFormat())
            .setDataSourceCode(request.dataSourceCode())
            .setCalcPeriod(request.calcPeriod())
            .setSceneCodesJson(JSON.toJSONString(request.sceneCodes()))
            .setStatus(request.status())
            .setCaliberDesc(request.caliberDesc());

        this.saveOrUpdate(entity);
        return MetricConverter.toMetricView(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMetric(String metricCode) {
        log.info("[Service] deleteMetric metricCode={}", metricCode);

        this.remove(new LambdaQueryWrapper<AppMetric>().eq(AppMetric::getMetricCode, metricCode));
    }
}