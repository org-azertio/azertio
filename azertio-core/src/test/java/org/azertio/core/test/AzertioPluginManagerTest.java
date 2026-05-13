package org.azertio.core.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.myjtools.imconfig.Config;
import org.azertio.core.AzertioConfig;
import org.azertio.core.AzertioPluginManager;
import java.nio.file.Path;
import java.util.Map;

class AzertioPluginManagerTest {

	@TempDir
	Path tempDir;

	@Test
	void loadPlugins() {
		AzertioPluginManager pluginManager = new AzertioPluginManager(Config.ofMap(Map.of(
			AzertioConfig.ENV_PATH, tempDir.toAbsolutePath()
		)));
		pluginManager.installPlugin("plugin");
	}
}
