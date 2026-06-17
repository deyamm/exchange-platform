package com.exchange.platform.analytics.service.impl;

import java.util.List;
import java.util.stream.Collectors;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exchange.platform.analytics.api.enums.AppSceneType;
import com.exchange.platform.analytics.api.enums.CommonStatus;
import com.exchange.platform.analytics.api.request.SceneSaveRequest;
import com.exchange.platform.analytics.api.response.AppSceneView;
import com.exchange.platform.analytics.converter.AppSceneConverter;
import com.exchange.platform.analytics.entity.AppScene;
import com.exchange.platform.analytics.mapper.AppSceneMapper;
import com.exchange.platform.analytics.service.AppSceneService;
import com.exchange.platform.common.core.page.PageResult;


@Service
public class AppSceneServiceImpl extends ServiceImpl<AppSceneMapper, AppScene> implements AppSceneService {
    
    private static final Logger log = LoggerFactory.getLogger(AppSceneServiceImpl.class);

    /** 
     * 获取应用场景列表，支持分页和按名称、分类、状态过滤 
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult<AppSceneView> getAppScenePageResult(String sceneName, AppSceneType sceneType, CommonStatus status, Integer pageNo, Integer pageSize) {
        
        log.info("[Service] getAppScenePageResult sceneName={}, sceneType={}, status={}, pageNo={}, pageSize={}",
                sceneName, sceneType, status, pageNo, pageSize);

        // 构建查询条件
        LambdaQueryWrapper<AppScene> wrapper = new LambdaQueryWrapper<>();

        wrapper.like(StringUtils.hasText(sceneName), AppScene::getSceneName, sceneName)
            .eq(sceneType != null, AppScene::getSceneType, sceneType)
            .eq(status != null, AppScene::getStatus, status);

        int safePageNo = (pageNo == null || pageNo < 1) ? 1 : pageNo;
        int safePageSize = (pageSize == null || pageSize < 1) ? 20 : Math.min(200, pageSize);

        Page<AppScene> page = this.page(new Page<>(safePageNo, safePageSize), wrapper);

        List<AppSceneView> viewList = page.getRecords().stream()
            .map(AppSceneConverter::toView)
            .collect(Collectors.toList());
        return new PageResult<>(safePageNo, safePageSize, page.getTotal(), viewList);
    }

    /** 获取所有已启用的应用场景列表，不分页 */
    @Override
    @Transactional(readOnly = true)
    public List<AppSceneView> getAllEnabledAppSceneList() {
        log.info("[Service] getAllEnabledAppSceneList");

        LambdaQueryWrapper<AppScene> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppScene::getStatus, CommonStatus.ENABLED);
        List<AppScene> appScenes = this.list(wrapper);
        
        return appScenes.stream().map(AppSceneConverter::toView).collect(Collectors.toList());
    }

    /**
     * 保存或更新应用场景
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppSceneView saveOrUpdateAppScene(SceneSaveRequest request) {
        
        log.info("[Service] saveOrUpdateAppScene appSceneView={}", request);

        AppScene appScene = this.getOne(
            new LambdaQueryWrapper<AppScene>()
                .eq(AppScene::getSceneCode, request.sceneCode())
                .last("LIMIT 1")
        );

        if (appScene == null) {
            appScene = new AppScene().setSceneCode(request.sceneCode());
        }
        appScene.setSceneName(request.sceneName())
            .setSceneType(request.sceneType())
            .setSortNo(request.sortNo())
            .setStatus(request.status())
            .setDescription(request.description());
        
        saveOrUpdate(appScene);

        log.info("[Service] saveOrUpdateAppScene saved appScene={}", appScene);
        
        return AppSceneConverter.toView(appScene);
    }

    /**
     * 删除应用场景
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAppScene(String sceneCode) {
        log.info("[Service] deleteAppScene sceneCode={}", sceneCode);

        this.remove(new LambdaQueryWrapper<AppScene>().eq(AppScene::getSceneCode, sceneCode));
        
        log.info("[Service] deleteAppScene successfully deleted sceneCode={}", sceneCode);
    }
}
