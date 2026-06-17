package com.exchange.platform.analytics.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exchange.platform.analytics.api.enums.AppSceneType;
import com.exchange.platform.analytics.api.enums.CommonStatus;
import com.exchange.platform.analytics.api.request.SceneSaveRequest;
import com.exchange.platform.analytics.api.response.AppSceneView;
import com.exchange.platform.analytics.entity.AppScene;
import com.exchange.platform.common.core.page.PageResult;


public interface AppSceneService extends IService<AppScene> {
    
    /** 获取应用场景列表，支持分页和按名称、分类、状态过滤 */
    public PageResult<AppSceneView> getAppScenePageResult(String sceneName, AppSceneType sceneType, CommonStatus status, Integer pageNo, Integer pageSize);

    /**
     * 获取所有已启用的应用场景列表，不分页
     * @return 应用场景列表
     */
    public List<AppSceneView> getAllEnabledAppSceneList();


    /**
     * 保存或更新应用场景
     */
    AppSceneView saveOrUpdateAppScene(SceneSaveRequest appSceneView);


    /**
     * 删除应用场景
     * @param sceneCode 场景编码
     */
    void deleteAppScene(String sceneCode);
}
