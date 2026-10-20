package com.datatrust.infrastructure.persistence.catalog;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.datatrust.domain.enums.ColumnType;
import com.datatrust.domain.enums.DatasetFormat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "dataset_versions")
public class DatasetVersionEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dataset_id", nullable = false, updatable = false)
    private DatasetEntity dataset;

    @Column(name = "version_number", nullable = false, updatable = false, length = 32)
    private String versionNumber;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "schema_snapshot", nullable = false, updatable = false, columnDefinition = "jsonb")
    private SchemaSnapshotEntity schemaSnapshot;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    protected DatasetVersionEntity() {
    }

    public DatasetVersionEntity(UUID id, DatasetEntity dataset, String versionNumber,
                                SchemaSnapshotEntity schemaSnapshot, Instant createdAt,
                                UUID createdBy) {
        this.id = id;
        this.dataset = dataset;
        this.versionNumber = versionNumber;
        this.schemaSnapshot = schemaSnapshot;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }

    public UUID getId() {
        return id;
    }

    public UUID getDatasetId() {
        return dataset.getId();
    }

    public String getVersionNumber() {
        return versionNumber;
    }

    public SchemaSnapshotEntity getSchemaSnapshot() {
        return schemaSnapshot;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public record SchemaSnapshotEntity(
            DatasetFormat format,
            List<ColumnSnapshotEntity> columns) {
    }

    public record ColumnSnapshotEntity(
            UUID id,
            String name,
            int position,
            ColumnType expectedType,
            boolean required) {
    }
}
