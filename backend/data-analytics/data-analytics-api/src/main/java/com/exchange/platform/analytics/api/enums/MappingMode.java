package com.exchange.platform.analytics.api.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

public enum MappingMode implements IEnum<String> { 
    FIELD, 
    CONSTANT;

    @Override
    public String getValue() {
        return this.name();
    }

}
