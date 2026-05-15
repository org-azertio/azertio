package org.azertio.plugins.rest;

import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Scope;
import org.azertio.core.contributors.HelpProvider;
import org.azertio.core.help.StepHelpAdapter;

import java.util.Map;

@Extension(scope = Scope.SINGLETON)
public class RestStepHelpProvider extends StepHelpAdapter implements HelpProvider {

    public RestStepHelpProvider() {
        super("rest.steps", "REST Steps", "REST Steps", "steps.yaml", Map.of(
            "dsl", "steps_dsl.yaml",
            "en",  "steps_en.yaml",
            "es",  "steps_es.yaml"
        ));
    }
}