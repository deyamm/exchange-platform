import logging
import re
from typing import Any

from sqlalchemy import text

from app.core.config import settings
from app.core.db import get_engine_by_schema
from app.schemas.collection_task.task_template import StorageColumnMapping, StorageMapping
from app.utils.db_utils import quote_identifier


logger = logging.getLogger(__name__)

async def write_rows(mapping: StorageMapping, columns: list[StorageColumnMapping], rows: list[dict[str, Any]]) -> int:
    if not rows:
        return 0
    
    # 验证schema是否在允许范围内，防止写入不受控的schema
    if mapping.physical_schema_name not in settings.allowed_schema_set:
        raise ValueError(f"不允许写入的 schema: {mapping.physical_schema_name}")

    # 验证表名和列名是否合法，防止SQL注入
    schema = quote_identifier(mapping.physical_schema_name)
    table = quote_identifier(mapping.physical_table_name)

    # 拼接SQL语句，使用参数化查询防止SQL注入
    column_names = list(rows[0].keys())
    column_sql = ", ".join(quote_identifier(c) for c in column_names)
    values_sql = ", ".join(f":{c}" for c in column_names)

    unique_fields = [col.physical_column_name for col in columns if col.unique_key]
    

    # 如果没有唯一键，或者写入模式是 APPEND，则直接插入，Mysql中，不需要schema_name.table_name 这种写法，直接 table_name 就行。
    if mapping.write_strategy == "APPEND" or not unique_fields:
        sql = f"""
            INSERT INTO {table} ({column_sql})
            VALUES ({values_sql})
        """

    elif mapping.write_strategy == "UPSERT":
        update_columns = [c for c in column_names if c not in unique_fields]

        if update_columns:
            update_sql = ", ".join(
                f"{quote_identifier(c)} = VALUES({quote_identifier(c)})"
                for c in update_columns
            )
        else:
            # 如果所有字段都是唯一键，MySQL 语法仍要求 UPDATE 子句非空
            first_unique_field = unique_fields[0]
            update_sql = (
                f"{quote_identifier(first_unique_field)} = "
                f"{quote_identifier(first_unique_field)}"
            )

        sql = f"""
            INSERT INTO {table} ({column_sql})
            VALUES ({values_sql})
            ON DUPLICATE KEY UPDATE {update_sql}
        """
    else:
        raise ValueError(f"暂不支持写入模式: {mapping.write_strategy}")

    engine = get_engine_by_schema(mapping.physical_schema_name)

    async with engine.begin() as conn:
        await conn.execute(text(sql), rows)

    return len(rows)
