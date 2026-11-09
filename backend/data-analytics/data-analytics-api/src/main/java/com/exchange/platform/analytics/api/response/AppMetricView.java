package com.exchange.platform.analytics.api.response;

import java.time.LocalDateTime;
import com.exchange.platform.analytics.api.enums.CommonStatus;
import com.exchange.platform.common.core.enums.AssetType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppMetricView {

    private String metricCode;
    private String metricName;
    private String metricCategory;
    /** 用户定义的指标适用标的类型。 */
    private AssetType targetType;
    private String targetTypeName;
    private String unit;
    /** 稳定数据集编码，作为指标来源绑定的一部分。 */
    private String datasetCode;
    /** 数据集当前数据类型版本中的稳定字段编码。 */
    private String fieldCode;
    /** 以下三个字段由查询时的 Dataset 字段结构派生，绝不作为指标定义落库。 */
    private String datasetName;
    private String fieldName;
    private String fieldType;
    private CommonStatus status;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
