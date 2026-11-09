-- Dataset 的 asset_type 是正式核心属性，来源于实际成功执行时快照锁定的任务模板版本。
ALTER TABLE dataset
    ADD COLUMN asset_type VARCHAR(16) NULL COMMENT '标的类型：STOCK/INDEX/FUTURE/FUND/BOND' AFTER dataset_name;

CREATE INDEX idx_dataset_asset_type_status ON dataset (asset_type, dataset_status);

-- 任务模板版本历史值先统一到跨语言协议编码；未知值不做兼容或猜测。
UPDATE task_template_version SET asset_type = CASE asset_type
    WHEN '股票' THEN 'STOCK' WHEN '指数' THEN 'INDEX' WHEN '期货' THEN 'FUTURE'
    WHEN '基金' THEN 'FUND' WHEN '债券' THEN 'BOND' ELSE asset_type END
WHERE asset_type IN ('股票', '指数', '期货', '基金', '债券');

-- 只对能通过“最近成功执行快照 → 实际任务模板版本”可靠定位的数据集回填；不猜测其余记录。
UPDATE dataset d
JOIN execution_config_snapshot s ON s.run_id = d.latest_task_run_id AND s.dataset_code = d.dataset_code
JOIN task_template_version v ON v.task_code = s.task_code AND v.version_no = s.task_template_version
SET d.asset_type = v.asset_type
WHERE d.asset_type IS NULL
  AND v.asset_type IN ('STOCK', 'INDEX', 'FUTURE', 'FUND', 'BOND');
