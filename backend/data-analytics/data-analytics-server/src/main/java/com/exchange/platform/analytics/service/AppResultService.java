package com.exchange.platform.analytics.service;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exchange.platform.analytics.api.enums.ResultType;
import com.exchange.platform.analytics.api.response.AppResultView;
import com.exchange.platform.analytics.entity.AppResult;
import com.exchange.platform.common.core.page.PageResult;

public interface AppResultService extends IService<AppResult> {
    /** 按结果类型、标的、场景、时间范围分页查询应用结果 */
    PageResult<AppResultView> pageResults(ResultType resultType, String targetCode, String sceneCode, String metricCode, LocalDateTime startTime, LocalDateTime endTime, Integer pageNo, Integer pageSize);

    /** 查询某一类型下指定标的的最新一条应用结果 */
    AppResultView getLatest(ResultType resultType, String targetCode, String sceneCode);

    /** 归集一条应用结果（用于排行、对比等执行类接口） */
    AppResultView saveResult(ResultType resultType, String sceneCode, String targetCode, String metricCode, String relatedObjectType, String relatedObjectId, Object resultSummary, LocalDateTime sourceDataTime);


}
