import type { CommonStatus } from '../appscene/constants';
import type { AssetType } from './constants';

/** 应用指标主档；名称和字段类型是后端从 Dataset 当前版本派生的只读展示信息。 */
export interface AppMetric {
    metricCode: string;
    metricName: string;
    metricCategory: string;
    /** 指标适用的标的类型，创建后由后端强制保持不可修改。 */
    targetType: AssetType | '';
    targetTypeName?: string;
    /** 仅用于指标来源绑定，绝不保存物理表名或物理列名。 */
    datasetCode: string;
    fieldCode: string;
    unit?: string;
    status: CommonStatus;
    description?: string;
    datasetName?: string;
    fieldName?: string;
    fieldType?: string;
    createdAt?: string;
    updatedAt?: string;
}

export function createDefaultAppMetric(): AppMetric {
    return {
        metricCode: '',
        metricName: '',
        // 从“全部”新增时必须让用户明确选择分类，不能把 ALL 或隐式默认值写入记录。
        metricCategory: '',
        targetType: '',
        datasetCode: '',
        fieldCode: '',
        unit: '',
        status: 'ENABLED',
        description: ''
    };
}
