package com.exchange.platform.collection.api.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

/**
 * 数据集管理状态。
 * <p>数据库使用从 0 开始的整数编码，禁止将枚举名称直接作为状态值持久化。</p>
 */
public enum DatasetStatus implements IEnum<Integer> {
    DRAFT(0, "草稿"),
    ENABLED(1, "已启用"),
    DISABLED(2, "已停用"),
    ARCHIVED(3, "已归档");

    private final Integer code;
    private final String desc;

    /**
     * 构造数据集状态枚举。
     *
     * @param code 数据库存储值
     * @param desc 状态中文描述
     */
    DatasetStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 返回数据库存储值。
     *
     * @return 从 0 开始的状态编码
     */
    @Override
    public Integer getValue() {
        return code;
    }

    /**
     * 返回状态展示描述。
     *
     * @return 状态中文描述
     */
    public String getDesc() {
        return desc;
    }
}
