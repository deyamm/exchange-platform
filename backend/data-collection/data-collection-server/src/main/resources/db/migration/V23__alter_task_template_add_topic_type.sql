ALTER TABLE task_template
ADD COLUMN data_topic_code VARCHAR(32) COMMENT '数据主题编号',
ADD COLUMN data_type_code VARCHAR(32) COMMENT '数据类型编号';