package org.myjtools.openbbt.plugins.pdfreport;

import org.myjtools.jexten.Extension;
import org.myjtools.openbbt.core.ConfigAdapter;
import org.myjtools.openbbt.core.contributors.ConfigProvider;

@Extension(extensionPointVersion = "1.0")
public class PdfReportConfigProvider extends ConfigAdapter implements ConfigProvider {

    @Override
    protected String resource() {
        return "config.yaml";
    }

}