package com.exchange.platform.analytics.service.masterdata;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.exchange.platform.analytics.api.enums.MappingMode;
import com.exchange.platform.analytics.api.enums.MasterDataType;
import com.exchange.platform.common.core.enums.MarketAssetType;

/**
 * 一次主数据发布真正执行时所使用的不可变配置快照。
 *
 * <p>
 * SourceBinding 在进入发布流程前会被解析为 PublishPlan。后续 Executor
 * 只使用这份计划，不再重新查询 Binding/Mapping，从而保证 readiness
 * 校验和真正执行使用的是同一套配置。
 * </p>
 */
public record PublishPlan(
        Long bindingId,
        String bindingCode,
        String bindingName,
        MasterDataType masterDataType,
        MarketAssetType marketAssetType,
        String datasetCode,
        List<String> datasetFieldCodes,
        List<FieldMapping> fieldMappings) {

    public PublishPlan {
        datasetFieldCodes = datasetFieldCodes == null
                ? List.of()
                : List.copyOf(datasetFieldCodes);

        fieldMappings = fieldMappings == null
                ? List.of()
                : List.copyOf(fieldMappings);
    }

    /** 是否来自 SourceBinding。 */
    public boolean bindingBased() {
        return bindingId != null;
    }

    /**
     * 将 Dataset 行映射为主数据标准字段行。
     *
     * <p>
     * 直接发布没有 fieldMappings，Dataset 本身已经使用标准 fieldCode；
     * Binding 发布则严格使用 Plan 中的 FIELD/CONSTANT 快照。
     * </p>
     */
    public Map<String, Object> mapRow(Map<String, Object> sourceRow) {
        Map<String, Object> safeSource = sourceRow == null ? Map.of() : sourceRow;

        if (fieldMappings.isEmpty()) {
            return new LinkedHashMap<>(safeSource);
        }

        Map<String, Object> mapped = new LinkedHashMap<>();

        for (FieldMapping mapping : fieldMappings) {
            Object value = mapping.mappingMode() == MappingMode.CONSTANT
                    ? mapping.constantValue()
                    : safeSource.get(mapping.sourceFieldCode());

            mapped.put(mapping.targetField(), value);
        }

        return mapped;
    }

    /**
     * 为自动分页选择一个尽量稳定的逻辑排序字段。
     *
     * <p>
     * 资产优先 assetCode；指数成分关系优先 parentAssetCode。
     * Binding 场景返回对应 FIELD 映射的来源 fieldCode。
     * </p>
     */
    public String orderByFieldCode() {
        String targetOrderField =
                masterDataType == MasterDataType.MARKET_ASSET_REL_INDEX_MEMBER
                        ? "parentAssetCode"
                        : "assetCode";

        if (fieldMappings.isEmpty()) {
            return datasetFieldCodes.contains(targetOrderField)
                    ? targetOrderField
                    : firstDatasetField();
        }

        return fieldMappings.stream()
                .filter(mapping -> mapping.mappingMode() == MappingMode.FIELD)
                .filter(mapping -> targetOrderField.equals(mapping.targetField()))
                .map(FieldMapping::sourceFieldCode)
                .filter(PublishPlan::hasText)
                .findFirst()
                .orElseGet(this::firstDatasetField);
    }

    private String firstDatasetField() {
        return datasetFieldCodes.isEmpty() ? null : datasetFieldCodes.get(0);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /** 来源绑定中的单个字段映射快照。 */
    public record FieldMapping(
            String targetField,
            MappingMode mappingMode,
            String sourceFieldCode,
            String constantValue) {
    }
}
