package com.exchange.platform.collection.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exchange.platform.collection.api.enums.CommonStatus;
import com.exchange.platform.collection.api.enums.DataTypeNodeType;
import com.exchange.platform.collection.api.enums.PublishStatus;
import com.exchange.platform.collection.api.request.dataType.DataTypeFieldSaveRequest;
import com.exchange.platform.collection.api.request.dataType.DataTypeSaveRequest;
import com.exchange.platform.collection.api.request.dataType.DataTypeVersionPublishRequest;
import com.exchange.platform.collection.api.response.dataType.DataTypeDetailView;
import com.exchange.platform.collection.api.response.dataType.DataTypeFieldView;
import com.exchange.platform.collection.api.response.dataType.DataTypeVersionView;
import com.exchange.platform.collection.api.response.dataType.DataTypeView;
import com.exchange.platform.collection.converter.DataTypeConverter;
import com.exchange.platform.collection.entity.dataType.DataType;
import com.exchange.platform.collection.entity.dataType.DataTypeField;
import com.exchange.platform.collection.entity.dataType.DataTypeVersion;
import com.exchange.platform.collection.mapper.dataType.DataTypeFieldMapper;
import com.exchange.platform.collection.mapper.dataType.DataTypeMapper;
import com.exchange.platform.collection.mapper.dataType.DataTypeVersioinMapper;
import com.exchange.platform.collection.service.DataTypeService;
import com.exchange.platform.common.core.exception.BusinessException;
import com.exchange.platform.common.core.page.PageResult;


@Service
public class DataTypeServiceImpl extends ServiceImpl<DataTypeMapper, DataType> implements DataTypeService {
    
    private static final Logger log = LoggerFactory.getLogger(DataTypeServiceImpl.class);

    private final DataTypeFieldMapper dataTypeFieldMapper;
    private final DataTypeVersioinMapper dataTypeVersionMapper;

    public DataTypeServiceImpl(DataTypeFieldMapper dataTypeFieldMapper, DataTypeVersioinMapper dataTypeVersionMapper) {
        this.dataTypeFieldMapper = dataTypeFieldMapper;
        this.dataTypeVersionMapper = dataTypeVersionMapper;
    }

    /**
     * 查询数据类型列表，支持树形结构和分页两种方式，以tree参数区分
     * 树形结构查询时，返回嵌套的DataTypeView列表；
     * 分页查询时，基于parentCode，以name和label为筛选条件，返回PageInfo<DataTypeView>对象
     */
    @Override
    @Transactional(readOnly = true)
    public Object queryDataTypes(String parentCode, String dataTypeName, String dataTypeLabel, CommonStatus status, Boolean tree, Integer pageNo, Integer pageSize) {
        
        log.info("[Service] queryDataTypes called with parentCode={}, dataTypeName={}, dataTypeLabel={}, status={}, tree={}, pageNo={}, pageSize={}", 
            parentCode, dataTypeName, dataTypeLabel, status, tree, pageNo, pageSize);

        // 默认tree模式
        boolean treeMode = tree == null || tree;
        LambdaQueryWrapper<DataType> queryWrapper = buildQueryWrapper(parentCode, dataTypeName, dataTypeLabel, status);
        if (treeMode) {
            List<DataType> records = list(queryWrapper);
            // 构建树形结构
            return buildTree(records);
        }

        // 分页查询逻辑，基于parentCode，以name和label为筛选条件，支持状态过滤和排序
        int safePageNo = (pageNo == null || pageNo < 1) ? 1 : pageNo;
        int safePageSize = (pageSize == null || pageSize < 1) ? 20 : Math.min(200, pageSize);

        // 如果传入的parentCode为空，则代表是刚进入页面，此时返回的内容可以待改善 TODO
        if (!StringUtils.hasText(parentCode)) {
            log.info("[Service] queryDataTopics page mode skipped: parentCode is empty, return empty page");
            return new PageResult<>(safePageNo, safePageSize, 0, new ArrayList<>());
        }

        // 使用MyBatis-Plus的分页插件进行分页查询
        Page<DataType> page = page(new Page<>(safePageNo, safePageSize), queryWrapper);
        // 将分页结果转换为DataTypeView列表
        List<DataTypeView> records = page.getRecords().stream()
                                        .map(this::toViewWithCount)
                                        .collect(Collectors.toList());
        // 返回分页结果，包含当前页码、页大小、总记录数和数据列表
        return new PageResult<>(safePageNo, safePageSize, page.getTotal(), records);
    }
    
    /**
     * 获取数据类型详情
     * @param dataTypeCode 数据类型编码
     * @return 数据类型详情视图，包括基本信息(包含版本字段统计信息)和字段列表
     */
    @Override
    @Transactional(readOnly = true)
    public DataTypeDetailView getDataTypeDetail(String dataTypeCode) {
        log.info("[Service] getDataTypeDetail called with dataTypeCode={}", dataTypeCode);
        DataTypeDetailView detailView = new DataTypeDetailView();

        // 查询数据类型基本信息
        DataType dataType = requireDataType(dataTypeCode);
        detailView.setDataTypeInfo(toViewWithCount(dataType));
        
        // 查询数据类型字段信息列表
        List<DataTypeField> fields = dataTypeFieldMapper.selectList(new LambdaQueryWrapper<DataTypeField>().eq(DataTypeField::getDataTypeCode, dataTypeCode));
        detailView.setFields(
            fields.stream().map(DataTypeConverter::toFieldView)
            .collect(Collectors.toList())
        );
        
        // 查询数据类型版本信息列表
        List<DataTypeVersion> versions = dataTypeVersionMapper.selectList(new LambdaQueryWrapper<DataTypeVersion>().eq(DataTypeVersion::getDataTypeCode, dataTypeCode).orderByDesc(DataTypeVersion::getVersion));
        detailView.setVersions(
            versions.stream().map(DataTypeConverter::toVersionView)
            .collect(Collectors.toList())
        );
        return detailView;
    }

    /** 
     * 保存数据类型信息，支持新增和更新两种操作
     * 新增时，dataTypeCode必须不存在；更新时，dataTypeCode必须存在
     * 返回的视图对象包含基本信息和统计信息（字段数量、版本数量）
    */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DataTypeView saveDataType(DataTypeSaveRequest request) {
        log.info("[Service] saveDataType called with request={}", request);
        String dataTypeCode = normalizeToNull(request.dataTypeCode());
        String parentCode = normalizeToNull(request.parentCode());

        if (dataTypeCode == null || parentCode == null) {
            throw new BusinessException(4001, "数据类型编码和父级编码不能为空");
        }

        // 根据saveOrUpdate标识判断操作类型
        DataType dataType = getByDataTypeCode(dataTypeCode);
        boolean isCreate;
        if ("save".equalsIgnoreCase(request.saveOrUpdate())) {
            // 新增：校验编码不能已存在
            if (dataType != null) {
                throw new BusinessException(4001, "数据类型编码已存在：" + dataTypeCode);
            }
            dataType = new DataType();
            dataType.setDataTypeCode(dataTypeCode);
            isCreate = true;
        } else if ("update".equalsIgnoreCase(request.saveOrUpdate())) {
            // 更新：校验编码必须存在
            if (dataType == null) {
                throw new BusinessException(4001, "数据类型编码不存在：" + dataTypeCode);
            }
            isCreate = false;
        } else {
            throw new BusinessException(4001, "saveOrUpdate参数值无效，必须为save或update");
        }
        dataType.setDataTypeName(request.dataTypeName())
                .setDataTypeLabel(request.dataTypeLabel())
                .setParentCode(parentCode)
                .setNodeType(request.nodeType())
                .setSortNo(request.sortNo())
                .setNodeLevel(request.nodeLevel())
                .setIsLeaf(request.isLeaf() == null ? Boolean.TRUE : request.isLeaf())
                .setStatus(request.status())
                .setDescription(request.description());
        
        // 校验要保存的数据类型中，nodeType为具体节点时，isLeaf必须为true
        if (dataType.getNodeType() == DataTypeNodeType.CONCRETE && !dataType.getIsLeaf()) {
            throw new BusinessException(4001, "当节点类型为具体节点时，isLeaf必须为true");
        }
        saveOrUpdate(dataType);
        
        // 将父级节点的isLeaf属性更新为false
        if (parentCode != null) {
            update(
                new DataType().setIsLeaf(false), 
                new LambdaQueryWrapper<DataType>().eq(DataType::getDataTypeCode, parentCode));
        }

        log.info("[Service] saveDataType completed for dataTypeCode={}, isCreate={}", dataTypeCode, isCreate);
        return toViewWithCount(dataType);
    }

    /**
     * 删除数据类型，删除前需要判断是否存在子节点，如果存在子节点则不允许删除；
     * 删除数据类型前，首先需要删除对应的字段信息和版本信息
     * @param dataTypeCode
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDataType(String dataTypeCode) {
        log.info("[Service] deleteDataType called with dataTypeCode={}", dataTypeCode);

        DataType dataType = requireDataType(dataTypeCode);
        // 判断是否有子节点，如果有则不允许删除
        long childCount = count(new LambdaQueryWrapper<DataType>().eq(DataType::getParentCode, dataTypeCode));
        if (childCount > 0) {
            throw new BusinessException(4001, "数据类型存在子节点，不允许删除");
        }
        // TODO: 目前不对版本数量、字段数量进行判断，后续可以增加判断逻辑，如果存在版本或字段则不允许删除
        // 删除数据类型对应的字段信息
        dataTypeFieldMapper.delete(
            new LambdaQueryWrapper<DataTypeField>().eq(DataTypeField::getDataTypeCode, dataTypeCode)
        );
        // 删除数据类型对应的版本信息
        dataTypeVersionMapper.delete(
            new LambdaQueryWrapper<DataTypeVersion>().eq(DataTypeVersion::getDataTypeCode, dataTypeCode)
        );
        // 删除数据类型
        removeById(dataType.getId());
    }

    /**
     * 查询数据类型字段列表，根据数据类型编码查询对应的字段信息列表，并转换为视图对象返回
     * @param dataTypeCode
     * @return
     */
    @Override
    @Transactional(readOnly = true)
    public List<DataTypeFieldView> queryDataTypeFields(String dataTypeCode) {
        log.info("[Service] queryFields called with dataTypeCode={}", dataTypeCode);

        // 需要确保数据类型存在，如果不存在则抛出异常
        requireDataType(dataTypeCode);
        // 根据数据类型编码查询对应的字段信息列表，按排序和编码字段排序，并转换为视图对象返回
        List<DataTypeField> fields = dataTypeFieldMapper.selectList(
            new LambdaQueryWrapper<DataTypeField>()
                .eq(DataTypeField::getDataTypeCode, dataTypeCode)
                .orderByAsc(DataTypeField::getSortNo)
                .orderByAsc(DataTypeField::getFieldCode)
        );

        return fields.stream().map(DataTypeConverter::toFieldView).collect(Collectors.toList());
    }

    /**
     * 批量保存字段信息，
     * @param dataTypeCode
     * @param fieldViews
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<DataTypeFieldView> saveDataTypeFields(String dataTypeCode, DataTypeFieldSaveRequest request) {
        log.info("[Service] saveFields called with dataTypeCode={}, fieldViews={}", dataTypeCode, request);

        // 需要确保数据类型存在，如果不存在则抛出异常
        requireDataType(dataTypeCode);

        // 查询当前数据类型所有的字段信息列表，并转化为Map, key为fieldCode, value为DataTypeField对象
        Map<String, DataTypeField> existingFieldMap = dataTypeFieldMapper.selectList(
            new LambdaQueryWrapper<DataTypeField>().eq(DataTypeField::getDataTypeCode, dataTypeCode)
        ).stream()
            .collect(Collectors.toMap(DataTypeField::getFieldCode, field -> field));
        // 定义一个需要删除的字段id集合，初始值为当前字段的id集合
        Set<Long> toDeleteIds = existingFieldMap.values().stream()
            .map(DataTypeField::getId).collect(Collectors.toSet());

        // 偏历传入的字段信息列表
        List<DataTypeField> fields = new ArrayList<>();
        for (DataTypeFieldSaveRequest.FieldItem item : request.fields()) {
            // 根据传入的fieldCode从existingFieldMap中查找对应的DataTypeField对象，如果不存在则创建新的对象
            DataTypeField field = existingFieldMap.get(item.fieldCode());

            if (field == null) {
                field = new DataTypeField();
            } else {
                // 从toDeleteIds中移除已经处理的字段id
                toDeleteIds.remove(field.getId());
            }
            field.setDataTypeCode(dataTypeCode)
                .setFieldCode(item.fieldCode())
                .setFieldName(item.fieldName())
                .setFieldType(item.fieldType())
                .setDefaultValue(item.defaultValue())
                .setRequired(item.required())
                .setUniqueKey(item.uniqueKey())
                .setSortNo(item.sortNo())
                .setDescription(item.description());
            if (field.getId() == null) {
                dataTypeFieldMapper.insert(field);
            } else {
                dataTypeFieldMapper.updateById(field);
            }
            fields.add(field);
        }
        // 删除toDeleteIds中剩余的字段id对应的字段信息
        if (!toDeleteIds.isEmpty()) {
            dataTypeFieldMapper.deleteBatchIds(toDeleteIds);
        }
        // 返回视图对象列表
        return fields.stream().map(DataTypeConverter::toFieldView).collect(Collectors.toList());
    }   

    /**
     * 发布数据类型版本，并冻结当前字段快照
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DataTypeVersionView publishDataTypeVersion(String dataTypeCode, DataTypeVersionPublishRequest request) {
        log.info("[Service] publishDataTypeVersion called with dataTypeCode={}, request={}", dataTypeCode, request);
        // 需要确保数据类型存在，如果不存在则抛出异常
        DataType dataType = requireDataType(dataTypeCode);
        // 读取当前数据类型的字段列表
        List<DataTypeField> fields = dataTypeFieldMapper.selectList(
            new LambdaQueryWrapper<DataTypeField>()
                .eq(DataTypeField::getDataTypeCode, dataTypeCode)
                .orderByAsc(DataTypeField::getSortNo)
                .orderByAsc(DataTypeField::getFieldCode)
        );
        if (fields.isEmpty()) {
            throw new BusinessException(4001, "数据类型没有字段信息，无法发布版本");
        } 
        // 查看版本是否已经存在，如果存在则不允许发布
        long versionCount = dataTypeVersionMapper.selectCount(
            new LambdaQueryWrapper<DataTypeVersion>()
                .eq(DataTypeVersion::getDataTypeCode, dataTypeCode)
                .eq(DataTypeVersion::getVersion, request.version())
        );
        if (versionCount > 0) {
            throw new BusinessException(4001, "数据类型版本已经存在，无法发布");
        }
        // 构建版本对象，并保存版本信息
        DataTypeVersion version = new DataTypeVersion()
            .setDataTypeCode(dataTypeCode)
            .setVersion(request.version())
            .setVersionName(request.versionName())
            .setChangeSummary(request.changeSummary())
            .setPublishStatus(PublishStatus.PUBLISHED)
            .setPublishTime(LocalDateTime.now())
            .setFieldSchemaContent(buildFieldSchemaJson(fields));
        dataTypeVersionMapper.insert(version);
        // 更新数据类型的当前版本号
        dataType.setVersion(request.version());
        updateById(dataType);

        log.info("[Service] publishDataTypeVersion completed for dataTypeCode={}, version={}", dataTypeCode, request.version());

        return DataTypeConverter.toVersionView(version);
    }


    /**
     * 查询数据类型版本列表，根据数据类型编码查询对应的版本信息列表，并转换为视图对象返回
     */
    @Override
    @Transactional(readOnly = true)
    public List<DataTypeVersionView> queryDataTypeVersion(String dataTypeCode) {
        log.info("[Service] queryDataTypeVersion called with dataTypeCode={}", dataTypeCode);
        // 需要确保数据类型存在，如果不存在则抛出异常
        requireDataType(dataTypeCode);
        // 根据数据类型编码查询对应的版本信息列表，按版本号降序排序，并转换为视图对象返回
        List<DataTypeVersion> versions = dataTypeVersionMapper.selectList(
            new LambdaQueryWrapper<DataTypeVersion>()
                .eq(DataTypeVersion::getDataTypeCode, dataTypeCode)
                .orderByDesc(DataTypeVersion::getVersion)
        );
        return versions.stream().map(DataTypeConverter::toVersionView).collect(Collectors.toList());
    }


    /**
     * 构建树形结构的DataTypeView列表，基于parentCode进行层级关系构建
     * @param records
     * @return
     */
    private List<DataTypeView> buildTree(List<DataType> records) {

        Map<String, DataTypeView> nodeMap = new LinkedHashMap<>();

        for (DataType dataType : records) {
            nodeMap.put(dataType.getDataTypeCode(), DataTypeConverter.toView(dataType));
        }
        
        // 根节点列表
        List<DataTypeView> roots = new ArrayList<>();
        for (DataTypeView node : nodeMap.values()) {
            String parentCode = node.getParentCode();
            if (parentCode != null && nodeMap.containsKey(parentCode)) {
                // 将当前节点添加到父节点的children列表中
                nodeMap.get(parentCode).getChildren().add(node);
            } else {
                // 没有父节点，直接添加到根节点列表中
                roots.add(node); 
            }
         }
         
         return roots;
    }

    /**
     * 构建查询条件，基于parentCode，以name和label为筛选条件，支持状态过滤和排序
     */
    private LambdaQueryWrapper<DataType> buildQueryWrapper(String parentCode, String dataTypeName, String dataTypeLabel, CommonStatus status) {
        LambdaQueryWrapper<DataType> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(StringUtils.hasText(parentCode), DataType::getParentCode, normalizeToNull(parentCode))
                    .like(StringUtils.hasText(dataTypeName), DataType::getDataTypeName, normalizeToNull(dataTypeName))
                    .like(StringUtils.hasText(dataTypeLabel), DataType::getDataTypeLabel, normalizeToNull(dataTypeLabel))
                    .eq(status != null, DataType::getStatus, status)
                    .orderByAsc(DataType::getSortNo);
        return queryWrapper;
    }

    /**
     * 根据数据类型编码查询数据类型，如果不存在则抛出异常
     */
    private DataType requireDataType(String dataTypeCode) {
        DataType dataType = getByDataTypeCode(dataTypeCode);
        if (dataType == null) {
            throw new BusinessException(4001, "数据类型不存在，code=" + dataTypeCode);
        }
        return dataType;
    }

    /**
     * 根据数据类型编码查询数据类型，如果不存在则返回null
     * @param dataTypeCode
     * @return
     */
    private DataType getByDataTypeCode(String dataTypeCode) {
        String code = normalizeToNull(dataTypeCode);
        if (code == null) {
            return null;
        }
        return getOne(
            new LambdaQueryWrapper<DataType>()
                .eq(DataType::getDataTypeCode, dataTypeCode)
                .last("LIMIT 1")
        );
    }

    /**
     * 将数据类型转换为视图对象，并包含字段、版本的统计信息
     * @param dataType 数据类型对象
     * @return 视图对象，包含基本信息和统计信息
     */
    private DataTypeView toViewWithCount(DataType dataType) {
        DataTypeView view = DataTypeConverter.toView(dataType);
        // 查询字段数量
        view.setFieldCount(
            dataTypeFieldMapper.selectCount(
                new LambdaQueryWrapper<DataTypeField>().eq(DataTypeField::getDataTypeCode, dataType.getDataTypeCode())
            )
        );
        // 查询版本数量
        view.setVersionCount(
            dataTypeVersionMapper.selectCount(
                new LambdaQueryWrapper<DataTypeVersion>().eq(DataTypeVersion::getDataTypeCode, dataType.getDataTypeCode())
            )
        );
        return view;
    }

    /**
     * 构建字段列表的JSON字符串，作为版本快照的一部分保存到数据库中
     * @param fields
     * @return
     */
    private String buildFieldSchemaJson(List<DataTypeField> fields) {
        // 将字段列表转换为JSON字符串，使用第FastJSON进行转换
        return JSON.toJSONString(fields);
    }

    /**
     * 如果字符串没有实际内容（null、空字符串或仅包含空白字符），则返回null；否则返回规范化后的字符串
     * @param value 输入字符串
     * @return 规范化字符串或null
     */
    private String normalizeToNull(String value) {
        return !StringUtils.hasText(value) ? null : value.trim();
    }

}
