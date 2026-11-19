package com.exchange.platform.analytics.service.masterdata;

import java.time.LocalDate;
import java.util.Map;

import com.exchange.platform.common.core.enums.MarketAssetType;

/**
 * 从 Dataset 行转换出的统一资产候选对象。
 *
 * <p>候选对象把股票、指数的公共身份字段提取出来，并保留其余标准字段，
 * 供校验和后续 upsert 使用。它还没有写入正式主数据表，因此不会绕过发布前校验。</p>
 */
public record MasterDataCandidate(MarketAssetType marketAssetType, String assetCode, String assetName,
        Map<String, Object> fields) {
    public MasterDataCandidate {
        // 字段快照不可变，防止映射阶段之后的数据被意外改写。
        fields = fields == null ? Map.of() : Map.copyOf(fields);
    }

    /** 读取字段的文本值；缺失字段统一返回 null。 */
    public String text(String code) { Object value = fields.get(code); return value == null ? null : value.toString(); }

    /** 将字段解析为整数；空值表示未提供可选字段。 */
    public Integer integer(String code) { String value = text(code); return value == null || value.isBlank() ? null : Integer.valueOf(value); }

    /** 将 ISO-8601 日期字段解析为 LocalDate；空值表示未提供可选字段。 */
    public LocalDate date(String code) { String value = text(code); return value == null || value.isBlank() ? null : LocalDate.parse(value); }
}
