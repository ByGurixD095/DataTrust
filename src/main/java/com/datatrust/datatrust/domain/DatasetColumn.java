package com.datatrust.datatrust.domain;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.datatrust.datatrust.domain.enums.ColumnType;

@Entity
@Table(name = "dataset_columns")
public class DatasetColumn{

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dataset_id", nullable = false, updatable = false)
    private Dataset dataset;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "position", nullable = false)
    private Integer position;

    @Enumerated(EnumType.STRING)
    @Column(name = "expected_type", nullable = false, length = 20)
    private ColumnType expectedType;

    @Column(name = "required", nullable = false)
    private Boolean required;

    protected DatasetColumn() {
        // requerido por JPA
    }

    public DatasetColumn(UUID id, Dataset dataset, String name,
                               Integer position, ColumnType expectedType, Boolean required) {
        this.id = id;
        this.dataset = dataset;
        this.name = name;
        this.position = position;
        this.expectedType = expectedType;
        this.required = required;
    }

}