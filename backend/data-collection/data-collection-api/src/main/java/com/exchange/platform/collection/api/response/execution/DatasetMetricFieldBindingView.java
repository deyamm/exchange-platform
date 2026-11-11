package com.exchange.platform.collection.api.response.execution;

import lombok.Data;

/**
 * 统一指标读取时使用的字段映射。
 * 该 DTO 仅在采集服务与分析服务的受控后端调用之间传递，绝不返回给普通前端页面。
 */
@Data
public class DatasetMetricFieldBindingView {
    /** AppMetric 保存的标准字段编码。 */
    private String fieldCode;
    /** 指标取数需要使用的物理列名。 */
    private String physicalColumnName;
}
