package com.exchange.platform.analytics.service.masterdata;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exchange.platform.analytics.api.enums.TriggerType;
import com.exchange.platform.analytics.api.request.MarketAssetListRequest;
import com.exchange.platform.analytics.api.request.MarketAssetRef;
import com.exchange.platform.analytics.api.request.ScopeResolveRequest;
import com.exchange.platform.analytics.api.response.AssetMemberView;
import com.exchange.platform.analytics.api.response.MarketAssetView;
import com.exchange.platform.analytics.api.response.SyncBatchView;
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
import com.exchange.platform.analytics.api.enums.MasterDataType;
import com.exchange.platform.common.core.enums.MarketAssetType;
import com.exchange.platform.common.core.exception.BusinessException;
import com.exchange.platform.common.core.page.PageResult;

/**
 * 主数据查询应用服务。
 *
 * <p>
 * 发布、数据映射、质量门禁及正式表写入集中在 {@link MasterDataPublishService}；本类仅承担
 * 查询 API 的资产和批次读取，并将所有发布入口委托给该服务。
 * </p>
 * 
 * TODO: 有两个值得优化的地方。
    1. search()、list() 在查询资产列表之后，通过 toView() 对每个资产单独查询股票或指数扩展表，可能形成 N+1 查询问题。
    2. batchDetail() 逐个调用 detail()，同样可能产生大量重复数据库查询。
 */
@Service
public class MasterDataServiceImpl implements MasterDataService {
    private final MdMarketAssetMapper assetMapper;
    private final MdMarketAssetStockMapper stockMapper;
    private final MdMarketAssetIndexMapper indexMapper;
    private final MdMarketAssetMemberMapper memberMapper;
    private final MdSyncBatchMapper batchMapper;
    private final MasterDataPublishService publishService;

    public MasterDataServiceImpl(MdMarketAssetMapper assetMapper, MdMarketAssetStockMapper stockMapper,
            MdMarketAssetIndexMapper indexMapper, MdMarketAssetMemberMapper memberMapper, MdSyncBatchMapper batchMapper,
            MasterDataPublishService publishService) {
        this.assetMapper = assetMapper;
        this.stockMapper = stockMapper;
        this.indexMapper = indexMapper;
        this.memberMapper = memberMapper;
        this.batchMapper = batchMapper;
        this.publishService = publishService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarketAssetView> search(String keyword, MarketAssetType marketAssetType, int limit) {
        if (!StringUtils.hasText(keyword))
            return List.of();
        int size = Math.min(Math.max(limit, 1), 200);
        LambdaQueryWrapper<MdMarketAsset> query = new LambdaQueryWrapper<MdMarketAsset>()
                .eq(marketAssetType != null, MdMarketAsset::getMarketAssetType, marketAssetType)
                .and(w -> w.like(MdMarketAsset::getAssetCode, keyword.trim()).or().like(MdMarketAsset::getAssetName,
                        keyword.trim()))
                .orderByAsc(MdMarketAsset::getAssetCode).last("LIMIT " + size);
        return assetMapper.selectList(query).stream().map(this::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<MarketAssetView> list(MarketAssetListRequest request) {
        Page<MdMarketAsset> page = assetMapper.selectPage(new Page<>(request.safePageNo(), request.safePageSize()),
                new LambdaQueryWrapper<MdMarketAsset>()
                        .eq(request.marketAssetType() != null, MdMarketAsset::getMarketAssetType, request.marketAssetType())
                        .and(StringUtils.hasText(request.keyword()),
                                w -> w.like(MdMarketAsset::getAssetCode, request.keyword().trim())
                                        .or().like(MdMarketAsset::getAssetName, request.keyword().trim()))
                        .orderByAsc(MdMarketAsset::getAssetCode));
        return new PageResult<>((int) page.getCurrent(), (int) page.getSize(), page.getTotal(),
                page.getRecords().stream().map(this::toView).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public MarketAssetView detail(MarketAssetType marketAssetType, String assetCode) {
        return toView(requireAsset(marketAssetType, assetCode));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarketAssetView> batchDetail(List<MarketAssetRef> assets) {
        if (assets == null || assets.isEmpty())
            return List.of();
        return assets.stream().filter(Objects::nonNull).map(ref -> detail(ref.marketAssetType(), ref.assetCode())).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssetMemberView> members(String indexCode, LocalDateTime asOfTime) {
        MdMarketAsset parent = requireAsset(MarketAssetType.INDEX, indexCode);
        LocalDateTime at = asOfTime == null ? LocalDateTime.now() : asOfTime;
        List<MdMarketAssetMember> relations = memberMapper.selectList(new LambdaQueryWrapper<MdMarketAssetMember>()
                .eq(MdMarketAssetMember::getParentMarketAssetId, parent.getId()).le(MdMarketAssetMember::getValidFrom, at)
                .and(w -> w.isNull(MdMarketAssetMember::getValidTo).or().gt(MdMarketAssetMember::getValidTo, at)));
        Map<Long, MdMarketAsset> assets = assetMapper
                .selectBatchIds(relations.stream().map(MdMarketAssetMember::getMemberMarketAssetId).toList())
                .stream().collect(Collectors.toMap(MdMarketAsset::getId, Function.identity()));
        return relations.stream().map(r -> new AssetMemberView(indexCode,
                Optional.ofNullable(assets.get(r.getMemberMarketAssetId())).map(MdMarketAsset::getAssetCode).orElse(null),
                r.getValidFrom(), r.getValidTo(), r.getBatchId())).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarketAssetView> resolveScope(ScopeResolveRequest request) {
        if (request == null || request.scopeValues() == null || request.scopeValues().isEmpty())
            return List.of();
        String scope = request.scopeType() == null ? "ASSET_CODE" : request.scopeType().toUpperCase(Locale.ROOT);
        LambdaQueryWrapper<MdMarketAsset> query = new LambdaQueryWrapper<>();
        if ("ASSET_CODE".equals(scope) || "INDEX".equals(scope) || "STOCK".equals(scope))
            query.in(MdMarketAsset::getAssetCode, request.scopeValues());
        else if ("ASSET_TYPE".equals(scope))
            query.in(MdMarketAsset::getMarketAssetType, request.scopeValues().stream().map(MarketAssetType::fromCode).toList());
        else
            throw new BusinessException(40001, "不支持的范围类型：" + request.scopeType());
        return assetMapper.selectList(query).stream().limit(request.safeLimit()).map(this::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<SyncBatchView> batches(String masterDataType, String datasetCode, String status, Long bindingId, String triggerType, int pageNo, int pageSize) {
        Page<MdSyncBatch> page = batchMapper.selectPage(
                new Page<>(Math.max(pageNo, 1), Math.min(Math.max(pageSize, 1), 200)),
                new LambdaQueryWrapper<MdSyncBatch>()
                        .eq(StringUtils.hasText(datasetCode), MdSyncBatch::getDatasetCode, datasetCode)
                        .eq(StringUtils.hasText(status), MdSyncBatch::getStatus, status)
                        .eq(StringUtils.hasText(masterDataType), MdSyncBatch::getMasterDataType, masterDataType)
                        .eq(bindingId != null, MdSyncBatch::getBindingId, bindingId)
                        .eq(StringUtils.hasText(triggerType), MdSyncBatch::getTriggerType, triggerType)
                        .orderByDesc(MdSyncBatch::getStartedAt));
        return new PageResult<>((int) page.getCurrent(), (int) page.getSize(), page.getTotal(),
                page.getRecords().stream().map(publishService::toView).toList());
    }

    @Override
    public SyncBatchView publish(String masterDataType, String sourceCode, String taskRunId, String datasetCode, String syncType) {
        return publishService.publish(MasterDataType.valueOf(masterDataType), sourceCode, taskRunId, datasetCode, syncType);
    }

    @Override
    public SyncBatchView republish(Long batchId) {
        return publishService.republish(batchId);
    }

    @Override
    public SyncBatchView publishBinding(Long bindingId, String taskRunId, String syncType, TriggerType triggerType) {
        return publishService.publishBinding(bindingId, taskRunId, syncType, triggerType);
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

    private MarketAssetView toView(MdMarketAsset asset) {
        MdMarketAssetStock stock = asset.getMarketAssetType() == MarketAssetType.STOCK ? stockMapper.selectById(asset.getId()) : null;
        
        MdMarketAssetIndex index = asset.getMarketAssetType() == MarketAssetType.INDEX ? indexMapper.selectById(asset.getId()) : null;

        return new MarketAssetView(asset.getId(), asset.getMarketAssetType(), asset.getAssetCode(), asset.getAssetName(),
                asset.getStatus(),
                stock == null ? null : stock.getMarketType(),
                stock == null ? (index == null ? null : index.getExchangeCode()) : stock.getExchangeCode(),
                stock == null ? null : stock.getCurrencyCode(), stock == null ? null : stock.getListDate(),
                stock == null ? null : stock.getDelistDate(),
                index == null ? null : index.getPublisherCode(), index == null ? null : index.getIndexCategory(),
                index == null ? null : index.getSchemeCode(),
                index == null ? null : index.getLevel(), index == null ? null : index.getLaunchDate(),
                index == null ? null : index.getEndDate());
    }
}
