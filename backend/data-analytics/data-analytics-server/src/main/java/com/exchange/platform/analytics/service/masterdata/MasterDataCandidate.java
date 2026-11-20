package com.exchange.platform.analytics.service.masterdata;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.exchange.platform.common.core.enums.MarketAssetType;

/**
 * 从 Dataset 标准行转换出的统一资产候选对象。
 *
 * <p>
 * 候选对象把股票、指数的公共身份字段提取出来，并保留其余标准字段，
 * 供后续业务校验和正式表 upsert 使用。
 * </p>
 */
public record MasterDataCandidate(
        MarketAssetType marketAssetType,
        String assetCode,
        String assetName,
        Map<String, Object> fields) {

    public MasterDataCandidate {
        fields = fields == null || fields.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(fields));
    }

    /** 字段不存在或值为 null 时返回 null。 */
    public String text(String code) {
        Object value = fields.get(code);
        return value == null ? null : value.toString();
    }

    /** 兼容 Number 和字符串形式的整数。 */
    public Integer integer(String code) {
        Object value = fields.get(code);

        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }

        String text = value.toString();
        return text.isBlank() ? null : Integer.valueOf(text);
    }

    /** 兼容 LocalDate、LocalDateTime 和 ISO-8601 日期字符串。 */
    public LocalDate date(String code) {
        Object value = fields.get(code);

        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.toLocalDate();
        }

        String text = value.toString();
        return text.isBlank() ? null : LocalDate.parse(text);
    }
}
