package com.exchange.platform.analytics.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exchange.platform.analytics.api.enums.CommonStatus;
import com.exchange.platform.analytics.api.request.ResearchTagSaveRequest;
import com.exchange.platform.analytics.api.response.ResearchTagView;
import com.exchange.platform.analytics.entity.ResearchTag;
import com.exchange.platform.common.core.page.PageResult;


public interface ResearchTagService extends IService<ResearchTag> {

    PageResult<ResearchTagView> getResearchTagList(String tagName, String tagCode, String tagCategory, CommonStatus status, Integer pageNo, Integer pageSize);

    ResearchTagView saveOrUpdateResearchTag(ResearchTagSaveRequest request);

    void deleteResearchTag(String tagCode);
}