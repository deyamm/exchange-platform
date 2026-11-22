package com.exchange.platform.analytics.service.masterdata;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.exchange.platform.analytics.api.enums.MappingMode;
import com.exchange.platform.analytics.api.enums.MasterDataType;

/**
 * 一次主数据发布真正执行时使用的不可变配置快照。
 *
 * <p>
 * SourceBinding 在进入发布流程前被解析为 PublishPlan。Executor 后续只依赖 这份计划，不再重新查询
 * Binding/Mapping，从而保证 readiness 校验和执行 使用同一套配置。
 * </p>
 *
 * <p>
 * Plan 只保留执行真正需要的信息。MarketAssetType 等类型元数据统一从 {@link MasterDataTypeRegistry}
 * 获取，避免在 Plan 与 Registry 中维护重复状态。
 * </p>
 */
public record PublishPlan(Long bindingId, String bindingCode, MasterDataType masterDataType, String datasetCode, List<String> datasetFieldCodes, List<FieldMapping> fieldMappings) {

    public PublishPlan {
        datasetFieldCodes = datasetFieldCodes == null ? List.of() : List.copyOf(datasetFieldCodes);

        fieldMappings = fieldMappings == null ? List.of() : List.copyOf(fieldMappings);
    }

    /**
     * 将 Dataset 行转换成主数据标准字段行。
     *
     * <p>
     * 直接发布没有字段映射，Dataset 本身已经使用标准 fieldCode；
     * Binding 发布则严格使用 Plan 中冻结的 FIELD/CONSTANT 映射。
     * </p>
     */
    public Map<String, Object> mapRow(Map<String, Object> sourceRow) {
        Map<String, Object> safeSource = sourceRow == null ? Map.of() : sourceRow;

        if (fieldMappings.isEmpty()) {
            return new LinkedHashMap<>(safeSource);
        }

        Map<String, Object> mapped = new LinkedHashMap<>();

        for (FieldMapping mapping : fieldMappings) {
            Object value = mapping.mappingMode() == MappingMode.CONSTANT ? mapping.constantValue()
                    : safeSource.get(mapping.sourceFieldCode());

            mapped.put(mapping.targetField(), value);
        }

        return mapped;
    }

    /**
     * 为自动分页选择尽量稳定的排序字段。
     *
     * <p>
     * 所有实体型主数据都以 assetCode 为统一身份字段。Binding 场景返回 assetCode 对应 FIELD 映射的来源
     * fieldCode；如果 assetCode 来自常量，则退化到第一个 实际读取字段。
     * </p>
     */
    public String orderByFieldCode() {
        String targetOrderField = "assetCode";

        if (fieldMappings.isEmpty()) {
            return datasetFieldCodes.contains(targetOrderField) ? targetOrderField : firstDatasetField();
        }

        return fieldMappings.stream().filter(mapping -> mapping.mappingMode() == MappingMode.FIELD)
                .filter(mapping -> targetOrderField.equals(mapping.targetField())).map(FieldMapping::sourceFieldCode)
                .filter(PublishPlan::hasText).findFirst().orElseGet(this::firstDatasetField);
    }

    private String firstDatasetField() {
        return datasetFieldCodes.isEmpty() ? null : datasetFieldCodes.get(0);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /** SourceBinding 中的一条目标字段映射快照。 */
    public record FieldMapping(String targetField, MappingMode mappingMode, String sourceFieldCode, String constantValue) {
        
    }
}