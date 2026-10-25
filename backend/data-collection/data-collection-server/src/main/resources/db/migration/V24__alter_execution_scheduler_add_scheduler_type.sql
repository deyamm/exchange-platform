ALTER TABLE execution_scheduler
ADD COLUMN trigger_type tinyint COMMENT '触发类型(1:SCHEDULED，使用cron表达式调度；2:BIZTABLE，基于业务表数据变化触发；)',
ADD COLUMN biz_data_source_code VARCHAR(32) COMMENT '业务表数据源编号',
ADD COLUMN biz_table_schema VARCHAR(64) COMMENT '业务表模式',
ADD COLUMN biz_table_name VARCHAR(64) COMMENT '业务表名称',
ADD COLUMN biz_schedule_column VARCHAR(64) COMMENT '业务表调度依据字段';