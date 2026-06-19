package org.azertio.core.test.backend;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.myjtools.imconfig.Config;
import org.azertio.core.AzertioConfig;
import org.azertio.core.AzertioRuntime;
import org.azertio.core.backend.ExecutionContext;
import org.azertio.core.backend.StepProviderBackend;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class TestExecutionContextInterpolation {

    static final Config CONFIG = Config.ofMap(Map.of(
        AzertioConfig.RESOURCE_PATH, "src/test/resources",
        AzertioConfig.ENV_PATH, "target/.azertio",
        "auth.tokenUrl", "http://localhost:9100/oauth2/token",
        "auth.clientId", "gateway",
        "auth.clientSecret", "secret"
    ));

    StepProviderBackend backend;

    @BeforeEach
    void setUp() {
        backend = new StepProviderBackend(new AzertioRuntime(CONFIG));
        backend.setUp(null, null, Map.of());
    }

    @AfterEach
    void tearDown() {
        backend.tearDown();
    }

    @Test
    void configPropertyIsResolvable() {
        assertThat(ExecutionContext.current().interpolateString("${auth.tokenUrl}"))
            .isEqualTo("http://localhost:9100/oauth2/token");
    }

    @Test
    void multipleConfigPropertiesAreResolvable() {
        assertThat(ExecutionContext.current().interpolateString("${auth.clientId}:${auth.clientSecret}"))
            .isEqualTo("gateway:secret");
    }

    @Test
    void explicitVariableOverridesConfigProperty() {
        ExecutionContext.current().setVariable("auth.clientId", "overridden");
        assertThat(ExecutionContext.current().interpolateString("${auth.clientId}"))
            .isEqualTo("overridden");
    }

    @Test
    void unknownPlaceholderIsLeftUnchanged() {
        assertThat(ExecutionContext.current().interpolateString("${unknown.key}"))
            .isEqualTo("${unknown.key}");
    }

    @Test
    void explicitVariableStillResolvable() {
        ExecutionContext.current().setVariable("myVar", "myValue");
        assertThat(ExecutionContext.current().interpolateString("${myVar}"))
            .isEqualTo("myValue");
    }
}