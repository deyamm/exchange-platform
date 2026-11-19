package com.exchange.platform.analytics.service.masterdata;

import java.util.List;
import com.exchange.platform.analytics.api.request.*;
import com.exchange.platform.analytics.api.response.*;
import com.exchange.platform.analytics.api.enums.TriggerType;
import com.exchange.platform.common.core.page.PageResult;

/**
 * 来源绑定配置的管理接口。
 * 来源绑定把采集服务的 Dataset 与主数据类型、字段映射和触发策略关联起来，
 * readiness 通过后才能进入发布流程。
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
    /** 按来源绑定创建并执行同步批次。 */
    SyncBatchView publish(Long id, String taskRunId, String syncType, TriggerType triggerType);
}
