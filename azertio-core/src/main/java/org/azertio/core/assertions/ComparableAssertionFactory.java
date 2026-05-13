package org.azertio.core.assertions;

import org.hamcrest.Matchers;
import org.azertio.core.DataType;
import org.azertio.core.messages.Messages;
import java.util.function.Function;


/**
 * Assertion factory for {@link Comparable} types such as numbers and decimals.
 *
 * <p>This factory provides assertions for comparing values using standard comparison
 * operators: equals, greater than, less than, and their negations. It uses Hamcrest
 * matchers internally for the actual comparisons.</p>
 *
 * <h2>Supported Assertions</h2>
 * <ul>
 *   <li>{@code assertion.number.equals} - Value is equal to the expected value</li>
 *   <li>{@code assertion.number.greater} - Value is greater than the expected value</li>
 *   <li>{@code assertion.number.less} - Value is less than the expected value</li>
 *   <li>{@code assertion.number.greater.equals} - Value is greater than or equal to</li>
 *   <li>{@code assertion.number.less.equals} - Value is less than or equal to</li>
 *   <li>{@code assertion.number.not.equals} - Value is not equal to</li>
 *   <li>{@code assertion.number.not.greater} - Value is not greater than</li>
 *   <li>{@code assertion.number.not.less} - Value is not less than</li>
 *   <li>{@code assertion.generic.null} - Value is null</li>
 *   <li>{@code assertion.generic.not.null} - Value is not null</li>
 * </ul>
 *
 * @param <T> the comparable type this factory handles
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 * @see AssertionFactoryAdapter
 * @see TemporalAssertionFactory
 */
public class ComparableAssertionFactory<T extends Comparable<T>> extends AssertionFactoryAdapter<T> {

	/** Message key for equality assertion. */
	public static final String ASSERTION_NUMBER_EQUALS = "assertion.number.equals";
	/** Message key for greater than assertion. */
	public static final String ASSERTION_NUMBER_GREATER = "assertion.number.greater";
	/** Message key for less than assertion. */
	public static final String ASSERTION_NUMBER_LESS = "assertion.number.less";
	/** Message key for greater than or equal assertion. */
	public static final String ASSERTION_NUMBER_GREATER_EQUALS = "assertion.number.greater.equals";
	/** Message key for less than or equal assertion. */
	public static final String ASSERTION_NUMBER_LESS_EQUALS = "assertion.number.less.equals";
	/** Message key for not equal assertion. */
	public static final String ASSERTION_NUMBER_NOT_EQUALS = "assertion.number.not.equals";
	/** Message key for not greater than assertion. */
	public static final String ASSERTION_NUMBER_NOT_GREATER = "assertion.number.not.greater";
	/** Message key for not less than assertion. */
	public static final String ASSERTION_NUMBER_NOT_LESS = "assertion.number.not.less";
	/** Message key for not greater than or equal assertion. */
	public static final String ASSERTION_NUMBER_NOT_GREATER_EQUALS = "assertion.number.not.greater.equals";
	/** Message key for not less than or equal assertion. */
	public static final String ASSERTION_NUMBER_NOT_LESS_EQUALS = "assertion.number.not.less.equals";
	/** Message key for null assertion. */
	public static final String ASSERTION_GENERIC_NULL = "assertion.generic.null";
	/** Message key for not null assertion. */
	public static final String ASSERTION_GENERIC_NOT_NULL = "assertion.generic.not.null";

	/**
	 * Creates a new comparable assertion factory.
	 *
	 * @param name     the unique name identifier for this factory
	 * @param parser   function to parse string values into the target type
	 * @param type     the data type this factory handles
	 * @param messages the message provider for localized patterns
	 */
	public ComparableAssertionFactory(String name, Function<String,T> parser, DataType type, Messages messages) {
		super(name,parser,type,messages);
	}

	/**
	 * {@inheritDoc}
	 */
	protected void fillSuppliers() {
		suppliers.put(
				ASSERTION_NUMBER_EQUALS,
				it -> new AssertionAdapter(Matchers.comparesEqualTo(it)));
		suppliers.put(
				ASSERTION_NUMBER_GREATER,
				it -> new AssertionAdapter(Matchers.greaterThan(it))
		);
		suppliers.put(
				ASSERTION_NUMBER_LESS,
				it -> new AssertionAdapter(Matchers.lessThan(it))
		);
		suppliers.put(
				ASSERTION_NUMBER_GREATER_EQUALS,
				it -> new AssertionAdapter(Matchers.greaterThanOrEqualTo((it)))
		);
		suppliers.put(
				ASSERTION_NUMBER_LESS_EQUALS,
				it -> new AssertionAdapter(Matchers.lessThanOrEqualTo((it)))
		);
		suppliers.put(
				ASSERTION_NUMBER_NOT_EQUALS,
				it -> new AssertionAdapter(Matchers.not(Matchers.equalTo(it)))
		);
		suppliers.put(
				ASSERTION_NUMBER_NOT_GREATER,
				it -> new AssertionAdapter(Matchers.not(Matchers.greaterThan((it))))
		);
		suppliers.put(
				ASSERTION_NUMBER_NOT_LESS,
				it -> new AssertionAdapter(Matchers.not(Matchers.lessThan((it))))
		);
		suppliers.put(
				ASSERTION_NUMBER_NOT_GREATER_EQUALS,
				it -> new AssertionAdapter(Matchers.not(Matchers.greaterThanOrEqualTo((it))))
		);
		suppliers.put(
				ASSERTION_NUMBER_NOT_LESS_EQUALS,
				it -> new AssertionAdapter(Matchers.not(Matchers.lessThanOrEqualTo((it))))
		);
		suppliers.put(
				ASSERTION_GENERIC_NULL,
				it -> new AssertionAdapter(Matchers.nullValue())
		);
		suppliers.put(
				ASSERTION_GENERIC_NOT_NULL,
				it -> new AssertionAdapter(Matchers.not(Matchers.nullValue()))
		);
	}



}
