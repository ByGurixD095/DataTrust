package com.datatrust.infrastructure.persistence.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.datatrust.support.StandaloneApplication;

class FlywayMigrationTest {

    @Test
    void appliesV1OnEmptyDatabase() {
        try (PostgreSQLContainer database = new PostgreSQLContainer("postgres:16-alpine")) {
            database.start();

            try (ConfigurableApplicationContext context = StandaloneApplication.start(database)) {
                JdbcTemplate jdbc = context.getBean(JdbcTemplate.class);

                Integer applied = jdbc.queryForObject(
                        "SELECT count(*) FROM flyway_schema_history WHERE version = '1' AND success",
                        Integer.class);

                Integer tables = jdbc.queryForObject(
                        "SELECT count(*) FROM information_schema.tables "
                                + "WHERE table_schema = 'public' "
                                + "AND table_name IN ('datasets', 'dataset_versions')",
                        Integer.class);
                Integer snapshots = jdbc.queryForObject(
                        "SELECT count(*) FROM information_schema.columns "
                                + "WHERE table_schema = 'public' "
                                + "AND table_name = 'dataset_versions' "
                                + "AND column_name = 'schema_snapshot' "
                                + "AND udt_name = 'jsonb'",
                        Integer.class);

                assertThat(applied).isEqualTo(1);
                assertThat(tables).isEqualTo(2);
                assertThat(snapshots).isEqualTo(1);
            }
        }
    }
}