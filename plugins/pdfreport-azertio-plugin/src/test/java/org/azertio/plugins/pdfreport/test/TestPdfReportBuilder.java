package org.azertio.plugins.pdfreport.test;

import org.junit.jupiter.api.Test;
import org.myjtools.imconfig.Config;
import org.azertio.core.AzertioRuntime;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TestPdfReportBuilder {

    @Test
    void pluginLoadsConfiguration() {
        Config config = Config.ofMap(Map.of(
            "core.resourcePath", "src/test/resources",
            "core.resourceFilter", "**/*.feature"
        ));
        AzertioRuntime runtime = new AzertioRuntime(config);
        assertThat(runtime.configuration().getString("pdfreport.outputDir")).isPresent();
    }

}