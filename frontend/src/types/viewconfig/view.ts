import { SelectOption } from "../common";
import type { ViewType } from "./constants";

export const VIEW_TYPE_OPTIONS: SelectOption<ViewType>[] = [
  { label: '看板', value: 'DASHBOARD' },
  { label: '列表', value: 'LIST' },
  { label: '详情', value: 'DETAIL' },
  { label: '默认', value: 'DEFAULT' },
];

export const VIEW_TYPE_TEXT: Record<ViewType, string> = {
    DASHBOARD: '看板',
    LIST: '列表',
    DETAIL: '详情',
    DEFAULT: '默认',
};