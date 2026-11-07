-- 数据集管理体系：补充数据集当前状态、来源执行和数据范围信息
ALTER TABLE `dataset`
    ADD COLUMN `task_config_code` VARCHAR(64) NULL COMMENT '当前关联任务配置编码' AFTER `task_code`,
    ADD COLUMN `latest_task_run_id` VARCHAR(64) NULL COMMENT '最近一次成功执行记录编号' AFTER `storage_mapping_code`,
    ADD COLUMN `record_count` BIGINT NULL COMMENT '可信的当前记录总数' AFTER `result_location`,
    ADD COLUMN `data_start_time` DATETIME NULL COMMENT '当前数据覆盖起始时间' AFTER `record_count`,
    ADD COLUMN `data_end_time` DATETIME NULL COMMENT '当前数据覆盖结束时间' AFTER `data_start_time`,
    ADD COLUMN `last_collect_time` DATETIME NULL COMMENT '最近采集时间' AFTER `data_end_time`,
    ADD COLUMN `last_success_time` DATETIME NULL COMMENT '最近成功时间' AFTER `last_collect_time`;

ALTER TABLE `dataset`
    MODIFY COLUMN `dataset_status` TINYINT NOT NULL DEFAULT 0 COMMENT '数据集状态：DRAFT=0/ENABLED=1/DISABLED=2/ARCHIVED=3';

CREATE INDEX `idx_dataset_task_config_code` ON `dataset` (`task_config_code`);
CREATE INDEX `idx_dataset_data_type_code` ON `dataset` (`data_type_code`);
