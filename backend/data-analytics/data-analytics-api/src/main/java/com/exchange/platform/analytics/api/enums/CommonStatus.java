package com.exchange.platform.analytics.api.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

public enum CommonStatus implements IEnum<Integer> {

    DRAFT(0, "草稿"),
    ENABLED(1, "已启用"),
    DISABLED(2, "已禁用"),
    DELETED(3, "已删除"),
    EXPIRED(4, "已过期");

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
