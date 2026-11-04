package com.exchange.platform.analytics.mapper;

import java.util.List;
import java.util.Map;

import com.exchange.platform.analytics.entity.WatchTarget;


public interface TypeBasicDataMapper {
    
    List<Map<String, Object>> getIndexBasic();

    List<WatchTarget> searchByCode(String keyword, Integer limit);

    List<WatchTarget> searchByName(String keyword, Integer limit);
}
