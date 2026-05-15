package org.azertio.plugins.db;

import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Scope;
import org.azertio.core.contributors.HelpProvider;
import org.azertio.core.help.ConfigHelpAdapter;

@Extension(scope = Scope.SINGLETON)
public class DbConfigHelpProvider extends ConfigHelpAdapter implements HelpProvider {

    @Override public String id()          { return "db.config"; }
    @Override public String displayName() { return "Database Configuration"; }
    @Override protected String title()    { return "Database Configuration"; }

    @Override
    protected String resource() {
        return "config.yaml";
    }
}