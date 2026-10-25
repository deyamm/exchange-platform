package com.exchange.platform.analytics.service;

import java.util.Map;

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

    /** 查询股票详细信息 */
    public Map<String, Object> getStockDetails(String targetCode, String tradeDate, boolean includeCharts, boolean includeFinancial, boolean includeHistory);

    /** 查询指数详细信息 */
    public Map<String, Object> getIndexDetails(String targetCode, String tradeDate);

    public Map<String, Object> getIndexMarket(String targetCode, String tradeDate, int historyLimit);

    public Map<String, Object> getIndexConstituents(String targetCode, String tradeDate);

    public Page<Map<String, Object>> getIndexConstituentPage(String targetCode, String tradeDate, int page, int pageSize);
}   