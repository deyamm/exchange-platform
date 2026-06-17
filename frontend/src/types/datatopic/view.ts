import { SelectOption } from "../common";
import { DataTopicStatus } from "./constants";

/**
 * OPTIONS用于下拉框，TEXT用于显示文本，TAG_TYPE用于标签颜色
 */

export const DATA_TOPIC_STATUS_OPTIONS: SelectOption<DataTopicStatus>[] = [
  { label: "启用", value: "ENABLED" },
  { label: "停用", value: "DISABLED" },
];

export const DATA_TOPIC_STATUS_TEXT: Record<DataTopicStatus, string> = {
  ENABLED: "启用",
  DISABLED: "停用",
};

export const DATA_TOPIC_STATUS_TAG_TYPE: Record<DataTopicStatus, 'success' | 'info'> = {
  ENABLED: "success",
  DISABLED: "info",
};

export interface DataTopicFormModel {
    dataTopicCode: string;
    dataTopicName: string;
    dataTopicLabel: string;
    parentCode?: string;
    sortNo: number;
    status: DataTopicStatus;
    description: string;
}