package com.exchange.platform.analytics.api.response;

import java.util.List;

import lombok.Data;

/** 一次筛选的完整结果，前端可安全按服务端已经完成的全量排序渲染。 */
@Data
public class ScreeningResultView {
    private long total;
    private List<ScreeningResultRowView> rows;
}
