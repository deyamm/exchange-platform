package com.exchange.platform.collection.service.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exchange.platform.collection.api.enums.DatasetStatus;
import com.exchange.platform.collection.api.enums.DatasetMetadataDefinition;
import com.exchange.platform.collection.api.enums.DatasetMetadataMaintainMode;
import com.exchange.platform.collection.api.request.execution.DatasetMetadataUpdateRequest;
import com.exchange.platform.collection.api.request.execution.DatasetMetricBindingQueryRequest;
import com.exchange.platform.collection.api.response.execution.DatasetDetailView;
import com.exchange.platform.collection.api.response.execution.DatasetMetricBindingView;
import com.exchange.platform.collection.api.response.execution.DatasetMetricFieldBindingView;
import com.exchange.platform.collection.api.response.execution.DatasetMetricReadBindingView;
import com.exchange.platform.collection.api.response.execution.DatasetMetadataView;
import com.exchange.platform.collection.api.response.execution.DatasetView;
import com.exchange.platform.collection.api.response.dataType.DataTypeFieldView;
import com.exchange.platform.collection.converter.ExecutionConverter;
import com.exchange.platform.collection.entity.dataType.DataTypeVersion;
import com.exchange.platform.collection.entity.datasource.DataSourceConfig;
import com.exchange.platform.collection.entity.execution.Dataset;
import com.exchange.platform.collection.entity.execution.DatasetMetadata;
import com.exchange.platform.collection.entity.execution.ExecutionConfigSnapshot;
import com.exchange.platform.collection.entity.execution.ResultSummary;
import com.exchange.platform.collection.entity.execution.TaskRun;
import com.exchange.platform.collection.entity.tasks.StorageMapping;
import com.exchange.platform.collection.entity.tasks.StorageColumnMapping;
import com.exchange.platform.collection.mapper.dataType.DataTypeVersioinMapper;
import com.exchange.platform.collection.mapper.datasource.DataSourceConfigMapper;
import com.exchange.platform.collection.mapper.execution.DatasetMapper;
import com.exchange.platform.collection.mapper.execution.DatasetMetadataMapper;
import com.exchange.platform.collection.mapper.execution.ResultSummaryMapper;
import com.exchange.platform.collection.mapper.execution.TaskRunMapper;
import com.exchange.platform.collection.mapper.tasks.StorageColumnMappingMapper;
import com.exchange.platform.collection.mapper.tasks.StorageMappingMapper;
import com.exchange.platform.collection.mapper.tasks.TaskTemplateVersionMapper;
import com.exchange.platform.collection.service.DatasetExecutionService;
import com.exchange.platform.collection.service.DatasetService;
import com.exchange.platform.common.core.exception.BusinessException;
import com.exchange.platform.common.core.page.PageResult;
import com.exchange.platform.common.core.enums.AssetType;
import com.exchange.platform.collection.entity.tasks.TaskTemplateVersion;

import cn.hutool.json.JSONObject;

import com.exchange.platform.collection.utils.JdbcUtils;

/**
 * 数据集管理服务实现。
 * <p>
 * 负责数据集执行前校验、成功执行登记、目录查询、详情查询及元数据维护。
 * </p>
 */
@Service
public class DatasetServiceImpl implements DatasetService, DatasetExecutionService {
    private static final Logger log = LoggerFactory.getLogger(DatasetServiceImpl.class);
    private static final String STORAGE = "STORAGE";
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z0-9_]{1,64}");
    private static final DateTimeFormatter ISO_TIME = DateTimeFormatter.ISO_DATE_TIME;

    private final DatasetMapper datasetMapper;
    private final DatasetMetadataMapper metadataMapper;
    private final TaskRunMapper taskRunMapper;
    private final ResultSummaryMapper summaryMapper;
    private final DataTypeVersioinMapper dataTypeVersionMapper;
    private final StorageColumnMappingMapper storageColumnMappingMapper;
    private final StorageMappingMapper storageMappingMapper;
    private final DataSourceConfigMapper dataSourceConfigMapper;
    private final TaskTemplateVersionMapper taskTemplateVersionMapper;

    /** 注入数据集主档、元数据、执行记录及结构查询所需的数据访问组件。 */
    public DatasetServiceImpl(DatasetMapper datasetMapper, DatasetMetadataMapper metadataMapper,
            TaskRunMapper taskRunMapper, ResultSummaryMapper summaryMapper,
            DataTypeVersioinMapper dataTypeVersionMapper,
            StorageColumnMappingMapper storageColumnMappingMapper, StorageMappingMapper storageMappingMapper,
            DataSourceConfigMapper dataSourceConfigMapper,
            TaskTemplateVersionMapper taskTemplateVersionMapper) {
        this.datasetMapper = datasetMapper;
        this.metadataMapper = metadataMapper;
        this.taskRunMapper = taskRunMapper;
        this.summaryMapper = summaryMapper;
        this.dataTypeVersionMapper = dataTypeVersionMapper;
        this.storageColumnMappingMapper = storageColumnMappingMapper;
        this.storageMappingMapper = storageMappingMapper;
        this.dataSourceConfigMapper = dataSourceConfigMapper;
        this.taskTemplateVersionMapper = taskTemplateVersionMapper;
    }

    /** 用于任务执行前对数据集进行校验，包括数据集编码、数据集状态（应为ENABLED）、执行快照中的物理表应与数据集绑定的物理表一致。 */
    @Override
    @Transactional(readOnly = true)
    public void validateForExecution(StorageMapping mapping) {
        if (mapping == null || !StringUtils.hasText(mapping.getDatasetCode())
                || !mapping.getDatasetCode().matches("^[A-Z0-9_]{1,64}$")) {
            throw new BusinessException(40006, "存储映射缺少合法的数据集编码（仅允许大写字母、数字和下划线）");
        }
        Dataset dataset = findByCode(mapping.getDatasetCode());
        if (dataset != null && dataset.getDatasetStatus() != DatasetStatus.ENABLED) {
            throw new BusinessException(40006, "数据集当前状态不允许写入：" + dataset.getDatasetStatus());
        }
        if (dataset != null && StringUtils.hasText(dataset.getStorageMappingCode())
                && !mapping.getStorageMappingCode().equals(dataset.getStorageMappingCode())) {
            String tableName = metadataValue(dataset.getId(), "table_name");
            if (StringUtils.hasText(tableName) && !tableName.equals(mapping.getPhysicalTableName())) {
                throw new BusinessException(40006, "数据集编码已绑定其他正式物理表，禁止执行");
            }
        }
    }

    /** 执行成功后，初始化数据集信息或在已有数据集信息的基础上更新。 */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void registerSuccessfulRun(TaskRun run, ExecutionConfigSnapshot snapshot, StorageMapping mapping,
            ResultSummary summary) {
        Dataset dataset = findByCode(snapshot.getDatasetCode());
        AssetType executedAssetType = requireExecutedAssetType(snapshot);
        LocalDateTime now = LocalDateTime.now();
        if (dataset == null) {
            dataset = new Dataset()
                    .setDatasetCode(snapshot.getDatasetCode())
                    .setDatasetName(StringUtils.hasText(mapping.getDatasetName()) ? mapping.getDatasetName()
                            : snapshot.getDatasetCode())
                    .setAssetType(executedAssetType.getValue())
                    .setDescription(mapping.getDatasetDesc())
                    .setTaskCode(run.getTaskCode())
                    .setTaskConfigCode(run.getTaskConfigCode())
                    .setDataTopicCode(snapshot.getDataTopicCode())
                    .setDataTypeCode(snapshot.getDataTypeCode())
                    .setDataTypeVersionNo(snapshot.getDataTypeVersion())
                    .setStorageMappingCode(mapping.getStorageMappingCode())
                    .setLatestTaskRunId(run.getRunId())
                    .setRunId(run.getRunId())
                    .setSummaryId(summary.getSummaryId())
                    .setResultLocation(summary.getResultLocation())
                    .setGeneratedTime(now)
                    .setLastCollectTime(now)
                    .setLastSuccessTime(now)
                    .setDatasetStatus(DatasetStatus.ENABLED);
            datasetMapper.insert(dataset);
        } else {
            // Dataset 身份一旦建立不能被后续任务版本静默改写，因此仅拒绝登记更新并记录异常。
            if (dataset.getAssetType() != null && !dataset.getAssetType().equals(executedAssetType.getValue())) {
                throw new BusinessException(40006, "数据集标的类型与本次执行模板版本不一致：dataset="
                        + dataset.getAssetType() + ", execution=" + executedAssetType.getValue());
            }
            if (dataset.getAssetType() == null) {
                dataset.setAssetType(executedAssetType.getValue());
            }
            dataset.setDataTypeVersionNo(snapshot.getDataTypeVersion())
                    .setTaskConfigCode(run.getTaskConfigCode())
                    .setStorageMappingCode(mapping.getStorageMappingCode())
                    .setLatestTaskRunId(run.getRunId())
                    .setRunId(run.getRunId())
                    .setSummaryId(summary.getSummaryId())
                    .setResultLocation(summary.getResultLocation())
                    .setLastCollectTime(now)
                    .setLastSuccessTime(now);
            datasetMapper.updateById(dataset);
        }
        refreshConfigDerivedMetadata(dataset, mapping);
        refreshDatasetStatistics(dataset, mapping);
    }

    /** 按条件分页查询数据集目录，统一处理分页参数并转换为目录视图。 */
    @Override
    @Transactional(readOnly = true)
    public PageResult<DatasetView> query(String datasetCode, String datasetName, String topicCode, String dataTypeCode, AssetType assetType, Integer datasetStatus, String taskConfigCode, String startTime, String endTime, Integer pageNo, Integer pageSize) {
        log.info("[Service] query start, datasetCode={}, datasetName={}, topicCode={}, dataTypeCode={}, assetType={}, datasetStatus={}, taskConfigCode={}, startTime={}, endTime={}, pageNo={}, pageSize={}",
                datasetCode, datasetName, topicCode, dataTypeCode, assetType, datasetStatus, taskConfigCode, startTime,
                endTime, pageNo, pageSize);
        int safePageNo = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int safePageSize = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 200);
        LambdaQueryWrapper<Dataset> wrapper = new LambdaQueryWrapper<Dataset>()
                .like(StringUtils.hasText(datasetCode), Dataset::getDatasetCode, datasetCode)
                .like(StringUtils.hasText(datasetName), Dataset::getDatasetName, datasetName)
                .eq(StringUtils.hasText(topicCode), Dataset::getDataTopicCode, topicCode)
                .eq(StringUtils.hasText(dataTypeCode), Dataset::getDataTypeCode, dataTypeCode)
                .eq(StringUtils.hasText(taskConfigCode), Dataset::getTaskConfigCode, taskConfigCode)
                .ge(StringUtils.hasText(startTime), Dataset::getLastSuccessTime, startTime)
                .le(StringUtils.hasText(endTime), Dataset::getLastSuccessTime, endTime)
                .orderByDesc(Dataset::getLastSuccessTime);
        if (datasetStatus != null)
            wrapper.eq(Dataset::getDatasetStatus, datasetStatus);
        if (assetType != null)
            wrapper.eq(Dataset::getAssetType, assetType.getValue());
        Page<Dataset> page = datasetMapper.selectPage(new Page<>(safePageNo, safePageSize), wrapper);
        return new PageResult<>(safePageNo, safePageSize, page.getTotal(),
                page.getRecords().stream().map(this::toView).collect(Collectors.toList()));
    }

    /** 查询数据集详情、元数据、字段结构及最近成功执行信息。 */
    @Override
    @Transactional(readOnly = true)
    public DatasetDetailView getDetail(String datasetCode) {
        log.info("[Service] getDetail start, datasetCode={}", datasetCode);
        Dataset dataset = requireDataset(datasetCode);
        DatasetDetailView detail = new DatasetDetailView();
        detail.setDataset(toView(dataset));
        detail.setMetadata(loadMetadata(dataset));
        detail.setStructure(loadStructure(dataset));
        if (StringUtils.hasText(dataset.getLatestTaskRunId())) {
            detail.setLatestTaskRun(
                ExecutionConverter.toRunView(
                    taskRunMapper.selectOne(
                        new LambdaQueryWrapper<TaskRun>()
                            .eq(TaskRun::getRunId, dataset.getLatestTaskRunId())
                            .last("LIMIT 1")
                    )
                )
            );
            detail.setLatestResultSummary(
                ExecutionConverter.toSummaryView(
                    summaryMapper.selectOne(
                        new LambdaQueryWrapper<ResultSummary>()
                            .eq(ResultSummary::getRunId, dataset.getLatestTaskRunId())
                            .last("LIMIT 1")
                    )
                )
            );
        }
        return detail;
    }

    /** 查询数据集扩展元数据，并按分组和属性编码排序。 */
    @Override
    @Transactional(readOnly = true)
    public List<DatasetMetadataView> getMetadata(String datasetCode) {
        log.info("[Service] getMetadata start, datasetCode={}", datasetCode);
        Dataset dataset = requireDataset(datasetCode);
        return loadMetadata(dataset);
    }

    /** 查询已加载数据集的元数据，供详情查询复用以避免重复主档查询和日志。 */
    private List<DatasetMetadataView> loadMetadata(Dataset dataset) {
        return metadataMapper
            .selectList(
                new LambdaQueryWrapper<DatasetMetadata>()
                    .eq(DatasetMetadata::getDatasetId, dataset.getId())
                    .orderByAsc(DatasetMetadata::getMetadataGroup)
                    .orderByAsc(DatasetMetadata::getMetadataKey)
            ).stream().map(this::toMetadataView).collect(Collectors.toList());
    }

    /** 校验并更新用户可维护的数据集名称、说明和扩展元数据。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMetadata(String datasetCode, DatasetMetadataUpdateRequest request) {
        log.info("[Service] updateMetadata start, datasetCode={}, itemCount={}", datasetCode,
                request == null || request.items() == null ? 0 : request.items().size());
        Dataset dataset = requireDataset(datasetCode);
        if (request == null || request.items() == null) {
            throw new BusinessException(40006, "元数据请求不能为空");
        }
        if (StringUtils.hasText(request.datasetName())) {
            dataset.setDatasetName(request.datasetName());
        }
        if (request.datasetDesc() != null) {
            dataset.setDescription(request.datasetDesc());
        }
        // 更新数据集主档
        datasetMapper.updateById(dataset);
        for (DatasetMetadataUpdateRequest.Item item : request.items()) {
            if (!StringUtils.hasText(item.metadataGroup()) || !StringUtils.hasText(item.metadataKey())) {
                throw new BusinessException(40006, "元数据分组和编码不能为空");
            }
            DatasetMetadataDefinition definition = DatasetMetadataDefinition.find(item.metadataGroup().toUpperCase(),
                    item.metadataKey());
            if (definition == null)
                throw new BusinessException(40006, "不支持的元数据属性：" + item.metadataGroup() + "." + item.metadataKey());
            if (definition.getMaintainMode() != DatasetMetadataMaintainMode.MANUAL) {
                throw new BusinessException(40006, "该元数据属性由系统维护，不能人工修改：" + definition.getKey());
            }
            validateManualValue(dataset, definition, item.metadataValue());
            if (!StringUtils.hasText(item.metadataValue())) {
                deleteMetadata(dataset.getId(), definition.getGroup(), definition.getKey());
            } else {
                upsertMetadata(dataset.getId(), definition.getGroup(), definition.getKey(), definition.getName(),
                        item.metadataValue(), definition.getValueType(), item.remark());
            }
        }
    }

    /** 查询数据集当前引用的数据类型版本字段结构；无结构时返回空列表。 */
    @Override
    @Transactional(readOnly = true)
    public List<?> getStructure(String datasetCode) {
        log.info("[Service] getStructure start, datasetCode={}", datasetCode);
        Dataset dataset = requireDataset(datasetCode);
        return loadStructure(dataset);
    }

    /**
     * 为应用指标等消费者批量读取数据集元数据，避免消费者对分页记录逐行发起 HTTP 请求。
     * 字段定义严格读取 Dataset 固定的当前版本快照，而不是仍可能变动的 DataType 草稿字段。
     */
    @Override
    @Transactional(readOnly = true)
    public List<DatasetMetricBindingView> getMetricBindings(DatasetMetricBindingQueryRequest request) {
        List<String> datasetCodes = request.datasetCodes().stream().filter(StringUtils::hasText).distinct().toList();
        if (datasetCodes.isEmpty()) {
            return Collections.emptyList();
        }
        List<Dataset> datasets = datasetMapper.selectList(
            new LambdaQueryWrapper<Dataset>()
                .in(Dataset::getDatasetCode, datasetCodes)
        );
        return datasets.stream().map(dataset -> {
            DatasetMetricBindingView view = new DatasetMetricBindingView();
            view.setDatasetCode(dataset.getDatasetCode());
            view.setDatasetName(dataset.getDatasetName());
            view.setAssetType(AssetType.fromCode(dataset.getAssetType()));
            view.setDatasetStatus(dataset.getDatasetStatus());
            view.setDataTypeVersionNo(dataset.getDataTypeVersionNo());
            DataTypeVersion version = dataTypeVersionMapper.selectOne(
                new LambdaQueryWrapper<DataTypeVersion>()
                    .eq(DataTypeVersion::getDataTypeCode, dataset.getDataTypeCode())
                    .eq(DataTypeVersion::getVersion, dataset.getDataTypeVersionNo()).last("LIMIT 1")
            );
            view.setFields(version == null || !StringUtils.hasText(version.getFieldSchemaContent())
                    ? Collections.emptyList()
                    : JSON.parseArray(version.getFieldSchemaContent(), DataTypeFieldView.class));
            return view;
        }).collect(Collectors.toList());
    }

    /**
     * 返回统一指标读取的最小物理绑定。目标代码和数据时间均来自已经确认的 Dataset 结构，
     * 不使用调用方传入的列名，从而保持 metricCode -> Dataset 的服务端边界。
     */
    @Override
    @Transactional(readOnly = true)
    public List<DatasetMetricReadBindingView> getMetricReadBindings(DatasetMetricBindingQueryRequest request) {
        List<String> datasetCodes = request.datasetCodes().stream().filter(StringUtils::hasText).distinct().toList();
        if (datasetCodes.isEmpty()) {
            return Collections.emptyList();
        }
        List<Dataset> datasets = datasetMapper.selectList(
            new LambdaQueryWrapper<Dataset>()
                .in(Dataset::getDatasetCode, datasetCodes)
        );
        return datasets.stream().map(this::toMetricReadBinding).collect(Collectors.toList());
    }

    /**
     * 
     * @param dataset
     * @return
     */
    private DatasetMetricReadBindingView toMetricReadBinding(Dataset dataset) {
        if (dataset.getDatasetStatus() != DatasetStatus.ENABLED) {
            throw new BusinessException(40006, "数据集未启用，不能读取指标值：" + dataset.getDatasetCode());
        }
        if (!StringUtils.hasText(dataset.getStorageMappingCode())) {
            throw new BusinessException(40006, "数据集缺少存储映射：" + dataset.getDatasetCode());
        }
        // storage mapping
        StorageMapping mapping = storageMappingMapper.selectOne(new LambdaQueryWrapper<StorageMapping>()
                .eq(StorageMapping::getStorageMappingCode, dataset.getStorageMappingCode()).last("LIMIT 1"));
        if (mapping == null || !StringUtils.hasText(mapping.getPhysicalSchemaName())
                || !StringUtils.hasText(mapping.getPhysicalTableName())) {
            throw new BusinessException(40006, "数据集物理存储定位无效：" + dataset.getDatasetCode());
        }
        // dataset 对应的 storage column mapping
        List<StorageColumnMapping> columns = storageColumnMappingMapper.selectList(
                new LambdaQueryWrapper<StorageColumnMapping>()
                        .eq(StorageColumnMapping::getStorageMappingCode, mapping.getStorageMappingCode()));
        DataTypeVersion version = dataTypeVersionMapper.selectOne(new LambdaQueryWrapper<DataTypeVersion>()
                .eq(DataTypeVersion::getDataTypeCode, dataset.getDataTypeCode())
                .eq(DataTypeVersion::getVersion, dataset.getDataTypeVersionNo()).last("LIMIT 1"));
        if (version == null || !StringUtils.hasText(version.getFieldSchemaContent())) {
            throw new BusinessException(40006, "数据集当前字段结构无效：" + dataset.getDatasetCode());
        }
        // 解析当前字段结构
        List<DataTypeFieldView> currentFields = JSON.parseArray(version.getFieldSchemaContent(), DataTypeFieldView.class);
        List<String> currentFieldCodes = currentFields
            .stream()
            .map(DataTypeFieldView::getFieldCode)
            .filter(StringUtils::hasText)
            .collect(Collectors.toList());
        Map<String, String> physicalColumns = columns
            .stream()
            .filter(column -> StringUtils.hasText(column.getDataTypeFieldCode()) && StringUtils.hasText(column.getPhysicalColumnName()))
            .filter(column -> currentFieldCodes.contains(column.getDataTypeFieldCode()))
            .collect(Collectors.toMap(StorageColumnMapping::getDataTypeFieldCode,
                    StorageColumnMapping::getPhysicalColumnName, (left, right) -> left));
        String targetCodeColumn = physicalColumns.get("full_symbole");
        String dataTimeField = metadataValue(dataset.getId(), "data_time_field");
        String dataTimeColumn = physicalColumns.get(dataTimeField);
        if (!StringUtils.hasText(targetCodeColumn) || !StringUtils.hasText(dataTimeColumn)) {
            throw new BusinessException(40006, "数据集缺少标的代码或数据时间字段映射：" + dataset.getDatasetCode());
        }
        DatasetMetricReadBindingView view = new DatasetMetricReadBindingView();
        view.setDatasetCode(dataset.getDatasetCode());
        view.setAssetType(AssetType.fromCode(dataset.getAssetType()));
        view.setDatasetStatus(dataset.getDatasetStatus());
        view.setPhysicalSchemaName(mapping.getPhysicalSchemaName());
        view.setPhysicalTableName(mapping.getPhysicalTableName());
        view.setTargetCodeColumn(targetCodeColumn);
        view.setDataTimeColumn(dataTimeColumn);
        view.setFields(physicalColumns.entrySet().stream().map(entry -> {
            DatasetMetricFieldBindingView field = new DatasetMetricFieldBindingView();
            field.setFieldCode(entry.getKey());
            field.setPhysicalColumnName(entry.getValue());
            return field;
        }).collect(Collectors.toList()));
        return view;
    }

    /** 查询已加载数据集当前引用的数据类型版本结构。 */
    private List<?> loadStructure(Dataset dataset) {
        DataTypeVersion version = dataTypeVersionMapper.selectOne(new LambdaQueryWrapper<DataTypeVersion>()
                .eq(DataTypeVersion::getDataTypeCode, dataset.getDataTypeCode())
                .eq(DataTypeVersion::getVersion, dataset.getDataTypeVersionNo()).last("LIMIT 1"));
        if (version == null || !StringUtils.hasText(version.getFieldSchemaContent())) {
            return Collections.emptyList();
        }
        List<?> structure = JSON.parseArray(version.getFieldSchemaContent(), Map.class);
        return structure;
    }

    /** 根据稳定数据集编码查询主档，不存在时返回 {@code null}。 */
    private Dataset findByCode(String datasetCode) {
        return datasetMapper
                .selectOne(new LambdaQueryWrapper<Dataset>().eq(Dataset::getDatasetCode, datasetCode).last("LIMIT 1"));
    }

    /** 查询数据集主档并在不存在时抛出业务异常。 */
    private Dataset requireDataset(String datasetCode) {
        Dataset result = findByCode(datasetCode);
        if (result == null)
            throw new BusinessException(40003, "数据集不存在：" + datasetCode);
        return result;
    }

    /** 从执行快照固定的任务版本读取标的类型。 */
    private AssetType requireExecutedAssetType(ExecutionConfigSnapshot snapshot) {
        TaskTemplateVersion version = taskTemplateVersionMapper.selectOne(
            new LambdaQueryWrapper<TaskTemplateVersion>()
                .eq(TaskTemplateVersion::getTaskCode, snapshot.getTaskCode())
                .eq(TaskTemplateVersion::getVersionNo, snapshot.getTaskTemplateVersion())
                .last("LIMIT 1")
            );
        if (version == null || version.getAssetType() == null) {
            throw new BusinessException(40006, "执行使用的任务模板版本缺少有效资产类型");
        }
        return AssetType.fromCode(version.getAssetType());
    }

    /** 读取指定数据集的存储分组元数据值。 */
    private String metadataValue(Long datasetId, String key) {
        DatasetMetadata value = metadataMapper.selectOne(new LambdaQueryWrapper<DatasetMetadata>()
                .eq(DatasetMetadata::getDatasetId, datasetId).eq(DatasetMetadata::getMetadataGroup, STORAGE)
                .eq(DatasetMetadata::getMetadataKey, key).last("LIMIT 1"));
        return value == null ? null : value.getMetadataValue();
    }

    /** 创建或更新任意分组的一项元数据。 */
    private void upsertMetadata(Long datasetId, String group, String key, String name, String value, String valueType,
            String remark) {
        DatasetMetadata item = metadataMapper.selectOne(new LambdaQueryWrapper<DatasetMetadata>()
                .eq(DatasetMetadata::getDatasetId, datasetId)
                .eq(DatasetMetadata::getMetadataGroup, group).eq(DatasetMetadata::getMetadataKey, key).last("LIMIT 1"));
        if (item == null)
            item = new DatasetMetadata().setDatasetId(datasetId).setMetadataGroup(group).setMetadataKey(key);
        item.setMetadataName(name).setMetadataValue(value).setValueType(valueType).setRemark(remark);
        if (item.getId() == null)
            metadataMapper.insert(item);
        else
            metadataMapper.updateById(item);
    }

    /** 删除指定分组和属性编码的元数据。 */
    private void deleteMetadata(Long datasetId, String group, String key) {
        metadataMapper.delete(new LambdaQueryWrapper<DatasetMetadata>().eq(DatasetMetadata::getDatasetId, datasetId)
                .eq(DatasetMetadata::getMetadataGroup, group).eq(DatasetMetadata::getMetadataKey, key));
    }

    /** 按元数据定义校验人工维护值的格式和业务约束。 */
    private void validateManualValue(Dataset dataset, DatasetMetadataDefinition definition, String value) {
        if (!StringUtils.hasText(value))
            return;
        if ("JSON".equals(definition.getValueType())) {
            try {
                JSON.parse(value);
            } catch (Exception ex) {
                throw new BusinessException(40006, definition.getName() + "必须是合法 JSON");
            }
        }
        if ("data_granularity".equals(definition.getKey()))
            validateGranularity(value, true);
        if ("update_frequency".equals(definition.getKey()))
            validateGranularity(value, false);
        if ("data_time_field".equals(definition.getKey()))
            validateTimeField(dataset, value);
    }

    /** 校验时间粒度 JSON 的单位、间隔及特殊粒度规则。 */
    private void validateGranularity(String value, boolean allowSpecial) {
        Map<?, ?> map;
        try {
            map = JSON.parseObject(value, Map.class);
        } catch (Exception ex) {
            throw new BusinessException(40006, "时间单位配置必须是 JSON 对象");
        }
        String unit = Objects.toString(map.get("unit"), "").toUpperCase();
        List<String> temporal = List.of("SECOND", "MINUTE", "HOUR", "DAY", "WEEK", "MONTH", "QUARTER", "YEAR");
        List<String> special = List.of("TICK", "EVENT", "SNAPSHOT", "STATIC");
        if (!temporal.contains(unit) && !(allowSpecial && special.contains(unit)))
            throw new BusinessException(40006, "不支持的时间单位：" + unit);
        if (temporal.contains(unit)) {
            Object interval = map.get("interval");
            if (!(interval instanceof Number) || ((Number) interval).intValue() < 1)
                throw new BusinessException(40006, "interval 必须是大于等于 1 的整数");
        } else if (map.containsKey("interval"))
            throw new BusinessException(40006, "特殊粒度不应包含 interval");
    }

    /** 校验数据时间字段存在且属于日期时间类型。 */
    private void validateTimeField(Dataset dataset, String fieldCode) {
        DataTypeVersion version = dataTypeVersionMapper.selectOne(new LambdaQueryWrapper<DataTypeVersion>()
                .eq(DataTypeVersion::getDataTypeCode, dataset.getDataTypeCode())
                .eq(DataTypeVersion::getVersion, dataset.getDataTypeVersionNo()).last("LIMIT 1"));
        if (version == null || !StringUtils.hasText(version.getFieldSchemaContent()))
            throw new BusinessException(40006, "当前数据类型版本不存在字段结构");
        List<JSONObject> fields = JSON.parseArray(version.getFieldSchemaContent(), JSONObject.class);
        JSONObject field = fields.stream().filter(f -> fieldCode.equals(Objects.toString(f.get("fieldCode"), ""))).findFirst()
                .orElse(null);
        if (field == null)
            throw new BusinessException(40006, "数据时间字段不存在：" + fieldCode);
        String type = Objects.toString(field.get("fieldType"), "").toUpperCase();
        if (!(type.contains("DATE") || type.contains("TIME") || type.contains("TIMESTAMP")))
            throw new BusinessException(40006, "字段不是时间类型：" + fieldCode);
    }

    /** 根据存储列映射刷新业务主键及存储定位等派生元数据。 */
    private void refreshConfigDerivedMetadata(Dataset dataset, StorageMapping mapping) {
        List<StorageColumnMapping> columns = storageColumnMappingMapper.selectList(
            new LambdaQueryWrapper<StorageColumnMapping>()
                .eq(StorageColumnMapping::getStorageMappingCode, mapping.getStorageMappingCode())
            );
        columns.sort(Comparator.comparing(StorageColumnMapping::getId, Comparator.nullsLast(Long::compareTo)));
        // 数据类型字段作为业务主键
        List<String> businessKeys = columns.stream().filter(c -> Boolean.TRUE.equals(c.getUniqueKey()))
                .map(StorageColumnMapping::getDataTypeFieldCode)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
        // 物理列作为存储唯一键
        List<String> storageKeys = columns.stream().filter(c -> Boolean.TRUE.equals(c.getUniqueKey()))
                .map(StorageColumnMapping::getPhysicalColumnName)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
        upsertMetadata(dataset.getId(), "BUSINESS", "business_key", "业务主键", JSON.toJSONString(businessKeys), "JSON",null);
        upsertMetadata(dataset.getId(), STORAGE, "storage_type", "存储类型", "MYSQL", "STRING", null);
        upsertMetadata(dataset.getId(), STORAGE, "datasource_code", "数据源编码", mapping.getDataSourceCode(), "STRING",null);
        upsertMetadata(dataset.getId(), STORAGE, "schema_name", "Schema", mapping.getPhysicalSchemaName(), "STRING",null);
        upsertMetadata(dataset.getId(), STORAGE, "table_name", "物理表", mapping.getPhysicalTableName(), "STRING", null);
        upsertMetadata(dataset.getId(), STORAGE, "storage_unique_key", "物理唯一键", JSON.toJSONString(storageKeys), "JSON",null);
    }

    /** 从实际物理表读取记录数和时间覆盖范围并写回元数据。 */
    private void refreshDatasetStatistics(Dataset dataset, StorageMapping mapping) {
        try {
            if (!IDENTIFIER.matcher(mapping.getPhysicalSchemaName() == null ? "" : mapping.getPhysicalSchemaName()).matches() || !IDENTIFIER.matcher(mapping.getPhysicalTableName() == null ? "" : mapping.getPhysicalTableName()).matches())
                throw new IllegalArgumentException("invalid physical identifier");
            DataSourceConfig source = dataSourceConfigMapper.selectOne(
                new LambdaQueryWrapper<DataSourceConfig>()
                    .eq(DataSourceConfig::getDataSourceCode, mapping.getDataSourceCode())
                    .last("LIMIT 1")
                );
            if (source == null)
                throw new IllegalArgumentException("data source not found");
            String timeField = metadataValue(dataset.getId(), "data_time_field");
            String physicalTimeColumn = null;
            if (StringUtils.hasText(timeField)) {
                StorageColumnMapping column = storageColumnMappingMapper.selectOne(
                    new LambdaQueryWrapper<StorageColumnMapping>()
                    .eq(StorageColumnMapping::getStorageMappingCode, mapping.getStorageMappingCode())
                    .eq(StorageColumnMapping::getDataTypeFieldCode, timeField)
                    .last("LIMIT 1")
                );
                if (column == null || !IDENTIFIER
                        .matcher(column.getPhysicalColumnName() == null ? "" : column.getPhysicalColumnName())
                        .matches())
                    throw new IllegalArgumentException("time field mapping not found");
                physicalTimeColumn = column.getPhysicalColumnName();
            }
            String table = quote(mapping.getPhysicalSchemaName()) + "." + quote(mapping.getPhysicalTableName());
            String sql = physicalTimeColumn == null ? "SELECT COUNT(*) FROM " + table
                    : "SELECT COUNT(*), MIN(" + quote(physicalTimeColumn) + "), MAX(" + quote(physicalTimeColumn)
                            + ") FROM " + table;
            try (Connection connection = JdbcUtils.openConnection(source);
                    PreparedStatement statement = connection.prepareStatement(sql);
                    ResultSet rs = statement.executeQuery()) {
                if (!rs.next())
                    return;
                long count = rs.getLong(1);
                dataset.setRecordCount(count);
                datasetMapper.updateById(dataset);
                upsertMetadata(dataset.getId(), "STATISTICS", "record_count", "数据总条数", String.valueOf(count), "NUMBER",
                        null);
                if (physicalTimeColumn != null) {
                    upsertMetadata(dataset.getId(), "STATISTICS", "time_coverage_field", "时间覆盖统计字段", timeField,
                            "STRING", null);
                    upsertMetadata(dataset.getId(), "STATISTICS", "time_coverage_start", "数据起始时间",
                            rs.getObject(2) == null ? null : String.valueOf(rs.getObject(2)), "STRING", null);
                    upsertMetadata(dataset.getId(), "STATISTICS", "time_coverage_end", "数据结束时间",
                            rs.getObject(3) == null ? null : String.valueOf(rs.getObject(3)), "STRING", null);
                }
                upsertMetadata(dataset.getId(), "STATISTICS", "statistics_updated_at", "统计更新时间",
                        LocalDateTime.now(ZoneId.of(JdbcUtils.DEFAULT_TIME_ZONE)).format(ISO_TIME), "STRING", null);
            }
        } catch (Exception ex) {}
    }

    /** 校验物理标识符并添加数据库标识符引用符。 */
    private String quote(String identifier) {
        if (!IDENTIFIER.matcher(identifier).matches())
            throw new IllegalArgumentException("invalid identifier");
        return "`" + identifier + "`";
    }

    /** 将元数据实体转换为接口视图。 */
    private DatasetMetadataView toMetadataView(DatasetMetadata entity) {
        DatasetMetadataView view = new DatasetMetadataView();
        view.setMetadataGroup(entity.getMetadataGroup());
        view.setMetadataKey(entity.getMetadataKey());
        view.setMetadataName(entity.getMetadataName());
        view.setMetadataValue(entity.getMetadataValue());
        view.setValueType(entity.getValueType());
        view.setRemark(entity.getRemark());
        return view;
    }

    /** 将数据集实体转换为目录视图，并补充存储定位元数据。 */
    private DatasetView toView(Dataset entity) {
        DatasetView view = new DatasetView();
        view.setDatasetCode(entity.getDatasetCode());
        view.setDatasetName(entity.getDatasetName());
        view.setAssetType(AssetType.fromCode(entity.getAssetType()));
        view.setAssetTypeName(entity.getAssetType() == null ? null : AssetType.fromCode(entity.getAssetType()).getDisplayName());
        view.setDescription(entity.getDescription());
        view.setTaskConfigCode(entity.getTaskConfigCode());
        view.setTaskCode(entity.getTaskCode());
        view.setDataTopicCode(entity.getDataTopicCode());
        view.setDataTypeCode(entity.getDataTypeCode());
        view.setDataTypeVersionNo(entity.getDataTypeVersionNo());
        view.setStorageMappingCode(entity.getStorageMappingCode());
        view.setDatasetStatus(entity.getDatasetStatus());
        view.setRecordCount(entity.getRecordCount());
        view.setLastSuccessTime(entity.getLastSuccessTime());
        List<DatasetMetadata> metadata = metadataMapper.selectList(new LambdaQueryWrapper<DatasetMetadata>()
                .eq(DatasetMetadata::getDatasetId, entity.getId()).eq(DatasetMetadata::getMetadataGroup, STORAGE));
        Map<String, String> values = metadata.stream().collect(
                Collectors.toMap(DatasetMetadata::getMetadataKey, DatasetMetadata::getMetadataValue, (a, b) -> a));
        view.setStorageType(values.get("storage_type"));
        view.setDatasourceCode(values.get("datasource_code"));
        view.setSchemaName(values.get("schema_name"));
        view.setTableName(values.get("table_name"));
        return view;
    }
}
