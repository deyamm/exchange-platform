package com.exchange.platform.analytics.converter;

import com.exchange.platform.analytics.api.response.WatchTargetView;
import com.exchange.platform.analytics.entity.WatchTarget;

public class WatchTargetConverter {

    private WatchTargetConverter() {
        // 私有构造函数，防止实例化
    }

    public static WatchTargetView toWatchTargetView(WatchTarget entity) {
        if (entity == null) {
            return null;
        }
        WatchTargetView view = new WatchTargetView();
        view.setTargetCode(entity.getTargetCode());
        view.setTargetName(entity.getTargetName());
        view.setTargetType(entity.getTargetType());
        view.setMarketCode(entity.getMarketCode());
        view.setSortNo(entity.getSortNo());
        view.setImportantFlag(entity.getImportantFlag());
        view.setWatchStatus(entity.getWatchStatus());
        view.setWatchReason(entity.getWatchReason());
        view.setCreatedAt(entity.getCreatedAt());
        view.setUpdatedAt(entity.getUpdatedAt());
        return view;
    }
}
