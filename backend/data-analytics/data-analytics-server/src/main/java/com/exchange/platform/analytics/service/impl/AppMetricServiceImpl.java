package com.exchange.platform.analytics.service.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exchange.platform.analytics.api.enums.CommonStatus;
import com.exchange.platform.analytics.api.request.AppMetricSaveRequest;
import com.exchange.platform.analytics.api.response.AppMetricView;
import com.exchange.platform.analytics.converter.MetricConverter;
import com.exchange.platform.analytics.entity.AppMetric;
import com.exchange.platform.analytics.entity.AlertRule;
import com.exchange.platform.analytics.mapper.AlertRuleMapper;
import com.exchange.platform.analytics.mapper.MetricMapper;
import com.exchange.platform.analytics.service.AppMetricService;
import com.exchange.platform.analytics.service.support.CollectionDatasetMetadataClient;
import com.exchange.platform.analytics.service.support.CollectionDatasetMetadataClient.DatasetBinding;
import com.exchange.platform.common.core.exception.BusinessException;
import com.exchange.platform.common.core.page.PageResult;
import com.exchange.platform.common.core.enums.AssetType;


@Service
public class AppMetricServiceImpl extends ServiceImpl<MetricMapper, AppMetric> implements AppMetricService {

    private static final Logger log = LoggerFactory.getLogger(AppMetricServiceImpl.class);
    private static final List<String> ALLOWED_CATEGORIES = List.of(
            "MARKET", "VALUATION", "FINANCE", "GROWTH", "RISK", "OTHER");

    private final AlertRuleMapper alertRuleMapper;
    private final CollectionDatasetMetadataClient collectionDatasetMetadataClient;

    public AppMetricServiceImpl(AlertRuleMapper alertRuleMapper,
            CollectionDatasetMetadataClient collectionDatasetMetadataClient) {
        this.alertRuleMapper = alertRuleMapper;
        this.collectionDatasetMetadataClient = collectionDatasetMetadataClient;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AppMetricView> getMetricList(String keyword, String metricCategory, AssetType targetType, CommonStatus status, Integer pageNo, Integer pageSize) {

        log.info("[Service] getMetricList keyword={}, metricCategory={}, targetType={}, status={}, pageNo={}, pageSize={}", keyword, metricCategory, targetType, status, pageNo, pageSize);

        Integer safePageNo = (pageNo == null || pageNo < 1) ? 1 : pageNo;
        Integer safePageSize = (pageSize == null || pageSize < 1) ? 20 : Math.min(200, pageSize);

        LambdaQueryWrapper<AppMetric> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(StringUtils.hasText(keyword), nested -> nested
                .like(AppMetric::getMetricName, keyword).or().like(AppMetric::getMetricCode, keyword))
            .eq(StringUtils.hasText(metricCategory), AppMetric::getMetricCategory, metricCategory)
            .eq(targetType != null, AppMetric::getTargetType, targetType)
            .eq(status != null, AppMetric::getStatus, status);

        Page<AppMetric> page = this.page(new Page<>(safePageNo, safePageSize), wrapper);
        Map<String, DatasetBinding> bindings = collectionDatasetMetadataClient.getBindings(
                page.getRecords().stream().map(AppMetric::getDatasetCode).collect(Collectors.toSet()));
        List<AppMetricView> viewList = page.getRecords().stream()
            .map(metric -> toView(metric, bindings.get(metric.getDatasetCode())))
            .collect(Collectors.toList());
        return new PageResult<>(safePageNo, safePageSize, page.getTotal(), viewList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppMetricView saveOrUpdateMetric(AppMetricSaveRequest request) {
        log.info("[Service] saveOrUpdateMetric request={}", request);

        validateMetricRequest(request);

        AppMetric entity = this.getOne(
            new LambdaQueryWrapper<AppMetric>()
                .eq(AppMetric::getMetricCode, request.metricCode())
        );

        if (entity == null) {
            entity = new AppMetric();
            entity.setMetricCode(request.metricCode());
            entity.setTargetType(request.targetType().getValue());
        } else if (entity.getTargetType() == null) {
            // 跨库迁移无法可靠反查旧指标类型，首次人工维护时补齐；此后同样不可变。
            entity.setTargetType(request.targetType().getValue());
        } else if (entity.getTargetType() != request.targetType().getValue()) {
            // targetType 参与下游标的身份，创建后不能修改，避免已有引用在不知情下跨类型。
            throw new BusinessException(40001, "指标标的类型创建后不可修改");
        }
        entity.setMetricName(request.metricName().trim())
            .setMetricCategory(request.metricCategory())
            .setUnit(request.unit())
            .setDatasetCode(request.datasetCode())
            .setFieldCode(request.fieldCode())
            .setDescription(request.description());

        // 新增指标默认启用；普通编辑绝不接收 status，从而不能绕过停用引用校验。
        if (entity.getId() == null) {
            entity.setStatus(CommonStatus.ENABLED);
        }

        this.saveOrUpdate(entity);
        DatasetBinding binding = collectionDatasetMetadataClient.getBindings(List.of(entity.getDatasetCode()))
                .get(entity.getDatasetCode());
        return toView(entity, binding);
    }

    /** 启用直接更新；停用先在事务内执行与删除完全一致的引用检查。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMetricStatus(String metricCode, CommonStatus status) {
        AppMetric metric = requireMetricForUpdate(metricCode);
        if (status != CommonStatus.ENABLED && status != CommonStatus.DISABLED) {
            throw new BusinessException(40001, "指标状态只允许设置为启用或停用");
        }
        if (status == CommonStatus.DISABLED) {
            assertMetricNotReferenced(metricCode, "停用");
        }
        metric.setStatus(status);
        this.updateById(metric);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMetric(String metricCode) {
        log.info("[Service] deleteMetric metricCode={}", metricCode);

        AppMetric metric = requireMetricForUpdate(metricCode);
        assertMetricNotReferenced(metricCode, "删除");
        this.removeById(metric.getId());
    }

    /** 统一检查当前已落库的四类下游业务引用；目前仅提醒规则具有真实持久化配置。 */
    private void assertMetricNotReferenced(String metricCode, String operation) {
        // 条件筛选、排行、对比在当前工程中尚无持久化配置对象，不能虚构表或关联查询。
        long alertReferenceCount = alertRuleMapper.selectCount(
                new LambdaQueryWrapper<AlertRule>().eq(AlertRule::getMetricCode, metricCode));
        if (alertReferenceCount > 0) {
            throw new BusinessException(40001, "当前指标已被业务配置引用，不能" + operation + "，请先处理相关业务配置。");
        }
    }

    /** 对来自 UI 或直接 API 调用的保存请求执行最终来源约束，前端校验不能替代此校验。 */
    private void validateMetricRequest(AppMetricSaveRequest request) {
        if (request == null || !StringUtils.hasText(request.metricCode()) || !StringUtils.hasText(request.metricName())) {
            throw new BusinessException(40001, "指标编码和名称不能为空");
        }
        if (!ALLOWED_CATEGORIES.contains(request.metricCategory())) {
            throw new BusinessException(40001, "指标分类不合法");
        }
        Map<String, DatasetBinding> bindings = collectionDatasetMetadataClient.getBindings(
                Collections.singletonList(request.datasetCode()));
        DatasetBinding dataset = bindings.get(request.datasetCode());
        if (dataset == null) {
            throw new BusinessException(40001, "来源数据集不存在");
        }
        if (!isDatasetEnabled(dataset.datasetStatus())) {
            throw new BusinessException(40001, "来源数据集未启用，不能保存指标绑定");
        }
        if (!request.targetType().name().equals(dataset.assetType())) {
            throw new BusinessException(40001, "指标标的类型与来源数据集标的类型不一致");
        }
        boolean fieldBelongsToDataset = dataset.fields().stream()
                .anyMatch(field -> request.fieldCode().equals(field.fieldCode()));
        if (!fieldBelongsToDataset) {
            throw new BusinessException(40001, "来源字段不属于数据集当前数据类型版本");
        }
    }

    /** 同时兼容 DatasetStatus 被 JSON 序列化为枚举名称或底层数值的部署配置。 */
    private boolean isDatasetEnabled(String datasetStatus) {
        return "ENABLED".equals(datasetStatus) || "1".equals(datasetStatus);
    }

    /**
     * 停用、删除采用行锁读取。提醒规则写入也会锁定同一指标，因此引用检查和变更之间
     * 不会被一个并发的新提醒规则插入打断。
     */
    private AppMetric requireMetricForUpdate(String metricCode) {
        AppMetric metric = baseMapper.selectByMetricCodeForUpdate(metricCode);
        if (metric == null) {
            throw new BusinessException(40001, "应用指标不存在：" + metricCode);
        }
        return metric;
    }

    /** 组装列表展示 DTO；字段名称、类型只从当前 Dataset 版本快照派生，永不反写主档。 */
    private AppMetricView toView(AppMetric metric, DatasetBinding dataset) {
        AppMetricView view = MetricConverter.toMetricView(metric);
        if (dataset == null) {
            return view;
        }
        view.setDatasetName(dataset.datasetName());
        dataset.fields().stream().filter(field -> metric.getFieldCode() != null
                && metric.getFieldCode().equals(field.fieldCode())).findFirst().ifPresent(field -> {
                    view.setFieldName(field.fieldName());
                    view.setFieldType(field.fieldType());
                });
        return view;
    }
}
