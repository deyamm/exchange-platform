package com.exchange.platform.collection.api.response.execution;

import java.time.LocalDateTime;

import com.exchange.platform.collection.api.enums.DatasetStatus;

import lombok.Data;

/** 数据集目录列表视图。 */
@Data
public class DatasetView {
    /** 稳定数据集编码。 */
    private String datasetCode;
    /** 数据集名称。 */
    private String datasetName;
    /** 数据集说明。 */
    private String description;
    /** 当前关联任务配置编码。 */
    private String taskConfigCode;
    /** 来源任务编码。 */
    private String taskCode;
    /** 所属数据主题编码。 */
    private String dataTopicCode;
    /** 所属数据类型编码。 */
    private String dataTypeCode;
    /** 当前数据类型版本号。 */
    private Integer dataTypeVersionNo;
    /** 当前存储映射编码。 */
    private String storageMappingCode;
    /** 数据集管理状态。 */
    private DatasetStatus datasetStatus;
    /** 可信的当前记录总数。 */
    private Long recordCount;
    /** 最近一次成功执行时间。 */
    private LocalDateTime lastSuccessTime;
    /** 存储类型。 */
    private String storageType;
    /** 数据源编码。 */
    private String datasourceCode;
    /** Schema 名称。 */
    private String schemaName;
    /** 正式物理表名称。 */
    private String tableName;
}
