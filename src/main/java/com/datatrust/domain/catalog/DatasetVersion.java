package com.datatrust.domain.catalog;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.datatrust.domain.enums.DatasetErrorCode;
import com.datatrust.domain.enums.DatasetFormat;

public class DatasetVersion {

    private static final int MAX_MAJOR = 2146;
    private static final int MAX_MINOR_PATCH = 999;

    private final UUID id;
    private final UUID datasetId;
    private final String versionNumber;
    private final DatasetSchemaSnapshot schemaSnapshot;
    private final Instant createdAt;
    private final UUID createdBy;

    private DatasetVersion(UUID id, UUID datasetId, String versionNumber,
                           DatasetSchemaSnapshot schemaSnapshot, Instant createdAt,
                           UUID createdBy) {
        this.id = id;
        this.datasetId = datasetId;
        this.versionNumber = versionNumber;
        this.schemaSnapshot = schemaSnapshot;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }

    public static DatasetVersion create(UUID datasetId, int major, int minor, int patch,
                                        DatasetFormat format, List<DatasetColumn> columns,
                                        UUID createdBy) {
        Objects.requireNonNull(datasetId, "datasetId");
        Objects.requireNonNull(createdBy, "createdBy");
        Objects.requireNonNull(format, "format");
        String versionNumber = encodeVersion(major, minor, patch);
        validateColumns(columns);
        return new DatasetVersion(UUID.randomUUID(), datasetId, versionNumber,
                new DatasetSchemaSnapshot(format, columns), Instant.now(), createdBy);
    }

    public static DatasetVersion restore(UUID id, UUID datasetId, String versionNumber,
                                         DatasetSchemaSnapshot schemaSnapshot,
                                         Instant createdAt, UUID createdBy) {
        return new DatasetVersion(id, datasetId, versionNumber, schemaSnapshot, createdAt, createdBy);
    }

    private static String encodeVersion(int major, int minor, int patch) {
        boolean valid = major >= 0 && major <= MAX_MAJOR
                && minor >= 0 && minor <= MAX_MINOR_PATCH
                && patch >= 0 && patch <= MAX_MINOR_PATCH;
        if (!valid) {
            throw new DatasetValidationException(DatasetErrorCode.VERSION_INVALID, "version",
                    "La versión debe ser major entre 0 y " + MAX_MAJOR
                            + " y minor y patch entre 0 y " + MAX_MINOR_PATCH);
        }
        return "%d.%d.%d".formatted(major, minor, patch);
    }

    private static void validateColumns(List<DatasetColumn> columns) {
        if (columns == null || columns.isEmpty()) {
            throw new DatasetValidationException(DatasetErrorCode.NO_COLUMNS, "columns",
                    "El dataset debe tener al menos una columna");
        }

        Set<String> names = new HashSet<>();
        for (DatasetColumn column : columns) {
            if (!names.add(column.name().toLowerCase(Locale.ROOT))) {
                throw new DatasetValidationException(DatasetErrorCode.COLUMN_NAME_DUPLICATED,
                        "columns.name", "Nombre de columna repetido: " + column.name());
            }
        }

        List<DatasetColumn> sorted = columns.stream()
                .sorted(Comparator.comparingInt(DatasetColumn::position))
                .toList();
        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).position() != i + 1) {
                throw new DatasetValidationException(DatasetErrorCode.COLUMN_POSITION_INVALID,
                        "columns.position", "Las posiciones deben ser consecutivas desde 1");
            }
        }
    }

    public UUID id() { return id; }
    public UUID datasetId() { return datasetId; }
    public String versionNumber() { return versionNumber; }
    public DatasetSchemaSnapshot schemaSnapshot() { return schemaSnapshot; }
    public Instant createdAt() { return createdAt; }
    public UUID createdBy() { return createdBy; }
}
