package org.azertio.plugins.db;

import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Scope;
import org.azertio.core.contributors.HelpProvider;
import org.azertio.core.help.StepHelpAdapter;

import java.util.Map;

@Extension(scope = Scope.SINGLETON)
public class DbStepHelpProvider extends StepHelpAdapter implements HelpProvider {

    @Override public String id()          { return "db.steps"; }
    @Override public String displayName() { return "Database Steps"; }
    @Override protected String title()    { return "Database Steps"; }

    @Override
    protected String resource() {
        return "steps.yaml";
    }

    @Override
    protected Map<String, String> languageResources() {
        return Map.of(
            "dsl", "steps_dsl.yaml",
            "en",  "steps_en.yaml",
            "es",  "steps_es.yaml"
        );
    }
}