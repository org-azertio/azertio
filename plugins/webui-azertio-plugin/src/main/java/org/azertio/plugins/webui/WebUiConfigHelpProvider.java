package org.azertio.plugins.webui;

import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Scope;
import org.azertio.core.contributors.HelpProvider;
import org.azertio.core.help.ConfigHelpAdapter;

@Extension(scope = Scope.SINGLETON)
public class WebUiConfigHelpProvider extends ConfigHelpAdapter implements HelpProvider {

    public WebUiConfigHelpProvider() {
        super("webui.config", "Web UI Configuration", "Web UI Configuration", "config.yaml");
    }

}