package org.azertio.cli.test;

import org.junit.jupiter.api.Test;
import org.azertio.cli.MainCommand;
import org.azertio.core.AzertioConfig;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ManCommandTest {

    static final String ENV_PATH = "target/.azertio-man";

    static final String[] BASE_ARGS = {
        "-f", "src/test/resources/azertio.yaml",
        "-D" + AzertioConfig.ENV_PATH + "=" + ENV_PATH
    };

    @Test
    void showHelp() {
        int exitCode = new CommandLine(new MainCommand()).execute(
            args("man", "--help")
        );
        assertEquals(0, exitCode);
    }

    @Test
    void listTopicsText() {
        int exitCode = new CommandLine(new MainCommand()).execute(
            args("man")
        );
        assertEquals(0, exitCode);
    }

    @Test
    void listTopicsJson() {
        int exitCode = new CommandLine(new MainCommand()).execute(
            args("man", "--json")
        );
        assertEquals(0, exitCode);
    }

    @Test
    void unknownTopicReturnsError() {
        int exitCode = new CommandLine(new MainCommand()).execute(
            args("man", "nonexistent-topic")
        );
        assertEquals(1, exitCode);
    }

    @Test
    void showTopicText() {
        int exitCode = new CommandLine(new MainCommand()).execute(
            args("man", "test-topic")
        );
        assertEquals(0, exitCode);
    }

    @Test
    void showTopicJson() {
        int exitCode = new CommandLine(new MainCommand()).execute(
            args("man", "test-topic", "--json")
        );
        assertEquals(0, exitCode);
    }

    static String[] args(String... extra) {
        List<String> all = new ArrayList<>(Arrays.asList(extra));
        all.addAll(Arrays.asList(BASE_ARGS));
        return all.toArray(String[]::new);
    }
}