package com.miduo.cloud.ticket.bootstrap.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Flyway 迁移策略配置
 * 根据已执行的 V54 选择原始脚本，保留其版本、描述与校验和。
 * 禁止启动时自动 repair；失败迁移或历史漂移必须经运维核对后处理。
 */
@Configuration
public class FlywayMigrationConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayMigrationConfig.class);

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            String variant = selectV54(flyway);
            String[] locations = Stream.concat(Arrays.stream(flyway.getConfiguration().getLocations())
                    .map(Location::getDescriptor), Stream.of("classpath:db/legacy-v54/" + variant))
                    .toArray(String[]::new);
            String v57 = historicalScript(flyway, "57");
            if (v57 != null) {
                if (!"V57__ensure_ticket_bug_info_manual_valid_report.sql".equals(v57)) {
                    throw new IllegalStateException("Unknown V57 history script: " + v57);
                }
                locations = Stream.concat(Arrays.stream(locations), Stream.of("classpath:db/legacy-v57"))
                        .toArray(String[]::new);
            }
            log.info("Flyway V54 history variant: {}; automatic repair disabled", variant);
            Flyway.configure().configuration(flyway.getConfiguration()).locations(locations).load().migrate();
        };
    }

    static String selectV54(Flyway flyway) {
        String script = historicalScript(flyway, "54");
        if (script == null || "V54__bug_report_add_resolve_time.sql".equals(script)) return "bug-report";
        if ("V54__workflow_customization_delete_guard.sql".equals(script)) return "workflow";
        throw new IllegalStateException("Unknown V54 history script: " + script);
    }

    private static String historicalScript(Flyway flyway, String version) {
        String table = flyway.getConfiguration().getTable();
        if (!table.matches("[A-Za-z0-9_]+")) throw new IllegalStateException("Unsupported Flyway history table");
        try (Connection connection = flyway.getConfiguration().getDataSource().getConnection()) {
            String schema = flyway.getConfiguration().getDefaultSchema();
            if (schema == null) schema = connection.getCatalog();
            try (ResultSet tables = connection.getMetaData().getTables(schema, null, table, new String[]{"TABLE"})) {
                if (!tables.next()) return null;
            }
            String qualified = "`" + schema.replace("`", "``") + "`.`" + table + "`";
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT script, success FROM " + qualified + " WHERE version = ?")) {
                statement.setString(1, version);
                try (ResultSet rows = statement.executeQuery()) {
                    if (!rows.next()) return null;
                    String script = rows.getString(1);
                    boolean success = rows.getBoolean(2);
                    if (rows.next() || !success) {
                        throw new IllegalStateException("V" + version + " history is failed or ambiguous; inspect before migration");
                    }
                    return script;
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Cannot inspect migration history", ex);
        }
    }
}
