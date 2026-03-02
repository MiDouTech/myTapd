package com.miduo.cloud.ticket.infrastructure.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Date;

/**
 * MyBatis-Plus 配置
 */
@Configuration
@MapperScan("com.miduo.cloud.ticket.infrastructure.persistence.mybatis.**.mapper")
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                Date now = new Date();
                this.strictInsertFill(metaObject, "createTime", Date.class, now);
                this.strictInsertFill(metaObject, "updateTime", Date.class, now);
                this.strictInsertFill(metaObject, "createBy", String.class, getCurrentUser());
                this.strictInsertFill(metaObject, "updateBy", String.class, getCurrentUser());
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                Date now = new Date();
                this.strictUpdateFill(metaObject, "updateTime", Date.class, now);
                this.strictUpdateFill(metaObject, "updateBy", String.class, getCurrentUser());
            }

            private String getCurrentUser() {
                try {
                    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                    if (authentication != null && authentication.isAuthenticated()
                            && !"anonymousUser".equals(authentication.getPrincipal())) {
                        return authentication.getName();
                    }
                } catch (Exception ignored) {
                }
                return "system";
            }
        };
    }
}
