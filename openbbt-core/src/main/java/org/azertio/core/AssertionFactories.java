package org.azertio.core;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry that groups multiple {@link AssertionFactory} instances and allows lookup by name.
 *
 * <p>This class provides a centralized way to manage and access assertion factories.
 * It includes a pre-configured instance with all core English assertion factories.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Use the pre-configured core factories
 * AssertionFactories factories = AssertionFactories.CORE_ENGLISH;
 * AssertionFactory<?> numberFactory = factories.byName("number-assertion");
 *
 * // Or create a custom registry
 * AssertionFactories custom = AssertionFactories.of(
 *     new MyCustomFactory(messages),
 *     new AnotherFactory(messages)
 * );
 * }</pre>
 *
 * @author Luis Iñesta Gelabert
 * @see AssertionFactory
 */
public class AssertionFactories {


	/**
	 * Creates a new registry from the given assertion factories.
	 *
	 * @param assertionFactories the factories to include in the registry
	 * @return a new {@code AssertionFactories} instance
	 */
	public static AssertionFactories of(AssertionFactory<?>... assertionFactories) {
		return new AssertionFactories(assertionFactories);
	}

	/**
	 * Creates a new registry from the given collection of assertion factories.
	 *
	 * @param assertionFactories the factories to include in the registry
	 * @return a new {@code AssertionFactories} instance
	 */
	public static AssertionFactories of(Collection<AssertionFactory<?>> assertionFactories) {
		return new AssertionFactories(assertionFactories.toArray(AssertionFactory[]::new));
	}

	private final Map<String, AssertionFactory<?>> assertionFactoriesByName = new HashMap<>();

	/**
	 * Private constructor that populates the factory map.
	 *
	 * @param assertionFactories the factories to register
	 */
	private AssertionFactories(AssertionFactory<?>[] assertionFactories) {
		for (AssertionFactory<?> assertionFactory : assertionFactories) {
			this.assertionFactoriesByName.put(assertionFactory.name(), assertionFactory);
		}
	}

	/**
	 * Retrieves an assertion factory by its name.
	 *
	 * @param value the name of the factory to retrieve
	 * @return the assertion factory with the given name
	 * @throws AzertioException if no factory with the given name exists
	 */
	public AssertionFactory<?> byName(String value) {
		AssertionFactory<?> assertionFactory = assertionFactoriesByName.get(value);
		if (assertionFactory == null) {
			throw new AzertioException(
				"Unknown assertion {}\n\tAccepted assertions are: {}",
				value,
				String.join(", ", assertionFactoriesByName.keySet())
			);
		}
		return assertionFactory;
	}

}
