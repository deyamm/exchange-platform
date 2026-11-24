package com.exchange.platform.analytics.api.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

public enum SyncBatchStatus implements IEnum<String> { 
    CREATED, 
    VALIDATING, 
    PUBLISHED, 
    FAILED;

    @Override 
    public String getValue() {
        return this.name();
    }
}
