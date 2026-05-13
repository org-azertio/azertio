/**
 * Provides the assertion framework for Azertio based on Hamcrest Matchers.
 *
 * <p>This package contains assertion factory implementations that validate values
 * at runtime using localizable expression patterns. Assertions integrate with
 * Hamcrest to provide descriptive error messages and an extensible API.</p>
 *
 * <h2>Main Classes</h2>
 *
 * <ul>
 *   <li>{@link org.azertio.core.assertions.AssertionAdapter} - Adapter that wraps
 *       a {@link org.hamcrest.Matcher} to implement the
 *       {@link org.azertio.core.Assertion} interface.</li>
 *   <li>{@link org.azertio.core.assertions.AssertionFactoryAdapter} - Abstract base
 *       class for implementing assertion factories with localization support.</li>
 *   <li>{@link org.azertio.core.AssertionFactories} - Registry that groups
 *       multiple assertion factories and allows lookup by name.</li>
 *   <li>{@link org.azertio.core.assertions.CoreAssertionFactories} - Main provider
 *       that registers the default assertion factories for the framework.</li>
 * </ul>
 *
 * <h2>Available Assertion Factories</h2>
 *
 * <ul>
 *   <li>{@link org.azertio.core.assertions.ComparableAssertionFactory} - For
 *       {@link java.lang.Comparable} types such as numbers and decimals.</li>
 *   <li>{@link org.azertio.core.assertions.StringAssertionFactory} - For text
 *       strings with operations like equals, contains, startsWith, etc.</li>
 *   <li>{@link org.azertio.core.assertions.TemporalAssertionFactory} - For temporal
 *       types such as {@link java.time.LocalDate}, {@link java.time.LocalTime} and
 *       {@link java.time.LocalDateTime}.</li>
 * </ul>
 *
 * <h2>Typical Usage</h2>
 *
 * <pre>{@code
 * // Get the factory registry
 * AssertionFactories factories = AssertionFactories.CORE_ENGLISH;
 *
 * // Get a specific factory
 * AssertionFactory<?> factory = factories.byName("number-assertion");
 *
 * // Get patterns for a locale
 * List<AssertionPattern<?>> patterns = factory.patterns(Locale.ENGLISH);
 *
 * // Create an assertion from a pattern
 * Assertion assertion = factory.assertion(pattern, "is greater than 10");
 *
 * // Evaluate the assertion
 * boolean passed = assertion.test(15); // true
 * }</pre>
 *
 * <h2>Extension</h2>
 *
 * <p>To add new assertion factories, extend
 * {@link org.azertio.core.assertions.AssertionFactoryAdapter} and implement
 * the {@code fillSuppliers()} method to register supported assertions.</p>
 *
 * @author Luis Iñesta Gelabert
 * @see org.azertio.core.Assertion
 * @see org.azertio.core.AssertionFactory
 * @see org.azertio.core.contributors.AssertionFactoryProvider
 * @see org.hamcrest.Matcher
 */
package org.azertio.core.assertions;