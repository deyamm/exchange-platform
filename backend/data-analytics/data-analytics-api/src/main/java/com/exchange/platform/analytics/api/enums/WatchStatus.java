package com.exchange.platform.analytics.api.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

public enum WatchStatus implements IEnum<Integer> {
    WATCHING(0, "观察中"),
    FOCUS(1, "重点关注"),
    PAUSED(2, "暂停观察"),
    CLOSED(3, "结束观察");

    private final Integer code;
    private final String desc;

    WatchStatus(Integer code, String desc) {
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