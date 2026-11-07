ALTER TABLE `execution_config_snapshot`
    ADD COLUMN `dataset_code` VARCHAR(64) NULL COMMENT '本次执行对应的稳定数据集编码' AFTER `data_type_version`;
