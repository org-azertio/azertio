package org.azertio.plugins.pdfreport;

import org.myjtools.jexten.Extension;
import org.azertio.core.ConfigAdapter;
import org.azertio.core.contributors.ConfigProvider;

@Extension(extensionPointVersion = "1.0")
public class PdfReportConfigProvider extends ConfigAdapter implements ConfigProvider {

    @Override
    protected String resource() {
        return "config.yaml";
    }

}