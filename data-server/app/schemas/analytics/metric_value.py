from datetime import datetime
from typing import Any

from pydantic import BaseModel, ConfigDict, Field


class MetricFieldQuery(BaseModel):
    model_config = ConfigDict(validate_by_name=True, serialize_by_alias=True)

    metric_code: str = Field(validation_alias="metricCode", serialization_alias="metricCode")
    field_code: str = Field(validation_alias="fieldCode", serialization_alias="fieldCode")
    physical_column: str = Field(validation_alias="physicalColumn", serialization_alias="physicalColumn")


class DatasetMetricValueQuery(BaseModel):
    """平台统一 Dataset 读取请求；物理定位由数据服务从 Dataset 元数据解析。"""
    model_config = ConfigDict(validate_by_name=True, serialize_by_alias=True)

    target_type: str = Field(validation_alias="targetType", serialization_alias="targetType")
    dataset_code: str = Field(validation_alias="datasetCode", serialization_alias="datasetCode")
    storage_type: str = Field(validation_alias="storageType", serialization_alias="storageType")
    data_source_code: str = Field(validation_alias="dataSourceCode", serialization_alias="dataSourceCode")
    schema_name: str = Field(validation_alias="schemaName", serialization_alias="schemaName")
    table_name: str = Field(validation_alias="tableName", serialization_alias="tableName")
    target_code_column: str = Field(validation_alias="targetCodeColumn", serialization_alias="targetCodeColumn")
    data_time_column: str = Field(validation_alias="dataTimeColumn", serialization_alias="dataTimeColumn")
    target_codes: list[str] = Field(min_length=1, validation_alias="targetCodes", serialization_alias="targetCodes")
    metrics: list[MetricFieldQuery] = Field(min_length=1)
    as_of_time: datetime | None = Field(default=None, validation_alias="asOfTime", serialization_alias="asOfTime")


class MetricValue(BaseModel):
    model_config = ConfigDict(validate_by_name=True, serialize_by_alias=True)

    target_type: str = Field(validation_alias="targetType", serialization_alias="targetType")
    target_code: str = Field(validation_alias="targetCode", serialization_alias="targetCode")
    metric_code: str = Field(validation_alias="metricCode", serialization_alias="metricCode")
    value: Any | None = None
    data_time: str | None = Field(default=None, validation_alias="dataTime", serialization_alias="dataTime")
