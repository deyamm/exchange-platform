package com.exchange.platform.analytics.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exchange.platform.analytics.api.enums.CommonStatus;
import com.exchange.platform.analytics.api.request.ViewConfigSaveRequest;
import com.exchange.platform.analytics.api.response.ViewConfigView;
import com.exchange.platform.analytics.converter.ViewConfigConverter;
import com.exchange.platform.analytics.entity.ViewConfig;
import com.exchange.platform.analytics.mapper.ViewConfigMapper;
import com.exchange.platform.analytics.service.ViewConfigService;
import com.exchange.platform.common.core.page.PageResult;


@Service
public class ViewConfigServiceImpl extends ServiceImpl<ViewConfigMapper, ViewConfig> implements ViewConfigService {

    private static final Logger log = LoggerFactory.getLogger(ViewConfigServiceImpl.class);

    @Override
    @Transactional(readOnly = true)
    public PageResult<ViewConfigView> getViewConfigList(String viewName, String viewCode, String sceneCode, String viewType, CommonStatus status, Integer pageNo, Integer pageSize) {

        log.info("[Service] getViewConfigList viewName={}, viewCode={}, sceneCode={}, viewType={}, status={}, pageNo={}, pageSize={}", viewName, viewCode, sceneCode, viewType, status, pageNo, pageSize);

        Integer safePageNo = (pageNo == null || pageNo < 1) ? 1 : pageNo;
        Integer safePageSize = (pageSize == null || pageSize < 1) ? 20 : Math.min(200, pageSize);

        LambdaQueryWrapper<ViewConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(viewName), ViewConfig::getViewName, viewName)
            .eq(StringUtils.hasText(viewCode), ViewConfig::getViewCode, viewCode)
            .eq(StringUtils.hasText(sceneCode), ViewConfig::getSceneCode, sceneCode)
            .eq(StringUtils.hasText(viewType), ViewConfig::getViewType, viewType)
            .eq(status != null, ViewConfig::getStatus, status);

        Page<ViewConfig> page = this.page(new Page<>(safePageNo, safePageSize), wrapper);
        List<ViewConfigView> viewList = page.getRecords().stream()
            .map(ViewConfigConverter::toViewConfigView)
            .collect(Collectors.toList());
        return new PageResult<>(safePageNo, safePageSize, page.getTotal(), viewList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ViewConfigView saveOrUpdateViewConfig(ViewConfigSaveRequest request) {
        log.info("[Service] saveOrUpdateViewConfig request={}", request);

        ViewConfig entity = this.getOne(
            new LambdaQueryWrapper<ViewConfig>()
                .eq(ViewConfig::getViewCode, request.viewCode())
        );

        if (entity == null) {
            entity = new ViewConfig();
            entity.setViewCode(request.viewCode());
        }

        entity.setViewName(request.viewName())
            .setSceneCode(request.sceneCode())
            .setViewType(request.viewType())
            .setDefaultFlag(request.defaultFlag())
            .setFieldConfigJson(JSON.toJSONString(request.fieldConfig()))
            .setSortConfigJson(JSON.toJSONString(request.sortConfig()))
            .setFilterConfigJson(JSON.toJSONString(request.filterConfig()))
            .setStatus(request.status());

        this.saveOrUpdate(entity);
        return ViewConfigConverter.toViewConfigView(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteViewConfig(String viewCode) {
        log.info("[Service] deleteViewConfig viewCode={}", viewCode);

        this.remove(new LambdaQueryWrapper<ViewConfig>().eq(ViewConfig::getViewCode, viewCode));
    }
}