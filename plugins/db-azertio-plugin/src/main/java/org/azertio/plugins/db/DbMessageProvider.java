package org.azertio.plugins.db;

import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Scope;
import org.azertio.core.messages.MessageProvider;
import org.azertio.core.messages.StepDocMessageAdapter;
import java.util.Map;

@Extension(
	name = "Database Message Provider",
	extensionPointVersion = "1.0",
	scope = Scope.SINGLETON
)
public class DbMessageProvider extends StepDocMessageAdapter implements MessageProvider {

	public DbMessageProvider() {
		super("steps.yaml");
	}

	@Override
	public boolean providerFor(String category) {
		return DbStepProvider.class.getSimpleName().equals(category);
	}


	@Override
	protected Map<String, String> languageResources() {
		return Map.of(
			"dsl", "steps_dsl.yaml",
			"en", "steps_en.yaml",
			"es", "steps_es.yaml"
		);
	}
}
