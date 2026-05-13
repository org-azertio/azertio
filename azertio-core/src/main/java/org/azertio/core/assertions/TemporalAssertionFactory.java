package org.azertio.core.assertions;

import org.azertio.core.DataType;
import org.azertio.core.messages.Messages;

import java.util.function.Function;

import static org.hamcrest.Matchers.*;

/**
 * Assertion factory for temporal types such as {@link java.time.LocalDate},
 * {@link java.time.LocalTime}, and {@link java.time.LocalDateTime}.
 *
 * <p>This factory extends {@link ComparableAssertionFactory} and provides temporal-specific
 * assertion patterns using terms like "before" and "after" instead of "less than" and
 * "greater than", which are more natural for date/time comparisons.</p>
 *
 * <h2>Supported Assertions</h2>
 * <ul>
 *   <li>{@code assertion.temporal.equals} - Value equals the expected date/time</li>
 *   <li>{@code assertion.temporal.after} - Value is after the expected date/time</li>
 *   <li>{@code assertion.temporal.before} - Value is before the expected date/time</li>
 *   <li>{@code assertion.temporal.after.equals} - Value is on or after</li>
 *   <li>{@code assertion.temporal.before.equals} - Value is on or before</li>
 *   <li>{@code assertion.temporal.not.equals} - Value is not equal to</li>
 *   <li>{@code assertion.temporal.not.after} - Value is not after</li>
 *   <li>{@code assertion.temporal.not.before} - Value is not before</li>
 *   <li>{@code assertion.generic.null} - Value is null</li>
 *   <li>{@code assertion.generic.not.null} - Value is not null</li>
 * </ul>
 *
 * @param <T> the temporal type this factory handles (must be Comparable)
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 * @see ComparableAssertionFactory
 * @see java.time.LocalDate
 * @see java.time.LocalTime
 * @see java.time.LocalDateTime
 */
public class TemporalAssertionFactory<T extends Comparable<T>> extends ComparableAssertionFactory<T> {

	/** Message key for temporal equality assertion. */
	public static final String ASSERTION_TEMPORAL_EQUALS = "assertion.temporal.equals";
	/** Message key for after assertion (greater than for dates). */
	public static final String ASSERTION_TEMPORAL_AFTER = "assertion.temporal.after";
	/** Message key for before assertion (less than for dates). */
	public static final String ASSERTION_TEMPORAL_LESS = "assertion.temporal.before";
	/** Message key for on or after assertion. */
	public static final String ASSERTION_TEMPORAL_AFTER_EQUALS = "assertion.temporal.after.equals";
	/** Message key for on or before assertion. */
	public static final String ASSERTION_TEMPORAL_LESS_EQUALS = "assertion.temporal.before.equals";
	/** Message key for temporal inequality assertion. */
	public static final String ASSERTION_TEMPORAL_NOT_EQUALS = "assertion.temporal.not.equals";
	/** Message key for not after assertion. */
	public static final String ASSERTION_TEMPORAL_NOT_AFTER = "assertion.temporal.not.after";
	/** Message key for not before assertion. */
	public static final String ASSERTION_TEMPORAL_NOT_LESS = "assertion.temporal.not.before";
	/** Message key for not on or after assertion. */
	public static final String ASSERTION_TEMPORAL_NOT_AFTER_EQUALS = "assertion.temporal.not.after.equals";
	/** Message key for not on or before assertion. */
	public static final String ASSERTION_TEMPORAL_NOT_LESS_EQUALS = "assertion.temporal.not.before.equals";
	/** Message key for null assertion. */
	public static final String ASSERTION_GENERIC_NULL = "assertion.generic.null";
	/** Message key for not null assertion. */
	public static final String ASSERTION_GENERIC_NOT_NULL = "assertion.generic.not.null";

	/**
	 * Creates a new temporal assertion factory.
	 *
	 * @param name     the unique name identifier for this factory
	 * @param parser   function to parse string values into the target temporal type
	 * @param type     the data type this factory handles
	 * @param messages the message provider for localized patterns
	 */
	public TemporalAssertionFactory(String name, Function<String,T> parser, DataType type, Messages messages) {
		super(name,parser,type,messages);
	}

	/**
	 * {@inheritDoc}
	 */
	protected void fillSuppliers() {
		suppliers.put(
				ASSERTION_TEMPORAL_EQUALS,
				it -> new AssertionAdapter(comparesEqualTo(it)));
		suppliers.put(
				ASSERTION_TEMPORAL_AFTER,
				it -> new AssertionAdapter(greaterThan(it))
		);
		suppliers.put(
				ASSERTION_TEMPORAL_LESS,
				it -> new AssertionAdapter(lessThan(it))
		);
		suppliers.put(
				ASSERTION_TEMPORAL_AFTER_EQUALS,
				it -> new AssertionAdapter(greaterThanOrEqualTo((it)))
		);
		suppliers.put(
				ASSERTION_TEMPORAL_LESS_EQUALS,
				it -> new AssertionAdapter(lessThanOrEqualTo((it)))
		);
		suppliers.put(
				ASSERTION_TEMPORAL_NOT_EQUALS,
				it -> new AssertionAdapter(not(equalTo(it)))
		);
		suppliers.put(
				ASSERTION_TEMPORAL_NOT_AFTER,
				it -> new AssertionAdapter(not(greaterThan((it))))
		);
		suppliers.put(
				ASSERTION_TEMPORAL_NOT_LESS,
				it -> new AssertionAdapter(not(lessThan((it))))
		);
		suppliers.put(
				ASSERTION_TEMPORAL_NOT_AFTER_EQUALS,
				it -> new AssertionAdapter(not(greaterThanOrEqualTo((it))))
		);
		suppliers.put(
				ASSERTION_TEMPORAL_NOT_LESS_EQUALS,
				it -> new AssertionAdapter(not(lessThanOrEqualTo((it))))
		);
		suppliers.put(
				ASSERTION_GENERIC_NULL,
				it -> new AssertionAdapter(nullValue())
		);
		suppliers.put(
				ASSERTION_GENERIC_NOT_NULL,
				it -> new AssertionAdapter(not(nullValue()))
		);
	}




}
