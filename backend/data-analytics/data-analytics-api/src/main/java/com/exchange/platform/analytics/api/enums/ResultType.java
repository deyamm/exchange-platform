package com.exchange.platform.analytics.api.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

/**
 * 应用结果类型枚举，对应数据库设计第 8.8 节 result_type 枚举。
 */
public enum ResultType implements IEnum<Integer> {

    MARKET_OVERVIEW(0, "市场概览"),
    TARGET_DETAIL(1, "标的详情"),
    FINANCIAL_SUMMARY(2, "财务摘要"),
    RANKING(3, "指标排行"),
    COMPARISON(4, "标的对比"),
    ALERT(5, "提醒结果");

    private final Integer code;
    private final String desc;

    ResultType(Integer code, String desc) {
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
