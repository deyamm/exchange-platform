import type { ColumnMappingForm } from '/@/types/collectiontask/view'

export const MYSQL_BASE_TYPE_OPTIONS_BY_FIELD_TYPE: Record<string, any[]> = {
    STRING: [
        { label: 'VARCHAR（变长字符串）', value: 'VARCHAR' },
        { label: 'CHAR（固定长度字符串）', value: 'CHAR' }
    ],
    TEXT: [
        { label: 'TEXT（长文本，约 64KB）', value: 'TEXT' },
        { label: 'MEDIUMTEXT（较大文本，约 16MB）', value: 'MEDIUMTEXT' },
        { label: 'LONGTEXT（超大文本）', value: 'LONGTEXT' }
    ],
    INTEGER: [
        { label: 'INT（常规整数）', value: 'INT' },
        { label: 'BIGINT（大整数、ID、计数）', value: 'BIGINT' },
        { label: 'SMALLINT（小范围整数）', value: 'SMALLINT' },
        { label: 'TINYINT（很小范围整数）', value: 'TINYINT' }
    ],
    DECIMAL: [
        { label: 'DECIMAL（精确小数）', value: 'DECIMAL' }
    ],
    DATE: [
        { label: 'DATE（日期）', value: 'DATE' }
    ],
    DATETIME: [
        { label: 'DATETIME（日期时间）', value: 'DATETIME' },
        { label: 'TIMESTAMP（时间戳）', value: 'TIMESTAMP' }
    ],
    BOOLEAN: [
        { label: 'TINYINT（布尔推荐）', value: 'TINYINT' },
        { label: 'BOOLEAN（MySQL 中等价于 TINYINT(1)）', value: 'BOOLEAN' }
    ],
    ENUM: [
        { label: 'VARCHAR（枚举编码，推荐）', value: 'VARCHAR' },
        { label: 'ENUM（需配置枚举值）', value: 'ENUM' }
    ]
}

export const MYSQL_LENGTH_OPTIONS_BY_BASE_TYPE: Record<string, any[]> = {
    VARCHAR: [
        { label: '255（通用字符串）', value: '255' },
        { label: '128（名称、短文本）', value: '128' },
        { label: '64（编码、状态值、枚举值）', value: '64' },
        { label: '32（短编码）', value: '32' },
        { label: '512（较长字符串）', value: '512' },
        { label: '1024（URL、较长描述）', value: '1024' }
    ],
    CHAR: [
        { label: '32（固定长度编码）', value: '32' },
        { label: '36（UUID）', value: '36' },
        { label: '64（固定长度编码）', value: '64' }
    ],
    DECIMAL: [
        { label: '20,2（大金额）', value: '20,2' },
        { label: '18,6（通用小数）', value: '18,6' },
        { label: '18,2（常用金额）', value: '18,2' },
        { label: '20,4（金额含更多小数位）', value: '20,4' },
        { label: '10,6（比例/比率）', value: '10,6' },
        { label: '8,4（百分比或较短比例）', value: '8,4' },
        { label: '20,8（高精度小数）', value: '20,8' }
    ],
    TINYINT: [
        { label: '1（布尔值）', value: '1' }
    ],
    ENUM: [
        { label: "'A','B'（示例，按实际枚举值调整）", value: "'A','B'" }
    ]
}

type MysqlColumnTypeOption = {
    label: string
    value: string
}

type MysqlColumnTypeRow = Pick<
    ColumnMappingForm,
    | 'physicalColumnBaseType'
    | 'physicalColumnTypeLength'
    | 'physicalColumnType'
>

type ParsedMysqlColumnType = {
    baseType: string
    length: string
    fullType: string
}

type UseMysqlColumnTypeOptions = {
    onChange?: () => void
}

export function useMysqlColumnType(options: UseMysqlColumnTypeOptions = {}) {
    /**
     * 根据数据类型字段类型生成mysql字段类型选项，默认字符串类型为VARCHAR
     */
    function getMysqlBaseTypeOptions(row: { dataTypeFieldType?: string }): MysqlColumnTypeOption[] {
        const fieldType = row.dataTypeFieldType?.toUpperCase?.() || ''

        return MYSQL_BASE_TYPE_OPTIONS_BY_FIELD_TYPE[fieldType] || [
            { label: 'VARCHAR（默认字符串）', value: 'VARCHAR' }
        ]
    }

    /**
     * 根据mysql字段基础类型生成长度/精度选项
     */
    function getMysqlColumnTypeLengthOptions(row: { physicalColumnBaseType?: string }): MysqlColumnTypeOption[] {
        if (!row.physicalColumnBaseType) {
            return []
        }

        return MYSQL_LENGTH_OPTIONS_BY_BASE_TYPE[row.physicalColumnBaseType] || []
    }

    /**
     * 判断当前mysql字段类型是否有长度/精度选项
     */
    function hasMysqlColumnTypeLength(row: { physicalColumnBaseType?: string }): boolean {
        return getMysqlColumnTypeLengthOptions(row).length > 0
    }

    /**
     * 根据mysql字段基础类型获取默认长度/精度
     */
    function getDefaultMysqlColumnTypeLength(baseType: string): string {
        const lengthOptions = MYSQL_LENGTH_OPTIONS_BY_BASE_TYPE[baseType]

        if (!lengthOptions?.length) {
            return ''
        }

        return lengthOptions[0].value
    }

    /**
     * 根据基础类型和长度生成完整的mysql字段类型
     */
    function buildMysqlColumnType(baseType: string, length: string): string {
        if (!baseType) {
            return ''
        }

        if (!length) {
            return baseType
        }

        return `${baseType}(${length})`
    }

    /**
     * 根据columnType字符串解析出基础类型和长度
     */
    function parseMysqlColumnType(
        columnType: string | undefined,
        fallbackBaseType = 'VARCHAR'
    ): ParsedMysqlColumnType {
        if (!columnType) {
            const fallbackLength = getDefaultMysqlColumnTypeLength(fallbackBaseType)

            return {
                baseType: fallbackBaseType,
                length: fallbackLength,
                fullType: buildMysqlColumnType(fallbackBaseType, fallbackLength)
            }
        }

        const normalized = columnType.trim()
        const matched = normalized.match(/^([A-Za-z]+)\s*(?:\((.*)\))?$/)

        const parsedBaseType = matched?.[1]?.toUpperCase() || fallbackBaseType
        const parsedLength = matched?.[2]?.trim() || getDefaultMysqlColumnTypeLength(parsedBaseType)

        return {
            baseType: parsedBaseType,
            length: parsedLength,
            fullType: buildMysqlColumnType(parsedBaseType, parsedLength)
        }
    }

    /**
     * 根据已选择的物理字段基础类型和长度，生成完整的物理字段类型字符串
     */
    function syncPhysicalColumnType(row: MysqlColumnTypeRow): void {
        row.physicalColumnType = buildMysqlColumnType(
            row.physicalColumnBaseType,
            row.physicalColumnTypeLength || getDefaultMysqlColumnTypeLength(row.physicalColumnBaseType)
        )

        options.onChange?.()
    }

    /**
     * 根据已选择的物理字段基础类型，自动设置默认长度/精度，并同步完整字段类型
     */
    function handlePhysicalColumnBaseTypeChange(row: MysqlColumnTypeRow): void {
        row.physicalColumnTypeLength = getDefaultMysqlColumnTypeLength(
            row.physicalColumnBaseType
        )

        syncPhysicalColumnType(row)
    }

    return {
        getMysqlBaseTypeOptions,
        getMysqlColumnTypeLengthOptions,
        hasMysqlColumnTypeLength,
        getDefaultMysqlColumnTypeLength,
        buildMysqlColumnType,
        parseMysqlColumnType,
        syncPhysicalColumnType,
        handlePhysicalColumnBaseTypeChange
    }
}
