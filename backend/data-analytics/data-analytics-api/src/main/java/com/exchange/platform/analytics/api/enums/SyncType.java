package com.exchange.platform.analytics.api.enums;

import java.util.Locale;

import org.springframework.util.StringUtils;

import com.exchange.platform.common.core.exception.BusinessException;

public enum SyncType { 
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


}
