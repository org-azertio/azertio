package org.azertio.cli.test;

import org.azertio.cli.MainCommand;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;

class VersionCommandTest {

    @Test
    void run_printsVersionAndExitsZero() {
        var capture = new ByteArrayOutputStream();
        PrintStream original = System.out;
        System.setOut(new PrintStream(capture));
        try {
            int exitCode = new CommandLine(new MainCommand()).execute("version");
            assertEquals(0, exitCode);
            assertThat(capture.toString().trim()).isNotEmpty();
        } finally {
            System.setOut(original);
        }
    }
}