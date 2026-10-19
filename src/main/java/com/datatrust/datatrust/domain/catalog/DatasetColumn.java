package com.datatrust.datatrust.domain.catalog;

import java.util.Objects;
import java.util.UUID;

import com.datatrust.datatrust.domain.enums.ColumnType;
import com.datatrust.datatrust.domain.enums.DatasetErrorCode;

public class DatasetColumn {

    private static final int MAX_NAME_LENGTH = 255;

    private final UUID id;
    private final String name;
    private final int position;
    private final ColumnType expectedType;
    private final boolean required;

    private DatasetColumn(UUID id, String name, int position,
                          ColumnType expectedType, boolean required) {
        this.id = id;
        this.name = name;
        this.position = position;
        this.expectedType = expectedType;
        this.required = required;
    }

    public static DatasetColumn create(String name, int position,
                                       ColumnType expectedType, boolean required) {
        if (name == null || name.isBlank()) {
            throw new DatasetValidationException(DatasetErrorCode.NAME_BLANK, "columns.name",
                    "El nombre de la columna es obligatorio");
        }
        String cleanName = name.strip();
        if (cleanName.length() > MAX_NAME_LENGTH) {
            throw new DatasetValidationException(DatasetErrorCode.NAME_TOO_LONG, "columns.name",
                    "El nombre de la columna no puede superar " + MAX_NAME_LENGTH + " caracteres");
        }
        if (position < 1) {
            throw new DatasetValidationException(DatasetErrorCode.COLUMN_POSITION_INVALID,
                    "columns.position", "La posición debe ser mayor o igual que 1");
        }
        Objects.requireNonNull(expectedType, "expectedType");

        return new DatasetColumn(UUID.randomUUID(), cleanName, position, expectedType, required);
    }

    public static DatasetColumn restore(UUID id, String name, int position,
                                        ColumnType expectedType, boolean required) {
        return new DatasetColumn(id, name, position, expectedType, required);
    }

    public UUID id() { return id; }
    public String name() { return name; }
    public int position() { return position; }
    public ColumnType expectedType() { return expectedType; }
    public boolean required() { return required; }
}