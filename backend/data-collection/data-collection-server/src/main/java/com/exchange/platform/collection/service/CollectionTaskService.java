package com.exchange.platform.collection.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exchange.platform.collection.api.request.tasks.StorageMappingRequest;
import com.exchange.platform.collection.api.request.tasks.TaskConfigSaveRequest;
import com.exchange.platform.collection.api.request.tasks.TaskStatusUpdateRequest;
import com.exchange.platform.collection.api.request.tasks.TaskSyncRequest;
import com.exchange.platform.collection.api.response.tasks.StorageMappingView;
import com.exchange.platform.collection.api.response.tasks.TaskConfigView;
import com.exchange.platform.collection.api.response.tasks.TaskDetailView;
import com.exchange.platform.collection.api.response.tasks.TaskTemplateVersionView;
import com.exchange.platform.collection.api.response.tasks.TaskTemplateView;
import com.exchange.platform.collection.entity.tasks.TaskTemplate;
import com.exchange.platform.common.core.page.PageResult;


public interface CollectionTaskService extends IService<TaskTemplate> {
    
    /**
     * 查询采集任务列表，用于列表展示
     * 支持按任务编码、名称、数据主题、数据类型、配置状态和启停状态筛选，结果分页返回。
     */
    PageResult<TaskTemplateView> queryTaskTemplates(
        String taskCode,
        String taskName,
        Integer pageNo,
        Integer pageSize
    );

    /**
     * 查询指定任务编码的详情信息，用于详情页展示
     * 包含任务模板基本信息、最新版本的配置信息、以及相关联的存储映射和列映射信息
     * @param taskCode
     * @return
     */
    TaskDetailView getTaskDetail(String taskCode);

    /**
     * 删除指定任务编码的采集任务，需要无配置时才能删除，同时删除相关的任务模板和版本信息
     * @param taskCode
     * @return
     */
    void deleteTaskTemplate(String taskCode);

    /**
     * 新增或更新采集任务配置
     * @param configView
     * @return
     */
    TaskConfigView saveTaskConfig(TaskConfigSaveRequest request);

    /**
     * 删除采集任务配置，同时删除相关的存储映射和列映射信息
     * @param taskConfigCode 任务配置编码
     * @param request
     */
    void deleteTaskConfig(String taskConfigCode);

    /**
     * 触发任务模板同步
     * @param request 包含同步所需的参数
     */
    void syncTaskTemplates(TaskSyncRequest request);

    /**
     * 查询指定任务编码的所有版本信息，用于版本管理展示
     * @param taskCode
     * @return
     */
    List<TaskTemplateVersionView> listTemplateVersions(String taskCode);

    /**
     * 查询指定任务编码和版本号的版本详情信息，用于版本详情展示
     * @param taskCode
     * @param versionNo
     * @return
     */
    TaskTemplateVersionView getTemplateVersionDetail(String taskCode, Integer versionNo);

    /**
     * 保存或更新存储映射关系
     * @param taskCode
     * @param request
     * @return
     */
    StorageMappingView saveStorageMapping(String taskCode, StorageMappingRequest request);

    /**
     * 确认存储映射关系
     * @param taskCode
     * @param request
     * @return
     */
    StorageMappingView confirmStorageMapping(String taskCode, StorageMappingRequest request);


    /**
     * 查询指定物理库表的存储映射信息，用于配置页自动加载
     * @param physicalSchemaName
     * @param physicalTableName
     * @return
     */
    StorageMappingView getStorageMapping(String physicalSchemaName, String physicalTableName);

    /**
     * 更新任务启停状态
     * @param taskCode
     * @param request
     * @return
     */
    TaskConfigView updateTaskStatus(String taskCode, TaskStatusUpdateRequest request);
    
}
