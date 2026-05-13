package org.azertio.core;

import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 */
public record AssertionPattern<T>(String key, Pattern pattern, Function<T, Assertion> supplier) {

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		AssertionPattern<T> that = (AssertionPattern<T>) o;
		return Objects.equals(key, that.key);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(key);
	}

	public Matcher matcher(String input) {
		return pattern.matcher(input);
	}

}
