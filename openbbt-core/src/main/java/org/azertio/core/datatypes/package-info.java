/**
 * Core data type implementations for Azertio.
 *
 * <p>This package provides implementations of the {@link org.azertio.core.DataType}
 * interface for commonly used data types in BDD testing scenarios. These data types are used
 * to parse and validate arguments in test step expressions.</p>
 *
 * <h2>Available Data Types</h2>
 *
 * <p>The following data types are provided through {@link CoreDataTypes}:</p>
 *
 * <h3>Text Types</h3>
 * <ul>
 *   <li>{@code word} - Single word without spaces</li>
 *   <li>{@code text} - Quoted text string</li>
 *   <li>{@code id} - Identifier (alphanumeric with underscores, dashes, dots)</li>
 *   <li>{@code url} - URL/URI</li>
 *   <li>{@code file} - File path</li>
 * </ul>
 *
 * <h3>Numeric Types</h3>
 * <ul>
 *   <li>{@code number} - Integer number</li>
 *   <li>{@code big-integer} - Long integer</li>
 *   <li>{@code decimal} - Decimal number (BigDecimal)</li>
 * </ul>
 *
 * <h3>Temporal Types</h3>
 * <ul>
 *   <li>{@code date} - ISO date (yyyy-MM-dd)</li>
 *   <li>{@code time} - ISO time (HH:mm:ss)</li>
 *   <li>{@code date-time} - ISO date-time (yyyy-MM-ddTHH:mm:ss)</li>
 *   <li>{@code duration} - Time duration (e.g., "2h 30m 15s")</li>
 *   <li>{@code period} - Date-based period (e.g., "1y 6m 15d")</li>
 * </ul>
 *
 * <h2>Creating Custom Data Types</h2>
 *
 * <p>Custom data types can be created by:</p>
 * <ul>
 *   <li>Using {@link RegexDataTypeAdapter} for simple regex-based types</li>
 *   <li>Extending {@link DataTypeAdapter} for more complex parsing logic</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * import static org.azertio.core.datatypes.CoreDataTypes.*;
 *
 * // Parse values
 * Integer count = (Integer) NUMBER.parse("1,234");
 * LocalDate date = (LocalDate) DATE.parse("2024-01-15");
 * Duration timeout = (Duration) DURATION.parse("5m 30s");
 *
 * // Pattern matching
 * boolean isValid = NUMBER.matcher("12345").matches();
 * }</pre>
 *
 * @see org.azertio.core.DataType
 * @see org.azertio.core.contributors.DataTypeProvider
 * @see CoreDataTypes
 * @see DataTypeAdapter
 * @see RegexDataTypeAdapter

 * @author Luis Iñesta Gelabert - luiinge@gmail.com */
package org.azertio.core.datatypes;