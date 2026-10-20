package com.datatrust.infrastructure.persistence.catalog;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.datatrust.domain.enums.DatasetFormat;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "datasets")
public class DatasetEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "format", nullable = false, length = 10)
    private DatasetFormat format;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Null en una entidad nueva: Spring Data la considera nueva y hace persist.
    // Hibernate asigna 0 al insertar y lo incrementa en cada update.
    @Version
    @Column(name = "lock_version", nullable = false)
    private Integer lockVersion;

    @OneToMany(mappedBy = "dataset", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<DatasetColumnEntity> columns = new ArrayList<>();

    protected DatasetEntity() {
        // requerido por JPA
    }

    public DatasetEntity(UUID id, String name, String description, UUID ownerId,
                         DatasetFormat format, Integer version, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.ownerId = ownerId;
        this.format = format;
        this.version = version;
        this.createdAt = createdAt;
    }

    public void addColumn(DatasetColumnEntity column) {
        columns.add(column);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public DatasetFormat getFormat() {
        return format;
    }

    public Integer getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Integer getLockVersion() {
        return lockVersion;
    }

    public List<DatasetColumnEntity> getColumns() {
        return columns;
    }
}