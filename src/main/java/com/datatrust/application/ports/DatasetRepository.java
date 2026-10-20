package com.datatrust.application.ports;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


import com.datatrust.domain.catalog.Dataset;

public interface DatasetRepository {
    void save(Dataset dataset);
    Optional<Dataset> findById(UUID id);
    List<Dataset> findPage(int page, int size);
}