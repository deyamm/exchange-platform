package com.exchange.platform.analytics.service.masterdata;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exchange.platform.analytics.api.enums.IndexCategory;
import com.exchange.platform.analytics.api.enums.MappingMode;
import com.exchange.platform.analytics.api.enums.MasterDataStatus;
import com.exchange.platform.analytics.api.enums.MasterDataType;
import com.exchange.platform.analytics.api.enums.PublisherCode;
import com.exchange.platform.analytics.api.enums.StockMarketType;
import com.exchange.platform.analytics.api.enums.SyncBatchStatus;
import com.exchange.platform.analytics.api.enums.SyncType;
import com.exchange.platform.analytics.api.enums.TriggerType;
import com.exchange.platform.analytics.api.response.SyncBatchView;
import com.exchange.platform.analytics.entity.masterdata.MdMarketAsset;
import com.exchange.platform.analytics.entity.masterdata.MdMarketAssetIndex;
import com.exchange.platform.analytics.entity.masterdata.MdMarketAssetMember;
import com.exchange.platform.analytics.entity.masterdata.MdMarketAssetStock;
import com.exchange.platform.analytics.entity.masterdata.MdSourceBinding;
import com.exchange.platform.analytics.entity.masterdata.MdSourceFieldMapping;
import com.exchange.platform.analytics.entity.masterdata.MdSyncBatch;
import com.exchange.platform.analytics.mapper.masterdata.MdMarketAssetIndexMapper;
import com.exchange.platform.analytics.mapper.masterdata.MdMarketAssetMapper;
import com.exchange.platform.analytics.mapper.masterdata.MdMarketAssetMemberMapper;
import com.exchange.platform.analytics.mapper.masterdata.MdMarketAssetStockMapper;
import com.exchange.platform.analytics.mapper.masterdata.MdSourceBindingMapper;
import com.exchange.platform.analytics.mapper.masterdata.MdSourceFieldMappingMapper;
import com.exchange.platform.analytics.mapper.masterdata.MdSyncBatchMapper;
import com.exchange.platform.common.core.enums.MarketAssetType;
import com.exchange.platform.common.core.exception.BusinessException;

/**
 * 主数据发布服务。
 *
 * <p>
 * 批次创建、Dataset 映射、候选校验、质量门禁和正式表写入在此集中实现。独立的
 * {@link #publish(MdSyncBatch)} 事务边界保证失败时回滚正式数据，调用者只保留 FAILED 批次事实。
 * </p>
 */
@Service
public class MasterDataPublishService {
    private static final List<String> STOCK_FIELDS = List.of("assetCode", "assetName", "marketType", "exchangeCode",
            "currencyCode", "listDate", "delistDate", "status");
    private static final List<String> INDEX_FIELDS = List.of("assetCode", "assetName", "publisherCode", "indexCategory",
            "schemeCode", "level", "exchangeCode", "launchDate", "endDate", "status");

    private final MdMarketAssetMapper assetMapper;
    private final MdMarketAssetStockMapper stockMapper;
    private final MdMarketAssetIndexMapper indexMapper;
    private final MdMarketAssetMemberMapper memberMapper;
    private final MdSyncBatchMapper batchMapper;
    private final MdSourceFieldMappingMapper mappingMapper;
    private final MdSourceBindingMapper bindingMapper;
    private final DatasetRowReader rowReader;
    private final MasterDataPublishService transactionalSelf;
    private final MasterDataPublishOrchestrator orchestrator;

    public MasterDataPublishService(MdMarketAssetMapper assetMapper, MdMarketAssetStockMapper stockMapper,
            MdMarketAssetIndexMapper indexMapper,
            MdMarketAssetMemberMapper memberMapper, MdSyncBatchMapper batchMapper, MdSourceFieldMappingMapper mappingMapper,
            MdSourceBindingMapper bindingMapper, DatasetRowReader rowReader,
            @Lazy MasterDataPublishService transactionalSelf, @Lazy MasterDataPublishOrchestrator orchestrator) {
        this.assetMapper = assetMapper;
        this.stockMapper = stockMapper;
        this.indexMapper = indexMapper;
        this.memberMapper = memberMapper;
        this.batchMapper = batchMapper;
        this.mappingMapper = mappingMapper;
        this.bindingMapper = bindingMapper;
        this.rowReader = rowReader;
        this.transactionalSelf = transactionalSelf;
        this.orchestrator = orchestrator;
    }

    /** 创建无来源绑定的手动发布批次并执行。 */
    public SyncBatchView publish(MasterDataType masterDataType, String sourceCode, String taskRunId, String datasetCode,
            String syncType) {
        MarketAssetType assetType = marketAssetTypeOf(masterDataType);
        if (assetType == null) throw new BusinessException(40001, "当前 MasterDataType 不支持直接发布");
        if (!StringUtils.hasText(datasetCode))
            throw new BusinessException(40001, "datasetCode 不能为空");
        MdSyncBatch batch = new MdSyncBatch().setMasterDataType(masterDataType)
                .setTriggerType(TriggerType.MANUAL).setSourceCode(sourceCode).setTaskRunId(taskRunId)
                .setDatasetCode(datasetCode)
                .setSyncType(parseSyncType(syncType)).setStatus(SyncBatchStatus.CREATED)
                .setStartedAt(LocalDateTime.now())
                .setTotalCount(0).setAddedCount(0).setUpdatedCount(0).setRemovedCount(0);
        batchMapper.insert(batch);
        execute(batch);
        return toView(batch);
    }

    /** 根据旧批次重新执行；绑定批次使用最新绑定配置。 */
    public SyncBatchView republish(Long batchId) {
        MdSyncBatch old = batchMapper.selectById(batchId);
        if (old == null)
            throw new BusinessException(40004, "同步批次不存在：" + batchId);
        if (old.getBindingId() != null)
            return orchestrator.publish(old.getBindingId(), old.getTaskRunId(), old.getSyncType().name(),
                    TriggerType.REPUBLISH);
        return publish(old.getMasterDataType(), old.getSourceCode(), old.getTaskRunId(), old.getDatasetCode(),
                old.getSyncType().name());
    }

    /** 委托编排器完成来源 readiness 检查和绑定批次初始化。 */
    public SyncBatchView publishBinding(Long bindingId, String taskRunId, String syncType, TriggerType triggerType) {
        return orchestrator.publish(bindingId, taskRunId, syncType, triggerType);
    }

    /** 正式发布事务；异常必须向外传播，以回滚本次对所有主数据表的写入。 */
    @Transactional(rollbackFor = Exception.class)
    public void publish(MdSyncBatch batch) {
        batch.setStatus(SyncBatchStatus.VALIDATING);
        batchMapper.updateById(batch);
        List<Map<String, Object>> rows = readAndMapRows(batch);
        if (batch.getMasterDataType() == MasterDataType.MARKET_ASSET_REL_INDEX_MEMBER) {
            validateRelationRows(rows);
            publishMembersFromRows(batch, rows);
            batch.setTotalCount(rows.size()).setAddedCount(0).setUpdatedCount(0).setRemovedCount(0)
                    .setStatus(SyncBatchStatus.PUBLISHED).setPublishedAt(LocalDateTime.now());
            batchMapper.updateById(batch);
            return;
        }
        MarketAssetType type = marketAssetTypeOf(batch.getMasterDataType());
        List<MasterDataCandidate> candidates = mapCandidates(type, rows);
        validateCandidates(candidates);
        validateMemberCandidates(type, candidates);
        long existing = assetMapper.selectCount(new LambdaQueryWrapper<MdMarketAsset>().eq(MdMarketAsset::getMarketAssetType, type)
                .eq(MdMarketAsset::getStatus, MasterDataStatus.ACTIVE));
        if (existing > 0 && candidates.size() < existing * 0.7)
            throw new IllegalArgumentException("资产数量异常下降，拒绝覆盖正式主数据");
        int added = 0, updated = 0;
        for (MasterDataCandidate candidate : candidates) {
            MdMarketAsset asset = assetMapper.selectOne(new LambdaQueryWrapper<MdMarketAsset>().eq(MdMarketAsset::getMarketAssetType, type)
                    .eq(MdMarketAsset::getAssetCode, candidate.assetCode()).last("LIMIT 1"));
            boolean isNew = asset == null;
            if (isNew)
                asset = new MdMarketAsset().setMarketAssetType(type).setAssetCode(candidate.assetCode());
            asset.setAssetName(candidate.assetName()).setStatus(parseStatus(candidate.text("status")));
            if (isNew) {
                assetMapper.insert(asset);
                added++;
            } else {
                assetMapper.updateById(asset);
                updated++;
            }
            if (type == MarketAssetType.STOCK)
                upsertStock(asset.getId(), candidate);
            else
                upsertIndex(asset.getId(), candidate);
        }
        if (type == MarketAssetType.INDEX)
            publishMembers(batch, candidates);
        batch.setTotalCount(candidates.size()).setAddedCount(added).setUpdatedCount(updated)
                .setStatus(SyncBatchStatus.PUBLISHED).setPublishedAt(LocalDateTime.now());
        batchMapper.updateById(batch);
    }

    /** 为直接发布入口跨过 self-invocation，确保事务代理生效。 */
    private void execute(MdSyncBatch batch) {
        try {
            transactionalSelf.publish(batch);
        } catch (Exception ex) {
            batch.setStatus(SyncBatchStatus.FAILED).setErrorMessage(trimError(ex));
            batchMapper.updateById(batch);
        }
    }

    /** 将批次实体转换为统一 API 视图，供查询服务和编排器复用。 */
    SyncBatchView toView(MdSyncBatch batch) {
        MdSourceBinding binding = batch.getBindingId() == null ? null : bindingMapper.selectById(batch.getBindingId());
        return new SyncBatchView(batch.getId(), batch.getMasterDataType(), batch.getBindingId(),
                binding == null ? null : binding.getBindingCode(), binding == null ? null : binding.getBindingName(),
                batch.getTriggerType(), batch.getSourceCode(), batch.getTaskRunId(),
                batch.getDatasetCode(),
                batch.getSyncType(), batch.getStatus(), n(batch.getTotalCount()), n(batch.getAddedCount()),
                n(batch.getUpdatedCount()),
                n(batch.getRemovedCount()), batch.getStartedAt(), batch.getPublishedAt(), batch.getErrorMessage());
    }

    private List<Map<String, Object>> readAndMapRows(MdSyncBatch batch) {
        if (batch.getBindingId() == null)
            return rowReader.read(new DatasetRowReader.DatasetReadRequest(batch.getDatasetCode(),
                    marketAssetTypeOf(batch.getMasterDataType()) == MarketAssetType.STOCK ? STOCK_FIELDS : INDEX_FIELDS, Map.of(), List.of(), null,
                    null, "assetCode", 1, 2000));
        List<MdSourceFieldMapping> mappings = mappingMapper.selectList(new LambdaQueryWrapper<MdSourceFieldMapping>()
                .eq(MdSourceFieldMapping::getBindingId, batch.getBindingId()));
        List<String> fields = mappings.stream().filter(m -> m.getMappingMode() == MappingMode.FIELD)
                .map(MdSourceFieldMapping::getSourceFieldCode).filter(StringUtils::hasText).distinct().toList();
        return rowReader
                .read(new DatasetRowReader.DatasetReadRequest(batch.getDatasetCode(), fields, Map.of(), List.of(), null,
                        null, null, 1, 2000))
                .stream().map(row -> mapRow(mappings, row)).toList();
    }

    private Map<String, Object> mapRow(List<MdSourceFieldMapping> mappings, Map<String, Object> row) {
        Map<String, Object> mapped = new LinkedHashMap<>();
        for (MdSourceFieldMapping mapping : mappings)
            mapped.put(mapping.getTargetField(),
                    mapping.getMappingMode() == MappingMode.CONSTANT ? mapping.getConstantValue()
                            : row.get(mapping.getSourceFieldCode()));
        return mapped;
    }

    private List<MasterDataCandidate> mapCandidates(MarketAssetType type, List<Map<String, Object>> rows) {
        if (rows == null)
            throw new IllegalArgumentException("统一 Dataset 读取返回为空");
        return rows.stream().map(row -> {
            if (!row.containsKey("assetCode") || !row.containsKey("assetName"))
                throw new IllegalArgumentException("缺少必需标准字段：assetCode/assetName");
            return new MasterDataCandidate(type, text(row, "assetCode"), text(row, "assetName"), row);
        }).toList();
    }

    private void validateCandidates(List<MasterDataCandidate> candidates) {
        Set<String> keys = new HashSet<>();
        for (MasterDataCandidate candidate : candidates) {
            if (!StringUtils.hasText(candidate.assetCode()) || !StringUtils.hasText(candidate.assetName()))
                throw new IllegalArgumentException("assetCode/assetName 不能为空");
            if (!keys.add(candidate.marketAssetType().getValue() + "\u0000" + candidate.assetCode()))
                throw new IllegalArgumentException("业务键重复：" + candidate.assetCode());
            if (parseStatus(candidate.text("status")) == null)
                throw new IllegalArgumentException("status 非法：" + candidate.assetCode());
            if (candidate.marketAssetType() == MarketAssetType.STOCK) {
                StockMarketType.fromCode(candidate.text("marketType"));
                if (candidate.date("listDate") != null && candidate.date("delistDate") != null
                        && candidate.date("listDate").isAfter(candidate.date("delistDate")))
                    throw new IllegalArgumentException("上市日期晚于退市日期：" + candidate.assetCode());
            } else {
                PublisherCode.fromCode(candidate.text("publisherCode"));
                IndexCategory.fromCode(candidate.text("indexCategory"));
                Integer level = candidate.integer("level");
                if (level != null && level < 1)
                    throw new IllegalArgumentException("指数层级必须 >= 1：" + candidate.assetCode());
            }
        }
    }

    private void validateMemberCandidates(MarketAssetType type, List<MasterDataCandidate> candidates) {
        if (type != MarketAssetType.INDEX)
            return;
        Set<String> relations = new HashSet<>();
        for (MasterDataCandidate candidate : candidates) {
            String parent = candidate.text("parentAssetCode"), member = candidate.text("memberAssetCode");
            if (!StringUtils.hasText(parent) && !StringUtils.hasText(member))
                continue;
            if (!StringUtils.hasText(parent) || !StringUtils.hasText(member) || parent.equals(member))
                throw new IllegalArgumentException("INDEX 成分关系非法：" + candidate.assetCode());
            if (!relations.add(parent + "\u0000" + member))
                throw new IllegalArgumentException("当前成分关系重复：" + parent + "/" + member);
        }
    }

    private void validateRelationRows(List<Map<String, Object>> rows) {
        Set<String> relations = new HashSet<>();
        for (Map<String, Object> row : rows) {
            String parent = text(row, "parentAssetCode"), member = text(row, "memberAssetCode");
            if (!StringUtils.hasText(parent) || !StringUtils.hasText(member) || parent.equals(member))
                throw new IllegalArgumentException("INDEX 成分关系非法：" + parent + "/" + member);
            if (!relations.add(parent + "\u0000" + member))
                throw new IllegalArgumentException("当前成分关系重复：" + parent + "/" + member);
        }
    }

    private void upsertStock(Long id, MasterDataCandidate candidate) {
        MdMarketAssetStock stock = stockMapper.selectById(id);
        boolean isNew = stock == null;
        if (isNew)
            stock = new MdMarketAssetStock().setMarketAssetId(id);
        stock.setMarketType(StockMarketType.fromCode(candidate.text("marketType")))
                .setExchangeCode(candidate.text("exchangeCode"))
                .setCurrencyCode(candidate.text("currencyCode")).setListDate(candidate.date("listDate"))
                .setDelistDate(candidate.date("delistDate"));
        if (isNew)
            stockMapper.insert(stock);
        else
            stockMapper.updateById(stock);
    }

    private void upsertIndex(Long id, MasterDataCandidate candidate) {
        MdMarketAssetIndex index = indexMapper.selectById(id);
        boolean isNew = index == null;
        if (isNew)
            index = new MdMarketAssetIndex().setMarketAssetId(id);
        index.setPublisherCode(candidate.text("publisherCode"))
                .setIndexCategory(IndexCategory.fromCode(candidate.text("indexCategory")))
                .setSchemeCode(candidate.text("schemeCode")).setLevel(candidate.integer("level"))
                .setExchangeCode(candidate.text("exchangeCode"))
                .setLaunchDate(candidate.date("launchDate")).setEndDate(candidate.date("endDate"));
        if (isNew)
            indexMapper.insert(index);
        else
            indexMapper.updateById(index);
    }

    private void publishMembers(MdSyncBatch batch, List<MasterDataCandidate> candidates) {
        Map<String, Set<String>> desired = new LinkedHashMap<>();
        for (MasterDataCandidate candidate : candidates) {
            String parent = candidate.text("parentAssetCode"), member = candidate.text("memberAssetCode");
            if (!StringUtils.hasText(parent) && !StringUtils.hasText(member))
                continue;
            desired.computeIfAbsent(requireAsset(MarketAssetType.INDEX, parent).getAssetCode(),
                    ignored -> new LinkedHashSet<>())
                    .add(requireAsset(MarketAssetType.STOCK, member).getAssetCode());
        }
        publishMemberSets(batch, desired);
    }

    private void publishMembersFromRows(MdSyncBatch batch, List<Map<String, Object>> rows) {
        Map<String, Set<String>> desired = new LinkedHashMap<>();
        for (Map<String, Object> row : rows)
            desired.computeIfAbsent(text(row, "parentAssetCode"), ignored -> new LinkedHashSet<>())
                    .add(text(row, "memberAssetCode"));
        publishMemberSets(batch, desired);
    }

    private void publishMemberSets(MdSyncBatch batch, Map<String, Set<String>> desired) {
        LocalDateTime now = LocalDateTime.now();
        for (Map.Entry<String, Set<String>> entry : desired.entrySet()) {
            MdMarketAsset parent = requireAsset(MarketAssetType.INDEX, entry.getKey());
            List<MdMarketAssetMember> current = memberMapper.selectList(new LambdaQueryWrapper<MdMarketAssetMember>()
                    .eq(MdMarketAssetMember::getParentMarketAssetId, parent.getId()).isNull(MdMarketAssetMember::getValidTo));
            if (!current.isEmpty() && entry.getValue().size() < current.size() * 0.7)
                throw new IllegalArgumentException("指数成分数量异常下降，拒绝覆盖正式主数据：" + entry.getKey());
            Map<String, MdMarketAssetMember> currentByCode = new HashMap<>();
            for (MdMarketAssetMember relation : current) {
                MdMarketAsset member = assetMapper.selectById(relation.getMemberMarketAssetId());
                if (member != null)
                    currentByCode.put(member.getAssetCode(), relation);
            }
            for (MdMarketAssetMember relation : current) {
                MdMarketAsset member = assetMapper.selectById(relation.getMemberMarketAssetId());
                if (member != null && !entry.getValue().contains(member.getAssetCode())) {
                    relation.setValidTo(now);
                    memberMapper.updateById(relation);
                }
            }
            for (String memberCode : entry.getValue())
                if (!currentByCode.containsKey(memberCode))
                    memberMapper.insert(new MdMarketAssetMember().setParentMarketAssetId(parent.getId())
                            .setMemberMarketAssetId(requireAsset(MarketAssetType.STOCK, memberCode).getId()).setValidFrom(now)
                            .setBatchId(batch.getId()));
        }
    }

    private MdMarketAsset requireAsset(MarketAssetType type, String code) {
        if (type == null || !StringUtils.hasText(code))
            throw new BusinessException(40004, "资产身份不完整");
        MdMarketAsset asset = assetMapper.selectOne(new LambdaQueryWrapper<MdMarketAsset>().eq(MdMarketAsset::getMarketAssetType, type)
                .eq(MdMarketAsset::getAssetCode, code.trim()).last("LIMIT 1"));
        if (asset == null)
            throw new BusinessException(40004, "资产不存在：" + type + "/" + code);
        return asset;
    }

    private SyncType parseSyncType(String value) {
        return value == null || value.isBlank() ? SyncType.FULL : SyncType.valueOf(value.toUpperCase(Locale.ROOT));
    }

    private MasterDataStatus parseStatus(String value) {
        return value == null || value.isBlank() ? MasterDataStatus.ACTIVE
                : MasterDataStatus.valueOf(value.toUpperCase(Locale.ROOT));
    }

    private String text(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? null : value.toString();
    }

    private String trimError(Exception ex) {
        String value = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        return value.length() > 1900 ? value.substring(0, 1900) : value;
    }

    private int n(Integer value) {
        return value == null ? 0 : value;
    }

    private MarketAssetType marketAssetTypeOf(MasterDataType type) {
        return switch (type) {
            case MARKET_ASSET_STOCK -> MarketAssetType.STOCK;
            case MARKET_ASSET_INDEX, MARKET_ASSET_REL_INDEX_MEMBER -> MarketAssetType.INDEX;
        };
    }
}
