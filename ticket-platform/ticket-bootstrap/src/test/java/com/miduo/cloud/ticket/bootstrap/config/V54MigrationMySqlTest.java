package com.miduo.cloud.ticket.bootstrap.config;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Opt-in, real MySQL integration. MYSQL_TEST_URL must name a disposable test schema. */
@EnabledIfEnvironmentVariable(named = "MYSQL_TEST_URL", matches = ".+")
class V54MigrationMySqlTest {
    private DriverManagerDataSource database(String suffix) {
        String url = System.getenv("MYSQL_TEST_URL");
        String schema = "mts3_migration_" + suffix + "_" + System.nanoTime();
        DriverManagerDataSource admin = new DriverManagerDataSource(url,
                System.getenv("MYSQL_TEST_USER"), System.getenv("MYSQL_TEST_PASSWORD"));
        new JdbcTemplate(admin).execute("CREATE DATABASE `" + schema + "` CHARACTER SET utf8mb4");
        String schemaUrl = url.replaceFirst("/[^/?]+(?=\\?|$)", "/" + schema);
        return new DriverManagerDataSource(schemaUrl, System.getenv("MYSQL_TEST_USER"), System.getenv("MYSQL_TEST_PASSWORD"));
    }

    private Flyway configure(DriverManagerDataSource ds) {
        return Flyway.configure().dataSource(ds).locations("classpath:db/migration").cleanDisabled(true).load();
    }

    @Test
    void freshDatabaseAndBothV54HistoriesConvergeWithoutRewritingHistory() {
        for (String variant : new String[]{"fresh", "bug-report", "workflow"}) {
            DriverManagerDataSource ds = database(variant.replace('-', '_'));
            JdbcTemplate jdbc = new JdbcTemplate(ds);
            List<Map<String, Object>> before = null;
            if (!"fresh".equals(variant)) {
                Flyway.configure().dataSource(ds).locations("classpath:db/migration", "classpath:db/legacy-v54/" + variant)
                        .target("69").load().migrate();
                before = jdbc.queryForList("SELECT * FROM flyway_schema_history ORDER BY installed_rank");
            }
            new FlywayMigrationConfig().flywayMigrationStrategy().migrate(configure(ds));
            if (before != null) {
                assertEquals(before, jdbc.queryForList("SELECT * FROM flyway_schema_history WHERE CAST(version AS UNSIGNED)<=69 ORDER BY installed_rank"));
            }
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='bug_report' AND column_name='resolve_time'", Integer.class));
            assertEquals(3, jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='workflow' AND column_name IN ('invocation_count','first_invoked_time','last_invoked_time')", Integer.class));
            List<Map<String, Object>> after = jdbc.queryForList("SELECT * FROM flyway_schema_history ORDER BY installed_rank");
            new FlywayMigrationConfig().flywayMigrationStrategy().migrate(configure(ds));
            assertEquals(after, jdbc.queryForList("SELECT * FROM flyway_schema_history ORDER BY installed_rank"));
            jdbc.update("UPDATE flyway_schema_history SET checksum=123 WHERE version='54'");
            assertThrows(RuntimeException.class, () -> new FlywayMigrationConfig().flywayMigrationStrategy().migrate(configure(ds)));
            assertEquals(123, jdbc.queryForObject("SELECT checksum FROM flyway_schema_history WHERE version='54'", Integer.class));
        }
    }
}
