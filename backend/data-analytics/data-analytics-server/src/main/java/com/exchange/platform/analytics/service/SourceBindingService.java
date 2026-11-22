package com.exchange.platform.analytics.service;

import java.util.List;

import com.exchange.platform.analytics.api.request.SourceBindingSaveRequest;
import com.exchange.platform.analytics.api.request.SourceFieldMappingSaveRequest;
import com.exchange.platform.analytics.api.response.ReadinessView;
import com.exchange.platform.analytics.api.response.SourceBindingView;
import com.exchange.platform.analytics.api.response.SourceFieldMappingView;
import com.exchange.platform.common.core.page.PageResult;

/**
 * 来源绑定配置管理接口。
 *
 * <p>
 * 本接口只负责 SourceBinding / FieldMapping 的配置与 readiness 展示。 主数据发布统一由
 * MasterDataService / MasterDataPublishApplicationService 负责，
 * 避免配置服务和发布服务同时暴露一套发布入口。
 * </p>
 */
public interface SourceBindingService {

    /** 按条件分页查询来源绑定，并返回运行时 readiness。 */
    PageResult<SourceBindingView> list(String keyword, String datasetCode, String masterDataType, String triggerMode,
            Boolean enabled, String readiness, int pageNo, int pageSize);

    /** 查询来源绑定详情。 */
    SourceBindingView detail(Long id);

    /** 新增或更新来源绑定基本信息。 */
    SourceBindingView save(Long id, SourceBindingSaveRequest request);

    /** 查询该绑定的目标字段映射。 */
    List<SourceFieldMappingView> mappings(Long id);

    /** 替换保存该绑定的全部字段映射。 */
    List<SourceFieldMappingView> saveMappings(Long id, SourceFieldMappingSaveRequest request);

    /** 计算来源是否具备发布条件，并返回具体错误。 */
    ReadinessView readiness(Long id);

    /** 启用或停用来源绑定。 */
    void updateEnabled(Long id, boolean enabled);
}
