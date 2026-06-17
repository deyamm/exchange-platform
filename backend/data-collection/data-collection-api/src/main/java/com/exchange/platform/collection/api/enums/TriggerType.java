package com.exchange.platform.collection.api.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

public enum TriggerType implements IEnum<Integer> {
    MANUAL(0, "手动触发"),
    SCHEDULED(1, "定时触发"),
    EVENT(2, "事件触发");

    private final int code;
    private final String description;

    TriggerType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    @Override
    public Integer getValue() {
        return code;
    }

    public String getDescription() {
        return description;
    }
    
}
