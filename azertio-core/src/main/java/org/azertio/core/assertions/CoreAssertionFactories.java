package org.azertio.core.assertions;

import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Inject;
import org.myjtools.jexten.Scope;
import org.azertio.core.AssertionFactory;
import org.azertio.core.contributors.AssertionFactoryProvider;
import org.azertio.core.datatypes.CoreDataTypes;
import org.azertio.core.messages.Messages;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Stream;

/**
 * Main provider that registers the default assertion factories for the Azertio framework.
 *
 * <p>This class is registered as a singleton extension via jexten and provides
 * assertion factories for the following data types:</p>
 *
 * <ul>
 *   <li>{@code number-assertion} - For integer comparisons</li>
 *   <li>{@code decimal-assertion} - For {@link BigDecimal} comparisons</li>
 *   <li>{@code date-assertion} - For {@link LocalDate} comparisons</li>
 *   <li>{@code time-assertion} - For {@link LocalTime} comparisons</li>
 *   <li>{@code datetime-assertion} - For {@link LocalDateTime} comparisons</li>
 *   <li>{@code text-assertion} - For String operations (equals, contains, starts/ends with, etc.)</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <p>This provider is automatically discovered and used by the Azertio framework.
 * The assertion factories can also be accessed programmatically via
 * {@link org.azertio.core.AssertionFactories#CORE_ENGLISH}.</p>
 *
 * @author Luis Iñesta Gelabert
 * @see AssertionFactoryProvider
 * @see ComparableAssertionFactory
 * @see TemporalAssertionFactory
 * @see StringAssertionFactory
 */
@Extension(scope = Scope.SINGLETON)
public class CoreAssertionFactories implements AssertionFactoryProvider {

	/** Message provider for localized assertion patterns. */
	@Inject("AssertionFactoryProvider")
	Messages messages;

	/**
	 * Default constructor for extension discovery.
	 */
	public CoreAssertionFactories() {
		// default constructor
	}

	/**
	 * Constructor for testing purposes.
	 *
	 * @param messages the message provider to use
	 */
	public CoreAssertionFactories(Messages messages) {
		// this constructor only exists for testing purposes
		this.messages = messages;
	}

	/**
	 * Returns a stream of all core assertion factories.
	 *
	 * @return stream of assertion factories for numbers, decimals, dates, times, date-times, and text
	 */
	@Override
	public Stream<org.azertio.core.AssertionFactory<?>> assertionFactories() {
		return Stream.of(
			new ComparableAssertionFactory<>("integer-assertion", Integer::valueOf, CoreDataTypes.NUMBER, messages),
			new ComparableAssertionFactory<>("decimal-assertion", BigDecimal::new, CoreDataTypes.DECIMAL, messages),
			new TemporalAssertionFactory<>("date-assertion", LocalDate::parse, CoreDataTypes.DATE, messages),
			new TemporalAssertionFactory<>("time-assertion", LocalTime::parse, CoreDataTypes.TIME, messages),
			new TemporalAssertionFactory<>("datetime-assertion", LocalDateTime::parse, CoreDataTypes.DATE_TIME, messages),
			new StringAssertionFactory("text-assertion", CoreDataTypes.TEXT, messages)
		);
	}


	/**
	 * Array of all core assertion factories for easy access.
	 */
	public static final AssertionFactory<?>[] ALL = new CoreAssertionFactories(
			Messages.of(List.of(new AssertionMessageProvider()))
		).assertionFactories().toArray(AssertionFactory[]::new);


}
