package com.exchange.platform.analytics.service.masterdata;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exchange.platform.analytics.api.enums.IndexCategory;
import com.exchange.platform.analytics.api.enums.MasterDataStatus;
import com.exchange.platform.analytics.api.enums.PublisherCode;
import com.exchange.platform.analytics.api.response.TargetScopeOptionView;
import com.exchange.platform.analytics.entity.masterdata.MdMarketAsset;
import com.exchange.platform.analytics.entity.masterdata.MdMarketAssetIndex;
import com.exchange.platform.analytics.entity.masterdata.MdMarketAssetMember;
import com.exchange.platform.analytics.mapper.masterdata.MdMarketAssetIndexMapper;
import com.exchange.platform.analytics.mapper.masterdata.MdMarketAssetMapper;
import com.exchange.platform.analytics.mapper.masterdata.MdMarketAssetMemberMapper;
import com.exchange.platform.analytics.service.support.TargetScopeTarget;
import com.exchange.platform.common.core.exception.BusinessException;
import com.exchange.platform.common.core.enums.MarketAssetType;

/**
 * 面向筛选、排行和对比的已发布 Asset 读取边界。
 *
 * <p>这里读取的是 analytics 自己维护的主数据读模型，而不是 Dataset 物理业务表。
 * 股票板块统一以行业指数表示：发布方和层级确定一套分类体系，再由有效成分关系
 * 解析为股票 Asset。</p>
 */
@Service
public class MasterDataTargetScopeService {
    private static final Set<String> INDUSTRY_PUBLISHERS = Set.of(PublisherCode.SW.getValue(), PublisherCode.EM.getValue(), PublisherCode.THS.getValue());

    private final MdMarketAssetMapper assetMapper;
    private final MdMarketAssetIndexMapper indexMapper;
    private final MdMarketAssetMemberMapper memberMapper;

    public MasterDataTargetScopeService(MdMarketAssetMapper assetMapper, MdMarketAssetIndexMapper indexMapper,
            MdMarketAssetMemberMapper memberMapper) {
        this.assetMapper = assetMapper;
        this.indexMapper = indexMapper;
        this.memberMapper = memberMapper;
    }

    public List<TargetScopeTarget> resolve(MarketAssetType marketAssetType, String scopeType, List<String> scopeValues, String industryPublisherCode, Integer industryLevel) {
        String scope = normalize(scopeType);
        List<String> values = normalizeCodes(scopeValues);
        if ("ALL".equals(scope)) {
            return toTargets(activeAssets(marketAssetType));
        }
        if (marketAssetType == MarketAssetType.STOCK && "BOARD".equals(scope)) {
            return resolveIndustryMembers(values, industryPublisherCode, industryLevel);
        }
        if (marketAssetType == MarketAssetType.INDEX && "INDEX_TYPE".equals(scope)) {
            return resolveIndicesByCategory(values);
        }
        // MARKET 是旧快照的兼容值；新请求由前端使用 PUBLISHER。
        if (marketAssetType == MarketAssetType.INDEX && ("PUBLISHER".equals(scope) || "MARKET".equals(scope))) {
            return resolveIndicesByPublisher(values);
        }
        throw new BusinessException(40001, "主数据不支持该范围类型：" + scopeType);
    }

    /** 返回可选择的行业指数，前端据 publisherCode + level 收窄为一个分类体系。 */
    public List<TargetScopeOptionView> industryBoardOptions() {
        List<MdMarketAssetIndex> indices = indexMapper.selectList(new LambdaQueryWrapper<MdMarketAssetIndex>()
                .eq(MdMarketAssetIndex::getIndexCategory, IndexCategory.INDUSTRY)
                .in(MdMarketAssetIndex::getPublisherCode, INDUSTRY_PUBLISHERS));
        Map<Long, MdMarketAsset> assets = activeAssetsByIds(indices.stream().map(MdMarketAssetIndex::getMarketAssetId).toList(), MarketAssetType.INDEX);
        return indices.stream().map(index -> {
            MdMarketAsset asset = assets.get(index.getMarketAssetId());
            return asset == null ? null : new TargetScopeOptionView(asset.getAssetCode(), asset.getAssetName(),
                    index.getPublisherCode(), index.getLevel());
        }).filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparing(TargetScopeOptionView::getPublisherCode)
                        .thenComparing(TargetScopeOptionView::getLevel, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(TargetScopeOptionView::getCode))
                .toList();
    }

    public List<TargetScopeTarget> findByCodes(MarketAssetType marketAssetType, Collection<String> assetCodes) {
        List<String> codes = normalizeCodes(assetCodes == null ? List.of() : new ArrayList<>(assetCodes));
        if (codes.isEmpty()) {
            return List.of();
        }
        Map<String, MdMarketAsset> assets = assetMapper.selectList(new LambdaQueryWrapper<MdMarketAsset>()
                .eq(MdMarketAsset::getMarketAssetType, marketAssetType)
                .eq(MdMarketAsset::getStatus, MasterDataStatus.ACTIVE)
                .in(MdMarketAsset::getAssetCode, codes)).stream()
                .collect(Collectors.toMap(MdMarketAsset::getAssetCode, Function.identity(), (left, right) -> left));
        return codes.stream().map(assets::get).filter(java.util.Objects::nonNull).map(this::toTarget).toList();
    }

    private List<TargetScopeTarget> resolveIndustryMembers(List<String> indexCodes, String publisherCode,
            Integer level) {
        if (indexCodes.isEmpty()) {
            throw new BusinessException(40001, "板块范围至少需要一个行业指数");
        }
        String publisher = normalize(publisherCode);
        if (!INDUSTRY_PUBLISHERS.contains(publisher) || level == null || level < 1) {
            throw new BusinessException(40001, "板块范围必须指定 SW、EM 或 THS 发布方及有效行业层级");
        }
        List<MdMarketAssetIndex> indices = indexMapper.selectList(new LambdaQueryWrapper<MdMarketAssetIndex>()
                .eq(MdMarketAssetIndex::getIndexCategory, IndexCategory.INDUSTRY)
                .eq(MdMarketAssetIndex::getPublisherCode, publisher)
                .eq(MdMarketAssetIndex::getLevel, level));
        Map<Long, MdMarketAsset> parentAssets = activeAssetsByIds(indices.stream().map(MdMarketAssetIndex::getMarketAssetId).toList(),
                MarketAssetType.INDEX);
        Set<Long> selectedIds = indices.stream().filter(index -> {
            MdMarketAsset asset = parentAssets.get(index.getMarketAssetId());
            return asset != null && indexCodes.contains(asset.getAssetCode());
        }).map(MdMarketAssetIndex::getMarketAssetId).collect(Collectors.toSet());
        if (selectedIds.size() != indexCodes.size()) {
            throw new BusinessException(40001, "板块不属于指定发布方或行业层级");
        }
        LocalDateTime now = LocalDateTime.now();
        List<MdMarketAssetMember> relations = memberMapper.selectList(new LambdaQueryWrapper<MdMarketAssetMember>()
                .in(MdMarketAssetMember::getParentMarketAssetId, selectedIds)
                .le(MdMarketAssetMember::getValidFrom, now)
                .and(wrapper -> wrapper.isNull(MdMarketAssetMember::getValidTo).or().gt(MdMarketAssetMember::getValidTo, now)));
        Map<Long, MdMarketAsset> members = activeAssetsByIds(relations.stream().map(MdMarketAssetMember::getMemberMarketAssetId).toList(),
                MarketAssetType.STOCK);
        return toTargets(members.values().stream().sorted(Comparator.comparing(MdMarketAsset::getAssetCode)).toList());
    }

    private List<TargetScopeTarget> resolveIndicesByCategory(List<String> categories) {
        if (categories.isEmpty()) {
            throw new BusinessException(40001, "指数分类不能为空");
        }
        List<IndexCategory> values = categories.stream().map(IndexCategory::fromCode).toList();
        return toTargets(activeIndexAssets(indexMapper.selectList(new LambdaQueryWrapper<MdMarketAssetIndex>()
                .in(MdMarketAssetIndex::getIndexCategory, values))));
    }

    private List<TargetScopeTarget> resolveIndicesByPublisher(List<String> publishers) {
        if (publishers.isEmpty()) {
            throw new BusinessException(40001, "指数发布方不能为空");
        }
        List<String> values = publishers.stream().map(PublisherCode::fromCode).map(PublisherCode::getValue).toList();
        return toTargets(activeIndexAssets(indexMapper.selectList(new LambdaQueryWrapper<MdMarketAssetIndex>()
                .in(MdMarketAssetIndex::getPublisherCode, values))));
    }

    private List<MdMarketAsset> activeAssets(MarketAssetType marketAssetType) {
        return assetMapper.selectList(new LambdaQueryWrapper<MdMarketAsset>()
                .eq(MdMarketAsset::getMarketAssetType, marketAssetType)
                .eq(MdMarketAsset::getStatus, MasterDataStatus.ACTIVE)
                .orderByAsc(MdMarketAsset::getAssetCode));
    }

    private List<MdMarketAsset> activeIndexAssets(List<MdMarketAssetIndex> indices) {
        Map<Long, MdMarketAsset> assets = activeAssetsByIds(indices.stream().map(MdMarketAssetIndex::getMarketAssetId).toList(), MarketAssetType.INDEX);
        return assets.values().stream().sorted(Comparator.comparing(MdMarketAsset::getAssetCode)).toList();
    }

    private Map<Long, MdMarketAsset> activeAssetsByIds(Collection<Long> ids, MarketAssetType type) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return assetMapper.selectList(new LambdaQueryWrapper<MdMarketAsset>().in(MdMarketAsset::getId, ids)
                .eq(MdMarketAsset::getMarketAssetType, type).eq(MdMarketAsset::getStatus, MasterDataStatus.ACTIVE)).stream()
                .collect(Collectors.toMap(MdMarketAsset::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
    }

    private List<TargetScopeTarget> toTargets(List<MdMarketAsset> assets) {
        return assets.stream().map(this::toTarget).toList();
    }

    private TargetScopeTarget toTarget(MdMarketAsset asset) {
        TargetScopeTarget target = new TargetScopeTarget();
        target.setTargetCode(asset.getAssetCode());
        target.setTargetName(asset.getAssetName());
        return target;
    }

    private List<String> normalizeCodes(Collection<String> values) {
        return values.stream().filter(StringUtils::hasText).map(String::trim).distinct().toList();
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase() : "";
    }
}
