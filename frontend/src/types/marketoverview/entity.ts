
export interface IndexBasic {
    fullSymbol: string
    name: string
    fullname: string
    market: string
    publisher: string
    indexType: string
    category: string
    baseDate: string
    basePoint: number
    listDate: string
    weightRule: string
    description: string
    expDate: string
}

export interface MatrixRow {
    market: string
    marketName: string
    counts: Record<string, number>
}
