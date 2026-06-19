package org.azertio.plugins.webui;

import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Scope;
import org.azertio.core.contributors.HelpProvider;
import org.azertio.core.help.StepHelpAdapter;

import java.util.Map;

@Extension(scope = Scope.SINGLETON)
public class WebUiStepHelpProvider extends StepHelpAdapter implements HelpProvider {

    public WebUiStepHelpProvider() {
        super("webui.steps", "Web UI Steps", "Web UI Steps", "steps.yaml", Map.of(
            "dsl", "steps_dsl.yaml",
            "en",  "steps_en.yaml",
            "es",  "steps_es.yaml"
        ));
    }

}