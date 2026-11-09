package com.exchange.platform.analytics.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exchange.platform.analytics.api.enums.CommonStatus;
import com.exchange.platform.analytics.api.request.AppMetricSaveRequest;
import com.exchange.platform.analytics.api.response.AppMetricView;
import com.exchange.platform.analytics.entity.AppMetric;
import com.exchange.platform.common.core.page.PageResult;
import com.exchange.platform.common.core.enums.AssetType;


public interface AppMetricService extends IService<AppMetric> {

    PageResult<AppMetricView> getMetricList(String keyword, String metricCategory, AssetType targetType, CommonStatus status, Integer pageNo, Integer pageSize);

    AppMetricView saveOrUpdateMetric(AppMetricSaveRequest request);

    /** 仅允许通过此入口启停，停用时会在同一事务内执行引用校验。 */
    void updateMetricStatus(String metricCode, CommonStatus status);

    void deleteMetric(String metricCode);
}
