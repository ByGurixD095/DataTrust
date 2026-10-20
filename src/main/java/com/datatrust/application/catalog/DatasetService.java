package com.datatrust.application.catalog;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.datatrust.application.ports.DatasetRepository;
import com.datatrust.domain.catalog.Dataset;
import com.datatrust.domain.catalog.DatasetColumn;
import com.datatrust.domain.catalog.DatasetVersion;
import com.datatrust.domain.catalog.DatasetWithCurrentVersion;
import com.datatrust.domain.enums.DatasetFormat;

@Service
public class DatasetService {

    private final DatasetRepository repository;

    public DatasetService(DatasetRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public UUID register(String name, String description, UUID ownerId,
                         DatasetFormat format, int major, int minor, int patch,
                         List<DatasetColumn> columns) {
        UUID datasetId = UUID.randomUUID();
        DatasetVersion version = DatasetVersion.create(
                datasetId, major, minor, patch, format, columns, ownerId);
        Dataset dataset = Dataset.create(datasetId, name, description, ownerId, version.id());
        repository.save(new DatasetWithCurrentVersion(dataset, version));
        return dataset.id();
    }

    @Transactional(readOnly = true)
    public DatasetWithCurrentVersion get(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new DatasetNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<DatasetWithCurrentVersion> list(int page, int size) {
        return repository.findPage(page, size);
    }
}