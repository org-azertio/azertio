package org.azertio.plugins.db;

import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Scope;
import org.azertio.core.contributors.HelpProvider;
import org.azertio.core.help.StepHelpAdapter;

import java.util.Map;

@Extension(scope = Scope.SINGLETON)
public class DbStepHelpProvider extends StepHelpAdapter implements HelpProvider {

    public DbStepHelpProvider() {
        super("db.steps", "Database Steps", "Database Steps", "steps.yaml", Map.of(
            "dsl", "steps_dsl.yaml",
            "en",  "steps_en.yaml",
            "es",  "steps_es.yaml"
        ));
    }
}