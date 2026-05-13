package org.azertio.it;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.myjtools.imconfig.Config;
import org.azertio.core.AzertioConfig;
import org.azertio.core.AzertioPluginManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AzertioPluginManagerTest {

    private static final String PLUGIN = "org.azertio.plugins:rest-azertio-plugin:1.0.0-alpha1";
    private static final String RUNTIME_CONFIG_FILE = "plugins/manifests/org.azertio.plugins-rest-azertio-plugin.runtime.yaml";


    private AzertioPluginManager managerFor(Path envPath) {
        return new AzertioPluginManager(Config.ofMap(Map.of(AzertioConfig.ENV_PATH, envPath.toString())));
    }


    @Test
    void installPlugin_withoutRuntimeDependency_returnsTrueAndNoRuntimeConfig(@TempDir Path envDir) {
        boolean result = managerFor(envDir).installPlugin(PLUGIN);

        assertThat(result).isTrue();
        assertThat(envDir.resolve(RUNTIME_CONFIG_FILE)).doesNotExist();
    }


    @Test
    void installPlugin_withRuntimeDependency_returnsTrueAndCreatesRuntimeConfig(@TempDir Path envDir) throws IOException {
        boolean result = managerFor(envDir).installPlugin(PLUGIN + " with com.h2database:h2-2.2.224");

        assertThat(result).isTrue();
        Path runtimeConfig = envDir.resolve(RUNTIME_CONFIG_FILE);
        assertThat(runtimeConfig).exists();
        String content = Files.readString(runtimeConfig);
        assertThat(content)
            .contains("com.h2database")
            .contains("h2-2.2.224");
    }


    @Test
    void installPlugin_withMultipleRuntimeDependencies_registersAll(@TempDir Path envDir) throws IOException {
        boolean result = managerFor(envDir).installPlugin(
            PLUGIN + " with com.h2database:h2-2.2.224,com.h2database:h2-2.3.232"
        );

        assertThat(result).isTrue();
        Path runtimeConfig = envDir.resolve(RUNTIME_CONFIG_FILE);
        assertThat(runtimeConfig).exists();
        String content = Files.readString(runtimeConfig);
        assertThat(content)
            .contains("com.h2database")
            .contains("h2-2.2.224")
            .contains("h2-2.3.232");
    }

}