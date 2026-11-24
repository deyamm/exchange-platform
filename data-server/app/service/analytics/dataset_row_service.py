"""统一 Dataset 标准行读取；主数据服务不直接访问物理表。"""
import re
from typing import Any
from sqlalchemy import text
from app.core.db import get_engine_by_schema
from app.schemas.analytics.dataset_rows import DatasetRow, DatasetRowsQuery

_IDENTIFIER = re.compile(r"^[A-Za-z_][A-Za-z0-9_]{0,63}$")


class DatasetRowService:

    async def query_rows(self, query: DatasetRowsQuery) -> list[DatasetRow]:
        self._validate(query)
        table = f"{self._quote(query.schema_name)}.{self._quote(query.table_name)}"
        selected = []
        for field_code in query.field_codes:
            physical = query.physical_columns.get(field_code)
            if not physical:
                raise ValueError(f"missing physical mapping for fieldCode: {field_code}")
            selected.append(f"source.{self._quote(physical)} AS `{field_code}`")
        predicates: list[str] = []
        params: dict[str, Any] = {}
        if query.asset_codes:
            if not query.target_code_column:
                raise ValueError("targetCodeColumn is required when assetCodes are specified")
            placeholders = []
            for index, code in enumerate(query.asset_codes):
                key = f"asset_code_{index}"
                placeholders.append(f":{key}")
                params[key] = code
            predicates.append(f"source.{self._quote(query.target_code_column)} IN ({', '.join(placeholders)})")
        if query.from_time is not None:
            if not query.data_time_column:
                raise ValueError("dataTimeColumn is required when from is specified")
            predicates.append(f"source.{self._quote(query.data_time_column)} >= :from_time")
            params["from_time"] = query.from_time
        if query.to_time is not None:
            if not query.data_time_column:
                raise ValueError("dataTimeColumn is required when to is specified")
            predicates.append(f"source.{self._quote(query.data_time_column)} <= :to_time")
            params["to_time"] = query.to_time
        # filters 只接受 fieldCode -> scalar，字段映射仍由 Java 侧解析为物理列。
        for index, (field_code, value) in enumerate(query.filters.items()):
            physical = query.physical_columns.get(field_code)
            if not physical:
                raise ValueError(f"missing physical mapping for filter fieldCode: {field_code}")
            key = f"filter_{index}"
            predicates.append(f"source.{self._quote(physical)} = :{key}")
            params[key] = value
        where = f" WHERE {' AND '.join(predicates)}" if predicates else ""
        # 对于排序字段，优先使用 orderBy，其次使用 dataTimeColumn，最后使用第一个 fieldCode。
        if query.order_by:
            order = self._quote(query.order_by) 
        elif query.data_time_column:
            order = self._quote(query.data_time_column)
        else:
            order = self._quote(query.field_codes[0])
        limit = min(max(query.page_size, 1), 2000)
        offset = max(query.page_no - 1, 0) * limit
        sql = f"SELECT {', '.join(selected)} FROM {table} source{where} ORDER BY source.{order} DESC LIMIT :limit OFFSET :offset"
        params.update(limit=limit, offset=offset)
        engine = get_engine_by_schema(query.schema_name)
        async with engine.connect() as connection:
            result = await connection.execute(text(sql), params)
            return [DatasetRow(fields=dict(row)) for row in result.mappings().all()]

    def _validate(self, query: DatasetRowsQuery) -> None:
        if not query.dataset_code or not query.field_codes:
            raise ValueError("datasetCode and fieldCodes are required")
        if query.page_no < 1 or query.page_size < 1:
            raise ValueError("pageNo/pageSize must be positive")

    def _quote(self, identifier: str) -> str:
        if not _IDENTIFIER.fullmatch(identifier or ""):
            raise ValueError(f"invalid SQL identifier: {identifier}")
        return f"`{identifier}`"
