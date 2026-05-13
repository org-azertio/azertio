package org.azertio.plugins.rest;

import org.myjtools.jexten.Extension;
import org.azertio.core.ConfigAdapter;
import org.azertio.core.contributors.ConfigProvider;

@Extension
public class RestConfigProvider extends ConfigAdapter implements ConfigProvider {
	
	@Override
	protected String resource() {
		return "config.yaml";
	}

}
