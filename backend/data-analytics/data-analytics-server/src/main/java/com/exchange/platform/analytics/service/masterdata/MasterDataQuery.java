package com.exchange.platform.analytics.service.masterdata;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public record MasterDataQuery(
    Map<String, Object> equalsFilters, 
    Map<String, ? extends Collection<?>> inFilters, 
    String keyword, 
    List<String> keywordFields, 
    String orderBy, 
    boolean ascending, 
    int pageNo, 
    int pageSize
) {
    
    public MasterDataQuery {
        equalsFilters = equalsFilters == null ? Map.of() : Map.copyOf(equalsFilters);
        inFilters = inFilters == null ? Map.of() : Map.copyOf(inFilters);
        keywordFields = keywordFields == null ? List.of() : List.copyOf(keywordFields);
        pageNo = Math.max(pageNo, 1);
        pageSize = Math.min(Math.max(pageSize, 1), 2000);
    }

    public static MasterDataQuery all() {
        return new MasterDataQuery(Map.of(), Map.of(), null, List.of(), null, true, 1, 200);
    }
}
