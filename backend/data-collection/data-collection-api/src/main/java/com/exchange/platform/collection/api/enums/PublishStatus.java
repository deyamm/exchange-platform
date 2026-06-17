package com.exchange.platform.collection.api.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

/**
 * 发布状态枚举，用于数据类型版本的发布状态
 */
public enum PublishStatus implements IEnum<Integer> {

    DRAFT(0, "草稿"),
    PUBLISHED(1, "已发布"),
    OFFLINE(2, "已下线");

    private final Integer code;
    private final String desc;

    PublishStatus(Integer code, String desc) {
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