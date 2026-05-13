package org.azertio.core.contenttypes;

import org.myjtools.jexten.Extension;
import org.azertio.core.Assertion;
import org.azertio.core.contributors.ContentType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Extension
public class TextContentType implements ContentType {

	@Override
	public boolean accepts(String contentType) {
		return "text".equalsIgnoreCase(contentType) ||
			"plain".equalsIgnoreCase(contentType) ||
			"text/plain".equalsIgnoreCase(contentType);
	}


	@Override
	public void assertContentEquals(String expected, String actual, ComparisonMode mode) {
		switch (mode) {
			case STRICT    -> assertStrict(expected, actual);
			case ANY_ORDER -> assertAnyOrder(expected, actual);
			case LOOSE     -> assertLoose(expected, actual);
		}
	}


	/**
	 * Extracts a fragment from the content identified by {@code fragmentPath}:
	 * <ul>
	 *   <li>If {@code fragmentPath} is a positive integer, it is treated as a 1-based line number.</li>
	 *   <li>Otherwise it is treated as a regular expression; the first match (or captured group 1
	 *       if present) across all lines is returned.</li>
	 * </ul>
	 */
	@Override
	public void assertFragmentEquals(String content, String fragmentPath, Assertion assertion) {
		Object value = extractFragment(content, fragmentPath);
		if (!assertion.test(value)) {
			throw new AssertionError(
				"Fragment assertion failed at path '" + fragmentPath + "':\n" +
				assertion.describeFailure(value)
			);
		}
	}


	@Override
	public String extractValue(String content, String fragmentPath) {
		return (String) extractFragment(content, fragmentPath);
	}


	/**
	 * Validates that the content matches the given regular expression pattern.
	 */
	@Override
	public void assertComplyWithSchema(String content, String schema) {
		try {
			Pattern pattern = Pattern.compile(schema, Pattern.DOTALL);
			if (!pattern.matcher(content).matches()) {
				throw new AssertionError(
					"Text does not match required pattern:\n  Pattern: " + schema +
					"\n  Content: " + truncate(content, 200)
				);
			}
		} catch (PatternSyntaxException e) {
			throw new AssertionError("Invalid pattern in schema: " + e.getMessage());
		}
	}


	// --- assertContentEquals helpers ---

	private void assertStrict(String expected, String actual) {
		if (expected.equals(actual)) {
			return;
		}
		List<String> expLines = lines(expected);
		List<String> actLines = lines(actual);
		int maxLines = Math.max(expLines.size(), actLines.size());
		for (int i = 0; i < maxLines; i++) {
			String expLine = i < expLines.size() ? expLines.get(i) : "<missing>";
			String actLine = i < actLines.size() ? actLines.get(i) : "<missing>";
			if (!expLine.equals(actLine)) {
				throw new AssertionError(
					"Text content mismatch at line " + (i + 1) + " [mode=STRICT]:\n" +
					"  Expected: \"" + expLine + "\"\n" +
					"  Actual:   \"" + actLine + "\""
				);
			}
		}
	}

	private void assertAnyOrder(String expected, String actual) {
		List<String> expLines = lines(expected);
		List<String> actLines = lines(actual);
		List<String> sortedExp = sorted(expLines);
		List<String> sortedAct = sorted(actLines);
		if (!sortedExp.equals(sortedAct)) {
			List<String> missing = new ArrayList<>(expLines);
			missing.removeAll(actLines);
			List<String> extra = new ArrayList<>(actLines);
			extra.removeAll(expLines);
			StringBuilder msg = new StringBuilder("Text content mismatch [mode=ANY_ORDER]:");
			if (!missing.isEmpty()) {
				msg.append("\n  Missing lines:");
				missing.forEach(l -> msg.append("\n    - \"").append(l).append("\""));
			}
			if (!extra.isEmpty()) {
				msg.append("\n  Unexpected lines:");
				extra.forEach(l -> msg.append("\n    + \"").append(l).append("\""));
			}
			throw new AssertionError(msg.toString());
		}
	}

	private void assertLoose(String expected, String actual) {
		List<String> expLines = lines(expected);
		List<String> actLines = lines(actual);
		List<String> missing = expLines.stream()
			.filter(line -> !actLines.contains(line))
			.toList();
		if (!missing.isEmpty()) {
			StringBuilder msg = new StringBuilder("Text content mismatch [mode=LOOSE] — missing lines:");
			missing.forEach(l -> msg.append("\n  - \"").append(l).append("\""));
			throw new AssertionError(msg.toString());
		}
	}


	// --- assertFragmentEquals helpers ---

	private Object extractFragment(String content, String fragmentPath) {
		try {
			int lineNumber = Integer.parseInt(fragmentPath.trim());
			List<String> contentLines = lines(content);
			if (lineNumber < 1 || lineNumber > contentLines.size()) {
				throw new AssertionError(
					"Line " + lineNumber + " does not exist (content has " + contentLines.size() + " lines)"
				);
			}
			return contentLines.get(lineNumber - 1);
		} catch (NumberFormatException ignored) {
			// not a line number — treat as regex
		}
		try {
			Pattern pattern = Pattern.compile(fragmentPath);
			for (String line : lines(content)) {
				Matcher matcher = pattern.matcher(line);
				if (matcher.find()) {
					return matcher.groupCount() > 0 ? matcher.group(1) : matcher.group();
				}
			}
			throw new AssertionError("No line matches pattern '" + fragmentPath + "'");
		} catch (PatternSyntaxException e) {
			throw new AssertionError("Invalid fragment path '" + fragmentPath + "': " + e.getMessage());
		}
	}


	// --- Utility ---

	private List<String> lines(String text) {
		return text.lines().toList();
	}

	private List<String> sorted(List<String> lines) {
		List<String> copy = new ArrayList<>(lines);
		Collections.sort(copy);
		return copy;
	}

	private String truncate(String text, int maxLength) {
		return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
	}
}
