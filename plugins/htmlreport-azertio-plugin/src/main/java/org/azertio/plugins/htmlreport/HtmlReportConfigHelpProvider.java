package org.azertio.plugins.htmlreport;

import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Scope;
import org.azertio.core.contributors.HelpProvider;
import org.azertio.core.help.ConfigHelpAdapter;

@Extension(scope = Scope.SINGLETON)
public class HtmlReportConfigHelpProvider extends ConfigHelpAdapter implements HelpProvider {

    public HtmlReportConfigHelpProvider() {
        super("htmlreport.config", "HTML Report Configuration", "HTML Report Configuration", "config.yaml");
    }
}