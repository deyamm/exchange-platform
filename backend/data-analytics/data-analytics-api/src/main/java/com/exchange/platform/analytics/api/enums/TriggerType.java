package com.exchange.platform.analytics.api.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

public enum TriggerType implements IEnum<String> { 
    MANUAL, 
    AUTO, 
    REPUBLISH;

    @Override
    public String getValue() {
        return this.name();
    }

}
