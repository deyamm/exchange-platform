package com.exchange.platform.collection.converter;

import com.exchange.platform.collection.api.response.dataType.DataTypeFieldView;
import com.exchange.platform.collection.api.response.dataType.DataTypeVersionView;
import com.exchange.platform.collection.api.response.dataType.DataTypeView;
import com.exchange.platform.collection.entity.dataType.DataType;
import com.exchange.platform.collection.entity.dataType.DataTypeField;
import com.exchange.platform.collection.entity.dataType.DataTypeVersion;


/**
 * 将数据类型实体类(DataType)转换为视图类(DataTypeView)以及相关的版本信息转换
 */
public final class DataTypeConverter {
    
    private DataTypeConverter() {}

    public static DataTypeView toView(DataType dataType) {
        if (dataType == null) {
            return null;
        }
        DataTypeView view = new DataTypeView();
        view.setDataTypeCode(dataType.getDataTypeCode());
        view.setDataTypeName(dataType.getDataTypeName());
        view.setDataTypeLabel(dataType.getDataTypeLabel());
        view.setParentCode(dataType.getParentCode());
        view.setNodeType(dataType.getNodeType());
        view.setSortNo(dataType.getSortNo());
        view.setNodeLevel(dataType.getNodeLevel());
        view.setIsLeaf(dataType.getIsLeaf());
        view.setStatus(dataType.getStatus());
        view.setVersion(dataType.getVersion());
        view.setDescription(dataType.getDescription());
        view.setCreatedAt(dataType.getCreatedAt());
        view.setUpdatedAt(dataType.getUpdatedAt());
        return view;
    }

    public static DataTypeFieldView toFieldView(DataTypeField dataTypeField) {
        if (dataTypeField == null) {
            return null;
        }
        DataTypeFieldView fieldView = new DataTypeFieldView();
        fieldView.setDataTypeCode(dataTypeField.getDataTypeCode());
        fieldView.setFieldCode(dataTypeField.getFieldCode());
        fieldView.setFieldName(dataTypeField.getFieldName());
        fieldView.setFieldType(dataTypeField.getFieldType());
        fieldView.setDefaultValue(dataTypeField.getDefaultValue());
        fieldView.setRequired(dataTypeField.getRequired());
        fieldView.setUniqueKey(dataTypeField.getUniqueKey());
        fieldView.setSortNo(dataTypeField.getSortNo());
        fieldView.setDescription(dataTypeField.getDescription());
        fieldView.setCreatedAt(dataTypeField.getCreatedAt());
        fieldView.setUpdatedAt(dataTypeField.getUpdatedAt());
        return fieldView;
    }

    public static DataTypeVersionView toVersionView(DataTypeVersion dataTypeVersion) {
        if (dataTypeVersion == null) {
            return null;
        }
        DataTypeVersionView versionView = new DataTypeVersionView();
        versionView.setDataTypeCode(dataTypeVersion.getDataTypeCode());
        versionView.setVersion(dataTypeVersion.getVersion());
        versionView.setVersionName(dataTypeVersion.getVersionName());
        versionView.setFieldSchemaContent(dataTypeVersion.getFieldSchemaContent());
        versionView.setChangeSummary(dataTypeVersion.getChangeSummary());
        versionView.setPublishStatus(dataTypeVersion.getPublishStatus());
        versionView.setPublishTime(dataTypeVersion.getPublishTime());
        versionView.setCreatedAt(dataTypeVersion.getCreatedAt());
        return versionView;
    }
}
