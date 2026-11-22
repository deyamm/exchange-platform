package com.exchange.platform.analytics.service.masterdata;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.exchange.platform.analytics.api.enums.IndexCategory;
import com.exchange.platform.analytics.api.enums.MappingMode;
import com.exchange.platform.analytics.api.enums.MasterDataStatus;
import com.exchange.platform.analytics.api.enums.MasterDataType;
import com.exchange.platform.analytics.api.enums.PublisherCode;
import com.exchange.platform.analytics.api.enums.StockMarketType;
import com.exchange.platform.analytics.api.registry.MasterDataTypeDefinition;
import com.exchange.platform.analytics.api.registry.MdFieldDefinition;
import com.exchange.platform.analytics.api.registry.StorageDefinition;
import com.exchange.platform.analytics.api.registry.ValidationRule;

/** Single source of truth for type-scoped master-data definitions. */
@Component
public class MasterDataTypeRegistry {
    private final Map<MasterDataType, MasterDataTypeDefinition> definitions = new EnumMap<>(MasterDataType.class);

    public MasterDataTypeRegistry() {
        register(definition(MasterDataType.MARKET_ASSET_STOCK,
                List.of(field("assetCode", "资产代码", "STRING", true),
                        field("assetName", "资产名称", "STRING", true),
                        enumField("status", "状态", false, MasterDataStatus.class),
                        enumField("marketType", "市场类型", false, StockMarketType.class),
                        field("exchangeCode", "交易所", "STRING", false),
                        field("currencyCode", "币种", "STRING", false),
                        field("listDate", "上市日期", "DATE", false),
                        field("delistDate", "退市日期", "DATE", false)),
                new StorageDefinition("md_market_asset_stock", Map.ofEntries(
                        Map.entry("assetCode", "asset_code"), Map.entry("assetName", "asset_name"),
                        Map.entry("status", "status"), Map.entry("marketType", "market_type"),
                        Map.entry("exchangeCode", "exchange_code"), Map.entry("currencyCode", "currency_code"),
                        Map.entry("listDate", "list_date"), Map.entry("delistDate", "delist_date"))),
                List.of(new ValidationRule("DATE_ORDER", List.of("listDate", "delistDate")))));

        register(definition(MasterDataType.MARKET_ASSET_INDEX,
                List.of(field("assetCode", "资产代码", "STRING", true),
                        field("assetName", "资产名称", "STRING", true),
                        enumField("status", "状态", false, MasterDataStatus.class),
                        enumField("publisherCode", "发布方", false, PublisherCode.class),
                        enumField("indexCategory", "指数分类", false, IndexCategory.class),
                        field("schemeCode", "业务体系", "STRING", false),
                        field("level", "层级", "INTEGER", false),
                        field("exchangeCode", "交易所", "STRING", false),
                        field("launchDate", "发布日期", "DATE", false),
                        field("endDate", "终止日期", "DATE", false)),
                new StorageDefinition("md_market_asset_index", Map.ofEntries(
                        Map.entry("assetCode", "asset_code"), Map.entry("assetName", "asset_name"),
                        Map.entry("status", "status"), Map.entry("publisherCode", "publisher_code"),
                        Map.entry("indexCategory", "index_category"), Map.entry("schemeCode", "scheme_code"),
                        Map.entry("level", "level"), Map.entry("exchangeCode", "exchange_code"),
                        Map.entry("launchDate", "launch_date"), Map.entry("endDate", "end_date"))),
                List.of(new ValidationRule("INTEGER_MIN", List.of("level", "1")),
                        new ValidationRule("DATE_ORDER", List.of("launchDate", "endDate")))));
    }

    public List<MasterDataTypeDefinition> list() { return List.copyOf(definitions.values()); }
    public MasterDataTypeDefinition get(MasterDataType type) { return definitions.get(type); }
    public MasterDataTypeDefinition require(MasterDataType type) {
        MasterDataTypeDefinition definition = get(type);
        if (definition == null) throw new IllegalArgumentException("未注册的主数据类型: " + type);
        return definition;
    }

    private void register(MasterDataTypeDefinition definition) {
        if (definitions.putIfAbsent(definition.typeCode(), definition) != null) {
            throw new IllegalStateException("重复注册主数据类型: " + definition.typeCode());
        }
    }

    private MasterDataTypeDefinition definition(MasterDataType type, List<MdFieldDefinition> fields,
            StorageDefinition storage, List<ValidationRule> rules) {
        return new MasterDataTypeDefinition(type, type.getDisplayName(), type.getDomain(), type.getCategory(), fields,
                List.of("assetCode"), storage, rules);
    }

    private MdFieldDefinition field(String code, String name, String valueType, boolean required) {
        return new MdFieldDefinition(code, name, valueType, required,
                null, List.of(MappingMode.FIELD, MappingMode.CONSTANT));
    }

    private MdFieldDefinition enumField(String code, String name, boolean required, Class<? extends Enum<?>> enumType) {
        return new MdFieldDefinition(code, name, "ENUM", required, enumType.getName(),
                List.of(MappingMode.FIELD, MappingMode.CONSTANT));
    }
}
