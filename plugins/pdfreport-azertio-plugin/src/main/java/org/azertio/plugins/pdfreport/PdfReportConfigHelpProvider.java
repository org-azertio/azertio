package org.azertio.plugins.pdfreport;

import org.azertio.core.contributors.HelpProvider;
import org.azertio.core.help.ConfigHelpAdapter;
import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Scope;

@Extension(scope = Scope.SINGLETON, extensionPointVersion = "1.0")
public class PdfReportConfigHelpProvider extends ConfigHelpAdapter implements HelpProvider {

	public PdfReportConfigHelpProvider() {
		super("pdfreport.config", "PDF Report Configuration", "PDF Report Configuration", "config.yaml");
	}
}
