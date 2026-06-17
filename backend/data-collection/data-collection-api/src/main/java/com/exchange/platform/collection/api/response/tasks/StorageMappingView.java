package com.exchange.platform.collection.api.response.tasks;

import java.time.LocalDateTime;
import java.util.List;

import com.exchange.platform.collection.api.enums.MappingStatus;

import lombok.Data;

/**
 * 存储映射视图对象，用于任务详情页面展示
 * 包含存储目标信息和字段映射信息等
 */
@Data
public class StorageMappingView {
    /** 存储映射编码 */
    private String storageMappingCode;

    /** 关联任务配置编码 */
    private String taskConfigCode;

    /** 数据源编码 */
    private String dataSourceCode;

    /** 数据类型编码 */
    private String dataTypeCode;

    /** 数据类型版本号 */
    private Integer dataTypeVersionNo;

    /** 目标物理库名 */
    private String physicalSchemaName;

    /** 目标物理表名 */
    private String physicalTableName;

    /** 写入策略 */
    private String writeStrategy;

    /** 映射状态 */
    private MappingStatus mappingStatus;

    /** 确认备注 */
    private String confirmRemark;

    /** 字段映射列表 */
    private List<StorageColumnMappingView> columns;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
