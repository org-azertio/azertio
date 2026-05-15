package org.azertio.core;

import org.myjtools.imconfig.Config;
import org.myjtools.jexten.ModuleLayerProvider;
import org.myjtools.jexten.Version;
import org.myjtools.jexten.maven.artifactstore.MavenArtifactStore;
import org.myjtools.jexten.plugin.PluginID;
import org.myjtools.jexten.plugin.PluginManager;
import org.myjtools.mavenfetcher.MavenFetcherProperties;
import org.azertio.core.util.Log;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class AzertioPluginManager {


	private static final Log log = Log.of();

	private final PluginManager pluginManager;

	public AzertioPluginManager(Config config) {

		Path envPath = config.get(AzertioConfig.ENV_PATH, Path::of).orElse(AzertioConfig.ENV_DEFAULT_PATH);
		this.pluginManager = new PluginManager(
			"org.azertio",
			getClass().getClassLoader(),
			envPath.resolve(AzertioConfig.PLUGINS_PATH)
		);
		Properties mavenFetcherProperties = computeMavenFetcherProperties(config, envPath);
		this.pluginManager.setArtifactStore(new MavenArtifactStore().configure(mavenFetcherProperties));
		logPluginArtifacts(envPath.resolve(AzertioConfig.PLUGINS_PATH));
	}

	private void logPluginArtifacts(Path pluginsPath) {
		Path artifactsPath = pluginsPath.resolve("artifacts");
		if (!Files.isDirectory(artifactsPath)) {
			return;
		}
		try (var stream = Files.walk(artifactsPath)) {
			stream.filter(p -> p.toString().endsWith(".jar"))
				.forEach(jar -> log.info("[plugins] artifact: {}", jar.toAbsolutePath()));
		} catch (IOException e) {
			log.warn("[plugins] could not list artifacts: {}", e.getMessage());
		}
	}

	public ModuleLayerProvider moduleLayerProvider() {
		return pluginManager;
	}

	public String moduleLayerTreeDescription() {
		return pluginManager.moduleLayerTree().description();
	}

	public boolean installPlugin(String pluginNameExpression) {
		String pluginName;
		String runtimePluginConfig;
		if (pluginNameExpression.contains(" with ")) {
			pluginName = pluginNameExpression.split(" with ")[0].trim();
			runtimePluginConfig = pluginNameExpression.split(" with ")[1].trim();
		} else {
			pluginName = pluginNameExpression.trim();
			runtimePluginConfig = null;
		}
		if (!pluginName.contains(":")) {
			pluginName = "org.azertio.plugins:" + pluginName;
		}
		String[] parts = pluginName.split(":");
		String groupId = parts[0];
		String artifactId = parts[1];
		Version version = (parts.length > 2) ? Version.of(parts[2]) : null;
		PluginID pluginID = new PluginID(groupId, artifactId);
		if (pluginManager.plugins().contains(pluginID)) {
			log.info("Plugin {} is already installed.", pluginName);
			if (runtimePluginConfig != null) {
				for (String runtimeConfigEntry : runtimePluginConfig.split(",")) {
					installPluginRuntimeConfig(pluginID, runtimeConfigEntry);
				}
			}
			return true;
		}
		log.info("Installing plugin {} from artifact store...", pluginName);
		try {
			pluginManager.installPluginFromArtifactStore(pluginID, version);
			log.info("Plugin {} installed successfully.", pluginName);
			if (runtimePluginConfig != null) {
				for (String runtimeConfigEntry : runtimePluginConfig.split(",")) {
					installPluginRuntimeConfig(pluginID, runtimeConfigEntry);
				}

			}
			return true;
		} catch (Exception e) {
			log.error(e,"Failed to resolve plugin {}", pluginName);
			return false;
		}
	}


	private void installPluginRuntimeConfig(PluginID pluginID, String runtimeConfig) {
		log.info("Applying runtime config for plugin {}: {}", pluginID, runtimeConfig);
		try {
			String[] parts = runtimeConfig.split(":");
			String groupId = parts[0];
			String artifactId = parts[1];
			pluginManager.addRuntimeDependency(pluginID, groupId, artifactId);
			log.info("Runtime config applied successfully for plugin {}: {}", pluginID, runtimeConfig);
		} catch (Exception e) {
			log.error(e, "Failed to apply runtime config for plugin {}: {}", pluginID, runtimeConfig);
		}
	}



	private static Properties computeMavenFetcherProperties(Config config, Path envPath) {
		Properties artifactStoreProperties = new Properties();
		String localRepository = config.getString(AzertioConfig.ARTIFACTS_LOCAL_REPOSITORY)
			.orElseGet(() -> Path.of(System.getProperty("user.home"), ".m2", "repository").toAbsolutePath().toString());
		artifactStoreProperties.setProperty(MavenFetcherProperties.LOCAL_REPOSITORY, localRepository);
		config.getString(AzertioConfig.ARTIFACTS_REPOSITORY_PROXY_URL).ifPresent(
			url -> artifactStoreProperties.setProperty(MavenFetcherProperties.PROXY_URL, url)
		);
		config.getString(AzertioConfig.ARTIFACTS_REPOSITORY_PROXY_USERNAME).ifPresent(
			username -> artifactStoreProperties.setProperty(MavenFetcherProperties.PROXY_USERNAME, username)
		);
		config.getString(AzertioConfig.ARTIFACTS_REPOSITORY_PROXY_PASSWORD).ifPresent(
			password -> artifactStoreProperties.setProperty(MavenFetcherProperties.PROXY_PASSWORD, password)
		);
		config.getString(AzertioConfig.ARTIFACTS_REPOSITORY_URL).ifPresent(url -> {
			String username = config.getString(AzertioConfig.ARTIFACTS_REPOSITORY_USERNAME).orElse(null);
			String password = config.getString(AzertioConfig.ARTIFACTS_REPOSITORY_PASSWORD).orElse(null);
			String urlWithCredentials = "remote=" + url;
			if (username != null && password != null) {
				urlWithCredentials = urlWithCredentials + "[" + username + ":" + password + "]";
			}
			artifactStoreProperties.setProperty(MavenFetcherProperties.REMOTE_REPOSITORIES, urlWithCredentials);
			artifactStoreProperties.setProperty(MavenFetcherProperties.USE_DEFAULT_REMOTE_REPOSITORY, "false");
		});
		return artifactStoreProperties;
	}



}
