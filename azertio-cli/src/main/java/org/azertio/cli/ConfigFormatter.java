package org.azertio.cli;

import org.myjtools.imconfig.Config;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

final class ConfigFormatter {

	private static final String MASKED_VALUE = "<masked>";
	private static final String UNDEFINED_VALUE = "<undefined>";
	private static final List<String> SECRET_KEY_MARKERS = List.of(
		"password",
		"secret",
		"token",
		"credential",
		"api-key",
		"apikey",
		"private-key",
		"privatekey"
	);

	private ConfigFormatter() {
	}

	static String toMaskedString(Config config) {
		return config.keys()
			.sorted()
			.map(key -> key + " : " + maskedValue(config, key))
			.collect(Collectors.joining("\n", "configuration:\n---------------\n", "\n---------------"));
	}

	private static String maskedValue(Config config, String key) {
		List<String> values = config.getList(key, String.class);
		if (values.isEmpty()) {
			return UNDEFINED_VALUE;
		}
		if (isSecretKey(key)) {
			return MASKED_VALUE;
		}
		return values.size() == 1 ? values.getFirst() : values.toString();
	}

	private static boolean isSecretKey(String key) {
		String normalizedKey = key.toLowerCase(Locale.ROOT);
		return SECRET_KEY_MARKERS.stream().anyMatch(normalizedKey::contains);
	}
}
