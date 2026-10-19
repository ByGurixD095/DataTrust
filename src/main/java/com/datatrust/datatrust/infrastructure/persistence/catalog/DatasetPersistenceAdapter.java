package com.datatrust.datatrust.infrastructure.persistence.catalog;

import com.datatrust.datatrust.application.ports.DatasetRepository;
import com.datatrust.datatrust.domain.catalog.Dataset;
import com.datatrust.datatrust.domain.catalog.DatasetColumn;
import com.datatrust.datatrust.domain.catalog.DuplicateDatasetNameException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class DatasetPersistenceAdapter implements DatasetRepository {

    private static final String NAME_INDEX = "ux_datasets_name_lower";

    private final DatasetJpaRepository jpa;

    public DatasetPersistenceAdapter(DatasetJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public void save(Dataset dataset) {
        DatasetEntity entity = toEntity(dataset);
        try {
            jpa.saveAndFlush(entity);
        } catch (DataIntegrityViolationException ex) {
            if (causedByNameIndex(ex)) {
                throw new DuplicateDatasetNameException(dataset.name());
            }
            throw ex; // cualquier otra violación es un fallo interno
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Dataset> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Dataset> findPage(int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt", "id").ascending());
        return jpa.findAll(pageable).getContent().stream()
                .map(this::toDomain)
                .toList();
    }

    private DatasetEntity toEntity(Dataset dataset) {
        DatasetEntity entity = new DatasetEntity(
                dataset.id(),
                dataset.name(),
                dataset.description(),
                dataset.ownerId(),
                dataset.format(),
                dataset.version(),
                dataset.createdAt());

        for (DatasetColumn column : dataset.columns()) {
            entity.addColumn(new DatasetColumnEntity(
                    column.id(),
                    entity,
                    column.name(),
                    column.position(),
                    column.expectedType(),
                    column.required()));
        }
        return entity;
    }

    private Dataset toDomain(DatasetEntity entity) {
        List<DatasetColumn> columns = entity.getColumns().stream()
                .map(c -> DatasetColumn.restore(
                        c.getId(),
                        c.getName(),
                        c.getPosition(),
                        c.getExpectedType(),
                        c.getRequired()))
                .toList();

        return Dataset.restore(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getOwnerId(),
                entity.getFormat(),
                entity.getVersion(),
                entity.getCreatedAt(),
                columns);
    }

    private static boolean causedByNameIndex(Throwable ex) {
        // Busca el nombre del índice en la cadena de causas, porque el tipo de
        for (Throwable t = ex; t != null; t = t.getCause()) {
            // excepción concreto cambia según el driver y la versión de Hibernate
            String message = t.getMessage();
            if (message != null && message.contains(NAME_INDEX)) {
                return true;
            }
        }
        return false;
    }
}