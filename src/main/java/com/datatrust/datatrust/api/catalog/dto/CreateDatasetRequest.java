package com.datatrust.datatrust.api.catalog.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.datatrust.datatrust.domain.enums.DatasetFormat;

// Integer y no int: si el campo falta en el JSON, @NotNull responde 400
// en lugar de convertirlo silenciosamente en 0
public record CreateDatasetRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank String description,
        @NotNull DatasetFormat format,
        @NotNull @Min(0) @Max(2146) Integer major,
        @NotNull @Min(0) @Max(999) Integer minor,
        @NotNull @Min(0) @Max(999) Integer patch,
        @NotEmpty @Valid List<ColumnRequest> columns) {
}