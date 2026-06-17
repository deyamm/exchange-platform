package com.exchange.platform.analytics.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exchange.platform.analytics.api.response.TargetGroupView;
import com.exchange.platform.analytics.entity.TargetGroup;
import com.exchange.platform.common.core.page.PageResult;
import com.exchange.platform.analytics.api.enums.CommonStatus;
import com.exchange.platform.analytics.api.request.TargetGroupSaveRequest;

public interface TargetGroupService extends IService<TargetGroup> {

    public PageResult<TargetGroupView> getTargetGroupList(String groupName, String groupCode, CommonStatus status, Integer pageNo, Integer pageSize);

    public TargetGroupView saveOrUpdateTargetGroup(TargetGroupSaveRequest request);

    public void deleteTargetGroup(String groupCode);
}