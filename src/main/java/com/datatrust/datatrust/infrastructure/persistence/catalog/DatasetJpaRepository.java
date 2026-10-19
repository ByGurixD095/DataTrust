package com.datatrust.datatrust.infrastructure.persistence.catalog;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DatasetJpaRepository extends JpaRepository<DatasetEntity, UUID> {
}