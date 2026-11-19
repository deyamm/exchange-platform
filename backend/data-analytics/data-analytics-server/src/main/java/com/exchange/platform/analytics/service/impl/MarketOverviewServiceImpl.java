package com.exchange.platform.analytics.service.impl;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.exchange.platform.analytics.entity.AppResult;
import com.exchange.platform.analytics.mapper.AppResultMapper;
import com.exchange.platform.analytics.service.MarketOverviewService;
import com.exchange.platform.analytics.service.masterdata.DatasetRowReader;
import com.exchange.platform.data.client.DataServiceClient;


@Service
public class MarketOverviewServiceImpl extends ServiceImpl<AppResultMapper, AppResult> implements MarketOverviewService {
    
    private static final Logger log = LoggerFactory.getLogger(MarketOverviewServiceImpl.class);

    private final DataServiceClient dataServiceClient;

    private static final List<String> INDEX_BASIC_FIELDS = List.of("full_symbol", "name", "fullname", "market",
            "publisher", "index_type", "category", "base_date", "base_point", "list_date", "weight_rule",
            "description", "exp_date");

    private final DatasetRowReader datasetRowReader;
    private final String indexBasicDatasetCode;

    public MarketOverviewServiceImpl(DatasetRowReader datasetRowReader, DataServiceClient dataServiceClient,
            @Value("${analytics.reference-datasets.index-basic-code:index_basic_data}") String indexBasicDatasetCode) {
        this.datasetRowReader = datasetRowReader;
        this.dataServiceClient = dataServiceClient;
        this.indexBasicDatasetCode = indexBasicDatasetCode;
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
     * 通过统一 Dataset 读取指数基础资料；Java 不再直连 type_basic_data。
     */
    @Override
    @Cacheable(cacheNames = "typeBasicDataCache", key = "'indexBasic'")
    public List<Map<String, Object>> getIndexBasic() {
        return datasetRowReader.read(new DatasetRowReader.DatasetReadRequest(indexBasicDatasetCode,
                INDEX_BASIC_FIELDS, Map.of(), List.of(), null, null, "full_symbol", 1, 2000)).stream()
                .map(this::toLegacyIndexBasicRow).toList();
    }

    /** 仅保留既有 HTTP 返回字段的 camelCase 兼容层；读取字段仍是 Dataset fieldCode。 */
    private Map<String, Object> toLegacyIndexBasicRow(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fullSymbol", row.get("full_symbol"));
        result.put("name", row.get("name"));
        result.put("fullname", row.get("fullname"));
        result.put("market", row.get("market"));
        result.put("publisher", row.get("publisher"));
        result.put("indexType", row.get("index_type"));
        result.put("category", row.get("category"));
        result.put("baseDate", row.get("base_date"));
        result.put("basePoint", row.get("base_point"));
        result.put("listDate", row.get("list_date"));
        result.put("weightRule", row.get("weight_rule"));
        result.put("description", row.get("description"));
        result.put("expDate", row.get("exp_date"));
        return result;
    }
}
