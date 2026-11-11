from datetime import datetime
from typing import Any

from pydantic import BaseModel, ConfigDict, Field


class MetricFieldQuery(BaseModel):
    model_config = ConfigDict(validate_by_name=True, serialize_by_alias=True)

    metric_code: str = Field(validation_alias="metricCode", serialization_alias="metricCode")
    physical_column_name: str = Field(validation_alias="physicalColumnName", serialization_alias="physicalColumnName")


class DatasetMetricValueQuery(BaseModel):
    """Spring 按 Dataset 聚合后下发的单表批量指标取数请求。"""
    model_config = ConfigDict(validate_by_name=True, serialize_by_alias=True)

    target_type: str = Field(validation_alias="targetType", serialization_alias="targetType")
    dataset_code: str = Field(validation_alias="datasetCode", serialization_alias="datasetCode")
    physical_schema_name: str = Field(validation_alias="physicalSchemaName", serialization_alias="physicalSchemaName")
    physical_table_name: str = Field(validation_alias="physicalTableName", serialization_alias="physicalTableName")
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
