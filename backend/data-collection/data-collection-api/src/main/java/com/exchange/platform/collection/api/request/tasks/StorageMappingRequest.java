package com.exchange.platform.collection.api.request.tasks;

import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.exchange.platform.collection.api.enums.MappingStatus;


/**
 * 维护存储映射草稿请求（I-08）
 *
 * @param saveOrUpdate           操作类型（save:新增, update:更新）
 * @param storageMappingCode     存储映射编码
 * @param taskConfigCode         任务配置编码
 * @param dataSourceCode         数据源编码
 * @param physicalSchemaName     目标物理库名
 * @param physicalTableName      目标物理表名
 * @param writeStrategy          写入策略（INSERT / UPSERT / REPLACE）
 * @param mappingStatus          映射状态
 * @param confirmRemark          确认备注
 * @param columns                字段映射列表
 */
public record StorageMappingRequest(
    @NotBlank String saveOrUpdate,
    @NotBlank String storageMappingCode,
    @NotBlank String taskConfigCode,
    @NotBlank String dataSourceCode,
    @NotBlank String physicalSchemaName,
    @NotBlank String physicalTableName,
    @NotBlank String writeStrategy,
    @NotNull MappingStatus mappingStatus,
    String confirmRemark,
    @NotNull  List<ColumnItem> columns
) {
    /**
     * 单字段映射条目
     *
     * @param sourceFieldCode    来源字段编码
     * @param sourceFieldName    来源字段名称
     * @param sourceFieldType    来源字段类型
     * @param dataTypeFieldCode  目标数据类型字段编码
     * @param dataTypeFieldType  目标数据类型字段类型
     * @param physicalColumnName 目标物理列名
     * @param physicalColumnType 目标物理列类型
     * @param defaultValue       默认值（可选）
     * @param required           是否必填
     * @param uniqueKey          是否业务唯一键
     */
    public record ColumnItem(
        @NotBlank String sourceFieldCode,
        String sourceFieldName,
        @NotBlank String sourceFieldType,
        @NotBlank String dataTypeFieldCode,
        @NotBlank String dataTypeFieldType,
        @NotBlank String physicalColumnName,
        String physicalColumnType,
        String defaultValue,
        Boolean required,
        Boolean uniqueKey
    ) {}
}
