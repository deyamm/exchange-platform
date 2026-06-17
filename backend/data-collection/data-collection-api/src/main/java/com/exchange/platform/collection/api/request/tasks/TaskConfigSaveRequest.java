package com.exchange.platform.collection.api.request.tasks;

import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.exchange.platform.collection.api.enums.CommonStatus;
import com.exchange.platform.collection.api.enums.ConfigStatus;



/**
 * 保存采集任务配置请求
 *
 * @param saveOrUpdate           操作类型（save:新增, update:更新）
 * @param taskConfigCode         任务配置业务编码（唯一）
 * @param taskCode               所属任务编码
 * @param taskTemplateVersionNo  绑定的任务模板版本号
 * @param dataTopicCode          所属数据主题编码
 * @param dataTypeCode           数据类型编码
 * @param dataTypeVersionNo      数据类型版本号
 * @param storageMappingCode     关联存储映射编码
 * @param ruleCodes              绑定规则编码列表
 * @param configStatus           配置状态
 * @param status                 启停状态
 * @param description            配置说明
 */
public record TaskConfigSaveRequest(
    @NotBlank String saveOrUpdate,
    @NotBlank String taskConfigCode,
    @NotBlank String taskCode,
    @NotNull  Integer taskTemplateVersionNo,
    @NotBlank String dataTopicCode,
    @NotBlank String dataTypeCode,
    @NotNull  Integer dataTypeVersionNo,
    String storageMappingCode,
    List<String> ruleCodes,
    @NotNull ConfigStatus configStatus,
    @NotNull CommonStatus status,
    String description
) {}
