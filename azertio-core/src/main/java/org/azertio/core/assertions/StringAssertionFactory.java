package org.azertio.core.assertions;

import org.hamcrest.Matchers;
import org.azertio.core.DataType;
import org.azertio.core.messages.Messages;

/**
 * Assertion factory for text strings with various comparison operations.
 *
 * <p>This factory provides assertions for string comparisons including equality,
 * prefix/suffix matching, substring containment, and case-insensitive variants.
 * It uses Hamcrest matchers internally for the actual comparisons.</p>
 *
 * <h2>Supported Assertions</h2>
 * <ul>
 *   <li>{@code assertion.string.equals} - Exact string equality</li>
 *   <li>{@code assertion.string.not.equals} - String inequality</li>
 *   <li>{@code assertion.string.equals.ignore.case} - Equality ignoring case</li>
 *   <li>{@code assertion.string.equals.ignore.whitespace} - Equality ignoring whitespace</li>
 *   <li>{@code assertion.string.starts.with} - String starts with prefix</li>
 *   <li>{@code assertion.string.ends.with} - String ends with suffix</li>
 *   <li>{@code assertion.string.contains} - String contains substring</li>
 *   <li>And their negations and case-insensitive variants</li>
 *   <li>{@code assertion.generic.null} - Value is null</li>
 *   <li>{@code assertion.generic.not.null} - Value is not null</li>
 * </ul>
 *
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 * @see AssertionFactoryAdapter
 */
public class StringAssertionFactory extends AssertionFactoryAdapter<String> {

	/** Message key for string equality assertion. */
	public static final String ASSERTION_STRING_EQUALS = "assertion.string.equals";
	/** Message key for string inequality assertion. */
	public static final String ASSERTION_STRING_NOT_EQUALS = "assertion.string.not.equals";
	/** Message key for null assertion. */
	public static final String ASSERTION_GENERIC_NULL = "assertion.generic.null";
	/** Message key for not null assertion. */
	public static final String ASSERTION_GENERIC_NOT_NULL = "assertion.generic.not.null";
	/** Message key for case-insensitive equality assertion. */
	public static final String ASSERTION_STRING_EQUALS_IGNORE_CASE = "assertion.string.equals.ignore.case";
	/** Message key for whitespace-insensitive equality assertion. */
	public static final String ASSERTION_STRING_EQUALS_IGNORE_WHITESPACE = "assertion.string.equals.ignore.whitespace";
	/** Message key for starts with assertion. */
	public static final String ASSERTION_STRING_STARTS_WITH = "assertion.string.starts.with";
	/** Message key for case-insensitive starts with assertion. */
	public static final String ASSERTION_STRING_STARTS_WITH_IGNORE_CASE = "assertion.string.starts.with.ignore.case";
	/** Message key for ends with assertion. */
	public static final String ASSERTION_STRING_ENDS_WITH = "assertion.string.ends.with";
	/** Message key for case-insensitive ends with assertion. */
	public static final String ASSERTION_STRING_ENDS_WITH_IGNORE_CASE = "assertion.string.ends.with.ignore.case";
	/** Message key for contains assertion. */
	public static final String ASSERTION_STRING_CONTAINS = "assertion.string.contains";
	/** Message key for case-insensitive contains assertion. */
	public static final String ASSERTION_STRING_CONTAINS_IGNORE_CASE = "assertion.string.contains.ignore.case";
	/** Message key for case-insensitive inequality assertion. */
	public static final String ASSERTION_STRING_NOT_EQUALS_IGNORE_CASE = "assertion.string.not.equals.ignore.case";
	/** Message key for whitespace-insensitive inequality assertion. */
	public static final String ASSERTION_STRING_NOT_EQUALS_IGNORE_WHITESPACE = "assertion.string.not.equals.ignore.whitespace";
	/** Message key for not starts with assertion. */
	public static final String ASSERTION_STRING_NOT_STARTS_WITH = "assertion.string.not.starts.with";
	/** Message key for case-insensitive not starts with assertion. */
	public static final String ASSERTION_STRING_NOT_STARTS_WITH_IGNORE_CASE = "assertion.string.not.starts.with.ignore.case";
	/** Message key for not ends with assertion. */
	public static final String ASSERTION_STRING_NOT_ENDS_WITH = "assertion.string.not.ends.with";
	/** Message key for case-insensitive not ends with assertion. */
	public static final String ASSERTION_STRING_NOT_ENDS_WITH_IGNORE_CASE = "assertion.string.not.ends.with.ignore.case";
	/** Message key for not contains assertion. */
	public static final String ASSERTION_STRING_NOT_CONTAINS = "assertion.string.not.contains";
	/** Message key for case-insensitive not contains assertion. */
	public static final String ASSERTION_STRING_NOT_CONTAINS_IGNORE_CASE = "assertion.string.not.contains.ignore.case";

	/**
	 * Creates a new string assertion factory.
	 *
	 * @param name     the unique name identifier for this factory
	 * @param type     the data type this factory handles
	 * @param messages the message provider for localized patterns
	 */
	public StringAssertionFactory(String name, DataType type, Messages messages) {
		super(name,StringAssertionFactory::parse,type,messages);
	}

	/**
	 * Parses a quoted string value by removing the surrounding quotes.
	 *
	 * @param value the quoted string value
	 * @return the unquoted string
	 */
	private static String parse(String value) {
		return value.substring(1,value.length()-1);
	}

	/**
	 * {@inheritDoc}
	 */
	protected void fillSuppliers() {
		suppliers.put(
				ASSERTION_STRING_EQUALS,
				it -> new AssertionAdapter(Matchers.comparesEqualTo(it)));
		suppliers.put(
				ASSERTION_STRING_NOT_EQUALS,
				it -> new AssertionAdapter(Matchers.not(Matchers.comparesEqualTo(it))));
		suppliers.put(
				ASSERTION_GENERIC_NULL,
				it -> new AssertionAdapter(Matchers.nullValue())
		);
		suppliers.put(
				ASSERTION_GENERIC_NOT_NULL,
				it -> new AssertionAdapter(Matchers.not(Matchers.nullValue()))
		);
		suppliers.put(
				ASSERTION_STRING_EQUALS_IGNORE_CASE,
				it -> new AssertionAdapter(Matchers.equalToIgnoringCase(it))
		);
		suppliers.put(
				ASSERTION_STRING_EQUALS_IGNORE_WHITESPACE,
				it -> new AssertionAdapter(Matchers.equalToIgnoringCase(it))
		);
		suppliers.put(
				ASSERTION_STRING_STARTS_WITH,
				it -> new AssertionAdapter(Matchers.startsWith(it))
		);
		suppliers.put(
				ASSERTION_STRING_STARTS_WITH_IGNORE_CASE,
				it -> new AssertionAdapter(Matchers.startsWithIgnoringCase(it))
		);
		suppliers.put(
				ASSERTION_STRING_ENDS_WITH,
				it -> new AssertionAdapter(Matchers.endsWith(it))
		);
		suppliers.put(
				ASSERTION_STRING_ENDS_WITH_IGNORE_CASE,
				it -> new AssertionAdapter(Matchers.endsWithIgnoringCase(it))
		);
		suppliers.put(
				ASSERTION_STRING_CONTAINS,
				it -> new AssertionAdapter(Matchers.containsString(it))
		);
		suppliers.put(
				ASSERTION_STRING_CONTAINS_IGNORE_CASE,
				it -> new AssertionAdapter(Matchers.containsStringIgnoringCase(it))
		);
		suppliers.put(
				ASSERTION_STRING_NOT_EQUALS_IGNORE_CASE,
				it -> new AssertionAdapter(Matchers.not(Matchers.equalToIgnoringCase(it)))
		);
		suppliers.put(
				ASSERTION_STRING_NOT_EQUALS_IGNORE_WHITESPACE,
				it -> new AssertionAdapter(Matchers.not(Matchers.equalToIgnoringCase(it)))
		);
		suppliers.put(
				ASSERTION_STRING_NOT_STARTS_WITH,
				it -> new AssertionAdapter(Matchers.not(Matchers.startsWith(it)))
		);
		suppliers.put(
				ASSERTION_STRING_NOT_STARTS_WITH_IGNORE_CASE,
				it -> new AssertionAdapter(Matchers.not(Matchers.startsWithIgnoringCase(it)))
		);
		suppliers.put(
				ASSERTION_STRING_NOT_ENDS_WITH,
				it -> new AssertionAdapter(Matchers.not(Matchers.endsWith(it)))
		);
		suppliers.put(
				ASSERTION_STRING_NOT_ENDS_WITH_IGNORE_CASE,
				it -> new AssertionAdapter(Matchers.not(Matchers.endsWithIgnoringCase(it)))
		);
		suppliers.put(
				ASSERTION_STRING_NOT_CONTAINS,
				it -> new AssertionAdapter(Matchers.not(Matchers.containsString(it)))
		);
		suppliers.put(
				ASSERTION_STRING_NOT_CONTAINS_IGNORE_CASE,
				it -> new AssertionAdapter(Matchers.not(Matchers.containsStringIgnoringCase(it)))
		);
	}



}
