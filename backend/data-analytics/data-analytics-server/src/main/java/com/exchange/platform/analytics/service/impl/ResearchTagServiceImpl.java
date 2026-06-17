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
import com.exchange.platform.analytics.api.request.ResearchTagSaveRequest;
import com.exchange.platform.analytics.api.response.ResearchTagView;
import com.exchange.platform.analytics.converter.ResearchTagConverter;
import com.exchange.platform.analytics.entity.ResearchTag;
import com.exchange.platform.analytics.mapper.ResearchTagMapper;
import com.exchange.platform.analytics.service.ResearchTagService;
import com.exchange.platform.common.core.page.PageResult;


@Service
public class ResearchTagServiceImpl extends ServiceImpl<ResearchTagMapper, ResearchTag> implements ResearchTagService {

    private static final Logger log = LoggerFactory.getLogger(ResearchTagServiceImpl.class);

    @Override
    @Transactional(readOnly = true)
    public PageResult<ResearchTagView> getResearchTagList(String tagName, String tagCode, String tagCategory, CommonStatus status, Integer pageNo, Integer pageSize) {
        log.info("[Service] getResearchTagList tagName={}, tagCode={}, tagCategory={}, status={}, pageNo={}, pageSize={}", tagName, tagCode, tagCategory, status, pageNo, pageSize);

        Integer safePageNo = (pageNo == null || pageNo < 1) ? 1 : pageNo;
        Integer safePageSize = (pageSize == null || pageSize < 1) ? 20 : Math.min(200, pageSize);

        LambdaQueryWrapper<ResearchTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(tagName), ResearchTag::getTagName, tagName)
            .eq(StringUtils.hasText(tagCode), ResearchTag::getTagCode, tagCode)
            .eq(StringUtils.hasText(tagCategory), ResearchTag::getTagCategory, tagCategory)
            .eq(status != null, ResearchTag::getStatus, status);

        Page<ResearchTag> page = this.page(new Page<>(safePageNo, safePageSize), wrapper);
        List<ResearchTagView> viewList = page.getRecords().stream()
            .map(ResearchTagConverter::toResearchTagView)
            .collect(Collectors.toList());
        return new PageResult<>(safePageNo, safePageSize, page.getTotal(), viewList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResearchTagView saveOrUpdateResearchTag(ResearchTagSaveRequest request) {
        log.info("[Service] saveOrUpdateResearchTag request={}", request);

        ResearchTag entity = this.getOne(
            new LambdaQueryWrapper<ResearchTag>()
                .eq(ResearchTag::getTagCode, request.tagCode())
        );

        if (entity == null) {
            entity = new ResearchTag();
            entity.setTagCode(request.tagCode());
        }

        entity.setTagName(request.tagName())
            .setTagCategory(request.tagCategory())
            .setStatus(request.status())
            .setDescription(request.description());

        this.saveOrUpdate(entity);
        return ResearchTagConverter.toResearchTagView(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteResearchTag(String tagCode) {
        log.info("[Service] deleteResearchTag tagCode={}", tagCode);

        this.remove(new LambdaQueryWrapper<ResearchTag>().eq(ResearchTag::getTagCode, tagCode));
    }
}