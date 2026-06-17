import http from '/@/utils/http'
import type { DataSource, DataSourceTable, DataSourceTableRequest } from '/@/types/datasource/entity'

export function listDataSources(params?: { keyword?: string; dataSourceType?: string }): Promise<DataSource[]> | any {
    return http.get<DataSource[]>('/api/v1/collection/data-sources', { params })
}

export function getDataSourceDetail(dataSourceCode: string): Promise<DataSource> | any {
    return http.get<DataSource>(`/api/v1/collection/data-sources/${dataSourceCode}`)
}

export function saveDataSource(data: DataSource): Promise<DataSource> | any {
    return http.post<DataSource>('/api/v1/collection/data-sources', data)
}

export function deleteDataSource(dataSourceCode: string): Promise<void> | any {
    return http.delete<void>(`/api/v1/collection/data-sources/${dataSourceCode}`)
}

export function testDataSourceConnection(dataSourceCode: string): Promise<boolean> | any {
    return http.post<boolean>(`/api/v1/collection/data-sources/${dataSourceCode}/test`)
}

export function listDataSourceTables(dataSourceCode: string): Promise<DataSourceTable[]> | any {
    return http.get<DataSourceTable[]>(`/api/v1/collection/data-sources/${dataSourceCode}/tables`)
}

export function createDataSourceTable(dataSourceCode: string, data: DataSourceTableRequest): Promise<void> | any {
    return http.post<void>(`/api/v1/collection/data-sources/${dataSourceCode}/tables`, data)
}

export function alterDataSourceTable(dataSourceCode: string, data: DataSourceTableRequest): Promise<void> | any {
    return http.put<void>(`/api/v1/collection/data-sources/${dataSourceCode}/tables`, data)
}

export function dropDataSourceTable(dataSourceCode: string, tableName: string): Promise<void> | any {
    return http.delete<void>(`/api/v1/collection/data-sources/${dataSourceCode}/tables/${tableName}`)
}
