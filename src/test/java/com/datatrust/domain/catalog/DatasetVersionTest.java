package com.datatrust.domain.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.datatrust.domain.enums.ColumnType;
import com.datatrust.domain.enums.DatasetErrorCode;
import com.datatrust.domain.enums.DatasetFormat;

class DatasetVersionTest {

    private static final UUID DATASET_ID = UUID.randomUUID();
    private static final UUID CREATOR = UUID.randomUUID();

    @Test
    void createsVersionNumberAndImmutableSchemaSnapshot() {
        DatasetVersion version = validVersion(1, 2, 3, validColumns());

        assertThat(version.versionNumber()).isEqualTo("1.2.3");
        assertThat(version.schemaSnapshot().columns()).hasSize(2);
        assertThatThrownBy(() -> version.schemaSnapshot().columns().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsDatasetWithoutColumns() {
        assertCode(DatasetErrorCode.NO_COLUMNS,
                () -> validVersion(1, 0, 0, List.of()));
    }

    @Test
    void rejectsColumnNamesRepeatedIgnoringCase() {
        List<DatasetColumn> columns = List.of(
                DatasetColumn.create("email", 1, ColumnType.STRING, true),
                DatasetColumn.create("EMAIL", 2, ColumnType.STRING, false));

        assertCode(DatasetErrorCode.COLUMN_NAME_DUPLICATED,
                () -> validVersion(1, 0, 0, columns));
    }

    @Test
    void rejectsDuplicatedPositions() {
        List<DatasetColumn> columns = List.of(
                DatasetColumn.create("id", 1, ColumnType.INTEGER, true),
                DatasetColumn.create("nombre", 1, ColumnType.STRING, true));

        assertCode(DatasetErrorCode.COLUMN_POSITION_INVALID,
                () -> validVersion(1, 0, 0, columns));
    }

    @Test
    void rejectsPositionsWithGaps() {
        List<DatasetColumn> columns = List.of(
                DatasetColumn.create("id", 1, ColumnType.INTEGER, true),
                DatasetColumn.create("nombre", 3, ColumnType.STRING, true));

        assertCode(DatasetErrorCode.COLUMN_POSITION_INVALID,
                () -> validVersion(1, 0, 0, columns));
    }

    @Test
    void rejectsMinorAbove999() {
        assertCode(DatasetErrorCode.VERSION_INVALID,
                () -> validVersion(1, 1000, 0, validColumns()));
    }

    @Test
    void rejectsPatchAbove999() {
        assertCode(DatasetErrorCode.VERSION_INVALID,
                () -> validVersion(1, 0, 1000, validColumns()));
    }

    @Test
    void rejectsMajorAboveIntegerLimit() {
        assertCode(DatasetErrorCode.VERSION_INVALID,
                () -> validVersion(2147, 0, 0, validColumns()));
    }

    @Test
    void acceptsMaximumMajor() {
        DatasetVersion version = validVersion(2146, 999, 999, validColumns());

        assertThat(version.versionNumber()).isEqualTo("2146.999.999");
    }

    private static DatasetVersion validVersion(int major, int minor, int patch,
                                               List<DatasetColumn> columns) {
        return DatasetVersion.create(DATASET_ID, major, minor, patch,
                DatasetFormat.CSV, columns, CREATOR);
    }

    private static List<DatasetColumn> validColumns() {
        return List.of(
                DatasetColumn.create("id", 1, ColumnType.INTEGER, true),
                DatasetColumn.create("nombre", 2, ColumnType.STRING, true));
    }

    private static void assertCode(DatasetErrorCode expected, Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(DatasetValidationException.class)
                .satisfies(ex -> assertThat(((DatasetValidationException) ex).code())
                        .isEqualTo(expected));
    }
}
