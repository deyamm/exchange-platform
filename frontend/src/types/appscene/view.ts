import type { SelectOption } from '/@/types/common'
import type { AppSceneType, CommonStatus } from './constants'


export const COMMON_STATUS_OPTIONS: SelectOption<CommonStatus>[] = [
  { label: '草稿', value: 'DRAFT' },
  { label: '启用', value: 'ENABLED' },
  { label: '停用', value: 'DISABLED' },
  { label: '删除', value: 'DELETED' },
  { label: '失效', value: 'EXPIRED' },
] as const;

export const COMMON_STATUS_TEXT: Record<CommonStatus, string> = {
  DRAFT: '草稿',
  ENABLED: '启用',
  DISABLED: '停用',
  DELETED: '删除',
  EXPIRED: '失效',
};

export const COMMON_STATUS_TAG_TYPE: Record<CommonStatus, '' | 'success' | 'info' | 'warning' | 'danger'> = {
  DRAFT: 'warning',
  ENABLED: 'success',
  DISABLED: 'info',
  DELETED: 'danger',
  EXPIRED: 'info',
};

export const SCENE_TYPE_OPTIONS: SelectOption<AppSceneType>[] = [
  { label: '市场概览', value: 'MARKET_OVERVIEW' },
  { label: '标的研究', value: 'TARGET_RESEARCH' },
  { label: '财务分析', value: 'FINANCIAL_ANALYSIS' },
  { label: '公告查看', value: 'ANNOUNCEMENT_VIEW' },
  { label: '筛选对比', value: 'SCREENING_COMPARISON' },
  { label: '提醒处理', value: 'ALERT_HANDLING' },
  { label: '研究记录', value: 'RESEARCH_RECORD' },
  { label: '追溯查询', value: 'TRACE_QUERY' },
];

export const SCENE_TYPE_TEXT: Record<AppSceneType, string> = {
  MARKET_OVERVIEW: '市场概览',
  TARGET_RESEARCH: '标的研究',
  FINANCIAL_ANALYSIS: '财务分析',
  ANNOUNCEMENT_VIEW: '公告查看',
  SCREENING_COMPARISON: '筛选对比',
  ALERT_HANDLING: '提醒处理',
  RESEARCH_RECORD: '研究记录',
  TRACE_QUERY: '追溯查询',
};