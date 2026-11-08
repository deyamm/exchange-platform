export type DatasetMetadataGroup = 'BUSINESS' | 'MANAGEMENT' | 'STORAGE' | 'STATISTICS' | 'EXTENSION'
export type DatasetMetadataValueType = 'STRING' | 'JSON' | 'NUMBER'
export type DatasetMetadataMaintainMode = 'MANUAL' | 'CONFIG_DERIVED' | 'EXECUTION_DERIVED'

export type DatasetMetadataDefinition = {
  group: DatasetMetadataGroup
  key: string
  name: string
  valueType: DatasetMetadataValueType
  maintainMode: DatasetMetadataMaintainMode
  component?: 'text' | 'textarea' | 'granularity' | 'time-field' | 'json'
  options?: string[]
}

const temporalUnits = ['SECOND', 'MINUTE', 'HOUR', 'DAY', 'WEEK', 'MONTH', 'QUARTER', 'YEAR']

export const datasetMetadataDefinitions: DatasetMetadataDefinition[] = [
  { group: 'BUSINESS', key: 'business_definition', name: '业务定义', valueType: 'STRING', maintainMode: 'MANUAL', component: 'textarea' },
  { group: 'BUSINESS', key: 'business_scope', name: '业务范围', valueType: 'STRING', maintainMode: 'MANUAL', component: 'textarea' },
  { group: 'BUSINESS', key: 'data_granularity', name: '数据粒度', valueType: 'JSON', maintainMode: 'MANUAL', component: 'granularity', options: [...temporalUnits, 'TICK', 'EVENT', 'SNAPSHOT', 'STATIC'] },
  { group: 'BUSINESS', key: 'data_time_field', name: '数据时间字段', valueType: 'STRING', maintainMode: 'MANUAL', component: 'time-field' },
  { group: 'BUSINESS', key: 'unit', name: '数据单位', valueType: 'STRING', maintainMode: 'MANUAL', component: 'text' },
  { group: 'BUSINESS', key: 'currency', name: '货币类型', valueType: 'STRING', maintainMode: 'MANUAL', component: 'text' },
  { group: 'BUSINESS', key: 'business_key', name: '业务主键', valueType: 'JSON', maintainMode: 'CONFIG_DERIVED' },
  { group: 'MANAGEMENT', key: 'data_owner', name: '数据负责人', valueType: 'STRING', maintainMode: 'MANUAL', component: 'text' },
  { group: 'MANAGEMENT', key: 'maintainer', name: '维护人员', valueType: 'STRING', maintainMode: 'MANUAL', component: 'text' },
  { group: 'MANAGEMENT', key: 'contact', name: '联系方式', valueType: 'STRING', maintainMode: 'MANUAL', component: 'text' },
  { group: 'MANAGEMENT', key: 'update_frequency', name: '更新频率', valueType: 'JSON', maintainMode: 'MANUAL', component: 'granularity', options: temporalUnits },
  { group: 'MANAGEMENT', key: 'sensitivity_level', name: '敏感级别', valueType: 'STRING', maintainMode: 'MANUAL', component: 'text' },
  { group: 'MANAGEMENT', key: 'usage_scope', name: '使用范围', valueType: 'STRING', maintainMode: 'MANUAL', component: 'text' },
  { group: 'MANAGEMENT', key: 'retention_policy', name: '保留策略', valueType: 'STRING', maintainMode: 'MANUAL', component: 'text' },
  { group: 'STORAGE', key: 'storage_type', name: '存储类型', valueType: 'STRING', maintainMode: 'CONFIG_DERIVED' },
  { group: 'STORAGE', key: 'datasource_code', name: '数据源编码', valueType: 'STRING', maintainMode: 'CONFIG_DERIVED' },
  { group: 'STORAGE', key: 'schema_name', name: 'Schema', valueType: 'STRING', maintainMode: 'CONFIG_DERIVED' },
  { group: 'STORAGE', key: 'table_name', name: '物理表', valueType: 'STRING', maintainMode: 'CONFIG_DERIVED' },
  { group: 'STORAGE', key: 'storage_unique_key', name: '物理唯一键', valueType: 'JSON', maintainMode: 'CONFIG_DERIVED' },
  { group: 'STATISTICS', key: 'record_count', name: '数据总条数', valueType: 'NUMBER', maintainMode: 'EXECUTION_DERIVED' },
  { group: 'STATISTICS', key: 'time_coverage_field', name: '时间覆盖统计字段', valueType: 'STRING', maintainMode: 'EXECUTION_DERIVED' },
  { group: 'STATISTICS', key: 'time_coverage_start', name: '数据起始时间', valueType: 'STRING', maintainMode: 'EXECUTION_DERIVED' },
  { group: 'STATISTICS', key: 'time_coverage_end', name: '数据结束时间', valueType: 'STRING', maintainMode: 'EXECUTION_DERIVED' },
  { group: 'STATISTICS', key: 'statistics_updated_at', name: '统计更新时间', valueType: 'STRING', maintainMode: 'EXECUTION_DERIVED' },
  { group: 'EXTENSION', key: 'tags', name: '数据标签', valueType: 'JSON', maintainMode: 'MANUAL', component: 'json' },
  { group: 'EXTENSION', key: 'keywords', name: '关键词', valueType: 'JSON', maintainMode: 'MANUAL', component: 'json' },
  { group: 'EXTENSION', key: 'custom_remark', name: '扩展说明', valueType: 'STRING', maintainMode: 'MANUAL', component: 'textarea' },
  { group: 'EXTENSION', key: 'custom_properties', name: '自定义属性', valueType: 'JSON', maintainMode: 'MANUAL', component: 'json' }
]

export const manualDatasetMetadataDefinitions = datasetMetadataDefinitions.filter(item => item.maintainMode === 'MANUAL')
export const metadataDefinition = (group: string, key: string) => datasetMetadataDefinitions.find(item => item.group === group && item.key === key)
