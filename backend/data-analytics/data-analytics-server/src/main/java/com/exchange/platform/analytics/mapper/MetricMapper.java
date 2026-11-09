package com.exchange.platform.analytics.mapper;

import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exchange.platform.analytics.entity.AppMetric;

public interface MetricMapper extends BaseMapper<AppMetric> {
    /**
     * 锁定指标主档，供启停/删除与提醒规则写入共用。
     * 两类事务使用同一行锁后，引用检查与实际状态变更之间不能插入新的提醒引用。
     */
    @Select("SELECT * FROM app_metric WHERE metric_code = #{metricCode} FOR UPDATE")
    AppMetric selectByMetricCodeForUpdate(String metricCode);
}
