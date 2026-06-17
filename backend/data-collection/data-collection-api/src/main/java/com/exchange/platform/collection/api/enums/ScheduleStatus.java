package com.exchange.platform.collection.api.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

public enum ScheduleStatus implements IEnum<Integer> {
    PAUSED(0, "暂停"),
    ACTIVE(1, "启用");

    private final Integer code;
    private final String desc;

    ScheduleStatus(Integer code, String desc) {
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
