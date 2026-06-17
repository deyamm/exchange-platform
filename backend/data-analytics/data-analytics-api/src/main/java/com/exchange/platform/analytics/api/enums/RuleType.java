package com.exchange.platform.analytics.api.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

public enum RuleType implements IEnum<Integer> {
    PRICE(0, "价格提醒"),
    METRIC(1, "指标提醒"),
    ANNOUNCEMENT(2, "公告提醒");

    private final Integer code;
    private final String desc;

    RuleType(Integer code, String desc) {
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
