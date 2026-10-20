package com.datatrust.infrastructure.persistence.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.datatrust.application.ports.DatasetRepository;
import com.datatrust.domain.catalog.Dataset;
import com.datatrust.domain.catalog.DatasetColumn;
import com.datatrust.domain.catalog.DuplicateDatasetNameException;
import com.datatrust.domain.catalog.DatasetVersion;
import com.datatrust.domain.catalog.DatasetWithCurrentVersion;
import com.datatrust.domain.enums.ColumnType;
import com.datatrust.domain.enums.DatasetFormat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(DatasetPersistenceAdapter.class)
@Testcontainers
class DatasetPersistenceAdapterTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private DatasetRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void rejectsNameDuplicatedIgnoringCase() {
        repository.save(dataset("clientes"));

        assertThatThrownBy(() -> repository.save(dataset("CLIENTES")))
                .isInstanceOf(DuplicateDatasetNameException.class);
    }

    @Test
    void returnsColumnsOrderedByPosition() {
        DatasetWithCurrentVersion original = dataset("pedidos");
        repository.save(original);

        // Limpiamos el contexto para que la lectura vaya a la base de datos
        // y no a la caché de primer nivel de Hibernate
        entityManager.flush();
        entityManager.clear();

        DatasetWithCurrentVersion found = repository.findById(original.dataset().id()).orElseThrow();

        assertThat(found.dataset().name()).isEqualTo("pedidos");
        assertThat(found.dataset().currentVersionId())
                .isEqualTo(found.currentVersion().id());
        assertThat(found.currentVersion().schemaSnapshot().columns())
                .extracting(DatasetColumn::name)
                .containsExactly("id", "nombre");
        assertThat(found.currentVersion().schemaSnapshot().format()).isEqualTo(DatasetFormat.CSV);
    }

    @Test
    void returnsEmptyWhenIdDoesNotExist() {
        assertThat(repository.findById(UUID.randomUUID())).isEmpty();
    }

    // Las columnas se pasan en orden inverso a propósito: el test comprueba
    // que la base de datos devuelve el orden por posición, no el de inserción
    private static DatasetWithCurrentVersion dataset(String name) {
        DatasetColumn nombre = DatasetColumn.create("nombre", 2, ColumnType.STRING, true);
        DatasetColumn id = DatasetColumn.create("id", 1, ColumnType.INTEGER, true);
        UUID datasetId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        DatasetVersion version = DatasetVersion.create(datasetId, 1, 0, 0,
                DatasetFormat.CSV, List.of(nombre, id), ownerId);
        Dataset dataset = Dataset.create(datasetId, name, "descripción de prueba",
                ownerId, version.id());
        return new DatasetWithCurrentVersion(dataset, version);
    }
}