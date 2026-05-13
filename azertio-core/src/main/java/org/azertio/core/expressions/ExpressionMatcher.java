package org.azertio.core.expressions;

import org.azertio.core.Assertion;

import java.util.*;

/**
 * Matches input strings against a compiled expression pattern.
 *
 * <p>An ExpressionMatcher is built by {@link ExpressionMatcherBuilder} and consists
 * of a sequence of {@link FragmentMatcher}s. When matching, each fragment is tested
 * in order, and the match succeeds only if all fragments match consecutively.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * ExpressionMatcherBuilder builder = new ExpressionMatcherBuilder(dataTypes, assertions);
 * ExpressionMatcher matcher = builder.buildExpressionMatcher(
 *     "the user {name:text} has {count:number} items"
 * );
 *
 * Match match = matcher.matches("the user John has 5 items", Locale.ENGLISH);
 * if (match.matched()) {
 *     ArgumentValue name = match.argument("name");
 *     ArgumentValue count = match.argument("count");
 * }
 * }</pre>
 *
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 * @see ExpressionMatcherBuilder
 * @see Match
 * @see FragmentMatcher
 */
public class ExpressionMatcher {

	List<FragmentMatcher> fragments;
	Map<String,ArgumentFragmentMatcher> arguments;

	/**
	 * Creates a new expression matcher with the given fragment matchers.
	 *
	 * @param fragments the list of fragment matchers
	 */
	public ExpressionMatcher(List<FragmentMatcher> fragments) {
		this.fragments = List.copyOf(fragments);
		this.arguments = new HashMap<>();
		for (FragmentMatcher fragment : fragments) {
			if (fragment instanceof ArgumentFragmentMatcher argumentFragmentMatcher)  {
				arguments.put(argumentFragmentMatcher.name(), argumentFragmentMatcher);
			}
		}
	}

	/**
	 * Returns the list of fragment matchers.
	 *
	 * @return immutable list of fragment matchers
	 */
	public List<FragmentMatcher> fragments() {
		return fragments;
	}



	public Optional<Match> matches(String value, Locale locale) {
		String remaining = value;

		List<ArgumentValue> arguments = new ArrayList<>();
		Assertion assertion = null;

		for (FragmentMatcher fragmentMatcher : fragments) {
			var result = fragmentMatcher.matches(remaining, locale);
			if (result.startMatched()) {
				remaining = remaining.substring(result.consumed());
				if (result.argument() != null) {
					arguments.add(result.argument());
				}
				if (result.assertion() != null) {
					assertion = result.assertion();
				}
			} else {
				return Optional.empty();
			}
		}
		if (!remaining.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(new Match(arguments, assertion));
	}




}
