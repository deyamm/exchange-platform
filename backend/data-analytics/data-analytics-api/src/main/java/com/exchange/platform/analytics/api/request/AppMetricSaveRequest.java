package com.exchange.platform.analytics.api.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.exchange.platform.common.core.enums.AssetType;

/**
 * 应用指标主档保存请求。
 * <p>数据来源只保存稳定的数据集编码和标准字段编码；字段名称、类型等展示信息始终
 * 从数据集当前版本的字段定义派生，避免形成两套可编辑的字段标准。</p>
 */
public record AppMetricSaveRequest(
    @NotBlank(message = "指标编码不能为空") @Size(max = 64, message = "指标编码长度不能超过64个字符") String metricCode,
    @NotBlank(message = "指标名称不能为空") @Size(max = 128, message = "指标名称长度不能超过128个字符") String metricName,
    @NotBlank(message = "指标分类不能为空") @Size(max = 64, message = "指标分类长度不能超过64个字符") String metricCategory,
    /** 用户明确选择的指标适用标的类型，创建后不可修改。 */
    @NotNull(message = "标的类型不能为空") AssetType targetType,
    @Size(max = 32, message = "单位长度不能超过32个字符") String unit,
    @NotBlank(message = "来源数据集不能为空") @Size(max = 64, message = "来源数据集编码长度不能超过64个字符") String datasetCode,
    @NotBlank(message = "来源字段不能为空") @Size(max = 64, message = "来源字段编码长度不能超过64个字符") String fieldCode,
    @Size(max = 500, message = "指标说明长度不能超过500个字符") String description
) {}
