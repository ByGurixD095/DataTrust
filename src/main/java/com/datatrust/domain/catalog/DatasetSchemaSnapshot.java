package com.datatrust.domain.catalog;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.datatrust.domain.enums.DatasetFormat;

public record DatasetSchemaSnapshot(DatasetFormat format, List<DatasetColumn> columns) {

    public DatasetSchemaSnapshot {
        Objects.requireNonNull(format, "format");
        columns = columns.stream()
                .sorted(Comparator.comparingInt(DatasetColumn::position))
                .toList();
    }
}
