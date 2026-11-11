package com.exchange.platform.analytics.api.response;

import java.util.List;

import lombok.Data;

/** 按 TargetScopeSelector 的四类可选项返回；当前类型不支持的列表为空。 */
@Data
public class TargetScopeOptionsView {
    private List<TargetScopeOptionView> board;
    private List<TargetScopeOptionView> watchGroup;
    private List<TargetScopeOptionView> indexType;
    private List<TargetScopeOptionView> market;
}
