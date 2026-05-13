package org.azertio.core.test.messages;

import org.junit.jupiter.api.Test;
import org.azertio.core.messages.MessageProvider;
import org.azertio.core.messages.StepDocMessageAdapter;
import java.util.Locale;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class TestStepDocMessageAdapter {


	@Test
	void test() {
		MessageProvider provider = new StepDocMessageAdapter("example-step-doc.yaml") {
			@Override
			public boolean providerFor(String category) {
				return true;
			}
			@Override
			protected Map<String, String> languageResources() {
				return Map.of();
			}
		};
		assertThat(provider.messages(Locale.ENGLISH).orElseThrow().get("rest.request.GET")).hasToString("I make a GET request to {endpoint:text}");
	}
}
