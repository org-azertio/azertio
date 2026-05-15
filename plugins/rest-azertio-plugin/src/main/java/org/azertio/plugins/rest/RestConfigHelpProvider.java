package org.azertio.plugins.rest;

import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Scope;
import org.azertio.core.contributors.HelpProvider;
import org.azertio.core.help.ConfigHelpAdapter;

@Extension(scope = Scope.SINGLETON)
public class RestConfigHelpProvider extends ConfigHelpAdapter implements HelpProvider {

    @Override public String id()          { return "rest.config"; }
    @Override public String displayName() { return "REST Configuration"; }
    @Override protected String title()    { return "REST Configuration"; }

    @Override
    protected String resource() {
        return "config.yaml";
    }
}