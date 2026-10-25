package com.exchange.platform.analytics.service;


import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exchange.platform.analytics.entity.AppResult;

public interface MarketOverviewService extends IService<AppResult> {
    
    Map<String, Object> getMarketOverview(String tradeDate);

    List<Map<String, Object>> getIndexBasic();

}
