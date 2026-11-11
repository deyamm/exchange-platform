package com.exchange.platform.analytics.api.response;

import lombok.Data;

/** 统一指标返回值；value 为 null 明确表示缺失，dataTime 始终保留来源时间。 */
@Data
public class MetricValueView {
    private String targetType;
    private String targetCode;
    private String metricCode;
    private Object value;
    /** ISO 8601 格式的源数据时间；缺失记录时为 null。 */
    private String dataTime;
}
