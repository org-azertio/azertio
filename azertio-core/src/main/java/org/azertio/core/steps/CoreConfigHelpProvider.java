package org.azertio.core.steps;

import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Scope;
import org.azertio.core.contributors.HelpProvider;
import org.azertio.core.help.ConfigHelpAdapter;

@Extension(scope = Scope.SINGLETON)
public class CoreConfigHelpProvider extends ConfigHelpAdapter implements HelpProvider {

    public CoreConfigHelpProvider() {
        super("core.config", "Core Configuration", "Core Configuration", "core-config.yaml");
    }
}