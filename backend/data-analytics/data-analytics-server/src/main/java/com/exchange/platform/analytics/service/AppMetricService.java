package com.exchange.platform.analytics.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exchange.platform.analytics.api.enums.CommonStatus;
import com.exchange.platform.analytics.api.request.AppMetricSaveRequest;
import com.exchange.platform.analytics.api.response.AppMetricView;
import com.exchange.platform.analytics.entity.AppMetric;
import com.exchange.platform.common.core.page.PageResult;


public interface AppMetricService extends IService<AppMetric> {

    PageResult<AppMetricView> getMetricList(String metricName, String metricCode, String metricCategory, String dataSourceCode, CommonStatus status, Integer pageNo, Integer pageSize);

    AppMetricView saveOrUpdateMetric(AppMetricSaveRequest request);

    void deleteMetric(String metricCode);
}