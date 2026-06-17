package com.exchange.platform.collection.api.request.datasource;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.exchange.platform.collection.api.enums.CommonStatus;


/**
 * 保存数据源配置请求
 *
 * @param saveOrUpdate     操作类型（save:新增, update:更新）
 * @param dataSourceCode   数据源编码
 * @param dataSourceName   数据源名称
 * @param dataSourceType   数据源类型（MYSQL）
 * @param host             主机
 * @param port             端口
 * @param username         用户名
 * @param password         密码
 * @param databaseName     数据库名
 * @param connectionParams 连接参数
 * @param status           启停状态
 * @param remark           备注
 */
public record DataSourceSaveRequest(
    @NotBlank String saveOrUpdate,
    @NotBlank String dataSourceCode,
    @NotBlank String dataSourceName,
    @NotBlank String dataSourceType,
    @NotBlank String host,
    @NotNull Integer port,
    @NotBlank String username,
    @NotBlank String password,
    @NotBlank String databaseName,
    String connectionParams,
    @NotNull CommonStatus status,
    String remark
) {}
