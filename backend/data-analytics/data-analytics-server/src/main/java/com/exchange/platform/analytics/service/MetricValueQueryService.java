package com.exchange.platform.analytics.service;

import java.util.List;

import com.exchange.platform.analytics.api.request.MetricQueryRequest;
import com.exchange.platform.analytics.api.response.MetricValueView;

/** 面向后续筛选、排行和对比页面的统一指标读取入口。 */
public interface MetricValueQueryService {
    List<MetricValueView> queryMetricValues(MetricQueryRequest request);
}
