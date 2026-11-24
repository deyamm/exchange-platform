package com.exchange.platform.analytics.service.support;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.exchange.platform.collection.api.request.execution.MetricDatasetQueryRequest;
import com.exchange.platform.collection.api.response.execution.MetricDatasetConfigView;
import com.exchange.platform.collection.api.response.execution.MetricDatasetExecutionView;
import com.exchange.platform.collection.client.feign.DataCollectionClient;
import com.exchange.platform.common.core.exception.BusinessException;
import com.exchange.platform.common.core.result.ApiResponse;

/**
 * 数据分析服务访问数据集元数据的最小只读客户端。
 * <p>
 * 应用指标只需要 Dataset 主档状态和当前版本的标准字段快照，因此这里不会读取
 * 物理表、StorageMapping 或 Python 服务。批量接口也避免了指标列表展示时的 N+1 请求。
 * </p>
 */
@Component
public class CollectionDatasetMetadataClient {
    private final DataCollectionClient dataCollectionClient;

    public CollectionDatasetMetadataClient(DataCollectionClient dataCollectionClient) {
        this.dataCollectionClient = dataCollectionClient;
    }

    /** 按 Dataset 编码批量读取名称、状态及当前版本字段定义。 */
    public Map<String, MetricDatasetConfigView> getDatasetsForMetricConfiguration(Collection<String> sourceCodes) {
        List<String> datasetCodes = sourceCodes.stream().filter(StringUtils::hasText).distinct().toList();

        if (datasetCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            ApiResponse<List<MetricDatasetConfigView>> response = dataCollectionClient
                    .queryMetricDatasetConfigurations(new MetricDatasetQueryRequest(datasetCodes));
            if (!isSuccessful(response)) {
                throw new BusinessException(40001, "读取数据集元数据失败");
            }
            List<MetricDatasetConfigView> data = response.data();
            if (data == null) {
                return Collections.emptyMap();
            }
            Map<String, MetricDatasetConfigView> result = new LinkedHashMap<>();
            for (MetricDatasetConfigView binding : data) {
                if (binding == null) {
                    continue;
                }
                String datasetCode = binding.getDatasetCode();
                if (!StringUtils.hasText(datasetCode)) {
                    continue;
                }
                result.put(datasetCode, binding);
            }
            return result;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(40001, "读取数据集元数据失败，请确认数据采集服务可用");
        }
    }

    /**
     * 统一指标取数专用的受控读取绑定。该方法只由后端服务调用；页面仍只能传 metricCode。
     */
    public Map<String, MetricDatasetExecutionView> getDatasetsForMetricExecution(Collection<String> sourceCodes) {
        List<String> datasetCodes = sourceCodes.stream().filter(StringUtils::hasText).distinct().toList();
        if (datasetCodes.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            ApiResponse<List<MetricDatasetExecutionView>> response = dataCollectionClient
                    .queryMetricDatasetExecutions(new MetricDatasetQueryRequest(datasetCodes));
            if (!isSuccessful(response)) {
                throw new BusinessException(40001, "读取数据集指标取数绑定失败，错误原因：" + response.message());
            }

            List<MetricDatasetExecutionView> data = response.data();
            if (data == null) {
                return Collections.emptyMap();
            }

            Map<String, MetricDatasetExecutionView> result = new LinkedHashMap<>();
            for (MetricDatasetExecutionView binding : data) {
                if (binding == null) {
                    continue;
                }
                String datasetCode = binding.getDatasetCode();
                if (!StringUtils.hasText(datasetCode)) {
                    continue;
                }
        
                result.put(datasetCode, binding);
            }
            return result;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(40001, "读取数据集指标取数绑定失败，请确认数据采集服务可用");
        }
    }

    /** 读取单个 Dataset 的受控 StorageMapping，供统一逻辑行读取适配器使用。 */
    public MetricDatasetExecutionView getDatasetForMetricExecution(String datasetCode) {
        return getDatasetsForMetricExecution(List.of(datasetCode)).get(datasetCode);
    }

    private boolean isSuccessful(ApiResponse<?> response) {
        return response != null && response.code() == 0;
    }

}
