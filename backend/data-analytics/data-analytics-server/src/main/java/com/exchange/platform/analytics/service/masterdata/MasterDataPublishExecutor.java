package com.exchange.platform.analytics.service.masterdata;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exchange.platform.analytics.api.enums.IndexCategory;
import com.exchange.platform.analytics.api.enums.MasterDataStatus;
import com.exchange.platform.analytics.api.enums.MasterDataType;
import com.exchange.platform.analytics.api.enums.PublisherCode;
import com.exchange.platform.analytics.api.enums.StockMarketType;
import com.exchange.platform.analytics.api.enums.SyncBatchStatus;
import com.exchange.platform.analytics.entity.masterdata.MdMarketAsset;
import com.exchange.platform.analytics.entity.masterdata.MdMarketAssetIndex;
import com.exchange.platform.analytics.entity.masterdata.MdMarketAssetMember;
import com.exchange.platform.analytics.entity.masterdata.MdMarketAssetStock;
import com.exchange.platform.analytics.entity.masterdata.MdSyncBatch;
import com.exchange.platform.analytics.mapper.masterdata.MdMarketAssetIndexMapper;
import com.exchange.platform.analytics.mapper.masterdata.MdMarketAssetMapper;
import com.exchange.platform.analytics.mapper.masterdata.MdMarketAssetMemberMapper;
import com.exchange.platform.analytics.mapper.masterdata.MdMarketAssetStockMapper;
import com.exchange.platform.analytics.mapper.masterdata.MdSyncBatchMapper;
import com.exchange.platform.common.core.enums.MarketAssetType;
import com.exchange.platform.common.core.exception.BusinessException;

/**
 * 主数据发布事务执行器。
 *
 * <p>
 * 调用方必须传入已经解析完成的 {@link PublishPlan}。Executor 不知道
 * SourceBindingService、Resolver 或 ApplicationService 的存在，也不会在执行期间
 * 重新读取字段映射，因此依赖方向始终保持单向。
 * </p>
 *
 * <p>
 * 整个 execute 方法是正式发布的事务边界：Dataset 读取之后的校验、
 * 质量、资产 upsert、指数成分维护和批次成功状态更新都在同一事务内。
 * 任意异常都会向外抛出，让 Spring 回滚本次正式数据写入。
 * </p>
 */
@Service
public class MasterDataPublishExecutor {

    private static final int DATASET_PAGE_SIZE = 2000;

    private final MdMarketAssetMapper assetMapper;
    private final MdMarketAssetStockMapper stockMapper;
    private final MdMarketAssetIndexMapper indexMapper;
    private final MdMarketAssetMemberMapper memberMapper;
    private final MdSyncBatchMapper batchMapper;
    private final DatasetRowReader rowReader;

    public MasterDataPublishExecutor(
            MdMarketAssetMapper assetMapper,
            MdMarketAssetStockMapper stockMapper,
            MdMarketAssetIndexMapper indexMapper,
            MdMarketAssetMemberMapper memberMapper,
            MdSyncBatchMapper batchMapper,
            DatasetRowReader rowReader) {

        this.assetMapper = assetMapper;
        this.stockMapper = stockMapper;
        this.indexMapper = indexMapper;
        this.memberMapper = memberMapper;
        this.batchMapper = batchMapper;
        this.rowReader = rowReader;
    }

    /**
     * 执行一整个发布事务。
     *
     * @param batch 已经在事务外创建的同步批次
     * @param plan  readiness 通过后生成的执行计划
     */
    @Transactional(rollbackFor = Exception.class)
    public void execute(MdSyncBatch batch, PublishPlan plan) {
        validateBatchMatchesPlan(batch, plan);

        batch.setStatus(SyncBatchStatus.VALIDATING);
        batchMapper.updateById(batch);

        /*
         * readAndMapRows 内部使用 readAll() 自动翻页，不再固定只读取第一页。
         * 因而质量门禁和正式写入看到的是完整 Dataset 结果集。
         */
        List<Map<String, Object>> rows = readAndMapRows(plan);

        if (plan.masterDataType()
                == MasterDataType.MARKET_ASSET_REL_INDEX_MEMBER) {

            publishRelationRows(batch, rows);
            return;
        }

        MarketAssetType type = plan.marketAssetType();
        if (type == null) {
            throw new IllegalArgumentException(
                    "资产类主数据缺少 marketAssetType: "
                            + plan.masterDataType());
        }

        List<MasterDataCandidate> candidates =
                mapCandidates(type, rows);

        validateCandidates(candidates);
        validateMemberCandidates(type, candidates);
        validateAssetCountGuard(type, candidates.size());

        int added = 0;
        int updated = 0;

        for (MasterDataCandidate candidate : candidates) {
            MdMarketAsset asset =
                    findMarketAsset(
                            type,
                            candidate.assetCode());

            boolean isNew = asset == null;

            if (isNew) {
                asset = new MdMarketAsset()
                        .setMarketAssetType(type)
                        .setAssetCode(candidate.assetCode());
            }

            asset.setAssetName(candidate.assetName())
                    .setStatus(
                            parseStatus(
                                    candidate.text("status")));

            if (isNew) {
                assetMapper.insert(asset);
                added++;
            } else {
                assetMapper.updateById(asset);
                updated++;
            }

            if (type == MarketAssetType.STOCK) {
                upsertStock(asset.getId(), candidate);
            } else {
                upsertIndex(asset.getId(), candidate);
            }
        }

        /*
         * 保留现有兼容逻辑：如果指数候选行里同时带有 parent/member 字段，
         * 则继续维护关系。当前 Registry 的 INDEX 定义并不包含这两个字段，
         * 因此常规指数发布通常不会产生关系变更。
         */
        if (type == MarketAssetType.INDEX) {
            publishMembers(batch, candidates);
        }

        batch.setTotalCount(candidates.size())
                .setAddedCount(added)
                .setUpdatedCount(updated)
                .setStatus(SyncBatchStatus.PUBLISHED)
                .setPublishedAt(LocalDateTime.now());

        batchMapper.updateById(batch);
    }

    private void publishRelationRows(
            MdSyncBatch batch,
            List<Map<String, Object>> rows) {

        validateRelationRows(rows);
        publishMembersFromRows(batch, rows);

        /*
         * 保持原有批次统计语义：关系发布暂未精确计算
         * added/updated/removed，只记录输入总量。
         */
        batch.setTotalCount(rows.size())
                .setAddedCount(0)
                .setUpdatedCount(0)
                .setRemovedCount(0)
                .setStatus(SyncBatchStatus.PUBLISHED)
                .setPublishedAt(LocalDateTime.now());

        batchMapper.updateById(batch);
    }

    /**
     * 读取完整 Dataset，并按照 PublishPlan 做字段标准化。
     */
    private List<Map<String, Object>> readAndMapRows(
            PublishPlan plan) {

        DatasetRowReader.DatasetReadRequest request =
                new DatasetRowReader.DatasetReadRequest(
                        plan.datasetCode(),
                        plan.datasetFieldCodes(),
                        Map.of(),
                        List.of(),
                        null,
                        null,
                        plan.orderByFieldCode(),
                        1,
                        DATASET_PAGE_SIZE);

        return rowReader.readAll(request).stream()
                .map(plan::mapRow)
                .toList();
    }

    private List<MasterDataCandidate> mapCandidates(
            MarketAssetType type,
            List<Map<String, Object>> rows) {

        if (rows == null) {
            throw new IllegalArgumentException(
                    "统一 Dataset 读取返回为空");
        }

        return rows.stream()
                .map(row -> {
                    if (!row.containsKey("assetCode")
                            || !row.containsKey("assetName")) {

                        throw new IllegalArgumentException(
                                "缺少必需标准字段：assetCode/assetName");
                    }

                    return new MasterDataCandidate(
                            type,
                            text(row, "assetCode"),
                            text(row, "assetName"),
                            row);
                })
                .toList();
    }

    private void validateCandidates(
            List<MasterDataCandidate> candidates) {

        Set<String> keys = new HashSet<>();

        for (MasterDataCandidate candidate : candidates) {
            if (!StringUtils.hasText(candidate.assetCode())
                    || !StringUtils.hasText(candidate.assetName())) {

                throw new IllegalArgumentException(
                        "assetCode/assetName 不能为空");
            }

            String businessKey =
                    candidate.marketAssetType().getValue()
                            + "\u0000"
                            + candidate.assetCode();

            if (!keys.add(businessKey)) {
                throw new IllegalArgumentException(
                        "业务键重复："
                                + candidate.assetCode());
            }

            parseStatus(candidate.text("status"));

            if (candidate.marketAssetType()
                    == MarketAssetType.STOCK) {

                validateStockCandidate(candidate);
            } else {
                validateIndexCandidate(candidate);
            }
        }
    }

    private void validateStockCandidate(
            MasterDataCandidate candidate) {

        StockMarketType.fromCode(
                candidate.text("marketType"));

        if (candidate.date("listDate") != null
                && candidate.date("delistDate") != null
                && candidate.date("listDate")
                        .isAfter(candidate.date("delistDate"))) {

            throw new IllegalArgumentException(
                    "上市日期晚于退市日期："
                            + candidate.assetCode());
        }
    }

    private void validateIndexCandidate(
            MasterDataCandidate candidate) {

        PublisherCode.fromCode(
                candidate.text("publisherCode"));

        IndexCategory.fromCode(
                candidate.text("indexCategory"));

        Integer level = candidate.integer("level");

        if (level != null && level < 1) {
            throw new IllegalArgumentException(
                    "指数层级必须 >= 1："
                            + candidate.assetCode());
        }
    }

    private void validateMemberCandidates(
            MarketAssetType type,
            List<MasterDataCandidate> candidates) {

        if (type != MarketAssetType.INDEX) {
            return;
        }

        Set<String> relations = new HashSet<>();

        for (MasterDataCandidate candidate : candidates) {
            String parent =
                    candidate.text("parentAssetCode");

            String member =
                    candidate.text("memberAssetCode");

            if (!StringUtils.hasText(parent)
                    && !StringUtils.hasText(member)) {
                continue;
            }

            if (!StringUtils.hasText(parent)
                    || !StringUtils.hasText(member)
                    || parent.equals(member)) {

                throw new IllegalArgumentException(
                        "INDEX 成分关系非法："
                                + candidate.assetCode());
            }

            if (!relations.add(
                    parent + "\u0000" + member)) {

                throw new IllegalArgumentException(
                        "当前成分关系重复："
                                + parent
                                + "/"
                                + member);
            }
        }
    }

    private void validateRelationRows(
            List<Map<String, Object>> rows) {

        Set<String> relations = new HashSet<>();

        for (Map<String, Object> row : rows) {
            String parent =
                    text(row, "parentAssetCode");

            String member =
                    text(row, "memberAssetCode");

            if (!StringUtils.hasText(parent)
                    || !StringUtils.hasText(member)
                    || parent.equals(member)) {

                throw new IllegalArgumentException(
                        "INDEX 成分关系非法："
                                + parent
                                + "/"
                                + member);
            }

            if (!relations.add(
                    parent + "\u0000" + member)) {

                throw new IllegalArgumentException(
                        "当前成分关系重复："
                                + parent
                                + "/"
                                + member);
            }
        }
    }

    /**
     * 防止来源异常缺失导致正式主数据被大面积覆盖。
     * 保持原有 70% 质量门禁。
     */
    private void validateAssetCountGuard(
            MarketAssetType type,
            int candidateCount) {

        long existing = assetMapper.selectCount(
                new LambdaQueryWrapper<MdMarketAsset>()
                        .eq(
                                MdMarketAsset::getMarketAssetType,
                                type)
                        .eq(
                                MdMarketAsset::getStatus,
                                MasterDataStatus.ACTIVE));

        if (existing > 0
                && candidateCount < existing * 0.7) {

            throw new IllegalArgumentException(
                    "资产数量异常下降，拒绝覆盖正式主数据");
        }
    }

    private void upsertStock(
            Long marketAssetId,
            MasterDataCandidate candidate) {

        MdMarketAssetStock stock =
                stockMapper.selectById(marketAssetId);

        boolean isNew = stock == null;

        if (isNew) {
            stock = new MdMarketAssetStock()
                    .setMarketAssetId(marketAssetId);
        }

        stock.setMarketType(
                        StockMarketType.fromCode(
                                candidate.text("marketType")))
                .setExchangeCode(
                        candidate.text("exchangeCode"))
                .setCurrencyCode(
                        candidate.text("currencyCode"))
                .setListDate(
                        candidate.date("listDate"))
                .setDelistDate(
                        candidate.date("delistDate"));

        if (isNew) {
            stockMapper.insert(stock);
        } else {
            stockMapper.updateById(stock);
        }
    }

    private void upsertIndex(
            Long marketAssetId,
            MasterDataCandidate candidate) {

        MdMarketAssetIndex index =
                indexMapper.selectById(marketAssetId);

        boolean isNew = index == null;

        if (isNew) {
            index = new MdMarketAssetIndex()
                    .setMarketAssetId(marketAssetId);
        }

        index.setPublisherCode(
                        candidate.text("publisherCode"))
                .setIndexCategory(
                        IndexCategory.fromCode(
                                candidate.text("indexCategory")))
                .setSchemeCode(
                        candidate.text("schemeCode"))
                .setLevel(
                        candidate.integer("level"))
                .setExchangeCode(
                        candidate.text("exchangeCode"))
                .setLaunchDate(
                        candidate.date("launchDate"))
                .setEndDate(
                        candidate.date("endDate"));

        if (isNew) {
            indexMapper.insert(index);
        } else {
            indexMapper.updateById(index);
        }
    }

    private void publishMembers(
            MdSyncBatch batch,
            List<MasterDataCandidate> candidates) {

        Map<String, Set<String>> desired =
                new LinkedHashMap<>();

        for (MasterDataCandidate candidate : candidates) {
            String parent =
                    candidate.text("parentAssetCode");

            String member =
                    candidate.text("memberAssetCode");

            if (!StringUtils.hasText(parent)
                    && !StringUtils.hasText(member)) {
                continue;
            }

            desired.computeIfAbsent(
                            requireMarketAsset(
                                    MarketAssetType.INDEX,
                                    parent)
                                    .getAssetCode(),
                            ignored ->
                                    new LinkedHashSet<>())
                    .add(
                            requireMarketAsset(
                                    MarketAssetType.STOCK,
                                    member)
                                    .getAssetCode());
        }

        publishMemberSets(batch, desired);
    }

    private void publishMembersFromRows(
            MdSyncBatch batch,
            List<Map<String, Object>> rows) {

        Map<String, Set<String>> desired =
                new LinkedHashMap<>();

        for (Map<String, Object> row : rows) {
            desired.computeIfAbsent(
                            text(
                                    row,
                                    "parentAssetCode"),
                            ignored ->
                                    new LinkedHashSet<>())
                    .add(
                            text(
                                    row,
                                    "memberAssetCode"));
        }

        publishMemberSets(batch, desired);
    }

    /**
     * 同步每个指数当前有效的成分集合。
     *
     * <p>
     * 本次保持原有 validFrom 语义：新增关系以“本次发布时间”为生效时间。
     * 如果未来要支持历史关系回放，建议单独扩展关系写入模型。
     * </p>
     */
    private void publishMemberSets(
            MdSyncBatch batch,
            Map<String, Set<String>> desired) {

        LocalDateTime now = LocalDateTime.now();

        for (Map.Entry<String, Set<String>> entry
                : desired.entrySet()) {

            MdMarketAsset parent =
                    requireMarketAsset(
                            MarketAssetType.INDEX,
                            entry.getKey());

            List<MdMarketAssetMember> current =
                    memberMapper.selectList(
                            new LambdaQueryWrapper<MdMarketAssetMember>()
                                    .eq(
                                            MdMarketAssetMember
                                                    ::getParentMarketAssetId,
                                            parent.getId())
                                    .isNull(
                                            MdMarketAssetMember
                                                    ::getValidTo));

            if (!current.isEmpty()
                    && entry.getValue().size()
                            < current.size() * 0.7) {

                throw new IllegalArgumentException(
                        "指数成分数量异常下降，拒绝覆盖正式主数据："
                                + entry.getKey());
            }

            /*
             * 一次构建 currentByCode，后续失效判断和新增判断都复用，
             * 避免对同一 member 重复 selectById。
             */
            Map<String, MdMarketAssetMember> currentByCode =
                    new HashMap<>();

            for (MdMarketAssetMember relation : current) {
                MdMarketAsset member =
                        assetMapper.selectById(
                                relation.getMemberMarketAssetId());

                if (member != null) {
                    currentByCode.put(
                            member.getAssetCode(),
                            relation);
                }
            }

            for (Map.Entry<String, MdMarketAssetMember> currentEntry
                    : currentByCode.entrySet()) {

                if (!entry.getValue()
                        .contains(currentEntry.getKey())) {

                    currentEntry.getValue()
                            .setValidTo(now);

                    memberMapper.updateById(
                            currentEntry.getValue());
                }
            }

            for (String memberCode : entry.getValue()) {
                if (currentByCode.containsKey(memberCode)) {
                    continue;
                }

                memberMapper.insert(
                        new MdMarketAssetMember()
                                .setParentMarketAssetId(
                                        parent.getId())
                                .setMemberMarketAssetId(
                                        requireMarketAsset(
                                                MarketAssetType.STOCK,
                                                memberCode)
                                                .getId())
                                .setValidFrom(now)
                                .setBatchId(batch.getId()));
            }
        }
    }

    private MdMarketAsset findMarketAsset(
            MarketAssetType type,
            String code) {

        if (type == null || !StringUtils.hasText(code)) {
            return null;
        }

        return assetMapper.selectOne(
                new LambdaQueryWrapper<MdMarketAsset>()
                        .eq(
                                MdMarketAsset::getMarketAssetType,
                                type)
                        .eq(
                                MdMarketAsset::getAssetCode,
                                code.trim())
                        .last("LIMIT 1"));
    }

    /** 从 MarketAsset 总表读取指定类型和代码的正式资产。 */
    private MdMarketAsset requireMarketAsset(
            MarketAssetType type,
            String code) {

        if (type == null || !StringUtils.hasText(code)) {
            throw new BusinessException(
                    40004,
                    "资产身份不完整");
        }

        MdMarketAsset asset =
                findMarketAsset(type, code);

        if (asset == null) {
            throw new BusinessException(
                    40004,
                    "资产不存在："
                            + type
                            + "/"
                            + code);
        }

        return asset;
    }

    private MasterDataStatus parseStatus(String value) {
        if (!StringUtils.hasText(value)) {
            return MasterDataStatus.ACTIVE;
        }

        return MasterDataStatus.valueOf(
                value.trim()
                        .toUpperCase(Locale.ROOT));
    }

    private String text(
            Map<String, Object> row,
            String key) {

        Object value = row.get(key);
        return value == null
                ? null
                : value.toString();
    }

    /**
     * 防止调用方把错误批次和错误计划组合后执行。
     */
    private void validateBatchMatchesPlan(
            MdSyncBatch batch,
            PublishPlan plan) {

        if (batch == null || plan == null) {
            throw new IllegalArgumentException(
                    "batch / PublishPlan 不能为空");
        }

        if (!Objects.equals(
                        batch.getMasterDataType(),
                        plan.masterDataType())
                || !Objects.equals(
                        batch.getDatasetCode(),
                        plan.datasetCode())
                || !Objects.equals(
                        batch.getBindingId(),
                        plan.bindingId())) {

            throw new IllegalArgumentException(
                    "同步批次与 PublishPlan 不匹配，拒绝执行");
        }
    }
}
