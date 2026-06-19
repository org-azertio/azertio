package org.azertio.plugins.webui;

import org.myjtools.imconfig.Config;
import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Scope;
import org.azertio.core.backend.ExecutionContext;
import org.azertio.core.contributors.StepProvider;

@Extension(
    name = "Web UI steps provider",
    scope = Scope.TRANSIENT,
    extensionPointVersion = "1.0"
)
public class WebUiStepProvider implements StepProvider {

    @Override
    public void init(Config config) {
    }

    protected String interpolate(String text) {
        return ExecutionContext.current().interpolateString(text);
    }

}