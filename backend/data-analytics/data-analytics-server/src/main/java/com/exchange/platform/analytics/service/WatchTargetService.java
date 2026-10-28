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

    /** 查询标的详情首屏轻量数据（对应前端 /details） */
    public Map<String, Object> getStockDetailShell(String targetCode, String tradeDate);

    /** 查询标的综合概览（对应前端 /overview） */
    public Map<String, Object> getTargetOverview(String targetCode, String tradeDate);

    /** 查询行情详情（对应前端 /quote-detail） */
    public Map<String, Object> getTargetQuoteDetail(
        String targetCode,
        String tradeDate,
        String rangeCode,
        String adjustType,
        String benchmarkCode,
        String industryCode
    );

    /** 查询估值摘要（对应前端 /valuation-summary） */
    public Map<String, Object> getTargetValuationSummary(
        String targetCode,
        String tradeDate,
        Integer historyLimit,
        String rangeCode
    );

    /** 查询估值通道（对应前端 /valuation-channel） */
    public Map<String, Object> getTargetValuationChannel(
        String targetCode,
        String tradeDate,
        String metricCode,
        Integer historyLimit,
        String rangeCode
    );

    /** 查询估值走势（对应前端 /valuation-trend） */
    public Map<String, Object> getTargetValuationTrend(
        String targetCode,
        String tradeDate,
        String metricCode,
        Integer historyLimit,
        String rangeCode
    );

    /** 查询分红走势（对应前端 /dividend-trend） */
    public Map<String, Object> getTargetDividendTrend(
        String targetCode,
        String tradeDate,
        Integer historyLimit,
        String rangeCode
    );

    /** 查询财务详情（对应前端 /financial-detail） */
    public Map<String, Object> getTargetFinancialDetail(String targetCode, Integer historyLimit);

    /** 查询资金流向详情（对应前端 /capital-flow-detail） */
    public Map<String, Object> getTargetCapitalFlowDetail(
        String targetCode,
        String tradeDate,
        Integer historyLimit,
        String rangeCode,
        boolean includeQuoteRows
    );

    /** 查询行业详情（对应前端 /industry-detail） */
    public Map<String, Object> getTargetIndustryDetail(String targetCode, String tradeDate);

    /** 查询指数详细信息 */
    public Map<String, Object> getIndexDetails(String targetCode, String tradeDate);

    public Map<String, Object> getIndexMarket(String targetCode, String tradeDate, int historyLimit);

    public Map<String, Object> getIndexConstituents(String targetCode, String tradeDate);

    public Page<Map<String, Object>> getIndexConstituentPage(String targetCode, String tradeDate, int page, int pageSize);
}   