package com.datatrust.datatrust.infrastructure.persistence.catalog;

import java.util.UUID;

import com.datatrust.datatrust.domain.enums.ColumnType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "dataset_columns")
public class DatasetColumnEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dataset_id", nullable = false, updatable = false)
    private DatasetEntity dataset;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "position", nullable = false)
    private Integer position;

    @Enumerated(EnumType.STRING)
    @Column(name = "expected_type", nullable = false, length = 20)
    private ColumnType expectedType;

    @Column(name = "required", nullable = false)
    private Boolean required;

    protected DatasetColumnEntity() {
        // requerido por JPA
    }

    public DatasetColumnEntity(UUID id, DatasetEntity dataset, String name,
                               Integer position, ColumnType expectedType, Boolean required) {
        this.id = id;
        this.dataset = dataset;
        this.name = name;
        this.position = position;
        this.expectedType = expectedType;
        this.required = required;
    }

    public UUID getId() {
        return id;
    }

    public DatasetEntity getDataset() {
        return dataset;
    }

    public String getName() {
        return name;
    }

    public Integer getPosition() {
        return position;
    }

    public ColumnType getExpectedType() {
        return expectedType;
    }

    public Boolean getRequired() {
        return required;
    }
}