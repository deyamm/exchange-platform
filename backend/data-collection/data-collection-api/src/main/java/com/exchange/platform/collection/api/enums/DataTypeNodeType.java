package com.exchange.platform.collection.api.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

/**
 * 数据类型节点类型枚举：分类节点和具体节点
 * 分类节点：类型树中非叶子节点，不能用于构建逻辑表结构
 * 具体节点：类型树中叶子结点，用于构建逻辑表结构，并挂载具体的数据集
 */
public enum DataTypeNodeType implements IEnum<Integer> {

    CATEGORY(0, "分类节点"),
    CONCRETE(1, "具体节点");

    private final Integer code;
    private final String desc;

    DataTypeNodeType(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    public Integer getValue() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
    
}
