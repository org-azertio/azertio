package org.azertio.core.test.util;

import org.azertio.core.Clock;
import org.azertio.core.util.FileUtil;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class FileUtilTest {

    @Test
    void createSafeTempDirectory_createsWritableDirectory() throws IOException {
        Path dir = FileUtil.createSafeTempDirectory("azertio-test-");
        try {
            assertThat(dir).exists().isDirectory();
            assertThat(dir.getFileName().toString()).startsWith("azertio-test-");
        } finally {
            Files.deleteIfExists(dir);
        }
    }

    @Test
    void createSafeTempFile_createsFileInsideTempDir() throws IOException {
        Path file = FileUtil.createSafeTempFile("azertio-", ".tmp");
        try {
            assertThat(file).exists().isRegularFile();
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(file.getParent());
        }
    }

    @Test
    void resolvePattern_replacesAllDateTimeTokens() {
        Clock clock = new Clock() {
            @Override public Instant now() {
                return ZonedDateTime.of(2024, 3, 5, 8, 7, 6, 0, ZoneId.of("UTC")).toInstant();
            }
            @Override public ZoneId zone() { return ZoneId.of("UTC"); }
        };

        String result = FileUtil.resolvePattern("%Y-%m-%d_%h:%M:%s", clock);

        assertThat(result).isEqualTo("2024-03-05_08:07:06");
    }

    @Test
    void resolvePattern_withNoTokens_returnsPatternUnchanged() {
        Clock clock = () -> Instant.now();
        assertThat(FileUtil.resolvePattern("static-name", clock)).isEqualTo("static-name");
    }
}