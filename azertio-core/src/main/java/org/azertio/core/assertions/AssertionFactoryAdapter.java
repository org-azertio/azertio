package org.azertio.core.assertions;

import org.azertio.core.Assertion;
import org.azertio.core.AssertionPattern;
import org.azertio.core.DataType;
import org.azertio.core.messages.Messages;
import org.azertio.core.util.Patterns;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;

/**
 * Abstract base class for implementing assertion factories with localization support.
 *
 * <p>This class provides the infrastructure for creating assertion factories that can
 * generate assertions based on localized text patterns. It manages pattern caching
 * per locale and handles parameter extraction from input expressions.</p>
 *
 * <h2>Implementation Guide</h2>
 * <p>Subclasses must implement the {@link #fillSuppliers()} method to register
 * the supported assertions by adding entries to the {@link #suppliers} map.</p>
 *
 * <pre>{@code
 * public class MyAssertionFactory extends AssertionFactoryAdapter<MyType> {
 *
 *     public MyAssertionFactory(Messages messages) {
 *         super("my-assertion", MyType::parse, myDataType, messages);
 *     }
 *
 *     @Override
 *     protected void fillSuppliers() {
 *         suppliers.put("assertion.my.equals",
 *             it -> new AssertionAdapter(name, Matchers.equalTo(it)));
 *     }
 * }
 * }</pre>
 *
 * @param <T> the type of values this factory creates assertions for
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 * @see org.azertio.core.AssertionFactory
 * @see AssertionAdapter
 */
public abstract class AssertionFactoryAdapter<T> implements org.azertio.core.AssertionFactory<T> {

	/** Map of assertion keys to their supplier functions. */
	protected final Map<String, Function<T, Assertion>> suppliers = new HashMap<>();

	/** Message provider for localized assertion patterns. */
	protected final Messages messages;

	/** Cache of assertion patterns by locale. */
	protected final Map<Locale, List<AssertionPattern<T>>> patternsByKey = new HashMap<>();

	/** The name identifier for this factory. */
	protected final String name;

	/** Function to parse string input into the target type. */
	protected final Function<String,T> parser;

	/** The data type this factory handles. */
	protected final DataType type;

	/**
	 * Creates a new assertion factory adapter.
	 *
	 * @param name     the unique name identifier for this factory
	 * @param parser   function to parse string values into the target type
	 * @param type     the data type this factory handles
	 * @param messages the message provider for localized patterns
	 */
	protected AssertionFactoryAdapter(String name, Function<String,T> parser, DataType type, Messages messages) {
		this.name = name;
		this.parser = parser;
		this.messages = messages;
		this.type = type;
		fillSuppliers();
	}

	/**
	 * Registers the assertion suppliers in the {@link #suppliers} map.
	 *
	 * <p>Subclasses must implement this method to populate the suppliers map
	 * with entries mapping message keys to assertion supplier functions.</p>
	 */
	protected abstract void fillSuppliers();



	/**
	 * {@inheritDoc}
	 */
	@Override
	public String name() {
		return name;
	}

	/**
	 * Returns the assertion patterns for the specified locale.
	 *
	 * <p>Patterns are cached per locale for performance. The first call for
	 * a given locale will create and cache the patterns.</p>
	 *
	 * @param locale the locale for which to get patterns
	 * @return list of assertion patterns for the locale
	 */
	@Override
	public List<AssertionPattern<T>> patterns(Locale locale) {
		return patternsByKey.computeIfAbsent(locale, this::createPatternsForLocale);
	}

	/**
	 * Creates an assertion from a pattern and input string.
	 *
	 * <p>The input string is matched against the pattern, and if successful,
	 * the parameter value is extracted, parsed, and used to create the assertion.</p>
	 *
	 * @param pattern the assertion pattern to use
	 * @param input   the input string to match and extract parameters from
	 * @return the created assertion, or {@code null} if the input doesn't match
	 */
	@Override
	public Assertion assertion(AssertionPattern<T> pattern, String input) {
		Matcher matcher = pattern.pattern().matcher(input);
		if (!matcher.find()) {
			return null;
		}
		try {
			String parameter = matcher.group("param");
			T parameterValue = (parameter == null ? null : parser.apply(parameter));
			return pattern.supplier().apply(parameterValue);
		} catch (IllegalArgumentException e) {
			return pattern.supplier().apply(null);
		}
	}

	/**
	 * Creates assertion patterns for a specific locale.
	 *
	 * @param locale the locale for which to create patterns
	 * @return list of assertion patterns for the locale
	 */
	private ArrayList<AssertionPattern<T>> createPatternsForLocale(Locale locale) {
		var localeMessages = messages.forLocale(locale);
		var patterns = new ArrayList<AssertionPattern<T>>();
		String parameterPattern = "(?<param>"+type.pattern().pattern()+")";
		for (String key : suppliers.keySet()) {
			String expression = localeMessages.get(key);
			expression = "\\s*" + expression + "\\s*"; // accept blanks before and after
			expression = expression.replace(")", ")?"); // make () optionals
			expression = expression.replace(")? ", ")?\\s*"); // make space after optional group also optional
			expression = expression.replace("_", parameterPattern);
			patterns.add(new AssertionPattern<>(key,Patterns.of(expression),suppliers.get(key)));
		}
		return patterns;
	}



}
