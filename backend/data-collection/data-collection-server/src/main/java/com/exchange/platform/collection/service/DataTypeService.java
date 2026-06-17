package com.exchange.platform.collection.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.exchange.platform.collection.api.enums.CommonStatus;
import com.exchange.platform.collection.api.request.dataType.DataTypeFieldSaveRequest;
import com.exchange.platform.collection.api.request.dataType.DataTypeSaveRequest;
import com.exchange.platform.collection.api.request.dataType.DataTypeVersionPublishRequest;
import com.exchange.platform.collection.api.response.dataType.DataTypeDetailView;
import com.exchange.platform.collection.api.response.dataType.DataTypeFieldView;
import com.exchange.platform.collection.api.response.dataType.DataTypeVersionView;
import com.exchange.platform.collection.api.response.dataType.DataTypeView;
import com.exchange.platform.collection.entity.dataType.DataType;

public interface DataTypeService extends IService<DataType> {

    /* 按树形或分页查询数据类型 */
    Object queryDataTypes(String parentCode, String dataTypeName, String dataTypeLabel, CommonStatus status, Boolean tree, Integer pageNo, Integer pageSize);

    /* 获取数据类型详情，包括基本信息和字段列表 */
    DataTypeDetailView getDataTypeDetail(String dataTypeCode);

    /* 保存或更新数据类型 */
    DataTypeView saveDataType(DataTypeSaveRequest request);

    /* 删除数据类型 */
    void deleteDataType(String dataTypeCode);

    /* 查询数据类型字段 */
    List<DataTypeFieldView> queryDataTypeFields(String dataTypeCode);

    /* 保存或更新数据类型字段 */
    List<DataTypeFieldView> saveDataTypeFields(String dataTypeCode, DataTypeFieldSaveRequest request);

    /* 发布数据类型版本 */
    DataTypeVersionView publishDataTypeVersion(String dataTypeCode, DataTypeVersionPublishRequest request);

    /* 获取数据类型历史版本详情 */
    List<DataTypeVersionView> queryDataTypeVersion(String dataTypeCode);
}
