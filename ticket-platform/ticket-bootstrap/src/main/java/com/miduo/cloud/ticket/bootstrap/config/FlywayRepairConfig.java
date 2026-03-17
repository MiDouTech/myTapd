package com.miduo.cloud.ticket.bootstrap.config;

import org.flywaydb.core.api.output.RepairResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Flyway 迁移策略配置
 * 在执行迁移前先执行 repair，清除历史上因 SQL 语法错误或网络中断留下的 FAILED 状态记录，
 * 使后续迁移可以正常重试，避免因脏数据导致服务无法启动。
 */
@Configuration
public class FlywayRepairConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayRepairConfig.class);

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            RepairResult repairResult = flyway.repair();
            if (!repairResult.repairActions.isEmpty()) {
                log.info("Flyway repair 已清理 {} 条失败迁移记录：{}",
                        repairResult.repairActions.size(), repairResult.repairActions);
            }
            flyway.migrate();
        };
    }
}
