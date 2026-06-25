package org.azertio.plugins.messaging.test;

import org.junit.jupiter.api.Test;
import org.myjtools.imconfig.Config;
import org.azertio.core.AzertioRuntime;

import java.util.Map;

class TestMessagingStepProvider {

    @Test
    void pluginLoadsConfiguration() {
        Config config = Config.ofMap(Map.of(
            "core.resourcePath", "src/test/resources",
            "core.resourceFilter", "**/*.feature"
        ));
        AzertioRuntime runtime = new AzertioRuntime(config);
        assert runtime != null;
    }

}