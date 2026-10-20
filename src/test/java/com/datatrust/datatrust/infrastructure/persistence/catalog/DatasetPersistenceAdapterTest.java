package com.datatrust.datatrust.infrastructure.persistence.catalog;

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

import com.datatrust.datatrust.application.ports.DatasetRepository;
import com.datatrust.datatrust.domain.catalog.Dataset;
import com.datatrust.datatrust.domain.catalog.DatasetColumn;
import com.datatrust.datatrust.domain.catalog.DuplicateDatasetNameException;
import com.datatrust.datatrust.domain.enums.ColumnType;
import com.datatrust.datatrust.domain.enums.DatasetFormat;

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
        Dataset original = dataset("pedidos");
        repository.save(original);

        // Limpiamos el contexto para que la lectura vaya a la base de datos
        // y no a la caché de primer nivel de Hibernate
        entityManager.flush();
        entityManager.clear();

        Dataset found = repository.findById(original.id()).orElseThrow();

        assertThat(found.name()).isEqualTo("pedidos");
        assertThat(found.columns())
                .extracting(DatasetColumn::name)
                .containsExactly("id", "nombre");
    }

    @Test
    void returnsEmptyWhenIdDoesNotExist() {
        assertThat(repository.findById(UUID.randomUUID())).isEmpty();
    }

    // Las columnas se pasan en orden inverso a propósito: el test comprueba
    // que la base de datos devuelve el orden por posición, no el de inserción
    private static Dataset dataset(String name) {
        DatasetColumn nombre = DatasetColumn.create("nombre", 2, ColumnType.STRING, true);
        DatasetColumn id = DatasetColumn.create("id", 1, ColumnType.INTEGER, true);
        return Dataset.create(name, "descripción de prueba", UUID.randomUUID(),
                DatasetFormat.CSV, 1, 0, 0, List.of(nombre, id));
    }
}