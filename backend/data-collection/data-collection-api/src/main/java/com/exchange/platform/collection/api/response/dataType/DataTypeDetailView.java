package com.exchange.platform.collection.api.response.dataType;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * 数据类型详情视图对象，包含数据类型的详细信息、逻辑表字段信息、版本信息等，用于数据类型详情页面展示
 */
@Getter
@Setter
public class DataTypeDetailView {
    
    private DataTypeView dataTypeInfo; // 数据类型基本信息

    private List<DataTypeFieldView> fields; // 数据类型字段信息列表

    private List<DataTypeVersionView> versions; // 数据类型版本信息列表

}


