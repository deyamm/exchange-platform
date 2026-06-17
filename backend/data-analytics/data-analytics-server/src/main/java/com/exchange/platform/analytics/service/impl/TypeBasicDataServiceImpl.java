package com.exchange.platform.analytics.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.exchange.platform.analytics.mapper.TypeBasicDataMapper;
import com.exchange.platform.analytics.service.TypeBasicDataService;

@Service
@DS("type_basic_data")
public class TypeBasicDataServiceImpl implements TypeBasicDataService {
    
    private final TypeBasicDataMapper typeBasicDataMapper;

    public TypeBasicDataServiceImpl(TypeBasicDataMapper typeBasicDataMapper) {
        this.typeBasicDataMapper = typeBasicDataMapper;
    }

    @Override
    @Cacheable(cacheNames = "typeBasicDataCache", key = "'indexBasic'")
    public List<Map<String, Object>> getIndexBasic() {
        return typeBasicDataMapper.getIndexBasic();
    }
}
