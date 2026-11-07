-- 移除已下线的业务表调度配置及其专属列。
DELETE FROM execution_scheduler WHERE trigger_type = 2;

ALTER TABLE execution_scheduler
    DROP COLUMN biz_data_source_code,
    DROP COLUMN biz_table_schema,
    DROP COLUMN biz_table_name,
    DROP COLUMN biz_schedule_column,
    MODIFY COLUMN trigger_type TINYINT COMMENT '触发类型(0:MANUAL，1:SCHEDULED，3:EVENT)';
