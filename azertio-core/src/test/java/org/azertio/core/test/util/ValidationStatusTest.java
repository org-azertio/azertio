package org.azertio.core.test.util;

import org.azertio.core.testplan.ValidationStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidationStatusTest {

    @Test
    void of_returnsOkForZero() {
        assertThat(ValidationStatus.of(0)).isEqualTo(ValidationStatus.OK);
    }

    @Test
    void of_returnsErrorForOne() {
        assertThat(ValidationStatus.of(1)).isEqualTo(ValidationStatus.ERROR);
    }

    @Test
    void of_throwsForUnknownValue() {
        assertThatThrownBy(() -> ValidationStatus.of(-1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("-1");
    }

    @Test
    void ok_hasIntValueZero() {
        assertThat(ValidationStatus.OK.value).isEqualTo(0);
    }

    @Test
    void error_hasIntValueOne() {
        assertThat(ValidationStatus.ERROR.value).isEqualTo(1);
    }
}