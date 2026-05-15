package org.azertio.core.steps;

import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Scope;
import org.azertio.core.contributors.HelpProvider;
import org.azertio.core.help.ConfigHelpAdapter;

@Extension(scope = Scope.SINGLETON)
public class CoreConfigHelpProvider extends ConfigHelpAdapter implements HelpProvider {

    @Override public String id()          { return "core.config"; }
    @Override public String displayName() { return "Core Configuration"; }
    @Override protected String title()    { return "Core Configuration"; }

    @Override
    protected String resource() {
        return "core-config.yaml";
    }
}