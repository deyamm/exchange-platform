package com.exchange.platform.analytics.api.enums;

import java.util.Locale;

import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.annotation.IEnum;
import com.exchange.platform.common.core.exception.BusinessException;

public enum SyncType implements IEnum<String> { 
    INIT, 
    FULL, 
    INCREMENTAL;

    public static SyncType parseSyncType(String value) {
        if (!StringUtils.hasText(value)) {
            return SyncType.FULL;
        }

        try {
            return SyncType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(40001, "不支持的 syncType：" + value);
        }
    }

    @Override
    public String getValue() {
        return this.name();
    }
}
