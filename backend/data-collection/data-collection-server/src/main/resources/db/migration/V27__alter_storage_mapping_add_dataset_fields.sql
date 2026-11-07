-- 一个任务配置在本期只对应一个稳定数据集输出。
ALTER TABLE `storage_mapping`
    ADD COLUMN `dataset_code` VARCHAR(64) NULL COMMENT '稳定数据集编码' AFTER `task_config_code`,
    ADD COLUMN `dataset_name` VARCHAR(128) NULL COMMENT '首次登记时的数据集名称' AFTER `dataset_code`,
    ADD COLUMN `dataset_desc` VARCHAR(1000) NULL COMMENT '首次登记时的数据集说明' AFTER `dataset_name`;

CREATE UNIQUE INDEX `uk_storage_mapping_dataset_code` ON `storage_mapping` (`dataset_code`);
