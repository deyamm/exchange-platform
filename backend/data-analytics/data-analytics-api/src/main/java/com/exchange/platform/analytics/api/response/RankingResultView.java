package com.exchange.platform.analytics.api.response;

import java.util.List;

import lombok.Data;

@Data
public class RankingResultView {
    private long total;
    private List<RankingResultRowView> rows;
}
