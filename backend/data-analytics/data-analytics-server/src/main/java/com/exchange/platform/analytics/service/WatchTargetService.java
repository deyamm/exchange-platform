package com.exchange.platform.analytics.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.exchange.platform.analytics.api.request.WatchTargetSaveRequest;
import com.exchange.platform.analytics.api.response.WatchTargetView;
import com.exchange.platform.analytics.entity.WatchTarget;

public interface WatchTargetService extends IService<WatchTarget> {

    public Page<WatchTargetView> queryWatchTargetPage(
        String groupCode,
        String targetCode,
        String targetName,
        String targetType,
        String marketCode,
        long pageNo,
        long pageSize
    );

    public WatchTargetView saveOrUpdateWatchTarget(WatchTargetSaveRequest request);

    public void deleteWatchTarget(String targetCode, String targetType, String groupCode);

    public WatchTargetView getWatchTargetGroups(String targetCode, String targetType);
}