package com.exchange.platform.collection.api.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

/**
 * 任务模板同步状态枚举
 * <ul>
 *   <li>SYNCED  - 同步成功</li>
 *   <li>FAILED  - 同步失败</li>
 *   <li>PENDING - 同步中</li>
 * </ul>
 */
public enum SyncStatus implements IEnum<Integer> {

    SYNCED(0, "已同步"),
    FAILED(1, "同步失败"),
    PENDING(2, "同步中");

    private final int code;
    private final String desc;

    SyncStatus(int code, String desc) {
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
