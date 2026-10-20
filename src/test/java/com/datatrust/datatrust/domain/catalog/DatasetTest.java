package com.datatrust.datatrust.domain.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.datatrust.datatrust.domain.enums.ColumnType;
import com.datatrust.datatrust.domain.enums.DatasetErrorCode;
import com.datatrust.datatrust.domain.enums.DatasetFormat;

class DatasetTest {

    private static final UUID OWNER = UUID.randomUUID();

    @Test
    void createsDatasetWithEncodedVersion() {
        Dataset dataset = validDataset("clientes", 1, 2, 3, validColumns());

        assertThat(dataset.version()).isEqualTo(1_002_003);
        assertThat(dataset.versionText()).isEqualTo("1.2.3");
    }

    @Test
    void stripsSurroundingSpacesFromName() {
        Dataset dataset = validDataset("  clientes  ", 1, 0, 0, validColumns());

        assertThat(dataset.name()).isEqualTo("clientes");
    }

    @Test
    void rejectsBlankName() {
        assertCode(DatasetErrorCode.NAME_BLANK,
                () -> validDataset("   ", 1, 0, 0, validColumns()));
    }

    @Test
    void rejectsNameLongerThan255Characters() {
        String longName = "a".repeat(256);

        assertCode(DatasetErrorCode.NAME_TOO_LONG,
                () -> validDataset(longName, 1, 0, 0, validColumns()));
    }

    @Test
    void rejectsBlankDescription() {
        assertCode(DatasetErrorCode.DESCRIPTION_BLANK,
                () -> Dataset.create("clientes", " ", OWNER, DatasetFormat.CSV,
                        1, 0, 0, validColumns()));
    }

    @Test
    void rejectsDatasetWithoutColumns() {
        assertCode(DatasetErrorCode.NO_COLUMNS,
                () -> validDataset("clientes", 1, 0, 0, List.of()));
    }

    @Test
    void rejectsColumnNamesRepeatedIgnoringCase() {
        List<DatasetColumn> columns = List.of(
                DatasetColumn.create("email", 1, ColumnType.STRING, true),
                DatasetColumn.create("EMAIL", 2, ColumnType.STRING, false));

        assertCode(DatasetErrorCode.COLUMN_NAME_DUPLICATED,
                () -> validDataset("clientes", 1, 0, 0, columns));
    }

    @Test
    void rejectsDuplicatedPositions() {
        List<DatasetColumn> columns = List.of(
                DatasetColumn.create("id", 1, ColumnType.INTEGER, true),
                DatasetColumn.create("nombre", 1, ColumnType.STRING, true));

        assertCode(DatasetErrorCode.COLUMN_POSITION_INVALID,
                () -> validDataset("clientes", 1, 0, 0, columns));
    }

    @Test
    void rejectsPositionsWithGaps() {
        List<DatasetColumn> columns = List.of(
                DatasetColumn.create("id", 1, ColumnType.INTEGER, true),
                DatasetColumn.create("nombre", 3, ColumnType.STRING, true));

        assertCode(DatasetErrorCode.COLUMN_POSITION_INVALID,
                () -> validDataset("clientes", 1, 0, 0, columns));
    }

    @Test
    void acceptsColumnsGivenInAnyOrderWhenPositionsAreConsecutive() {
        List<DatasetColumn> columns = List.of(
                DatasetColumn.create("nombre", 2, ColumnType.STRING, true),
                DatasetColumn.create("id", 1, ColumnType.INTEGER, true));

        Dataset dataset = validDataset("clientes", 1, 0, 0, columns);

        assertThat(dataset.columns()).hasSize(2);
    }

    @Test
    void rejectsMinorAbove999() {
        assertCode(DatasetErrorCode.VERSION_INVALID,
                () -> validDataset("clientes", 1, 1000, 0, validColumns()));
    }

    @Test
    void rejectsPatchAbove999() {
        assertCode(DatasetErrorCode.VERSION_INVALID,
                () -> validDataset("clientes", 1, 0, 1000, validColumns()));
    }

    @Test
    void rejectsMajorAboveIntegerLimit() {
        // 2147 * 1_000_000 no cabe en un INTEGER de PostgreSQL
        assertCode(DatasetErrorCode.VERSION_INVALID,
                () -> validDataset("clientes", 2147, 0, 0, validColumns()));
    }

    @Test
    void acceptsMaximumMajor() {
        Dataset dataset = validDataset("clientes", 2146, 999, 999, validColumns());

        assertThat(dataset.version()).isEqualTo(2_146_999_999);
    }

    @Test
    void restoreDoesNotValidate() {
        // restore reconstruye datos ya guardados: no debe lanzar aunque la regla haya cambiado
        Dataset dataset = Dataset.restore(UUID.randomUUID(), "x".repeat(300), " ", OWNER,
                DatasetFormat.CSV, 1_000_000, java.time.Instant.now(), List.of());

        assertThat(dataset.name()).hasSize(300);
    }

    private static Dataset validDataset(String name, int major, int minor, int patch,
                                        List<DatasetColumn> columns) {
        return Dataset.create(name, "descripción de prueba", OWNER, DatasetFormat.CSV,
                major, minor, patch, columns);
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