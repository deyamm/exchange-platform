package com.exchange.platform.collection.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exchange.platform.collection.api.enums.CommonStatus;
import com.exchange.platform.collection.entity.dataTopic.DataTopic;
import com.exchange.platform.collection.mapper.dataTopic.DataTopicMapper;
import com.exchange.platform.collection.service.DataTopicService;
import com.exchange.platform.collection.api.request.dataTopic.DataTopicSaveRequest;
import com.exchange.platform.collection.api.response.dataTopic.DataTopicView;
import com.exchange.platform.collection.converter.DataTopicConverter;
import com.exchange.platform.common.core.exception.BusinessException;
import com.exchange.platform.common.core.page.PageResult;


@Service
public class DataTopicServiceImpl extends ServiceImpl<DataTopicMapper, DataTopic> implements DataTopicService {

    private static final Logger log = LoggerFactory.getLogger(DataTopicServiceImpl.class);

    /**
     * 查询数据主题（树形或分页）
     */
    @Override
    @Transactional(readOnly = true)
    public Object queryDataTopics(String parentCode, String dataTopicName, String dataTopicLabel, CommonStatus status, Boolean tree, Integer pageNo, Integer pageSize) {
        log.info("[Service] queryDataTopics start, parentCode={}, dataTopicName={}, dataTopicLabel={}, status={}, tree={}, pageNo={}, pageSize={}",
                parentCode, dataTopicName, dataTopicLabel, status, tree, pageNo, pageSize);

        // 如果tree参数为true或未传入，则以树形结构返回数据主题列表；否则返回分页列表
        boolean treeMode = tree == null || tree;
        if (treeMode) {
            LambdaQueryWrapper<DataTopic> queryWrapper = buildQueryWrapper(parentCode, dataTopicName, dataTopicLabel, status, treeMode);
            List<DataTopic> dataTopics = list(queryWrapper);
            List<DataTopicView> treeResult = buildTree(dataTopics);

            log.info("[Service] queryDataTopics tree mode success, totalNodes={}, rootNodes={}", dataTopics.size(), treeResult.size());
            return treeResult;
        }

        int safePageNo = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int safePageSize = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 200);

        // 如果传入的parentCode为空，则代表是刚进入页面，此时返回的内容可以待改善 TODO
        if (!StringUtils.hasText(parentCode)) {
            log.info("[Service] queryDataTopics page mode skipped: parentCode is empty, return empty page");
            return new PageResult<>(safePageNo, safePageSize, 0, new ArrayList<>());
        }

        LambdaQueryWrapper<DataTopic> queryWrapper = buildQueryWrapper(parentCode, dataTopicName, dataTopicLabel, status, treeMode);
        Page<DataTopic> page = page(new Page<>(safePageNo, safePageSize), queryWrapper);
        List<DataTopicView> records = page.getRecords().stream().map(DataTopicConverter::toView).collect(Collectors.toList());

        log.info("[Service] queryDataTopics page mode success, pageNo={}, pageSize={}, total={}, records={}", safePageNo, safePageSize, page.getTotal(), records.size());
        return new PageResult<>(safePageNo, safePageSize, page.getTotal(), records);
    }

    /**
     * 保存数据主题
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DataTopicView saveDataTopic(DataTopicSaveRequest request) {
        log.info("[Service] saveDataTopic start, dataTopicCode={}, parentCode={}, nodeLevel={}, isLeaf={}, status={}",
                request.dataTopicCode(), request.parentCode(), request.nodeLevel(), request.isLeaf(), request.status());

        String topicCode = normalizeToNull(request.dataTopicCode());
        String parentCode = normalizeToNull(request.parentCode());
        
        // 校验topicCode和parentCode
        if (topicCode == null || parentCode == null) {
            log.warn("[Service] saveDataTopic validation failed: dataTopicCode or parentCode is blank");
            throw new BusinessException(4001, "数据主题编码和父级编码不能为空");
        }

        // 校验父级数据主题是否存在（除非parentCode为null，表示顶层节点）
        if (parentCode != null && !existsByDataTopicCode(parentCode)) {
            log.warn("[Service] saveDataTopic validation failed: parent not found, topicCode={}, parentCode={}", topicCode, parentCode);
            throw new BusinessException(4003, "父级数据主题不存在");
        }

        DataTopic entity = getByDataTopicCode(topicCode);

        // 根据saveOrUpdate标识判断操作类型
        boolean creating;
        if ("save".equalsIgnoreCase(request.saveOrUpdate())) {
            // 新增：校验编码不能已存在
            if (entity != null) {
                throw new BusinessException(4001, "数据主题编码已存在：" + topicCode);
            }
            entity = new DataTopic();
            entity.setDataTopicCode(topicCode);
            creating = true;
        } else if ("update".equalsIgnoreCase(request.saveOrUpdate())) {
            // 更新：校验编码必须存在
            if (entity == null) {
                throw new BusinessException(4001, "数据主题编码不存在：" + topicCode);
            }
            creating = false;
        } else {
            throw new BusinessException(4001, "saveOrUpdate参数值无效，必须为save或update");
        }
        entity.setDataTopicName(request.dataTopicName())
                .setDataTopicLabel(StringUtils.hasText(request.dataTopicLabel()) ? request.dataTopicLabel().trim() : null)
                .setParentCode(parentCode)
                .setNodeLevel(request.nodeLevel())
                .setIsLeaf(request.isLeaf())
                .setStatus(request.status())
                .setSortNo(request.sortNo())
                .setDescription(StringUtils.hasText(request.description()) ? request.description().trim() : null);
        saveOrUpdate(entity);

        // 将父节点更新为非叶子节点
        update(
                new DataTopic().setIsLeaf(false),
                new LambdaQueryWrapper<DataTopic>().eq(DataTopic::getDataTopicCode, parentCode)
        );

        log.info("[Service] saveDataTopic success, operation={}, dataTopicCode={}", creating ? "CREATE" : "UPDATE", entity.getDataTopicCode());
        return DataTopicConverter.toView(entity);
    }

    /**
     * 删除数据主题
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDataTopic(String dataTopicCode) {
        String topicCode = normalizeToNull(dataTopicCode);
        if (topicCode == null) {
            log.warn("[Service] deleteDataTopic validation failed: dataTopicCode is blank");
            throw new BusinessException(4001, "数据主题编码不能为空");
        }

        DataTopic entity = getByDataTopicCode(topicCode);
        if (entity == null) {
            log.warn("[Service] deleteDataTopic validation failed: topic not found, dataTopicCode={}", topicCode);
            throw new BusinessException(4004, "数据主题不存在");
        }

        long childCount = count(new LambdaQueryWrapper<DataTopic>().eq(DataTopic::getParentCode, topicCode));
        if (childCount > 0) {
            log.warn("[Service] deleteDataTopic blocked: has children, dataTopicCode={}, childCount={}", topicCode, childCount);
            throw new BusinessException(4005, "当前数据主题存在子节点，无法删除");
        }

        boolean removed = remove(new LambdaQueryWrapper<DataTopic>().eq(DataTopic::getDataTopicCode, topicCode));
        if (!removed) {
            log.warn("[Service] deleteDataTopic failed, dataTopicCode={}", topicCode);
            throw new BusinessException(5001, "删除数据主题失败");
        }

        // 如果父节点存在且删除后没有其他子节点，则将父节点更新为叶子节点
        if (entity.getParentCode() != null) {
            // 删除该节点后，查询父节点的子节点数量
            long siblingCount = count(
                    new LambdaQueryWrapper<DataTopic>().eq(DataTopic::getParentCode, entity.getParentCode())
            );
            if (siblingCount == 0) {
                update(
                        new DataTopic().setIsLeaf(true),
                        new LambdaQueryWrapper<DataTopic>().eq(DataTopic::getDataTopicCode, entity.getParentCode())
                );

                log.info("[Service] deleteDataTopic updated parent to leaf, parentCode={}, siblingCount={}", entity.getParentCode(), siblingCount);
            }
        }

        log.info("[Service] deleteDataTopic success, dataTopicCode={}", topicCode);
    }

    /**
     * 构建树形结构或分页查询条件
     */
    private LambdaQueryWrapper<DataTopic> buildQueryWrapper(String parentCode, String dataTopicName, String dataTopicLabel, CommonStatus status, boolean treeMode) {
        LambdaQueryWrapper<DataTopic> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(StringUtils.hasText(parentCode), DataTopic::getParentCode, normalizeToNull(parentCode))
                .like(StringUtils.hasText(dataTopicName), DataTopic::getDataTopicName, normalizeToNull(dataTopicName))
                .like(StringUtils.hasText(dataTopicLabel), DataTopic::getDataTopicLabel, normalizeToNull(dataTopicLabel))
                .eq(status != null, DataTopic::getStatus, status)
                .orderByAsc(DataTopic::getSortNo);
        return queryWrapper;
    }

    /**
     * 根据数据主题编码获取实体
     */
    private DataTopic getByDataTopicCode(String dataTopicCode) {
        return getOne(new LambdaQueryWrapper<DataTopic>().eq(DataTopic::getDataTopicCode, dataTopicCode).last("LIMIT 1"));
    }

    /**
     * 判断数据主题编码是否存在
     */
    private boolean existsByDataTopicCode(String dataTopicCode) {
        return count(new LambdaQueryWrapper<DataTopic>().eq(DataTopic::getDataTopicCode, dataTopicCode)) > 0;
    }

    /**
     * 根据数据主题列表构建树形结构,将各个数据主题放到对应parentCode的children中
     * @param dataTopics 数据主题列表
     * @return 树形结构列表
     */
    private List<DataTopicView> buildTree(List<DataTopic> dataTopics) {
        Map<String, DataTopicView> nodeMap = new LinkedHashMap<>();
        for (DataTopic dataTopic : dataTopics) {
            nodeMap.put(dataTopic.getDataTopicCode(), DataTopicConverter.toView(dataTopic));
        }
        List<DataTopicView> roots = new ArrayList<>();
        for (DataTopicView node : nodeMap.values()) {
            String parentCode = node.getParentCode();
            if (parentCode != null && nodeMap.containsKey(parentCode)) {
                nodeMap.get(parentCode).getChildren().add(node);
            } else {
                roots.add(node);
            }
        }
        return roots;
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
