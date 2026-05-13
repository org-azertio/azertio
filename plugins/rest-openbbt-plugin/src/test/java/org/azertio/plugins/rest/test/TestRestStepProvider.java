package org.azertio.plugins.rest.test;

import org.junit.jupiter.api.Test;
import org.myjtools.imconfig.Config;
import org.azertio.core.AzertioRuntime;
import java.util.Map;

class TestRestStepProvider {

	@Test
	void restPluginLoadsConfiguration() {
		Config config = Config.ofMap(Map.of(
			"core.resourcePath", "src/test/resources",
			"rest.baseUrl", "http://localhost:8080"
		));
		AzertioRuntime runtime = new AzertioRuntime(config);
		// Verify runtime initializes and REST configuration is available
		assert runtime.configuration().getString("rest.baseUrl").isPresent();
	}

}
