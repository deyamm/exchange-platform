package com.exchange.platform.analytics.api.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

/**
 * 应用场景的类型枚举，暂时包括市场观察、标的研究、财务分析、公告查看、筛选对比、提醒处理、研究记录、追溯查询
 */
public enum AppSceneType implements IEnum<Integer> {

    MARKET_OVERVIEW(1, "市场概览"),
    TARGET_RESEARCH(2, "标的研究"),
    FINANCIAL_ANALYSIS(3, "财务分析"),
    ANNOUNCEMENT(4, "公告查看"),
    SCREENING_COMPARISON(5, "筛选对比"),
    ALERT_MANAGEMENT(6, "提醒处理"),
    RESEARCH_RECORD(7, "研究记录"),
    TRACE_QUERY(8, "追溯查询");
    
    private final Integer code;
    private final String desc;

    AppSceneType(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    public Integer getValue() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}   
