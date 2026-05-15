package org.azertio.core.steps;

import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Scope;
import org.azertio.core.contributors.HelpProvider;
import org.azertio.core.help.StepHelpAdapter;

import java.util.Map;

@Extension(scope = Scope.SINGLETON)
public class CoreStepHelpProvider extends StepHelpAdapter implements HelpProvider {

    @Override public String id()          { return "core.steps"; }
    @Override public String displayName() { return "Core Steps"; }
    @Override protected String title()    { return "Core Steps"; }

    @Override
    protected String resource() {
        return "core-steps.yaml";
    }

    @Override
    protected Map<String, String> languageResources() {
        return Map.of(
            "dsl", "core-steps_dsl.yaml",
            "en",  "core-steps_en.yaml",
            "es",  "core-steps_es.yaml"
        );
    }
}