package com.datatrust.application.catalog;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.datatrust.application.ports.DatasetRepository;
import com.datatrust.domain.catalog.Dataset;
import com.datatrust.domain.catalog.DatasetColumn;
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
        Dataset dataset = Dataset.create(name, description, ownerId, format,
                major, minor, patch, columns);
        repository.save(dataset);
        return dataset.id();
    }

    @Transactional(readOnly = true)
    public Dataset get(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new DatasetNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Dataset> list(int page, int size) {
        return repository.findPage(page, size);
    }
}