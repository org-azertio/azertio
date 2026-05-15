package org.azertio.plugins.db;

import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Scope;
import org.azertio.core.contributors.HelpProvider;
import org.azertio.core.help.ConfigHelpAdapter;

@Extension(scope = Scope.SINGLETON)
public class DbConfigHelpProvider extends ConfigHelpAdapter implements HelpProvider {

    public DbConfigHelpProvider() {
        super("db.config", "Database Configuration", "Database Configuration", "config.yaml");
    }
}