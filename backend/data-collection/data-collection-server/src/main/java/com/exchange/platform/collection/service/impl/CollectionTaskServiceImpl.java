package com.exchange.platform.collection.service.impl;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exchange.platform.collection.api.enums.CommonStatus;
import com.exchange.platform.collection.api.enums.ConfigStatus;
import com.exchange.platform.collection.api.enums.MappingStatus;
import com.exchange.platform.collection.api.enums.SyncStatus;
import com.exchange.platform.collection.api.request.datasource.DataSourceTableRequest;
import com.exchange.platform.collection.api.request.tasks.StorageMappingRequest;
import com.exchange.platform.collection.api.request.tasks.TaskConfigSaveRequest;
import com.exchange.platform.collection.api.request.tasks.TaskStatusUpdateRequest;
import com.exchange.platform.collection.api.request.tasks.TaskSyncRequest;
import com.exchange.platform.collection.api.response.tasks.StorageMappingView;
import com.exchange.platform.collection.api.response.tasks.TaskConfigView;
import com.exchange.platform.collection.api.response.tasks.TaskDetailView;
import com.exchange.platform.collection.api.response.tasks.TaskTemplateVersionView;
import com.exchange.platform.collection.api.response.tasks.TaskTemplateView;
import com.exchange.platform.collection.converter.TaskTemplateConverter;
import com.exchange.platform.collection.entity.tasks.StorageColumnMapping;
import com.exchange.platform.collection.entity.tasks.StorageMapping;
import com.exchange.platform.collection.entity.tasks.TaskConfig;
import com.exchange.platform.collection.entity.tasks.TaskTemplate;
import com.exchange.platform.collection.entity.tasks.TaskTemplateVersion;
import com.exchange.platform.common.core.exception.BusinessException;
import com.exchange.platform.common.core.page.PageResult;
import com.exchange.platform.data.client.DataServiceClient;
import com.exchange.platform.data.client.dto.TaskTemplateSync;
import com.exchange.platform.collection.mapper.tasks.StorageColumnMappingMapper;
import com.exchange.platform.collection.mapper.tasks.StorageMappingMapper;
import com.exchange.platform.collection.mapper.tasks.TaskConfigMapper;
import com.exchange.platform.collection.mapper.tasks.TaskTemplateMapper;
import com.exchange.platform.collection.mapper.tasks.TaskTemplateVersionMapper;
import com.exchange.platform.collection.service.CollectionTaskService;
import com.exchange.platform.collection.service.DataSourceService;

@Service
public class CollectionTaskServiceImpl extends ServiceImpl<TaskTemplateMapper, TaskTemplate> implements CollectionTaskService {
    
    private static final Logger log = LoggerFactory.getLogger(CollectionTaskServiceImpl.class);

    private final TaskTemplateVersionMapper taskTemplateVersionMapper;

    private final TaskConfigMapper taskConfigMapper;
    
    private final StorageMappingMapper storageMappingMapper;
    
    private final StorageColumnMappingMapper storageColumnMappingMapper;
    
    private final DataServiceClient dataServiceClient;

    private final DataSourceService dataSourceService;

    public CollectionTaskServiceImpl(
        TaskTemplateVersionMapper taskTemplateVersionMapper,
        TaskConfigMapper taskConfigMapper,
        StorageMappingMapper storageMappingMapper,
        StorageColumnMappingMapper storageColumnMappingMapper,
        DataServiceClient dataServiceClient,
        DataSourceService dataSourceService
    ) {
        this.taskTemplateVersionMapper = taskTemplateVersionMapper;
        this.taskConfigMapper = taskConfigMapper;
        this.storageMappingMapper = storageMappingMapper;
        this.storageColumnMappingMapper = storageColumnMappingMapper;
        this.dataServiceClient = dataServiceClient;
        this.dataSourceService = dataSourceService;
    }

    /**
     * 查询采集任务列表
     * 支持按任务编码、名称、数据主题、数据类型、配置状态和启停状态筛选，结果分页返回。
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult<TaskTemplateView> queryTaskTemplates(
        String taskCode,
        String taskName,
        Integer pageNo,
        Integer pageSize
    ) {
        log.info("[Service] queryTaskTemplates taskCode={}, taskName={}, pageNo={}, pageSize={}",
                taskCode, taskName, pageNo, pageSize);
        
        int safePageNo = (pageNo == null || pageNo < 1) ? 1 : pageNo;
        int safePageSize = (pageSize == null || pageSize < 1) ? 20 : Math.min(200, pageSize);

        LambdaQueryWrapper<TaskTemplate> queryWrapper = new LambdaQueryWrapper<TaskTemplate>()
            .like(StringUtils.hasText(taskCode), TaskTemplate::getTaskCode, taskCode)
            .like(StringUtils.hasText(taskName), TaskTemplate::getTaskName, taskName)
            .orderByDesc(TaskTemplate::getUpdatedAt);
        
        // 分页查询TaskTemplate
        Page<TaskTemplate> page = this.page(new Page<>(safePageNo, safePageSize), queryWrapper);
        // 为每个TaskTemplate查询最新的TaskConfig，并转换成包含配置信息的TaskTemplateView
        List<TaskTemplateView> viewPage = page.getRecords().stream()
            .map(this::toViewWithConfig)
            .collect(Collectors.toList());
        
        return new PageResult<>(safePageNo, safePageSize, page.getTotal(), viewPage);
    }

    /**
     * 查询指定任务编码的详情信息，用于详情页展示
     * 包含任务模板基本信息、最新版本的配置信息、以及相关联的存储映射和列映射信息
     */
    @Override
    @Transactional(readOnly = true)
    public TaskDetailView getTaskDetail(String taskCode) {
        log.info("[Service] getTaskDetail taskCode={}", taskCode);

        TaskTemplate template = requireTaskTemplate(taskCode);
        TaskDetailView detailView = new TaskDetailView();
        
        // 设置摘要信息
        detailView.setTaskInfo(toViewWithConfig(template));

        // 采集任务的描述信息
        if (template.getCurrentVersionId() != null) {
            detailView.setCurrentVersion(
                TaskTemplateConverter.toVersionView(
                taskTemplateVersionMapper.selectById(template.getCurrentVersionId()))
            );
        }

        // 配置信息
        TaskConfig config = getLatestConfig(taskCode);
        detailView.setConfig(TaskTemplateConverter.toConfigView(config));

        // 存储映射信息
        if (config != null && StringUtils.hasText(config.getStorageMappingCode())) {
            detailView.setStorageMapping(
                loadStorageMappingView(config.getStorageMappingCode())
            );
        }

        return detailView;
    }

    /**
     * 删除指定任务编码的采集任务，需要无配置时才能删除，同时删除相关的任务模板和版本信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTaskTemplate(String taskCode) {
        log.info("[Service] deleteTaskTemplate taskCode={}", taskCode);
        // 查询现有的TaskTemplate，若不存在则直接返回
        TaskTemplate template = this.getOne(
            new LambdaQueryWrapper<TaskTemplate>()
            .eq(TaskTemplate::getTaskCode, taskCode)
            .last("LIMIT 1")
        );
        if (template == null) {
            log.warn("TaskTemplate not found for taskCode={}, nothing to delete", taskCode);
            return;
        }
        // 删除任务模板版本，暂时保留模板版本信息，以备任务执行记录可追溯
        // taskTemplateVersionMapper.delete(new LambdaQueryWrapper<TaskTemplateVersion>()
        //     .eq(TaskTemplateVersion::getTaskCode, taskCode)
        // );
        // 删除任务模板
        this.removeById(template.getId());
        
        log.info("[Service] deleteTaskTemplate completed for taskCode={}", taskCode);
    }

    /**
     * 新增或更新采集任务配置
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskConfigView saveTaskConfig(TaskConfigSaveRequest request) {

        log.info("[Service] saveTaskConfig request={}", request);

        // 校验关联的TaskTemplate必须存在
        requireTaskTemplate(request.taskCode());

        // 校验任务模板版本信息
        requireTaskTemplateVersion(request.taskCode(), request.taskTemplateVersionNo());

        // 按taskConfigCode查询现有配置，根据saveOrUpdate标识判断操作类型
        TaskConfig config = taskConfigMapper.selectOne(new LambdaQueryWrapper<TaskConfig>()
            .eq(TaskConfig::getTaskConfigCode, request.taskConfigCode())
            .last("LIMIT 1"));
        boolean isCreate;
        if ("save".equalsIgnoreCase(request.saveOrUpdate())) {
            // 新增：校验编码不能已存在
            if (config != null) {
                throw new BusinessException(40001, "任务配置编码已存在：" + request.taskConfigCode());
            }
            config = new TaskConfig();
            isCreate = true;
        } else if ("update".equalsIgnoreCase(request.saveOrUpdate())) {
            // 更新：校验编码必须存在
            if (config == null) {
                throw new BusinessException(40001, "任务配置编码不存在：" + request.taskConfigCode());
            }
            isCreate = false;
        } else {
            throw new BusinessException(40001, "saveOrUpdate参数值无效，必须为save或update");
        }

        config.setTaskConfigCode(request.taskConfigCode());
        config.setTaskCode(request.taskCode());
        config.setTaskTemplateVersionNo(request.taskTemplateVersionNo());
        config.setDataTopicCode(request.dataTopicCode());
        config.setDataTypeCode(request.dataTypeCode());
        config.setDataTypeVersionNo(request.dataTypeVersionNo());
        config.setStorageMappingCode(request.storageMappingCode());
        config.setRuleCodesJson(request.ruleCodes() != null ? JSON.toJSONString(request.ruleCodes()) : null);
        config.setConfigStatus(request.configStatus());
        config.setStatus(request.status());
        config.setDescription(request.description());
        
        if (isCreate) {
            taskConfigMapper.insert(config);
        } else {
            taskConfigMapper.updateById(config);
        }
        
        log.info("[Service] saveTaskConfig done, isCreate: {}, saved config: {}", isCreate, config);
        return TaskTemplateConverter.toConfigView(config);
    }

    /**
     * 删除采集任务配置，同时删除相关的存储映射和列映射信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTaskConfig(String taskConfigCode) {
        log.info("[Service] deleteTaskConfig taskConfigCode={}", taskConfigCode);
        // 查询现有配置，若不存在则直接返回
        TaskConfig config = taskConfigMapper.selectOne(new LambdaQueryWrapper<TaskConfig>()
            .eq(TaskConfig::getTaskConfigCode, taskConfigCode)
            .last("LIMIT 1"));
        if (config == null) {
            log.warn("TaskConfig not found for taskConfigCode={}, nothing to delete", taskConfigCode);
            return;
        }
        // 删除列映射信息
        if (StringUtils.hasText(config.getStorageMappingCode())) {
            storageColumnMappingMapper.delete(new LambdaQueryWrapper<StorageColumnMapping>()
                .eq(StorageColumnMapping::getStorageMappingCode, config.getStorageMappingCode())
            );
            // 删除存储映射信息
            storageMappingMapper.delete(new LambdaQueryWrapper<StorageMapping>()
                .eq(StorageMapping::getStorageMappingCode, config.getStorageMappingCode())
            );
        }
        // 删除任务配置
        taskConfigMapper.deleteById(config.getId());
        log.info("[Service] deleteTaskConfig completed for taskConfigCode={}", taskConfigCode);
    }

    /**
     * 从任务处理层同步任务模板数据，进行新增或更新操作
     * 当前是依据输出字段是否有变化来判断是否需要新增，如果输出字段没有变化，仅更新现有记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncTaskTemplates(TaskSyncRequest request) {
        log.info("[Service] syncTaskTemplates request={}", request);
        
        // 调用client接口获取当前的任务模板数据
        List<TaskTemplateSync> syncDataList = dataServiceClient.fetchCurrentTaskTemplates(request.taskCode());

        // 为了解决存在删除任务的情况，同步前先将所有所有的任务模板状态更新为PENDING，等同步数据处理完成后再将没有被更新到的记录状态更新为FAILED，这样可以通过状态来判断哪些任务是被删除了的
        if (StringUtils.hasText(request.taskCode())) {
            this.update(new TaskTemplate().setSyncStatus(SyncStatus.PENDING),
                new LambdaQueryWrapper<TaskTemplate>()
                    .eq(TaskTemplate::getTaskCode, request.taskCode())
            );
        } else {
            this.update(new TaskTemplate().setSyncStatus(SyncStatus.PENDING), null);
        }

        for (TaskTemplateSync syncData : syncDataList) {
            doUpsertTaskTemplate(syncData);
        }

        // 将没有被更新到的记录状态更新为FAILED，表示这些记录可能已经被删除了，需要人工确认后再进行删除或其他处理
        if (StringUtils.hasText(request.taskCode())) {
            this.update(new TaskTemplate().setSyncStatus(SyncStatus.FAILED),
                new LambdaQueryWrapper<TaskTemplate>()
                    .eq(TaskTemplate::getTaskCode, request.taskCode())
                    .eq(TaskTemplate::getSyncStatus, SyncStatus.PENDING)
            );
        } else {
            this.update(new TaskTemplate().setSyncStatus(SyncStatus.FAILED),
                new LambdaQueryWrapper<TaskTemplate>()
                .eq(TaskTemplate::getSyncStatus, SyncStatus.PENDING)
            );
        }

        log.info("[Service] syncTaskTemplates completed, total synced templates: {}", syncDataList.size());

    }
    
    /**
     * 查询指定任务编码的所有版本信息，用于版本管理展示
     * @param taskCode
     * @return
     */
    @Override
    @Transactional(readOnly = true)
    public List<TaskTemplateVersionView> listTemplateVersions(String taskCode) {
        log.info("[Service] listTemplateVersions taskCode={}", taskCode);
        
        // 校验任务模板必须存在
        requireTaskTemplate(taskCode);

        List<TaskTemplateVersion> versions = taskTemplateVersionMapper.selectList(
            new LambdaQueryWrapper<TaskTemplateVersion>()
            .eq(TaskTemplateVersion::getTaskCode, taskCode)
            .orderByDesc(TaskTemplateVersion::getVersionNo)
        );
        return versions.stream()
            .map(TaskTemplateConverter::toVersionView)
            .collect(Collectors.toList());
    }

    /**
     * 查询指定任务编码和版本号的版本详情信息，用于版本详情展示
     * @param taskCode
     * @param versionNo
     * @return
     */
    @Override
    @Transactional(readOnly = true)
    public TaskTemplateVersionView getTemplateVersionDetail(String taskCode, Integer versionNo) {
        log.info("[Service] getTemplateVersionDetail taskCode={}, versionNo={}", taskCode, versionNo);

        TaskTemplateVersion version = requireTaskTemplateVersion(taskCode, versionNo);
        
        return TaskTemplateConverter.toVersionView(version);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StorageMappingView saveStorageMapping(String taskCode, StorageMappingRequest request) {
        log.info("[Service] saveStorageMapping taskCode={}, request={}", taskCode, request);

        // 根据saveOrUpdate标识判断操作类型
        StorageMapping mapping = storageMappingMapper.selectOne(
            new LambdaQueryWrapper<StorageMapping>()
            .eq(StorageMapping::getStorageMappingCode, request.storageMappingCode())
            .last("LIMIT 1")
        );

        boolean isCreate;
        if ("save".equalsIgnoreCase(request.saveOrUpdate())) {
            // 新增：校验编码不能已存在
            if (mapping != null) {
                throw new BusinessException(40001, "存储映射编码已存在：" + request.storageMappingCode());
            }
            mapping = new StorageMapping();
            isCreate = true;
        } else if ("update".equalsIgnoreCase(request.saveOrUpdate())) {
            // 更新：校验编码必须存在
            if (mapping == null) {
                throw new BusinessException(40001, "存储映射编码不存在：" + request.storageMappingCode());
            }
            isCreate = false;
        } else {
            throw new BusinessException(40001, "saveOrUpdate参数值无效，必须为save或update");
        }

        String schemaName = request.physicalSchemaName();
        if (StringUtils.hasText(request.dataSourceCode())) {
            var dataSource = dataSourceService.requireDataSource(request.dataSourceCode());
            if (StringUtils.hasText(dataSource.getDatabaseName())) {
                schemaName = dataSource.getDatabaseName();
            }
        }

        mapping
            .setStorageMappingCode(request.storageMappingCode())
            .setTaskConfigCode(request.taskConfigCode())
            .setDataSourceCode(request.dataSourceCode())
            .setPhysicalSchemaName(schemaName)
            .setPhysicalTableName(request.physicalTableName())
            .setWriteStrategy(request.writeStrategy())
            .setMappingStatus(request.mappingStatus() != null ? request.mappingStatus() : MappingStatus.DRAFT)
            .setConfirmRemark(request.confirmRemark());

        if (isCreate) {
            storageMappingMapper.insert(mapping);
            // 如果是新建映射，则需要将对应taskConfig中的storageMappingCode更新为新建的映射编码
            taskConfigMapper.update(new TaskConfig().setStorageMappingCode(request.storageMappingCode()),
                new LambdaQueryWrapper<TaskConfig>()
                    .eq(TaskConfig::getTaskConfigCode, request.taskConfigCode())
            );
        } else {
            storageMappingMapper.updateById(mapping);
        }

        // 删去并重新插入StorageColumnMapping
        storageColumnMappingMapper.delete(new LambdaQueryWrapper<StorageColumnMapping>()
            .eq(StorageColumnMapping::getStorageMappingCode, request.storageMappingCode())
        );
        List<StorageColumnMapping> columnMappings = new ArrayList<>();
        if (request.columns() != null) {
            for (StorageMappingRequest.ColumnItem column : request.columns()) {
                StorageColumnMapping columnMapping = TaskTemplateConverter.toStorageColumnMapping(request.storageMappingCode(), column);
                storageColumnMappingMapper.insert(columnMapping);
                columnMappings.add(columnMapping);
            }
        }

        log.info("[Service] saveStorageMappingDraft done, mappingCode={}, columns={}", request.storageMappingCode(), columnMappings.size());

        return TaskTemplateConverter.toStorageMappingView(mapping, columnMappings); 
    }

    /**
     * 确认存储映射，主要是将映射状态更新为CONFIRMED，并且根据映射信息在数据源中创建表（如果不存在的话）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public StorageMappingView confirmStorageMapping(String taskCode, StorageMappingRequest request) {
        log.info("[Service] confirmStorageMapping taskCode={}, request={}", taskCode, request);

        requireTaskTemplate(taskCode);

        StorageMapping mapping = storageMappingMapper.selectOne(
            new LambdaQueryWrapper<StorageMapping>()
                .eq(StorageMapping::getStorageMappingCode, request.storageMappingCode())
                .last("LIMIT 1")
        );

        if (mapping == null) {
            throw new BusinessException(40001, "未配置存储映射信息，无法确认");
        }

        // 确认时需要校验数据源信息是否存在
        String schemaName = request.physicalSchemaName();
        var dataSource = dataSourceService.requireDataSource(request.dataSourceCode());
        if (StringUtils.hasText(dataSource.getDatabaseName())) {
            schemaName = dataSource.getDatabaseName();
        }

        mapping
            .setStorageMappingCode(request.storageMappingCode())
            .setTaskConfigCode(request.taskConfigCode())
            .setDataSourceCode(request.dataSourceCode())
            .setPhysicalSchemaName(schemaName)
            .setPhysicalTableName(request.physicalTableName())
            .setWriteStrategy(request.writeStrategy())
            .setMappingStatus(MappingStatus.CONFIRMED)
            .setConfirmRemark(request.confirmRemark());

        storageMappingMapper.updateById(mapping);

        // 删去并重新插入StorageColumnMapping
        storageColumnMappingMapper.delete(new LambdaQueryWrapper<StorageColumnMapping>()
            .eq(StorageColumnMapping::getStorageMappingCode, request.storageMappingCode())
        );
        List<StorageColumnMapping> columnMappings = new ArrayList<>();
        if (request.columns() != null) {
            for (StorageMappingRequest.ColumnItem column : request.columns()) {
                if (!StringUtils.hasText(column.physicalColumnType())) {
                    throw new RuntimeException("physicalColumnType is required for column: " + column.physicalColumnName());
                }
                StorageColumnMapping columnMapping = TaskTemplateConverter.toStorageColumnMapping(request.storageMappingCode(), column);
                storageColumnMappingMapper.insert(columnMapping);
                columnMappings.add(columnMapping);
            }
        }
        // 根据映射信息在数据源中创建表（如果不存在的话）
        dataSourceService.createTableIfMissing(
            dataSource,
            request.physicalTableName(),
            request.columns().stream().map(item -> new DataSourceTableRequest.ColumnItem(
                item.physicalColumnName(),
                item.physicalColumnType(),
                item.required() == null ? null : !item.required(),
                item.defaultValue(),
                item.uniqueKey()
            )).toList()
        );

        taskConfigMapper.update(new TaskConfig().setConfigStatus(ConfigStatus.CONFIRMED),
            new LambdaQueryWrapper<TaskConfig>()
                .eq(TaskConfig::getTaskConfigCode, request.taskConfigCode())
        );

        log.info("[Service] confirmStorageMapping done, mappingCode={}, columns={}", request.storageMappingCode(), columnMappings.size());

        return TaskTemplateConverter.toStorageMappingView(mapping, columnMappings);
    }

    /**
     * 查询指定物理库表的存储映射信息，用于配置页自动加载
     * @param physicalSchemaName
     * @param physicalTableName
     * @return
     */
    @Override
    @Transactional(readOnly = true)
    public StorageMappingView getStorageMapping(String physicalSchemaName, String physicalTableName) {
        log.info("[Service] getStorageMapping physicalSchemaName={}, physicalTableName={}", physicalSchemaName, physicalTableName);
        
        StorageMapping mapping = storageMappingMapper.selectOne(
            new LambdaQueryWrapper<StorageMapping>()
                .eq(StorageMapping::getPhysicalSchemaName, physicalSchemaName)
                .eq(StorageMapping::getPhysicalTableName, physicalTableName)
                .last("LIMIT 1")
        );

        if (mapping == null) {
            log.info("No StorageMapping found for physicalSchemaName={}, physicalTableName={}", physicalSchemaName, physicalTableName);
            return null;
        }

        List<StorageColumnMapping> columnMappings = storageColumnMappingMapper.selectList(
            new LambdaQueryWrapper<StorageColumnMapping>()
                .eq(StorageColumnMapping::getStorageMappingCode, mapping.getStorageMappingCode())
        );

        return TaskTemplateConverter.toStorageMappingView(mapping, columnMappings);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskConfigView updateTaskStatus(String taskCode, TaskStatusUpdateRequest request) {
        log.info("[Service] updateTaskStatus taskCode={}, status={}", taskCode, request.status());

        TaskConfig config = getLatestConfig(taskCode);
        if (config == null) {
            throw new BusinessException(40002, "任务未配置，无法变更状态");
        }

        if (request.status() == CommonStatus.ENABLED && config.getConfigStatus() != ConfigStatus.CONFIRMED) {
            throw new BusinessException(40003, "配置未确认，无法启用任务");
        }

        config.setStatus(request.status());
        taskConfigMapper.updateById(config);
        return TaskTemplateConverter.toConfigView(config);
    }

    /**
     * 查询指定任务编码的最新配置，用于列表展示当前版本配置信息
     * @param taskCode
     * @return
     */
    private TaskConfig getLatestConfig(String taskCode) {
        return taskConfigMapper.selectOne(new LambdaQueryWrapper<TaskConfig>()
            .eq(TaskConfig::getTaskCode, taskCode)
            .orderByDesc(TaskConfig::getUpdatedAt)
            .last("LIMIT 1"));
    }

    /**
     * 向TaskTemplateView转换器增加配置信息，返回包含当前版本配置信息的视图对象，用于列表展示
     * @param template
     * @return
     */
    private TaskTemplateView toViewWithConfig(TaskTemplate template) {
        TaskTemplateView view = TaskTemplateConverter.toView(template);
        TaskConfig config = getLatestConfig(template.getTaskCode());
        view.setStatus(config != null ? config.getStatus() : CommonStatus.DISABLED);
        if (config != null) {
            // 假设TaskTemplateView有相应的setter方法来设置配置信息
            view.setDataTopicCode(config.getDataTopicCode());
            view.setDataTypeCode(config.getDataTypeCode());
            view.setDataTypeVersionNo(config.getDataTypeVersionNo());
            view.setConfigStatus(config.getConfigStatus() != null ? config.getConfigStatus().name() : null);
        }
        return view;
    }

    /**
     * 根据任务编码查询TaskTemplate，若不存在则抛出异常，用于需要确保存在的场景
     * @param taskCode
     * @return
     */
    private TaskTemplate requireTaskTemplate(String taskCode) {
        TaskTemplate template = this.getOne(
            new LambdaQueryWrapper<TaskTemplate>()
            .eq(TaskTemplate::getTaskCode, taskCode)
            .last("LIMIT 1")
        );
        if (template == null) {
            throw new RuntimeException("TaskTemplate not found for taskCode: " + taskCode);
        }
        return template;
    }

    /**
     * 根据任务编码和版本号查询TaskTemplateVersion，若不存在则抛出异常，用于需要确保存在的场景
     * @param taskCode
     * @param versionNo
     * @return
     */
    private TaskTemplateVersion requireTaskTemplateVersion(String taskCode, Integer versionNo) {
        TaskTemplateVersion version = taskTemplateVersionMapper.selectOne(
            new LambdaQueryWrapper<TaskTemplateVersion>()
            .eq(TaskTemplateVersion::getTaskCode, taskCode)
            .eq(TaskTemplateVersion::getVersionNo, versionNo)
            .last("LIMIT 1")
        );
        if (version == null) {
            throw new RuntimeException("TaskTemplateVersion not found for taskCode: " + taskCode + ", versionNo: " + versionNo);
        }
        return version;
    }

    /**
     * 根据storageMappingCode查询StorageMapping及其相关的StorageColumnMapping，转换成StorageMappingView返回
     * @param storageMappingCode
     * @return
     */
    private StorageMappingView loadStorageMappingView(String storageMappingCode) {
        
        // 根据storageMappingCode查询StorageMapping
        StorageMapping mapping = storageMappingMapper.selectOne(
            new LambdaQueryWrapper<StorageMapping>()
            .eq(StorageMapping::getStorageMappingCode, storageMappingCode)
            .last("LIMIT 1")
        );
        if (mapping == null) {
            return null;
        }
        // 查询StorageMapping中的字段映射信息
        List<StorageColumnMapping> columnMappings = storageColumnMappingMapper.selectList(
            new LambdaQueryWrapper<StorageColumnMapping>()
            .eq(StorageColumnMapping::getStorageMappingCode, storageMappingCode)
        );
        // 转换成StorageMappingView
        return TaskTemplateConverter.toStorageMappingView(mapping, columnMappings);
    }
    
    /**
     * 处理从client接口获取的任务模板数据，进行新增或更新操作，同步更新TaskTemplate和TaskTemplateVersion表
     * @param syncData
     */
    private void doUpsertTaskTemplate(TaskTemplateSync syncData) {

        // 将输出字段列表转换成JSON字符串存储
        String outputFieldsJson = JSON.toJSONString(syncData.outputFields());
        // 判断幂等
        String outputFieldsHash = md5Hex(outputFieldsJson);

        // 通过taskcode查询最新版本记录，如果hash记录相同，则更新任务模板中基础信息，不生成新版本，如果不存在相同hash记录，需要新增版本记录
        TaskTemplateVersion latestVersion = taskTemplateVersionMapper.selectOne(
            new LambdaQueryWrapper<TaskTemplateVersion>()
            .eq(TaskTemplateVersion::getTaskCode, syncData.taskCode())
            .orderByDesc(TaskTemplateVersion::getVersionNo)
            .last("LIMIT 1")
        );
        if (latestVersion != null && latestVersion.getOutputFieldsHash().equals(outputFieldsHash)) {
            // 更新现有模板版本的基础信息，但不更新输出字段相关的信息，也不新增版本记录
            latestVersion.setTaskName(syncData.taskName())
                .setTaskDesc(syncData.taskDesc())
                .setHandlerName(syncData.handlerName())
                .setDataSource(syncData.dataSource())
                .setAssetType(syncData.assetType())
                .setBizType(syncData.bizType())
                .setParamsSchemaJson(syncData.paramsSchema() != null ? JSON.toJSONString(syncData.paramsSchema()) : null)
                .setRuleCategoryCodes(syncData.ruleCategoryCodes() != null ? String.join(",", syncData.ruleCategoryCodes()) : null)
                .setSyncTime(syncData.syncTime() != null ? syncData.syncTime() : LocalDateTime.now());
            taskTemplateVersionMapper.updateById(latestVersion);
            // 更新taskTemplate的同步状态和最新同步时间，但不新增版本记录
            this.update(new TaskTemplate()
                .setSyncStatus(SyncStatus.SYNCED)
                .setLatestSyncTime(syncData.syncTime() != null ? syncData.syncTime() : LocalDateTime.now()),
                new LambdaQueryWrapper<TaskTemplate>()
                    .eq(TaskTemplate::getTaskCode, syncData.taskCode())
            );
            log.info("No output fields changed for taskCode={}, update the existing version", syncData.taskCode());
            return;
        }
        // 不存在相同hash的记录，说明输出字段有变化，需要新增版本记录，最新版本号在原有基础上加1，如果不存在记录则版本号为1
        Integer newVersionNo = (latestVersion == null) ? 1 : (latestVersion.getVersionNo() + 1);

        String paramsJson = syncData.paramsSchema() != null ? JSON.toJSONString(syncData.paramsSchema()) : null;
        String ruleCats = syncData.ruleCategoryCodes() != null ? String.join(",", syncData.ruleCategoryCodes()) : null;

        TaskTemplateVersion newVersion = new TaskTemplateVersion()
            .setVersionNo(newVersionNo)
            .setTaskCode(syncData.taskCode())
            .setTaskName(syncData.taskName())
            .setTaskDesc(syncData.taskDesc())
            .setHandlerName(syncData.handlerName())
            .setDataSource(syncData.dataSource())
            .setAssetType(syncData.assetType())
            .setBizType(syncData.bizType())
            .setParamsSchemaJson(paramsJson)
            .setOutputFieldsJson(outputFieldsJson)
            .setOutputFieldsHash(outputFieldsHash)
            .setRuleCategoryCodes(ruleCats)
            .setChangeSummary("变更")
            .setSyncTime(syncData.syncTime() != null ? syncData.syncTime() : LocalDateTime.now());
        
        // 只会出现新增的情况
        taskTemplateVersionMapper.insert(newVersion);

        // 更新或创建TaskTemplate，使其指向最新的版本
        TaskTemplate template = this.getOne(
            new LambdaQueryWrapper<TaskTemplate>()
            .eq(TaskTemplate::getTaskCode, syncData.taskCode())
            .last("LIMIT 1")
        );
        if (template == null) {
            template = new TaskTemplate()
                .setTaskCode(syncData.taskCode());
        }
        template.setTaskName(syncData.taskName())
            .setCurrentVersionId(newVersion.getId())
            .setCurrentVersionNo(newVersionNo)
            .setSyncStatus(SyncStatus.SYNCED)
            .setLatestSyncTime(newVersion.getSyncTime());
        this.saveOrUpdate(template);
        log.info("Upserted TaskTemplate for taskCode={}, newVersionNo={}, outputFieldsHash={}", 
            syncData.taskCode(), newVersionNo, outputFieldsHash);
    }

    /**
    * 计算字符串的MD5值，返回16进制字符串
    * @param input
    * @return
    */
   private static String md5Hex(String input) {
       try {
           MessageDigest md = MessageDigest.getInstance("MD5");
           byte[] digest = md.digest(input.getBytes("UTF-8"));
           StringBuilder sb = new StringBuilder();
           for (byte b : digest) {
               sb.append(String.format("%02x", b));
           }
           return sb.toString();
       } catch (Exception e) {
           throw new RuntimeException("Failed to calculate MD5 hash", e);
       }
   }
}
