package org.azertio.core.test.util;

import org.junit.jupiter.api.Test;
import org.azertio.core.util.ClassFinder;

import static org.assertj.core.api.Assertions.assertThat;

class TestClassFinder {

    @Test
    void find_knownClass_returnsItViaClassForName() {
        Class<?> found = ClassFinder.find("java.lang.String", TestClassFinder.class);
        assertThat(found).isEqualTo(String.class);
    }

    @Test
    void find_unknownClass_returnsNull() {
        Class<?> found = ClassFinder.find("com.example.DoesNotExist", TestClassFinder.class);
        assertThat(found).isNull();
    }

}
