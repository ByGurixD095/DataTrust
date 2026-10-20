package com.datatrust.domain.catalog;

import java.util.Objects;
import java.util.UUID;

import com.datatrust.domain.enums.DatasetErrorCode;

public class Dataset {

    private static final int MAX_NAME_LENGTH = 255;

    private final UUID id;
    private final String name;
    private final String description;
    private final UUID ownerId;
    private final UUID currentVersionId;

    private Dataset(UUID id, String name, String description, UUID ownerId,
                    UUID currentVersionId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.ownerId = ownerId;
        this.currentVersionId = currentVersionId;
    }

    public static Dataset create(String name, String description, UUID ownerId,
                                 UUID currentVersionId) {
        if (name == null || name.isBlank()) {
            throw new DatasetValidationException(DatasetErrorCode.NAME_BLANK, "name",
                    "El nombre es obligatorio");
        }
        String cleanName = name.strip();
        if (cleanName.length() > MAX_NAME_LENGTH) {
            throw new DatasetValidationException(DatasetErrorCode.NAME_TOO_LONG, "name",
                    "El nombre no puede superar " + MAX_NAME_LENGTH + " caracteres");
        }
        if (description == null || description.isBlank()) {
            throw new DatasetValidationException(DatasetErrorCode.DESCRIPTION_BLANK, "description",
                    "La descripción es obligatoria");
        }
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(currentVersionId, "currentVersionId");
        return new Dataset(UUID.randomUUID(), cleanName, description.strip(), ownerId, currentVersionId);
    }

    public static Dataset create(UUID id, String name, String description, UUID ownerId,
                                 UUID currentVersionId) {
        Dataset dataset = create(name, description, ownerId, currentVersionId);
        return new Dataset(Objects.requireNonNull(id, "id"), dataset.name, dataset.description,
                dataset.ownerId, dataset.currentVersionId);
    }

    public static Dataset restore(UUID id, String name, String description, UUID ownerId,
                                  UUID currentVersionId) {
        return new Dataset(id, name, description, ownerId, currentVersionId);
    }

    public UUID id() { return id; }
    public String name() { return name; }
    public String description() { return description; }
    public UUID ownerId() { return ownerId; }
    public UUID currentVersionId() { return currentVersionId; }
}
