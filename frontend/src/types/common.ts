export interface ApiResponse<T> {
    code: number;
    message: string;
    data: T;
    requestId: string;
    timestamp: string;
}

export interface PageResult<T> {
    pageNo: number;
    pageSize: number;
    total: number;
    records: T[];
}

export interface SelectOption<T = string> {
    label: string;
    value: T;
    disabled?: boolean;
}