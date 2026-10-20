package com.datatrust.api.catalog.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.datatrust.domain.catalog.Dataset;
import com.datatrust.domain.catalog.DatasetColumn;
import com.datatrust.domain.catalog.DatasetVersion;
import com.datatrust.domain.catalog.DatasetWithCurrentVersion;
import com.datatrust.domain.enums.ColumnType;
import com.datatrust.domain.enums.DatasetFormat;

public record DatasetResponse(
        UUID id,
        String name,
        String description,
        UUID ownerId,
        DatasetFormat format,
        String version,
        Instant createdAt,
        List<ColumnResponse> columns) {

    public static DatasetResponse from(DatasetWithCurrentVersion catalogEntry) {
        Dataset dataset = catalogEntry.dataset();
        DatasetVersion version = catalogEntry.currentVersion();
        return new DatasetResponse(
                dataset.id(),
                dataset.name(),
                dataset.description(),
                dataset.ownerId(),
                version.schemaSnapshot().format(),
                version.versionNumber(),
                version.createdAt(),
                version.schemaSnapshot().columns().stream().map(ColumnResponse::from).toList());
    }

    public record ColumnResponse(
            UUID id,
            String name,
            int position,
            ColumnType expectedType,
            boolean required) {

        public static ColumnResponse from(DatasetColumn column) {
            return new ColumnResponse(column.id(), column.name(), column.position(),
                    column.expectedType(), column.required());
        }
    }
}