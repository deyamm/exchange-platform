package com.exchange.platform.analytics.mapper;

import java.util.List;
import java.util.Map;

import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Param;
import com.exchange.platform.analytics.entity.WatchTarget;
import com.exchange.platform.analytics.service.support.TargetScopeTarget;


public interface TypeBasicDataMapper {
    
    List<Map<String, Object>> getIndexBasic();

    List<WatchTarget> searchByCode(@Param("keyword") String keyword, @Param("limit") Integer limit, @Param("targetType") String targetType);

    List<WatchTarget> searchByName(@Param("keyword") String keyword, @Param("limit") Integer limit, @Param("targetType") String targetType);

    /** 所有基础资料查询固定走 type_basic_data，避免筛选服务误读应用库。 */
    @DS("type_basic_data")
    List<TargetScopeTarget> selectActiveStockTargets(@Param("boardCodes") List<String> boardCodes);

    @DS("type_basic_data")
    List<TargetScopeTarget> selectIndexTargets(@Param("filterType") String filterType,
            @Param("filterValues") List<String> filterValues);

    @DS("type_basic_data")
    List<TargetScopeTarget> selectStockBoardOptions();

    @DS("type_basic_data")
    List<TargetScopeTarget> selectIndexTypeOptions();

    @DS("type_basic_data")
    List<TargetScopeTarget> selectIndexMarketOptions();

    @DS("type_basic_data")
    List<TargetScopeTarget> selectTargetsByCodes(@Param("targetType") String targetType, @Param("targetCodes") List<String> targetCodes);
}
