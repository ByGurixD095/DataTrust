package com.datatrust.datatrust.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.datatrust.datatrust.domain.enums.DatasetFormat;

@Entity
@Table(name = "datasets")
public class Dataset {

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

    @Version
    @Column(name = "lock_version", nullable = false)
    private Integer lockVersion;

    @OneToMany(mappedBy = "dataset", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<DatasetColumn> columns = new ArrayList<>();

    protected Dataset() {
        // requerido por JPA
    }

    public Dataset(UUID id, String name, String description, UUID ownerId,
                         DatasetFormat format, Integer version, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.ownerId = ownerId;
        this.format = format;
        this.version = version;
        this.createdAt = createdAt;
    }

    public void addColumn(DatasetColumn column) {
        columns.add(column);
    }

}