package com.example.eams.common.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.example.eams.security.filter.SecurityContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 自动填充处理器
 * <p>
 * 自动填充 create_time/create_by/update_time/update_by 四个审计字段。
 * 前端不可传递这些字段，由后端统一填充。
 */
@Slf4j
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        Long userId = SecurityContextHolder.getCurrentUserId();
        String username = SecurityContextHolder.getCurrentUsername();
        if (username == null) { username = "SYSTEM"; }

        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "createBy", String.class, username);
        this.strictInsertFill(metaObject, "updateBy", String.class, username);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        String username = SecurityContextHolder.getCurrentUsername();
        if (username == null) { username = "SYSTEM"; }

        // 使用 setFieldValByName 而非 strictUpdateFill，确保每次修改都刷新修改时间
        this.setFieldValByName("updateTime", now, metaObject);
        this.setFieldValByName("updateBy", username, metaObject);
    }
}
