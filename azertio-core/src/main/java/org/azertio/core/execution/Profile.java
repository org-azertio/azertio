package org.azertio.core.execution;

import org.myjtools.imconfig.Config;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record Profile(String name, Map<String,String> properties) {

	private static final Pattern pattern = Pattern.compile("\\$\\{([^}]+)}");

	public static final Profile NONE = new Profile("", Map.of());

	public Config applyProfile(Config baseConfig) {

		if (properties == null || properties.isEmpty()) {
			return baseConfig;
		}

		Map<String, String> resolved = new LinkedHashMap<>();
		baseConfig.forEach((key, value) -> {
			var matcher = pattern.matcher(value);
			resolved.put(key, matcher.replaceAll(match -> {
				String placeholder = match.group(1);
				String replacement = properties.getOrDefault(placeholder, match.group(0));
				return Matcher.quoteReplacement(replacement);
			}));
		});
		return Config.ofMap(resolved);
	}

}
