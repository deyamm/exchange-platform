package com.exchange.platform.analytics.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exchange.platform.analytics.api.request.WatchTargetSaveRequest;
import com.exchange.platform.analytics.api.response.TargetGroupView;
import com.exchange.platform.analytics.api.response.WatchTargetView;
import com.exchange.platform.analytics.converter.WatchTargetConverter;
import com.exchange.platform.analytics.entity.WatchTarget;
import com.exchange.platform.analytics.mapper.WatchTargetMapper;
import com.exchange.platform.analytics.service.WatchTargetService;
import com.exchange.platform.common.core.exception.BusinessException;



@Service
public class WatchTargetServiceImpl extends ServiceImpl<WatchTargetMapper, WatchTarget> implements WatchTargetService {

    private static final Logger log = LoggerFactory.getLogger(WatchTargetServiceImpl.class);

    /**
     * 根据分组编码和筛选条件分页查询关注标的。
     * 返回的 WatchTargetView 会聚合同一 targetCode + targetType 所属的全部标的分组。
     */
    @Override
    @Transactional(readOnly = true)
    public Page<WatchTargetView> queryWatchTargetPage(
        String groupCode,
        String targetCode,
        String targetName,
        String targetType,
        String marketCode,
        long pageNo,
        long pageSize
    ) {
        log.info(
            "[Service] queryWatchTargetPage groupCode={}, targetCode={}, targetName={}, targetType={}, marketCode={}, pageNo={}, pageSize={}",
            groupCode, targetCode, targetName, targetType, marketCode, pageNo, pageSize
        );

        Page<WatchTarget> entityPage = this.page(
            new Page<>(pageNo, pageSize),
            new LambdaQueryWrapper<WatchTarget>()
                .eq(StringUtils.hasText(groupCode), WatchTarget::getGroupCode, groupCode)
                .like(StringUtils.hasText(targetCode), WatchTarget::getTargetCode, targetCode)
                .like(StringUtils.hasText(targetName), WatchTarget::getTargetName, targetName)
                .eq(StringUtils.hasText(targetType), WatchTarget::getTargetType, targetType)
                .eq(StringUtils.hasText(marketCode), WatchTarget::getMarketCode, marketCode)
                .orderByAsc(WatchTarget::getSortNo)
                .orderByDesc(WatchTarget::getUpdatedAt)
        );

        Page<WatchTargetView> viewPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        viewPage.setRecords(buildWatchTargetViews(entityPage.getRecords()));
        return viewPage;
    }

    /**
     * 保存或更新关注标的。
     * 在分组自选场景中，仅新增或更新 request.groupCodes 指定的分组记录，不再删除其它已有分组关系。
     * 删除某个分组中的标的请调用 deleteWatchTarget(targetCode, targetType, groupCode)。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public WatchTargetView saveOrUpdateWatchTarget(WatchTargetSaveRequest request) {
        log.info("[Service] saveOrUpdateWatchTarget request={}", request);

        if (request == null || !StringUtils.hasText(request.targetCode())) {
            throw new BusinessException(40001, "标的编码不能为空");
        }

        String targetCode = request.targetCode();
        List<String> groupCodes = normalizeGroupCodes(request.groupCodes());

        // 查询该 targetCode + targetType 下所有已存在的 WatchTarget，便于按 groupCode 定位新增或更新。
        List<WatchTarget> existingList = this.list(
            new LambdaQueryWrapper<WatchTarget>()
                .eq(WatchTarget::getTargetCode, targetCode)
                .eq(StringUtils.hasText(request.targetType()), WatchTarget::getTargetType, request.targetType())
        );

        Map<String, WatchTarget> existingMap = existingList.stream()
            .collect(Collectors.toMap(WatchTarget::getGroupCode, wt -> wt, (a, b) -> a));

        List<WatchTarget> toSave = new ArrayList<>();
        for (String groupCode : groupCodes) {
            WatchTarget entity = existingMap.get(groupCode);
            if (entity == null) {
                entity = new WatchTarget();
                entity.setTargetCode(targetCode);
                entity.setGroupCode(groupCode);
            }
            entity.setTargetName(request.targetName());
            entity.setTargetType(request.targetType());
            entity.setMarketCode(request.marketCode());
            entity.setSortNo(request.sortNo());
            entity.setImportantFlag(request.importantFlag());
            entity.setWatchStatus(request.watchStatus());
            entity.setWatchReason(request.watchReason());
            toSave.add(entity);
            existingMap.remove(groupCode); // 从existingMap中移除已处理的groupCode，剩余的groupCode对应的记录需要删除
        }

        if (!toSave.isEmpty()) {
            this.saveOrUpdateBatch(toSave);
        }
        
        // 删掉 request.groupCodes 之外的分组记录
        if (!existingMap.isEmpty()) {
            this.removeByIds(existingMap.values().stream().map(WatchTarget::getId).collect(Collectors.toList()));
        }

        return getWatchTargetView(targetCode, request.targetType());
    }

    /**
     * 根据targetCode删除关注标的。
     * 传入 groupCode 时，仅删除该分组中的标的；不传 groupCode 时删除该标的的所有分组记录。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWatchTarget(String targetCode, String targetType, String groupCode) {
        log.info("[Service] deleteWatchTarget targetCode={}, targetType={}, groupCode={}", targetCode, targetType, groupCode);

        this.remove(
            new LambdaQueryWrapper<WatchTarget>()
                .eq(WatchTarget::getTargetCode, targetCode)
                .eq(StringUtils.hasText(targetType), WatchTarget::getTargetType, targetType)
                .eq(StringUtils.hasText(groupCode), WatchTarget::getGroupCode, groupCode)
        );
    }

    /**
     * 根据targetCode和targetType查询该标的所关联的groupCode，聚合所有groupCode的数据
     * @param targetCode
     * @param targetType
     * @return
     */
    @Override
    @Transactional(readOnly = true)
    public WatchTargetView getWatchTargetGroups(String targetCode, String targetType) {
        log.info("[Service] getWatchTargetGroups targetCode={}, targetType={}", targetCode, targetType);
        return getWatchTargetView(targetCode, targetType);
    }

    /**
     * 根据targetCode和targetType查询该标的所关联的groupCode，聚合所有groupCode到WatchTargetView的targetGroups字段中
     * @param targetCode
     * @param targetType
     * @return
     */
    private WatchTargetView getWatchTargetView(String targetCode, String targetType) {

        /** 查询与目标代码和类型匹配的WatchTarget实体，一个标的代码可能存在多个实体，对应多个分组， */
        List<WatchTarget> entities = this.list(
            new LambdaQueryWrapper<WatchTarget>()
                .eq(WatchTarget::getTargetCode, targetCode)
                .eq(StringUtils.hasText(targetType), WatchTarget::getTargetType, targetType)
        );

        if (entities.isEmpty()) {
            return null;
        }

        WatchTargetView view = WatchTargetConverter.toWatchTargetView(entities.get(0));
        view.setTargetGroups(buildTargetGroups(entities));
        return view;
    }

    /**
     * 为分页查询返回的 WatchTarget 实体列表构建 WatchTargetView 列表。该功能用于根据标的代码和分组编码进行分页查询，所以pageRecords中不会出现重复标的代码。
     * 该方法用于为每个WatchTarget实体查询其所属的全部分组信息，并将这些分组信息聚合到WatchTargetView的targetGroups字段中返回。
     * @param pageRecords
     * @return
     */
    private List<WatchTargetView> buildWatchTargetViews(List<WatchTarget> pageRecords) {
        if (pageRecords == null || pageRecords.isEmpty()) {
            return Collections.emptyList();
        }

        // 需要查询这些记录中所有的标的代码对应的分组信息，避免N+1查询问题
        List<String> targetCodes = pageRecords.stream()
            .map(WatchTarget::getTargetCode)
            .filter(StringUtils::hasText)
            .distinct()
            .collect(Collectors.toList());

        if (targetCodes.isEmpty()) {
            return Collections.emptyList();
        }

        // 查询这些标的代码对应的全部 WatchTarget 记录，以便从中提取分组信息
        List<WatchTarget> relatedTargets = this.list(
            new LambdaQueryWrapper<WatchTarget>()
                .in(WatchTarget::getTargetCode, targetCodes)
        );

        // 以targetCode + targetType 作为key，将相关的WatchTarget记录分组，便于后续构建每个标的所属的分组信息
        Map<String, List<WatchTarget>> relatedGroupMap = relatedTargets.stream()
            .collect(Collectors.groupingBy(this::buildTargetKey));

        // 构建WatchTargetView列表，聚合同一标的代码和类型的分组信息
        return pageRecords.stream()
            .map(entity -> {
                WatchTargetView view = WatchTargetConverter.toWatchTargetView(entity);
                List<WatchTarget> sameTargetRecords = relatedGroupMap.getOrDefault(buildTargetKey(entity), Collections.emptyList());
                view.setTargetGroups(buildTargetGroups(sameTargetRecords));
                return view;
            })
            .collect(Collectors.toList());
    }


    /** 
     * 为每个WatchTarge获取所属的分组信息，构建TargetGroup列表返回。
     * 一个标的可能属于多个分组，因此输入是一个WatchTarget列表，输出是一个TargetGroup列表。
     */
    private List<TargetGroupView> buildTargetGroups(List<WatchTarget> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }

        // 当前仅为了满足接口需求，简单地将groupCode封装到TargetGroup对象中返回。后续如果TargetGroup需要更多信息，可以在此方法中进行查询和构建。
        return entities.stream()
            .map(WatchTarget::getGroupCode)
            .filter(Objects::nonNull)
            .filter(StringUtils::hasText)
            .distinct()
            .map(groupCode -> {
                TargetGroupView group = new TargetGroupView();
                group.setGroupCode(groupCode);
                return group;
            })
            .collect(Collectors.toList());
    }

    private String buildTargetKey(WatchTarget entity) {
        return entity.getTargetCode() + "||" + (entity.getTargetType() == null ? "" : entity.getTargetType());
    }

    /**
     * 规范化分组编码列表，去除空值、重复值，并进行必要的格式化处理（如去除前后空格）。如果输入列表为null或空，则返回一个空列表。
     * @param groupCodes
     * @return
     */
    private List<String> normalizeGroupCodes(List<String> groupCodes) {
        if (groupCodes == null || groupCodes.isEmpty()) {
            return Collections.emptyList();
        }

        return groupCodes.stream()
            .filter(StringUtils::hasText)
            .map(String::trim)
            .distinct()
            .collect(Collectors.toList());
    }
}