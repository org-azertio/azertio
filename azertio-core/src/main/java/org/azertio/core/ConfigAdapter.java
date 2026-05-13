package org.azertio.core;

import org.myjtools.imconfig.Config;
import org.azertio.core.contributors.ConfigProvider;
import org.azertio.core.util.Lazy;

/**
 * An abstract adapter for providing configuration resources.
 * This class implements the ConfigProvider interface and provides a mechanism
 * to load configuration definitions from a specified resource.

 * @author Luis Iñesta Gelabert - luiinge@gmail.com */
public abstract class ConfigAdapter implements ConfigProvider {

	private final Lazy<Config> config = Lazy.of(
		()-> Config.withDefinitions(Config.loadDefinitionsFromResource(resource(),getClass().getClassLoader()))
	);


	/**
	 * Returns the resource path from which the configuration definitions
	 * will be loaded.
	 *
	 * @return the resource path as a String
	 */
	protected abstract String resource();


	@Override
	public Config config() {
		return config.get();
	}


}
