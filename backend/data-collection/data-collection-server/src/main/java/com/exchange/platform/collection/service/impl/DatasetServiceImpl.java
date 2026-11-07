package com.exchange.platform.collection.service.impl;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
import com.exchange.platform.collection.api.request.execution.DatasetMetadataUpdateRequest;
import com.exchange.platform.collection.api.response.execution.DatasetDetailView;
import com.exchange.platform.collection.api.response.execution.DatasetMetadataView;
import com.exchange.platform.collection.api.response.execution.DatasetView;
import com.exchange.platform.collection.converter.ExecutionConverter;
import com.exchange.platform.collection.entity.dataType.DataTypeVersion;
import com.exchange.platform.collection.entity.execution.Dataset;
import com.exchange.platform.collection.entity.execution.DatasetMetadata;
import com.exchange.platform.collection.entity.execution.ExecutionConfigSnapshot;
import com.exchange.platform.collection.entity.execution.ResultSummary;
import com.exchange.platform.collection.entity.execution.TaskRun;
import com.exchange.platform.collection.entity.tasks.StorageMapping;
import com.exchange.platform.collection.mapper.dataType.DataTypeVersioinMapper;
import com.exchange.platform.collection.mapper.execution.DatasetMapper;
import com.exchange.platform.collection.mapper.execution.DatasetMetadataMapper;
import com.exchange.platform.collection.mapper.execution.ResultSummaryMapper;
import com.exchange.platform.collection.mapper.execution.TaskRunMapper;
import com.exchange.platform.collection.service.DatasetService;
import com.exchange.platform.common.core.exception.BusinessException;
import com.exchange.platform.common.core.page.PageResult;

/**
 * 数据集管理服务实现。
 * <p>负责数据集执行前校验、成功执行登记、目录查询、详情查询及元数据维护。</p>
 */
@Service
public class DatasetServiceImpl implements DatasetService {
    private static final Logger log = LoggerFactory.getLogger(DatasetServiceImpl.class);
    private static final String STORAGE = "STORAGE";
    private final DatasetMapper datasetMapper;
    private final DatasetMetadataMapper metadataMapper;
    private final TaskRunMapper taskRunMapper;
    private final ResultSummaryMapper summaryMapper;
    private final DataTypeVersioinMapper dataTypeVersionMapper;

    public DatasetServiceImpl(DatasetMapper datasetMapper, DatasetMetadataMapper metadataMapper,
        TaskRunMapper taskRunMapper, ResultSummaryMapper summaryMapper, DataTypeVersioinMapper dataTypeVersionMapper) {
        this.datasetMapper = datasetMapper;
        this.metadataMapper = metadataMapper;
        this.taskRunMapper = taskRunMapper;
        this.summaryMapper = summaryMapper;
        this.dataTypeVersionMapper = dataTypeVersionMapper;
        log.info("DatasetService initialized");
    }

    /** 校验执行使用的数据集编码、状态及正式物理表身份。 */
    @Override
    @Transactional(readOnly = true)
    public void validateForExecution(StorageMapping mapping) {
        log.info("Validating dataset before execution, datasetCode={}, storageMappingCode={}",
            mapping == null ? null : mapping.getDatasetCode(), mapping == null ? null : mapping.getStorageMappingCode());
        if (mapping == null || !StringUtils.hasText(mapping.getDatasetCode())
            || !mapping.getDatasetCode().matches("^[A-Z0-9_]{1,64}$")) {
            throw new BusinessException(40006, "存储映射缺少合法的数据集编码（仅允许大写字母、数字和下划线）");
        }
        Dataset dataset = findByCode(mapping.getDatasetCode());
        log.debug("Dataset pre-execution validation lookup completed, datasetCode={}, exists={}",
            mapping.getDatasetCode(), dataset != null);
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

    /** 登记一次成功执行对应的数据集主档和存储元数据。 */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void registerSuccessfulRun(TaskRun run, ExecutionConfigSnapshot snapshot, StorageMapping mapping, ResultSummary summary) {
        log.info("Registering successful dataset run, runId={}, datasetCode={}, storageMappingCode={}",
            run == null ? null : run.getRunId(), snapshot == null ? null : snapshot.getDatasetCode(),
            mapping == null ? null : mapping.getStorageMappingCode());
        Dataset dataset = findByCode(snapshot.getDatasetCode());
        LocalDateTime now = LocalDateTime.now();
        if (dataset == null) {
            log.info("Creating dataset master record, datasetCode={}", snapshot.getDatasetCode());
            dataset = new Dataset()
                .setDatasetCode(snapshot.getDatasetCode())
                .setDatasetName(StringUtils.hasText(mapping.getDatasetName()) ? mapping.getDatasetName() : snapshot.getDatasetCode())
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
            log.info("Updating dataset master record, datasetId={}, datasetCode={}", dataset.getId(), dataset.getDatasetCode());
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
        upsertStorageMetadata(dataset.getId(), "storage_type", "存储类型", "MYSQL");
        upsertStorageMetadata(dataset.getId(), "datasource_code", "数据源编码", mapping.getDataSourceCode());
        upsertStorageMetadata(dataset.getId(), "schema_name", "Schema 名称", mapping.getPhysicalSchemaName());
        upsertStorageMetadata(dataset.getId(), "table_name", "物理表名称", mapping.getPhysicalTableName());
        log.info("Dataset registration completed, datasetId={}, datasetCode={}, runId={}", dataset.getId(), dataset.getDatasetCode(), run.getRunId());
    }

    /** 按条件分页查询数据集目录。 */
    @Override
    @Transactional(readOnly = true)
    public PageResult<DatasetView> query(String datasetCode, String datasetName, String topicCode, String dataTypeCode,
        Integer datasetStatus, String taskConfigCode, String startTime, String endTime, Integer pageNo, Integer pageSize) {
        log.info("Querying datasets, datasetCode={}, datasetName={}, topicCode={}, dataTypeCode={}, status={}, taskConfigCode={}, pageNo={}, pageSize={}",
            datasetCode, datasetName, topicCode, dataTypeCode, datasetStatus, taskConfigCode, pageNo, pageSize);
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
        if (datasetStatus != null) wrapper.eq(Dataset::getDatasetStatus, datasetStatus);
        Page<Dataset> page = datasetMapper.selectPage(new Page<>(safePageNo, safePageSize), wrapper);
        log.info("Dataset query completed, pageNo={}, pageSize={}, total={}", safePageNo, safePageSize, page.getTotal());
        return new PageResult<>(safePageNo, safePageSize, page.getTotal(), page.getRecords().stream().map(this::toView).collect(Collectors.toList()));
    }

    /** 查询数据集详情及最近成功执行信息。 */
    @Override
    @Transactional(readOnly = true)
    public DatasetDetailView getDetail(String datasetCode) {
        log.info("Getting dataset detail, datasetCode={}", datasetCode);
        Dataset dataset = requireDataset(datasetCode);
        DatasetDetailView detail = new DatasetDetailView();
        detail.setDataset(toView(dataset));
        detail.setMetadata(getMetadata(datasetCode));
        detail.setStructure(getStructure(datasetCode));
        if (StringUtils.hasText(dataset.getLatestTaskRunId())) {
            detail.setLatestTaskRun(ExecutionConverter.toRunView(taskRunMapper.selectOne(new LambdaQueryWrapper<TaskRun>().eq(TaskRun::getRunId, dataset.getLatestTaskRunId()).last("LIMIT 1"))));
            detail.setLatestResultSummary(ExecutionConverter.toSummaryView(summaryMapper.selectOne(new LambdaQueryWrapper<ResultSummary>().eq(ResultSummary::getRunId, dataset.getLatestTaskRunId()).last("LIMIT 1"))));
        }
        log.info("Dataset detail loaded, datasetCode={}, latestTaskRunId={}", datasetCode, dataset.getLatestTaskRunId());
        return detail;
    }

    /** 查询数据集扩展元数据。 */
    @Override
    @Transactional(readOnly = true)
    public List<DatasetMetadataView> getMetadata(String datasetCode) {
        log.info("Getting dataset metadata, datasetCode={}", datasetCode);
        Dataset dataset = requireDataset(datasetCode);
        List<DatasetMetadataView> result = metadataMapper.selectList(new LambdaQueryWrapper<DatasetMetadata>().eq(DatasetMetadata::getDatasetId, dataset.getId())
            .orderByAsc(DatasetMetadata::getMetadataGroup).orderByAsc(DatasetMetadata::getMetadataKey)).stream().map(this::toMetadataView).collect(Collectors.toList());
        log.info("Dataset metadata loaded, datasetCode={}, itemCount={}", datasetCode, result.size());
        return result;
    }

    /** 更新用户可维护的数据集名称、说明和扩展元数据。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMetadata(String datasetCode, DatasetMetadataUpdateRequest request) {
        log.info("Updating dataset metadata, datasetCode={}, itemCount={}", datasetCode,
            request == null || request.items() == null ? 0 : request.items().size());
        Dataset dataset = requireDataset(datasetCode);
        if (StringUtils.hasText(request.datasetName())) {
            dataset.setDatasetName(request.datasetName());
        }
        if (request.datasetDesc() != null) {
            dataset.setDescription(request.datasetDesc());
        }
        datasetMapper.updateById(dataset);
        for (DatasetMetadataUpdateRequest.Item item : request.items()) {
            if (STORAGE.equalsIgnoreCase(item.metadataGroup())) {
                throw new BusinessException(40006, "存储定位信息只能由执行成功后的登记流程维护");
            }
            if (!StringUtils.hasText(item.metadataGroup()) || !StringUtils.hasText(item.metadataKey())) {
                throw new BusinessException(40006, "元数据分组和编码不能为空");
            }
            DatasetMetadata current = metadataMapper.selectOne(new LambdaQueryWrapper<DatasetMetadata>().eq(DatasetMetadata::getDatasetId, dataset.getId())
                .eq(DatasetMetadata::getMetadataGroup, item.metadataGroup()).eq(DatasetMetadata::getMetadataKey, item.metadataKey()).last("LIMIT 1"));
            if (current == null) current = new DatasetMetadata().setDatasetId(dataset.getId()).setMetadataGroup(item.metadataGroup()).setMetadataKey(item.metadataKey());
            current.setMetadataName(item.metadataName()).setMetadataValue(item.metadataValue()).setValueType(item.valueType()).setRemark(item.remark());
            if (current.getId() == null) metadataMapper.insert(current); else metadataMapper.updateById(current);
        }
        log.info("Dataset metadata update completed, datasetCode={}", datasetCode);
    }

    /** 查询数据集当前引用的数据类型版本字段结构。 */
    @Override
    @Transactional(readOnly = true)
    public List<?> getStructure(String datasetCode) {
        log.info("Getting dataset structure, datasetCode={}", datasetCode);
        Dataset dataset = requireDataset(datasetCode);
        DataTypeVersion version = dataTypeVersionMapper.selectOne(new LambdaQueryWrapper<DataTypeVersion>()
            .eq(DataTypeVersion::getDataTypeCode, dataset.getDataTypeCode()).eq(DataTypeVersion::getVersion, dataset.getDataTypeVersionNo()).last("LIMIT 1"));
        if (version == null || !StringUtils.hasText(version.getFieldSchemaContent())) {
            log.info("Dataset structure is empty, datasetCode={}", datasetCode);
            return Collections.emptyList();
        }
        List<?> structure = JSON.parseArray(version.getFieldSchemaContent(), Map.class);
        log.info("Dataset structure loaded, datasetCode={}, fieldCount={}", datasetCode, structure.size());
        return structure;
    }

    /** 按稳定数据集编码查询主档。 */
    private Dataset findByCode(String datasetCode) {
        log.debug("Finding dataset by code, datasetCode={}", datasetCode);
        return datasetMapper.selectOne(new LambdaQueryWrapper<Dataset>().eq(Dataset::getDatasetCode, datasetCode).last("LIMIT 1"));
    }

    /** 查询并校验数据集主档存在。 */
    private Dataset requireDataset(String datasetCode) {
        log.debug("Requiring dataset, datasetCode={}", datasetCode);
        Dataset result = findByCode(datasetCode);
        if (result == null) throw new BusinessException(40003, "数据集不存在：" + datasetCode);
        return result;
    }

    /** 查询指定数据集的存储元数据值。 */
    private String metadataValue(Long datasetId, String key) {
        log.debug("Reading dataset storage metadata, datasetId={}, key={}", datasetId, key);
        DatasetMetadata value = metadataMapper.selectOne(new LambdaQueryWrapper<DatasetMetadata>().eq(DatasetMetadata::getDatasetId, datasetId).eq(DatasetMetadata::getMetadataGroup, STORAGE).eq(DatasetMetadata::getMetadataKey, key).last("LIMIT 1"));
        return value == null ? null : value.getMetadataValue();
    }

    /** 创建或更新单项存储定位元数据。 */
    private void upsertStorageMetadata(Long datasetId, String key, String name, String value) {
        log.debug("Upserting dataset storage metadata, datasetId={}, key={}", datasetId, key);
        DatasetMetadata item = metadataMapper.selectOne(new LambdaQueryWrapper<DatasetMetadata>().eq(DatasetMetadata::getDatasetId, datasetId).eq(DatasetMetadata::getMetadataGroup, STORAGE).eq(DatasetMetadata::getMetadataKey, key).last("LIMIT 1"));
        if (item == null) { item = new DatasetMetadata().setDatasetId(datasetId).setMetadataGroup(STORAGE).setMetadataKey(key); }
        item.setMetadataName(name).setMetadataValue(value).setValueType("STRING");
        if (item.getId() == null) metadataMapper.insert(item); else metadataMapper.updateById(item);
    }

    /** 将元数据实体转换为接口视图。 */
    private DatasetMetadataView toMetadataView(DatasetMetadata entity) {
        log.debug("Converting dataset metadata entity, metadataId={}", entity == null ? null : entity.getId());
        DatasetMetadataView view = new DatasetMetadataView(); view.setMetadataGroup(entity.getMetadataGroup()); view.setMetadataKey(entity.getMetadataKey()); view.setMetadataName(entity.getMetadataName()); view.setMetadataValue(entity.getMetadataValue()); view.setValueType(entity.getValueType()); view.setRemark(entity.getRemark()); return view;
    }

    /** 将数据集实体转换为目录视图，并补充存储定位元数据。 */
    private DatasetView toView(Dataset entity) {
        log.debug("Converting dataset entity, datasetId={}, datasetCode={}", entity == null ? null : entity.getId(), entity == null ? null : entity.getDatasetCode());
        DatasetView view = new DatasetView(); view.setDatasetCode(entity.getDatasetCode()); view.setDatasetName(entity.getDatasetName()); view.setDescription(entity.getDescription()); view.setTaskConfigCode(entity.getTaskConfigCode()); view.setTaskCode(entity.getTaskCode()); view.setDataTopicCode(entity.getDataTopicCode()); view.setDataTypeCode(entity.getDataTypeCode()); view.setDataTypeVersionNo(entity.getDataTypeVersionNo()); view.setStorageMappingCode(entity.getStorageMappingCode()); view.setDatasetStatus(entity.getDatasetStatus()); view.setRecordCount(entity.getRecordCount()); view.setLastSuccessTime(entity.getLastSuccessTime());
        List<DatasetMetadata> metadata = metadataMapper.selectList(new LambdaQueryWrapper<DatasetMetadata>().eq(DatasetMetadata::getDatasetId, entity.getId()).eq(DatasetMetadata::getMetadataGroup, STORAGE));
        Map<String, String> values = metadata.stream().collect(Collectors.toMap(DatasetMetadata::getMetadataKey, DatasetMetadata::getMetadataValue, (a, b) -> a)); view.setStorageType(values.get("storage_type")); view.setDatasourceCode(values.get("datasource_code")); view.setSchemaName(values.get("schema_name")); view.setTableName(values.get("table_name")); return view;
    }
}
