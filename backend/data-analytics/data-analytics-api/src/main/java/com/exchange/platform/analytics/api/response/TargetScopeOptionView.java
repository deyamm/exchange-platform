package com.exchange.platform.analytics.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 可供 TargetScopeSelector 展示的最小主数据项，不向浏览器暴露数据表结构。 */
@Data
@AllArgsConstructor
public class TargetScopeOptionView {
    private String code;
    private String name;
    /** 仅行业指数选项携带，用于在前端按发布方和层级过滤。 */
    private String publisherCode;
    private Integer level;

    public TargetScopeOptionView(String code, String name) {
        this(code, name, null, null);
    }
}
