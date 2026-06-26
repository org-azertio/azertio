package org.azertio.plugins.messaging;

import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Scope;
import org.azertio.core.contributors.HelpProvider;
import org.azertio.core.help.ConfigHelpAdapter;

@Extension(scope = Scope.SINGLETON)
public class MessagingConfigHelpProvider extends ConfigHelpAdapter implements HelpProvider {

    public MessagingConfigHelpProvider() {
        super("messaging.config", "Messaging Configuration", "Messaging Configuration", "config.yaml");
    }

}