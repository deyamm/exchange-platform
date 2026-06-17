import type { CommonStatus } from '/@/types/collectiontask/constants'

export type DataSourceType = 'MYSQL'

export interface DataSource {
    dataSourceCode: string
    dataSourceName: string
    dataSourceType: DataSourceType
    host: string
    port: number
    username: string
    password?: string
    databaseName: string
    connectionParams?: string
    status: CommonStatus
    remark?: string
    createdAt?: string
    updatedAt?: string
}

export interface DataSourceTableRequest {
    tableName: string
    columns: DataSourceTableColumn[]
}

export interface DataSourceTable {
    tableName: string
    schemaName: string
    dataSourceCode: string
    currentRows: number
    columns: DataSourceTableColumn[]
}

export interface DataSourceTableColumn {
    columnName: string
    columnType: string
    nullable?: boolean
    defaultValue?: string
    uniqueKey?: boolean
}
