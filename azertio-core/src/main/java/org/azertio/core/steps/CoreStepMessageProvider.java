package org.azertio.core.steps;

import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Scope;
import org.azertio.core.messages.MessageProvider;
import org.azertio.core.messages.StepDocMessageAdapter;
import java.util.LinkedHashMap;
import java.util.Map;

@Extension(
	name = "Core Step Message Provider",
	extensionPointVersion = "1.0",
	scope = Scope.SINGLETON
)
public class CoreStepMessageProvider extends StepDocMessageAdapter implements MessageProvider {

	public CoreStepMessageProvider() {
		super("core-steps.yaml");
	}

	@Override
	public boolean providerFor(String category) {
		return CoreStepProvider.class.getSimpleName().equals(category);
	}

	@Override
	protected Map<String, String> languageResources() {
		var map = new LinkedHashMap<String, String>();
		map.put("dsl", "core-steps_dsl.yaml");
		map.put("en",  "core-steps_en.yaml");
		map.put("es",  "core-steps_es.yaml");
		return map;
	}

}
