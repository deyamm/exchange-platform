package com.exchange.platform.collection.api.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

/**
 * 存储映射状态枚举
 * <ul>
 *   <li>DRAFT     - 草稿</li>
 *   <li>CONFIRMED - 已确认</li>
 *   <li>ENABLED   - 已启用</li>
 *   <li>DISABLED  - 已停用</li>
 * </ul>
 */
public enum MappingStatus implements IEnum<Integer>{

    DRAFT(0, "草稿"),
    CONFIRMED(1, "已确认"),
    ENABLED(2, "已启用"),
    DISABLED(3, "已停用");

    private final int code;
    private final String desc;

    MappingStatus(int code, String desc) {
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
