package com.exchange.platform.analytics.service.masterdata;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.exchange.platform.analytics.api.enums.MappingMode;
import com.exchange.platform.analytics.api.enums.MasterDataType;
import com.exchange.platform.analytics.api.registry.MasterDataTypeDefinition;
import com.exchange.platform.analytics.api.registry.TargetFieldDefinition;

/**
 * 主数据类型及目标字段的系统注册表。
 *
 * <p>
 * 字段定义是来源绑定校验和前端配置展示的共同契约。新增一个 Dataset 来源
 * 只需新增 Binding/Mapping；只有引入新的主数据业务类型时才需要扩展本注册表。
 * </p>
 */
@Component
public class MasterDataTypeRegistry {
    private final Map<MasterDataType, MasterDataTypeDefinition> definitions = new EnumMap<>(MasterDataType.class);

    public MasterDataTypeRegistry() {
        // 股票主数据：公共身份字段 + 股票市场、交易所、币种和生命周期属性。
        definitions.put(MasterDataType.MARKET_ASSET_STOCK, definition(MasterDataType.MARKET_ASSET_STOCK,
                List.of(
                        field("assetCode", "资产代码", "STRING", true),
                        field("assetName", "资产名称", "STRING", true),
                        field("marketType", "市场类型", "ENUM", false),
                        field("exchangeCode", "交易所", "STRING", false),
                        field("currencyCode", "币种", "STRING", false),
                        field("listDate", "上市日期", "DATE", false),
                        field("delistDate", "退市日期", "DATE", false),
                        field("status", "状态", "ENUM", false))));
        // 指数主数据：公共身份字段 + 发布方、分类、层级和生命周期属性。
        definitions.put(MasterDataType.MARKET_ASSET_INDEX, definition(MasterDataType.MARKET_ASSET_INDEX,
                List.of(
                        field("assetCode", "资产代码", "STRING", true),
                        field("assetName", "资产名称", "STRING", true),
                        field("publisherCode", "发布方", "ENUM", false),
                        field("indexCategory", "指数分类", "ENUM", false),
                        field("schemeCode", "业务体系", "STRING", false),
                        field("level", "Level", "INTEGER", false),
                        field("exchangeCode", "交易所", "STRING", false),
                        field("launchDate", "发布日期", "DATE", false),
                        field("endDate", "终止日期", "DATE", false),
                        field("status", "状态", "ENUM", false))));
        // 指数成分关系：以指数和股票代码建立带生效时间的关系记录。
        definitions.put(MasterDataType.MARKET_ASSET_REL_INDEX_MEMBER, definition(MasterDataType.MARKET_ASSET_REL_INDEX_MEMBER,
                List.of(
                        field("parentAssetCode", "指数代码", "STRING", true),
                        field("memberAssetCode", "股票代码", "STRING", true),
                        field("validFrom", "生效时间", "DATETIME", false))));
    }

    /** 返回不可修改的注册定义列表，供配置页面和校验逻辑复用。 */
    public List<MasterDataTypeDefinition> list() {
        return List.copyOf(definitions.values());
    }

    /** 按业务类型取得字段契约；未注册类型返回 null，由调用方生成 readiness 错误。 */
    public MasterDataTypeDefinition get(MasterDataType type) {
        return definitions.get(type);
    }

    /** 构造一份类型定义并绑定其展示信息和目标字段。 */
    private MasterDataTypeDefinition definition(MasterDataType type, List<TargetFieldDefinition> fields) {
        return new MasterDataTypeDefinition(type, type.getDisplayName(), type.getDomain(), type.getCategory(), fields);
    }

    private TargetFieldDefinition field(String code, String name, String valueType, boolean required) {
        // 所有目标字段都允许直接取来源字段，或由配置提供固定常量。
        return new TargetFieldDefinition(code, name, valueType, required, null,
                List.of(MappingMode.FIELD, MappingMode.CONSTANT));
    }
}
