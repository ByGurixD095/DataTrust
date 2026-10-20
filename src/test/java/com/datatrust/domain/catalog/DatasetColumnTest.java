package com.datatrust.domain.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.datatrust.domain.enums.ColumnType;
import com.datatrust.domain.enums.DatasetErrorCode;

class DatasetColumnTest {

    @Test
    void createsColumnStrippingName() {
        DatasetColumn column = DatasetColumn.create("  email ", 1, ColumnType.STRING, true);

        assertThat(column.name()).isEqualTo("email");
        assertThat(column.position()).isEqualTo(1);
        assertThat(column.required()).isTrue();
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> DatasetColumn.create(" ", 1, ColumnType.STRING, true))
                .isInstanceOf(DatasetValidationException.class)
                .satisfies(ex -> assertThat(((DatasetValidationException) ex).code())
                        .isEqualTo(DatasetErrorCode.NAME_BLANK));
    }

    @Test
    void rejectsPositionBelowOne() {
        assertThatThrownBy(() -> DatasetColumn.create("id", 0, ColumnType.INTEGER, true))
                .isInstanceOf(DatasetValidationException.class)
                .satisfies(ex -> assertThat(((DatasetValidationException) ex).code())
                        .isEqualTo(DatasetErrorCode.COLUMN_POSITION_INVALID));
    }
}