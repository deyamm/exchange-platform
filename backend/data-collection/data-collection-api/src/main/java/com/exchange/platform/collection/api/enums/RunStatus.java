package com.exchange.platform.collection.api.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

public enum RunStatus implements IEnum<Integer> {
    CREATED(0, "已创建"),
    PENDING(1, "待执行"),
    RUNNING(2, "执行中"),
    SUCCESS(3, "成功"),
    FAILED(4, "失败");

    private final Integer code;
    private final String desc;

    RunStatus(Integer code, String desc) {
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
