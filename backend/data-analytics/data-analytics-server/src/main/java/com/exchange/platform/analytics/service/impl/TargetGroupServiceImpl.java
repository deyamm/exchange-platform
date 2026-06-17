package com.exchange.platform.analytics.service.impl;

import java.util.List;
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
import com.exchange.platform.analytics.api.request.TargetGroupSaveRequest;
import com.exchange.platform.analytics.api.response.TargetGroupView;
import com.exchange.platform.analytics.converter.TargetGroupConverter;
import com.exchange.platform.analytics.entity.TargetGroup;
import com.exchange.platform.analytics.mapper.TargetGroupMapper;
import com.exchange.platform.analytics.service.TargetGroupService;
import com.exchange.platform.common.core.page.PageResult;


@Service
public class TargetGroupServiceImpl extends ServiceImpl<TargetGroupMapper, TargetGroup> implements TargetGroupService {

    private static final Logger log = LoggerFactory.getLogger(TargetGroupServiceImpl.class);

    /**
     * 获取标的分组列表，支持分页和多条件过滤
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult<TargetGroupView> getTargetGroupList(String groupName, String groupCode, CommonStatus status, Integer pageNo, Integer pageSize) {

        log.info("[Service] getTargetGroupList groupName={}, groupCode={}, status={}, pageNo={}, pageSize={}", groupName, groupCode, status, pageNo, pageSize);

        Integer safePageNo = (pageNo == null || pageNo < 1) ? 1 : pageNo;
        Integer safePageSize = (pageSize == null || pageSize < 1) ? 20 : Math.min(200, pageSize);

        LambdaQueryWrapper<TargetGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(groupName), TargetGroup::getGroupName, groupName)
            .like(StringUtils.hasText(groupCode), TargetGroup::getGroupCode, groupCode)
            .eq(status != null, TargetGroup::getStatus, status);

        Page<TargetGroup> page = this.page(new Page<>(safePageNo, safePageSize), wrapper);
        List<TargetGroupView> viewList = page.getRecords().stream()
            .map(TargetGroupConverter::toTargetGroupView)
            .collect(Collectors.toList());
        return new PageResult<>(safePageNo, safePageSize, page.getTotal(), viewList);
    }

    /**
     * 保存或更新标的分组，先根据groupCode查询，如果存在则更新，不存在则新增
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TargetGroupView saveOrUpdateTargetGroup(TargetGroupSaveRequest request) {
        log.info("[Service] saveOrUpdateTargetGroup request={}", request);

        TargetGroup entity = this.getOne(
            new LambdaQueryWrapper<TargetGroup>()
                .eq(TargetGroup::getGroupCode, request.groupCode())
        );

        if (entity == null) {
            entity = new TargetGroup();
            entity.setGroupCode(request.groupCode());
        }
        entity.setGroupName(request.groupName())
            .setSortNo(request.sortNo())
            .setStatus(request.status())
            .setDescription(request.description());

        this.saveOrUpdate(entity);
        return TargetGroupConverter.toTargetGroupView(entity);
    }

    /**
     * 删除标的分组
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTargetGroup(String groupCode) {
        log.info("[Service] deleteTargetGroup groupCode={}", groupCode);

        this.remove(new LambdaQueryWrapper<TargetGroup>().eq(TargetGroup::getGroupCode, groupCode));
    }
}