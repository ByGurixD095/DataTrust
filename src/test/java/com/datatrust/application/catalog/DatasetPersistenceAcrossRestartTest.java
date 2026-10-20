package com.datatrust.application.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;

import com.datatrust.domain.catalog.Dataset;
import com.datatrust.domain.catalog.DatasetColumn;
import com.datatrust.domain.enums.ColumnType;
import com.datatrust.domain.enums.DatasetFormat;
import com.datatrust.support.SharedPostgres;
import com.datatrust.support.StandaloneApplication;

class DatasetPersistenceAcrossRestartTest {

    @Test
    void datasetSurvivesApplicationRestart() {
        UUID id;

        try (ConfigurableApplicationContext first =
                     StandaloneApplication.start(SharedPostgres.CONTAINER)) {
            id = first.getBean(DatasetService.class).register(
                    "persistente", "sobrevive al reinicio", UUID.randomUUID(),
                    DatasetFormat.CSV, 1, 0, 0,
                    List.of(DatasetColumn.create("id", 1, ColumnType.INTEGER, true)));
        }

        try (ConfigurableApplicationContext second =
                     StandaloneApplication.start(SharedPostgres.CONTAINER)) {
            Dataset found = second.getBean(DatasetService.class).get(id);

            assertThat(found.name()).isEqualTo("persistente");
            assertThat(found.columns()).hasSize(1);
            assertThat(found.versionText()).isEqualTo("1.0.0");
        }
    }
}