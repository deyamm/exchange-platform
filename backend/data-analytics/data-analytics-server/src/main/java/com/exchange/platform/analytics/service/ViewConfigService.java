package com.exchange.platform.analytics.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exchange.platform.analytics.api.enums.CommonStatus;
import com.exchange.platform.analytics.api.request.ViewConfigSaveRequest;
import com.exchange.platform.analytics.api.response.ViewConfigView;
import com.exchange.platform.analytics.entity.ViewConfig;
import com.exchange.platform.common.core.page.PageResult;

public interface ViewConfigService extends IService<ViewConfig> {

    PageResult<ViewConfigView> getViewConfigList(String viewName, String viewCode, String sceneCode, String viewType, CommonStatus status, Integer pageNo, Integer pageSize);

    ViewConfigView saveOrUpdateViewConfig(ViewConfigSaveRequest request);

    void deleteViewConfig(String viewCode);
}