import { CommonStatus } from '../appscene/constants';
import type { AssetType, WatchStatus } from './constants';

export interface WatchTarget {
  targetCode: string;
  targetName: string;
  /** 与应用指标一致的标准标的类型协议编码。 */
  targetType: AssetType;
  marketCode: string;
  targetGroups?: TargetGroup[];
  sortNo: number;
  importantFlag: boolean;
  watchStatus: WatchStatus;
  watchReason?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface TargetGroup {
  groupCode: string;
  groupName: string;
  sortNo: number;
  status: CommonStatus;
  description?: string;
  targetCount?: number;
  createdAt?: string;
  updatedAt?: string;
}

export function createDefaultWatchTarget(): WatchTarget {
  return {
    targetCode: '',
    targetName: '',
    targetType: 'STOCK',
    marketCode: 'SZ',
    targetGroups: [],
    importantFlag: false,
    watchStatus: 'WATCHING',
    watchReason: '',
    sortNo: 1,
  };
}

export function createDefaultTargetGroup(): TargetGroup {
  return {
    groupCode: '',
    groupName: '',
    sortNo: 0,
    status: 'ENABLED',
    description: '',
    targetCount: 0,
  };
}
