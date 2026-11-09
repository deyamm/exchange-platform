package com.exchange.platform.analytics.service.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import com.exchange.platform.common.core.exception.BusinessException;

/**
 * 数据分析服务访问数据集元数据的最小只读客户端。
 * <p>应用指标只需要 Dataset 主档状态和当前版本的标准字段快照，因此这里不会读取
 * 物理表、StorageMapping 或 Python 服务。批量接口也避免了指标列表展示时的 N+1 请求。</p>
 */
@Component
public class CollectionDatasetMetadataClient {
    private final RestTemplate restTemplate;
    private final String collectionBaseUrl;

    public CollectionDatasetMetadataClient(RestTemplate restTemplate,
            @Value("${collection-metadata.base-url:http://localhost:8082}") String collectionBaseUrl) {
        this.restTemplate = restTemplate;
        this.collectionBaseUrl = collectionBaseUrl.replaceAll("/+$", "");
    }

    /** 按 Dataset 编码批量读取名称、状态及当前版本字段定义。 */
    @SuppressWarnings("unchecked")
    public Map<String, DatasetBinding> getBindings(Collection<String> sourceCodes) {
        List<String> datasetCodes = sourceCodes.stream().filter(StringUtils::hasText).distinct().toList();
        if (datasetCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            Map<String, Object> response = restTemplate.postForObject(
                    collectionBaseUrl + "/api/v1/collection/monitor/datasets/metric-bindings/query",
                    Collections.singletonMap("datasetCodes", datasetCodes), Map.class);
            if (response == null || !Integer.valueOf(0).equals(asInteger(response.get("code")))) {
                throw new BusinessException(40001, "读取数据集元数据失败");
            }
            Object data = response.get("data");
            if (!(data instanceof List<?>)) {
                return Collections.emptyMap();
            }
            Map<String, DatasetBinding> result = new LinkedHashMap<>();
            for (Object item : (List<?>) data) {
                if (!(item instanceof Map<?, ?>)) {
                    continue;
                }
                Map<String, Object> binding = (Map<String, Object>) item;
                String datasetCode = asString(binding.get("datasetCode"));
                if (!StringUtils.hasText(datasetCode)) {
                    continue;
                }
                List<FieldDefinition> fields = new ArrayList<>();
                Object rawFields = binding.get("fields");
                if (rawFields instanceof List<?>) {
                    for (Object rawField : (List<?>) rawFields) {
                        if (rawField instanceof Map<?, ?>) {
                            Map<String, Object> field = (Map<String, Object>) rawField;
                            fields.add(new FieldDefinition(asString(field.get("fieldCode")),
                                    asString(field.get("fieldName")), asString(field.get("fieldType"))));
                        }
                    }
                }
                result.put(datasetCode, new DatasetBinding(datasetCode, asString(binding.get("datasetName")),
                        asString(binding.get("assetType")), asString(binding.get("datasetStatus")), fields));
            }
            return result;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(40001, "读取数据集元数据失败，请确认数据采集服务可用");
        }
    }

    private Integer asInteger(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return value == null ? null : Integer.valueOf(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /** Dataset 当前可用于指标绑定的必要元数据。 */
    public record DatasetBinding(String datasetCode, String datasetName, String assetType, String datasetStatus,
            List<FieldDefinition> fields) {}

    /** 当前 DataTypeVersion 快照中的标准字段定义。 */
    public record FieldDefinition(String fieldCode, String fieldName, String fieldType) {}
}
