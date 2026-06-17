package com.exchange.platform.analytics.api.response;

import java.time.LocalDateTime;

import com.exchange.platform.analytics.api.enums.ResultType;

import lombok.Getter;
import lombok.Setter;

/**
 * 应用结果概要视图。
 */
@Getter
@Setter
public class AppResultView {

    private String resultId;
    private ResultType resultType;
    private String sceneCode;
    private String targetCode;
    private String metricCode;
    private String relatedObjectType;
    private String relatedObjectId;
    /** 结果摘要，已由 JSON 字符串解析为对象 */
    private Object resultSummary;
    private LocalDateTime sourceDataTime;
    private LocalDateTime createdAt;
}
