package com.exchange.platform.analytics.service.impl;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.exchange.platform.analytics.entity.AppResult;
import com.exchange.platform.analytics.mapper.AppResultMapper;
import com.exchange.platform.analytics.mapper.TypeBasicDataMapper;
import com.exchange.platform.analytics.service.MarketOverviewService;
import com.exchange.platform.data.client.DataServiceClient;


@Service
public class MarketOverviewServiceImpl extends ServiceImpl<AppResultMapper, AppResult> implements MarketOverviewService {
    
    private static final Logger log = LoggerFactory.getLogger(MarketOverviewServiceImpl.class);

    private final DataServiceClient dataServiceClient;

    private final TypeBasicDataMapper typeBasicDataMapper;

    public MarketOverviewServiceImpl(TypeBasicDataMapper typeBasicDataMapper, DataServiceClient dataServiceClient) {
        this.typeBasicDataMapper = typeBasicDataMapper;
        this.dataServiceClient = dataServiceClient;
    }

    /**
     * 调用数据服务接口获取市场概览数据，并记录日志以便调试和监控。
     */
    @Override
    public Map<String, Object> getMarketOverview(String tradeDate) {
        log.info("Fetching market overview from data service...");
        Map<String, Object> marketOverview = dataServiceClient.getMarketOverview(tradeDate);
        log.info("Received market overview: {}", marketOverview);
        return marketOverview;
    }

    /**
     * 从type_basic_data库中查询指数基本信息，并使用Spring Cache进行缓存，以提高性能和减少数据库负载。
     */
    @Override
    @DS("type_basic_data")
    @Cacheable(cacheNames = "typeBasicDataCache", key = "'indexBasic'")
    public List<Map<String, Object>> getIndexBasic() {
        return typeBasicDataMapper.getIndexBasic();
    }
}
