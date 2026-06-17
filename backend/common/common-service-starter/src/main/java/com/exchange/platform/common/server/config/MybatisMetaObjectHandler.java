package com.exchange.platform.common.server.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;

import java.time.LocalDateTime;

public class MybatisMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        strictInsertFill(metaObject, "createdBy", String.class, "system");
        strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());

        strictInsertFill(metaObject, "updatedBy", String.class, "system");
        strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());

    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updatedBy", String.class, "system");
        strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }
}
