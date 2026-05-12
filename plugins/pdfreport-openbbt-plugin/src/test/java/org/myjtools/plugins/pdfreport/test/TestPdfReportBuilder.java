package org.myjtools.plugins.pdfreport.test;

import org.junit.jupiter.api.Test;
import org.myjtools.imconfig.Config;
import org.myjtools.openbbt.core.OpenBBTRuntime;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TestPdfReportBuilder {

    @Test
    void pluginLoadsConfiguration() {
        Config config = Config.ofMap(Map.of(
            "core.resourcePath", "src/test/resources",
            "core.resourceFilter", "**/*.feature"
        ));
        OpenBBTRuntime runtime = new OpenBBTRuntime(config);
        assertThat(runtime.configuration().getString("pdfreport.outputDir")).isPresent();
    }

}