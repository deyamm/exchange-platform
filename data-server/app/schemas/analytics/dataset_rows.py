from datetime import datetime
from typing import Any
from pydantic import BaseModel, ConfigDict, Field


class DatasetRowsQuery(BaseModel):
    """内部逻辑行协议；physical column 只在服务间受控传递，不由前端提交。"""
    model_config = ConfigDict(validate_by_name=True, serialize_by_alias=True)
    target_type: str = Field(validation_alias="targetType", serialization_alias="targetType")
    dataset_code: str = Field(validation_alias="datasetCode", serialization_alias="datasetCode")
    storage_type: str = Field(validation_alias="storageType", serialization_alias="storageType")
    data_source_code: str = Field(validation_alias="dataSourceCode", serialization_alias="dataSourceCode")
    schema_name: str = Field(validation_alias="schemaName", serialization_alias="schemaName")
    table_name: str = Field(validation_alias="tableName", serialization_alias="tableName")
    target_code_column: str | None = Field(validation_alias="targetCodeColumn", serialization_alias="targetCodeColumn")
    data_time_column: str | None = Field(validation_alias="dataTimeColumn", serialization_alias="dataTimeColumn")
    field_codes: list[str] = Field(min_length=1, validation_alias="fieldCodes", serialization_alias="fieldCodes")
    physical_columns: dict[str, str] = Field(default_factory=dict, validation_alias="physicalColumns", serialization_alias="physicalColumns")
    filters: dict[str, Any] = Field(default_factory=dict)
    asset_codes: list[str] = Field(default_factory=list, validation_alias="assetCodes", serialization_alias="assetCodes")
    from_time: datetime | None = Field(default=None, validation_alias="from", serialization_alias="from")
    to_time: datetime | None = Field(default=None, validation_alias="to", serialization_alias="to")
    order_by: str | None = Field(default=None, validation_alias="orderBy", serialization_alias="orderBy")
    page_no: int = Field(default=1, validation_alias="pageNo", serialization_alias="pageNo")
    page_size: int = Field(default=2000, validation_alias="pageSize", serialization_alias="pageSize")


class DatasetRow(BaseModel):
    model_config = ConfigDict(validate_by_name=True, serialize_by_alias=True)
    fields: dict[str, Any]
