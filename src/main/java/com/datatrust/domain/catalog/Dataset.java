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

public class Dataset {

    private static final int MAX_NAME_LENGTH = 255;
    // 2146 * 1_000_000 + 999_999 cabe en un INTEGER; 2147 no
    private static final int MAX_MAJOR = 2146;
    private static final int MAX_MINOR_PATCH = 999;

    private final UUID id;
    private final String name;
    private final String description;
    private final UUID ownerId;
    private final DatasetFormat format;
    private final int version;
    private final Instant createdAt;
    private final List<DatasetColumn> columns;

    private Dataset(UUID id, String name, String description, UUID ownerId,
                    DatasetFormat format, int version, Instant createdAt,
                    List<DatasetColumn> columns) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.ownerId = ownerId;
        this.format = format;
        this.version = version;
        this.createdAt = createdAt;
        this.columns = List.copyOf(columns);
    }

    public static Dataset create(String name, String description, UUID ownerId,
                                 DatasetFormat format,
                                 int major, int minor, int patch,
                                 List<DatasetColumn> columns) {
        String cleanName = requireText(name, DatasetErrorCode.NAME_BLANK, "name",
                "El nombre es obligatorio");
        if (cleanName.length() > MAX_NAME_LENGTH) {
            throw new DatasetValidationException(DatasetErrorCode.NAME_TOO_LONG, "name",
                    "El nombre no puede superar " + MAX_NAME_LENGTH + " caracteres");
        }
        String cleanDescription = requireText(description, DatasetErrorCode.DESCRIPTION_BLANK,
                "description", "La descripción es obligatoria");
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(format, "format");

        int encodedVersion = encodeVersion(major, minor, patch);
        validateColumns(columns);

        return new Dataset(UUID.randomUUID(), cleanName, cleanDescription, ownerId, format,
                encodedVersion, Instant.now(), columns);
    }

    // Reconstruye desde la base de datos. Los datos ya fueron validados al crearse.
    public static Dataset restore(UUID id, String name, String description, UUID ownerId,
                                  DatasetFormat format, int version, Instant createdAt,
                                  List<DatasetColumn> columns) {
        return new Dataset(id, name, description, ownerId, format, version, createdAt, columns);
    }

    private static String requireText(String value, DatasetErrorCode code,
                                      String field, String message) {
        if (value == null || value.isBlank()) {
            throw new DatasetValidationException(code, field, message);
        }
        return value.strip();
    }

    private static int encodeVersion(int major, int minor, int patch) {
        boolean valid = major >= 0 && major <= MAX_MAJOR
                && minor >= 0 && minor <= MAX_MINOR_PATCH
                && patch >= 0 && patch <= MAX_MINOR_PATCH;
        if (!valid) {
            throw new DatasetValidationException(DatasetErrorCode.VERSION_INVALID, "version",
                    "La versión debe ser major entre 0 y " + MAX_MAJOR
                            + " y minor y patch entre 0 y " + MAX_MINOR_PATCH);
        }
        return major * 1_000_000 + minor * 1_000 + patch;
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

        // Las posiciones deben ser 1..n sin huecos ni repeticiones
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
    public String name() { return name; }
    public String description() { return description; }
    public UUID ownerId() { return ownerId; }
    public DatasetFormat format() { return format; }
    public int version() { return version; }
    public Instant createdAt() { return createdAt; }
    public List<DatasetColumn> columns() { return columns; }

    // Versión legible para la API: 2000000 -> "2.0.0"
    public String versionText() {
        return "%d.%d.%d".formatted(version / 1_000_000,
                (version % 1_000_000) / 1_000, version % 1_000);
    }
}