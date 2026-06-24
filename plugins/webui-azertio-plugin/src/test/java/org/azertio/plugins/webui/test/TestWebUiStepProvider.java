package org.azertio.plugins.webui.test;

import org.junit.jupiter.api.Test;
import org.myjtools.imconfig.Config;
import org.azertio.core.AzertioRuntime;

import java.util.Map;

class TestWebUiStepProvider {

    @Test
    void webUiPluginLoadsConfiguration() {
        Config config = Config.ofMap(Map.of(
            "core.resourcePath", "src/test/resources",
            "webui.baseURL", "http://localhost"
        ));
        AzertioRuntime runtime = new AzertioRuntime(config);
        assert runtime.configuration().getString("webui.baseURL").isPresent();
    }

}