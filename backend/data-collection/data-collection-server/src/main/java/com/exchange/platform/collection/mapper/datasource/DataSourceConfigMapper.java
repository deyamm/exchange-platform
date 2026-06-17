package com.exchange.platform.collection.mapper.datasource;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exchange.platform.collection.entity.datasource.DataSourceConfig;

public interface DataSourceConfigMapper extends BaseMapper<DataSourceConfig> {

    List<Map<String, Object>> selectTablesInfo(@Param("databaseName") String databaseName);

}
