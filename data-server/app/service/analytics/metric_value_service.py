"""单 Dataset 的统一指标读取，严格保留截至时间与缺失值语义。"""
from datetime import date, datetime
import re
from typing import Any

from sqlalchemy import text

from app.core.db import get_engine_by_schema
from app.schemas.analytics.metric_value import DatasetMetricValueQuery, MetricValue


_IDENTIFIER = re.compile(r"^[A-Za-z_][A-Za-z0-9_]{0,63}$")


class MetricValueService:
    """只对一个受控物理表查询，禁止跨 Dataset JOIN。"""

    async def query_dataset_values(self, query: DatasetMetricValueQuery) -> list[MetricValue]:
        self._validate_query(query)
        engine = get_engine_by_schema(query.physical_schema_name)
        sql, params = self._build_latest_value_query(query)
        async with engine.connect() as connection:
            result = await connection.execute(text(sql), params)
            rows = result.mappings().all()

        rows_by_target = {str(row["target_code"]): row for row in rows}
        values: list[MetricValue] = []
        # 即使某个标的截至时间内没有记录，也显式返回 null，绝不以 0 替代。
        for target_code in query.target_codes:
            row = rows_by_target.get(target_code)
            for index, metric in enumerate(query.metrics):
                values.append(MetricValue(
                    targetType=query.target_type,
                    targetCode=target_code,
                    metricCode=metric.metric_code,
                    value=None if row is None else row[f"metric_{index}"],
                    dataTime=None if row is None else self._to_iso_time(row["data_time"]),
                ))
        return values

    def _build_latest_value_query(self, query: DatasetMetricValueQuery) -> tuple[str, dict[str, Any]]:
        target_params = {f"target_code_{index}": code for index, code in enumerate(query.target_codes)}
        placeholders = ", ".join(f":{name}" for name in target_params)
        table = self._qualified_identifier(query.physical_schema_name, query.physical_table_name)
        target_column = self._quoted_identifier(query.target_code_column)
        data_time_column = self._quoted_identifier(query.data_time_column)
        metric_columns = ", ".join(
            f"source.{self._quoted_identifier(metric.physical_column_name)} AS metric_{index}"
            for index, metric in enumerate(query.metrics)
        )
        as_of_predicate = ""
        params: dict[str, Any] = dict(target_params)
        if query.as_of_time is not None:
            as_of_predicate = f" AND {data_time_column} <= :as_of_time"
            params["as_of_time"] = query.as_of_time

        sql = f"""
            SELECT source.{target_column} AS target_code,
                   source.{data_time_column} AS data_time,
                   {metric_columns}
            FROM {table} source
            INNER JOIN (
                SELECT {target_column} AS target_code,
                       MAX({data_time_column}) AS data_time
                FROM {table}
                WHERE {target_column} IN ({placeholders}){as_of_predicate}
                GROUP BY {target_column}
            ) latest
                ON source.{target_column} = latest.target_code
               AND source.{data_time_column} = latest.data_time
            ORDER BY source.{target_column}
        """
        return sql, params

    def _validate_query(self, query: DatasetMetricValueQuery) -> None:
        # Schema 必须由 db.py 的白名单解析；字段与表名仍逐一校验，防止动态 SQL 标识符注入。
        self._assert_identifier(query.physical_schema_name)
        self._assert_identifier(query.physical_table_name)
        self._assert_identifier(query.target_code_column)
        self._assert_identifier(query.data_time_column)
        if not query.target_type or not query.dataset_code:
            raise ValueError("targetType and datasetCode are required")
        if any(not code.strip() for code in query.target_codes):
            raise ValueError("targetCodes must not contain blank values")
        metric_codes: set[str] = set()
        for metric in query.metrics:
            if not metric.metric_code or metric.metric_code in metric_codes:
                raise ValueError("metrics must contain unique metricCode values")
            metric_codes.add(metric.metric_code)
            self._assert_identifier(metric.physical_column_name)

    def _qualified_identifier(self, schema: str, table: str) -> str:
        return f"{self._quoted_identifier(schema)}.{self._quoted_identifier(table)}"

    def _quoted_identifier(self, identifier: str) -> str:
        self._assert_identifier(identifier)
        return f"`{identifier}`"

    def _assert_identifier(self, identifier: str) -> None:
        if not _IDENTIFIER.fullmatch(identifier or ""):
            raise ValueError(f"invalid SQL identifier: {identifier}")

    def _to_iso_time(self, value: Any) -> str | None:
        if value is None:
            return None
        if isinstance(value, (datetime, date)):
            return value.isoformat()
        return str(value)
