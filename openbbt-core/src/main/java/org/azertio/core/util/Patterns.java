package org.azertio.core.util;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 */
public class Patterns {

	private static final LoadingCache<String, Pattern> patterns = Caffeine.newBuilder()
			.maximumSize(1000)
			.build(Pattern::compile);

	private static final LoadingCache<String, Pattern> patternsIgnoreCase = Caffeine.newBuilder()
			.maximumSize(1000)
			.build(key -> Pattern.compile(key, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));

	private Patterns() {
		// prevent instantiation
	}

	public static Pattern of(String regex) {
		return patterns.get(regex);
	}

	public static Pattern ofIgnoreCase(String regex) {
		return patternsIgnoreCase.get(regex);
	}

	public static Matcher match(String value, String regex) {
		return of(regex).matcher(value);
	}

	public static String replace(String value, String regex, String replacement) {
		return match(value, regex).replaceAll(replacement);
	}

}