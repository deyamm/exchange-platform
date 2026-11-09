import type { SelectOption } from '/@/types/common';

/**
 * 全站统一的标的类型协议值。
 *
 * 此处必须与 Python `AssetType`、Java `AssetType` 及数据库保存值保持完全一致；
 * 中文名称仅用于界面展示，不能作为接口或持久化值传递。
 */
export type AssetType = 'STOCK' | 'INDEX' | 'FUTURE' | 'FUND' | 'BOND';

/** 应用于数据集、应用指标及标的相关筛选器的统一下拉选项。 */
export const ASSET_TYPE_OPTIONS: SelectOption<AssetType>[] = [
    { label: '股票', value: 'STOCK' },
    { label: '指数', value: 'INDEX' },
    { label: '期货', value: 'FUTURE' },
    { label: '基金', value: 'FUND' },
    { label: '债券', value: 'BOND' },
];

/** 标准协议值到展示名称的唯一映射，避免各页面自行维护中文判断。 */
export const ASSET_TYPE_TEXT: Record<AssetType, string> = {
    STOCK: '股票',
    INDEX: '指数',
    FUTURE: '期货',
    FUND: '基金',
    BOND: '债券',
};
