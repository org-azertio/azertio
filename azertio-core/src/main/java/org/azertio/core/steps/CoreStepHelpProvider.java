package org.azertio.core.steps;

import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Scope;
import org.azertio.core.contributors.HelpProvider;
import org.azertio.core.help.StepHelpAdapter;

import java.util.Map;

@Extension(scope = Scope.SINGLETON)
public class CoreStepHelpProvider extends StepHelpAdapter implements HelpProvider {

    public CoreStepHelpProvider() {
        super("core.steps", "Core Steps", "Core Steps", "core-steps.yaml", Map.of(
            "dsl", "core-steps_dsl.yaml",
            "en",  "core-steps_en.yaml",
            "es",  "core-steps_es.yaml"
        ));
    }
}