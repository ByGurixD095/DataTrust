package com.datatrust.api.catalog.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.datatrust.domain.enums.ColumnType;

public record ColumnRequest(
        @NotBlank @Size(max = 255) String name,
        @NotNull @Min(1) Integer position,
        @NotNull ColumnType expectedType,
        @NotNull Boolean required) {
}