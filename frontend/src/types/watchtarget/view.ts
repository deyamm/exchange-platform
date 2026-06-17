import { AssetType, WatchStatus } from "./constants";

export const WATCH_STATUS_OPTIONS = [
    { label: '观察中', value: 'WATCHING' },
    { label: '重点关注', value: 'FOCUS' },
    { label: '暂停观察', value: 'PAUSED' },
    { label: '结束观察', value: 'CLOSED' },
] as const;

export const WATCH_STATUS_TEXT: Record<WatchStatus, string> = {
    WATCHING: '观察中',
    FOCUS: '重点关注',
    PAUSED: '暂停观察',
    CLOSED: '结束观察'
};

export const WATCH_STATUS_TAG_TYPE: Record<WatchStatus, '' | 'success' | 'info' | 'warning'> = {
    WATCHING: 'success', 
    FOCUS: 'warning', 
    PAUSED: 'info', 
    CLOSED: 'info'
};

export const ASSET_TYPE_OPTIONS = [
    { label: '股票', value: 'STOCK' },
    { label: '基金', value: 'FUND' },
    { label: '指数', value: 'INDEX' },
    { label: '债券', value: 'BOND' },
    { label: '其他', value: 'OTHER' },
] as const;

export const ASSET_TYPE_TEXT: Record<AssetType, string> = {
    STOCK: '股票',
    FUND: '基金',
    INDEX: '指数',
    BOND: '债券',
    OTHER: '其他'
};