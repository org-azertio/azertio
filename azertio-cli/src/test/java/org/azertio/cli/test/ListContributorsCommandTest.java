package org.azertio.cli.test;

import org.junit.jupiter.api.Test;
import org.azertio.cli.MainCommand;
import org.azertio.core.AzertioConfig;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ListContributorsCommandTest {

    static final String ENV_PATH = "target/.azertio-listcontributors";

    static final String[] BASE_ARGS = {
        "-f", "src/test/resources/azertio.yaml",
        "-D" + AzertioConfig.ENV_PATH + "=" + ENV_PATH
    };

    @Test
    void showHelp() {
        int exitCode = new CommandLine(new MainCommand()).execute(
            args("list-contributors", "--help")
        );
        assertEquals(0, exitCode);
    }

    @Test
    void listContributorsText() {
        int exitCode = new CommandLine(new MainCommand()).execute(
            args("list-contributors")
        );
        assertEquals(0, exitCode);
    }

    @Test
    void listContributorsJson() {
        int exitCode = new CommandLine(new MainCommand()).execute(
            args("list-contributors", "--json")
        );
        assertEquals(0, exitCode);
    }

    static String[] args(String... extra) {
        List<String> all = new ArrayList<>(Arrays.asList(extra));
        all.addAll(Arrays.asList(BASE_ARGS));
        return all.toArray(String[]::new);
    }
}
