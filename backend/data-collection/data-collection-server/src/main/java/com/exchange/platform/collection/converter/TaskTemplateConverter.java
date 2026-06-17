package com.exchange.platform.collection.converter;

import java.util.List;

import com.exchange.platform.collection.api.request.tasks.StorageMappingRequest;
import com.exchange.platform.collection.api.response.tasks.StorageColumnMappingView;
import com.exchange.platform.collection.api.response.tasks.StorageMappingView;
import com.exchange.platform.collection.api.response.tasks.TaskConfigView;
import com.exchange.platform.collection.api.response.tasks.TaskTemplateVersionView;
import com.exchange.platform.collection.api.response.tasks.TaskTemplateView;
import com.exchange.platform.collection.entity.tasks.StorageColumnMapping;
import com.exchange.platform.collection.entity.tasks.StorageMapping;
import com.exchange.platform.collection.entity.tasks.TaskConfig;
import com.exchange.platform.collection.entity.tasks.TaskTemplate;
import com.exchange.platform.collection.entity.tasks.TaskTemplateVersion;



/**
 * 采集任务模板转换器，只包含TaskTemplate的信息，不包括配置信息
 */
public class TaskTemplateConverter {
    
    private TaskTemplateConverter() {
        // 私有构造函数，防止实例化
    }

    /**
     * 将采集任务模板实体转换为视图对象，用于列表展示
     * @param template
     * @return
     */
    public static TaskTemplateView toView(TaskTemplate template) {
        if (template == null) {
            return null;
        }
        TaskTemplateView view = new TaskTemplateView();
        view.setTaskCode(template.getTaskCode());
        view.setTaskName(template.getTaskName());
        view.setCurrentVersionNo(template.getCurrentVersionNo());
        view.setSyncStatus(template.getSyncStatus());
        view.setLatestSyncTime(template.getLatestSyncTime());
        view.setCreatedAt(template.getCreatedAt());
        view.setUpdatedAt(template.getUpdatedAt());
        return view;
    }

    /**
     * 将采集任务模板版本实体转换为视图对象，用于版本管理展示
     * @param version
     * @return
     */
    public static TaskTemplateVersionView toVersionView(TaskTemplateVersion version) {
        if (version == null) {
            return null;
        }
        TaskTemplateVersionView versionView = new TaskTemplateVersionView();
        versionView.setTaskCode(version.getTaskCode());
        versionView.setVersionNo(version.getVersionNo());
        versionView.setTaskName(version.getTaskName());
        versionView.setTaskDesc(version.getTaskDesc());
        versionView.setHandlerName(version.getHandlerName());
        versionView.setDataSource(version.getDataSource());
        versionView.setAssetType(version.getAssetType());
        versionView.setBizType(version.getBizType());
        versionView.setParamsSchemaJson(version.getParamsSchemaJson());
        versionView.setOutputFieldsJson(version.getOutputFieldsJson());
        versionView.setOutputFieldsHash(version.getOutputFieldsHash());
        versionView.setRuleCategoryCodes(version.getRuleCategoryCodes());
        versionView.setChangeSummary(version.getChangeSummary());
        versionView.setSyncTime(version.getSyncTime());
        versionView.setCreatedAt(version.getCreatedAt());
        return versionView;
    }

    /**
     * 将采集任务配置实体转换为视图对象，用于详情页展示
     * @param config
     * @return
     */
    public static TaskConfigView toConfigView(TaskConfig config) {
        if (config == null) {
            return null;
        }
        TaskConfigView configView = new TaskConfigView();
        configView.setTaskConfigCode(config.getTaskConfigCode());
        configView.setTaskCode(config.getTaskCode());
        configView.setDataTopicCode(config.getDataTopicCode());
        configView.setDataTypeCode(config.getDataTypeCode());
        configView.setDataTypeVersionNo(config.getDataTypeVersionNo());
        configView.setStorageMappingCode(config.getStorageMappingCode());
        configView.setRuleCodesJson(config.getRuleCodesJson());
        configView.setConfigStatus(config.getConfigStatus());
        configView.setStatus(config.getStatus());
        configView.setDescription(config.getDescription());
        configView.setCreatedAt(config.getCreatedAt());
        configView.setUpdatedAt(config.getUpdatedAt());
        return configView;
    }
    
    /**
     * 将存储映射实体及其相关的列映射信息转换为视图对象，用于任务详情页展示
     * @param mapping
     * @param columnMappings
     * @return
     */
    public static StorageMappingView toStorageMappingView(StorageMapping mapping, List<StorageColumnMapping> columnMappings) {
        if (mapping == null) {
            return null;
        }
        StorageMappingView view = new StorageMappingView();
        view.setStorageMappingCode(mapping.getStorageMappingCode());
        view.setTaskConfigCode(mapping.getTaskConfigCode());
        view.setDataSourceCode(mapping.getDataSourceCode());
        view.setPhysicalSchemaName(mapping.getPhysicalSchemaName());
        view.setPhysicalTableName(mapping.getPhysicalTableName());
        view.setWriteStrategy(mapping.getWriteStrategy());
        view.setMappingStatus(mapping.getMappingStatus());
        view.setConfirmRemark(mapping.getConfirmRemark());
        view.setCreatedAt(mapping.getCreatedAt());
        view.setUpdatedAt(mapping.getUpdatedAt());
        // 加载列映射信息
        if (columnMappings != null) {
            List<StorageColumnMappingView> columnMappingViews = columnMappings.stream()
                .map(TaskTemplateConverter::toColumnMappingView)
                .toList();
            view.setColumns(columnMappingViews);
        }
        return view;
    }

    /**
     * 将存储列映射实体转换为视图对象
     * @param columnMapping
     * @return
     */
    public static StorageColumnMappingView toColumnMappingView(StorageColumnMapping columnMapping) {
        if (columnMapping == null) {
            return null;
        }
        StorageColumnMappingView view = new StorageColumnMappingView();
        view.setStorageMappingCode(columnMapping.getStorageMappingCode());
        view.setSourceFieldCode(columnMapping.getSourceFieldCode());
        view.setSourceFieldName(columnMapping.getSourceFieldName());
        view.setSourceFieldType(columnMapping.getSourceFieldType());
        view.setDataTypeFieldCode(columnMapping.getDataTypeFieldCode());
        view.setDataTypeFieldType(columnMapping.getDataTypeFieldType());
        view.setPhysicalColumnName(columnMapping.getPhysicalColumnName());
        view.setPhysicalColumnType(columnMapping.getPhysicalColumnType());
        view.setDefaultValue(columnMapping.getDefaultValue());
        view.setRequired(columnMapping.getRequired());
        view.setUniqueKey(columnMapping.getUniqueKey());
        return view;
    }

    /**
     * 将存储映射请求中的列映射信息转换为存储列映射实体，用于保存或更新存储映射关系
     * @param storageMappingCode
     * @param column
     * @return
     */
    public static StorageColumnMapping toStorageColumnMapping(String storageMappingCode, StorageMappingRequest.ColumnItem column) {
        
        StorageColumnMapping columnMapping = new StorageColumnMapping();
        columnMapping
            .setStorageMappingCode(storageMappingCode)
            .setSourceFieldCode(column.sourceFieldCode())
            .setSourceFieldName(column.sourceFieldName())
            .setSourceFieldType(column.sourceFieldType())
            .setDataTypeFieldCode(column.dataTypeFieldCode())
            .setDataTypeFieldType(column.dataTypeFieldType())
            .setPhysicalColumnName(column.physicalColumnName())
            .setPhysicalColumnType(column.physicalColumnType())
            .setDefaultValue(column.defaultValue())
            .setRequired(column.required())
            .setUniqueKey(column.uniqueKey());
        return columnMapping;
    }


}
