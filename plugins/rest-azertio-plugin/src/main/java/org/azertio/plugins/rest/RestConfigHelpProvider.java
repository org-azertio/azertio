package org.azertio.plugins.rest;

import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Scope;
import org.azertio.core.contributors.HelpProvider;
import org.azertio.core.help.ConfigHelpAdapter;

@Extension(scope = Scope.SINGLETON)
public class RestConfigHelpProvider extends ConfigHelpAdapter implements HelpProvider {

    public RestConfigHelpProvider() {
        super("rest.config", "REST Configuration", "REST Configuration", "config.yaml");
    }
}