package com.exchange.platform.collection.api.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

/**
 * 任务配置状态枚举
 * <ul>
 *   <li>DRAFT     - 草稿</li>
 *   <li>CONFIRMED - 已确认</li>
 *   <li>ENABLED   - 已启用</li>
 *   <li>DISABLED  - 已停用</li>
 *   <li>EXPIRED   - 已失效</li>
 * </ul>
 */
public enum ConfigStatus implements IEnum<Integer> {

    DRAFT(0, "草稿"),
    CONFIRMED(1, "已确认"),
    ENABLED(2, "已启用"),
    DISABLED(3, "已停用"),
    EXPIRED(4, "已失效");

    private final int code;
    private final String desc;

    ConfigStatus(int code, String desc) {
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
