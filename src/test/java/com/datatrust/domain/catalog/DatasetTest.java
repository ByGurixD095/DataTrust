package com.datatrust.domain.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.datatrust.domain.enums.DatasetErrorCode;

class DatasetTest {

    private static final UUID OWNER = UUID.randomUUID();

    @Test
    void createsStableDatasetIdentityWithCurrentVersionId() {
        UUID currentVersionId = UUID.randomUUID();

        Dataset dataset = Dataset.create("clientes", "descripción", OWNER, currentVersionId);

        assertThat(dataset.currentVersionId()).isEqualTo(currentVersionId);
        assertThat(dataset.name()).isEqualTo("clientes");
    }

    @Test
    void stripsSurroundingSpacesFromName() {
        Dataset dataset = Dataset.create("  clientes  ", "descripción", OWNER, UUID.randomUUID());

        assertThat(dataset.name()).isEqualTo("clientes");
    }

    @Test
    void rejectsBlankName() {
        assertCode(DatasetErrorCode.NAME_BLANK,
                () -> Dataset.create("   ", "descripción", OWNER, UUID.randomUUID()));
    }

    @Test
    void rejectsNameLongerThan255Characters() {
        assertCode(DatasetErrorCode.NAME_TOO_LONG,
                () -> Dataset.create("a".repeat(256), "descripción", OWNER, UUID.randomUUID()));
    }

    @Test
    void rejectsBlankDescription() {
        assertCode(DatasetErrorCode.DESCRIPTION_BLANK,
                () -> Dataset.create("clientes", " ", OWNER, UUID.randomUUID()));
    }

    @Test
    void restoreDoesNotValidate() {
        Dataset dataset = Dataset.restore(UUID.randomUUID(), "x".repeat(300), " ",
                OWNER, UUID.randomUUID());

        assertThat(dataset.name()).hasSize(300);
    }

    private static void assertCode(DatasetErrorCode expected, Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(DatasetValidationException.class)
                .satisfies(ex -> assertThat(((DatasetValidationException) ex).code())
                        .isEqualTo(expected));
    }
}
