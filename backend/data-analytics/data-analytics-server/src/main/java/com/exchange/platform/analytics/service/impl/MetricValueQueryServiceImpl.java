package com.exchange.platform.analytics.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exchange.platform.analytics.api.enums.CommonStatus;
import com.exchange.platform.analytics.api.request.MetricQueryRequest;
import com.exchange.platform.analytics.api.response.MetricValueView;
import com.exchange.platform.analytics.entity.AppMetric;
import com.exchange.platform.analytics.mapper.MetricMapper;
import com.exchange.platform.analytics.service.MetricValueQueryService;
import com.exchange.platform.analytics.service.support.CollectionDatasetMetadataClient;
import com.exchange.platform.analytics.service.support.CollectionDatasetMetadataClient.DatasetMetricReadBinding;
import com.exchange.platform.common.core.exception.BusinessException;
import com.exchange.platform.data.client.DataServiceClient;
import com.exchange.platform.data.client.dto.DatasetMetricValueQuery;
import com.exchange.platform.data.client.dto.MetricFieldQuery;
import com.exchange.platform.data.client.dto.MetricValue;

/**
 * 负责 AppMetric 到 Dataset 的受控解析和按 Dataset 聚合；不直接访问物理数据表，
 * 也不会执行跨 Dataset JOIN。
 */
@Service
public class MetricValueQueryServiceImpl implements MetricValueQueryService {
    private final MetricMapper metricMapper;
    private final CollectionDatasetMetadataClient datasetMetadataClient;
    private final DataServiceClient dataServiceClient;

    public MetricValueQueryServiceImpl(MetricMapper metricMapper,
            CollectionDatasetMetadataClient datasetMetadataClient, DataServiceClient dataServiceClient) {
        this.metricMapper = metricMapper;
        this.datasetMetadataClient = datasetMetadataClient;
        this.dataServiceClient = dataServiceClient;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MetricValueView> queryMetricValues(MetricQueryRequest request) {
        List<String> targetCodes = normalizeCodes(request.targetCodes(), "标的编码");
        List<String> metricCodes = normalizeCodes(request.metricCodes(), "指标编码");
        Map<String, AppMetric> metrics = loadAndValidateMetrics(metricCodes, request);
        Map<String, List<AppMetric>> metricsByDataset = metrics.values().stream()
                .collect(Collectors.groupingBy(AppMetric::getDatasetCode, LinkedHashMap::new, Collectors.toList()));
        Map<String, DatasetMetricReadBinding> bindings = datasetMetadataClient
                .getMetricReadBindings(metricsByDataset.keySet());

        List<MetricValueView> result = new ArrayList<>();
        for (Map.Entry<String, List<AppMetric>> entry : metricsByDataset.entrySet()) {
            String datasetCode = entry.getKey();
            DatasetMetricReadBinding binding = bindings.get(datasetCode);
            validateBinding(datasetCode, binding, request.targetType().getValue(), entry.getValue());
            List<MetricFieldQuery> fieldQueries = entry.getValue().stream().map(metric ->
                    new MetricFieldQuery(metric.getMetricCode(), binding.fieldColumns().get(metric.getFieldCode())))
                    .collect(Collectors.toList());
            List<MetricValue> values = dataServiceClient.queryDatasetMetricValues(new DatasetMetricValueQuery(
                    request.targetType().getValue(), datasetCode, binding.physicalSchemaName(),
                    binding.physicalTableName(), binding.targetCodeColumn(), binding.dataTimeColumn(), targetCodes,
                    fieldQueries, request.asOfTime()));
            if (values != null) {
                values.forEach(value -> result.add(toView(value)));
            }
        }
        return result;
    }

    private Map<String, AppMetric> loadAndValidateMetrics(List<String> metricCodes, MetricQueryRequest request) {
        List<AppMetric> entities = metricMapper.selectList(new LambdaQueryWrapper<AppMetric>()
                .in(AppMetric::getMetricCode, metricCodes));
        Map<String, AppMetric> result = entities.stream().collect(Collectors.toMap(AppMetric::getMetricCode,
                metric -> metric, (left, right) -> left, LinkedHashMap::new));
        for (String metricCode : metricCodes) {
            AppMetric metric = result.get(metricCode);
            if (metric == null) {
                throw new BusinessException(40001, "应用指标不存在：" + metricCode);
            }
            if (metric.getStatus() != CommonStatus.ENABLED) {
                throw new BusinessException(40001, "应用指标未启用：" + metricCode);
            }
            if (!request.targetType().getValue().equals(metric.getTargetType())) {
                throw new BusinessException(40001, "应用指标标的类型不匹配：" + metricCode);
            }
            if (!StringUtils.hasText(metric.getDatasetCode()) || !StringUtils.hasText(metric.getFieldCode())) {
                throw new BusinessException(40001, "应用指标来源配置不完整：" + metricCode);
            }
        }
        return result;
    }

    private void validateBinding(String datasetCode, DatasetMetricReadBinding binding, String targetType,
            Collection<AppMetric> metrics) {
        if (binding == null) {
            throw new BusinessException(40001, "指标来源数据集不存在：" + datasetCode);
        }
        if (!"ENABLED".equals(binding.datasetStatus()) && !"1".equals(binding.datasetStatus())) {
            throw new BusinessException(40001, "指标来源数据集未启用：" + datasetCode);
        }
        if (!targetType.equals(binding.assetType())) {
            throw new BusinessException(40001, "指标来源数据集标的类型不匹配：" + datasetCode);
        }
        if (!StringUtils.hasText(binding.physicalSchemaName()) || !StringUtils.hasText(binding.physicalTableName())
                || !StringUtils.hasText(binding.targetCodeColumn()) || !StringUtils.hasText(binding.dataTimeColumn())) {
            throw new BusinessException(40001, "指标来源数据集读取配置不完整：" + datasetCode);
        }
        for (AppMetric metric : metrics) {
            if (!StringUtils.hasText(binding.fieldColumns().get(metric.getFieldCode()))) {
                throw new BusinessException(40001, "指标字段不在当前数据集结构中：" + metric.getMetricCode());
            }
        }
    }

    private List<String> normalizeCodes(List<String> codes, String label) {
        if (codes == null) {
            throw new BusinessException(40001, label + "不能为空");
        }
        Set<String> unique = new LinkedHashSet<>();
        for (String code : codes) {
            if (!StringUtils.hasText(code)) {
                throw new BusinessException(40001, label + "不能为空");
            }
            unique.add(code.trim());
        }
        if (unique.isEmpty()) {
            throw new BusinessException(40001, label + "不能为空");
        }
        return new ArrayList<>(unique);
    }

    private MetricValueView toView(MetricValue value) {
        MetricValueView view = new MetricValueView();
        view.setTargetType(value.targetType());
        view.setTargetCode(value.targetCode());
        view.setMetricCode(value.metricCode());
        view.setValue(value.value());
        view.setDataTime(value.dataTime());
        return view;
    }
}
