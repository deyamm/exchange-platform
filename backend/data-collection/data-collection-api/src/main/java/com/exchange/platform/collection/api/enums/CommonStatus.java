package com.exchange.platform.collection.api.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

public enum CommonStatus implements IEnum<Integer> {

    ENABLED(1, "启用"),
    DISABLED(0, "禁用"),
    OFFLINE(2, "下线");

    private final Integer code;
    private final String desc;

    CommonStatus(Integer code, String desc) {
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
