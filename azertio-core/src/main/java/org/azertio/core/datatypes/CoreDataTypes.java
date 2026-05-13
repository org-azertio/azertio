package org.azertio.core.datatypes;


import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Scope;
import org.azertio.core.DataType;
import org.azertio.core.contributors.DataTypeProvider;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.stream.Stream;

/**
 * Provides the core set of built-in data types for Azertio.
 *
 * <p>This class is registered as an extension and provides commonly used data types
 * for BDD test step expressions. All data types are available as public static constants
 * for direct usage.</p>
 *
 * <h2>Available Data Types</h2>
 *
 * <h3>Text Types</h3>
 * <ul>
 *   <li>{@link #WORD} - Single word (alphanumeric with dashes)</li>
 *   <li>{@link #TEXT} - Quoted text string</li>
 *   <li>{@link #ID} - Identifier string</li>
 *   <li>{@link #URL} - URL/URI</li>
 *   <li>{@link #FILE} - File path</li>
 * </ul>
 *
 * <h3>Numeric Types</h3>
 * <ul>
 *   <li>{@link #NUMBER} - Integer (int)</li>
 *   <li>{@link #BIG_NUMBER} - Large integer (long)</li>
 *   <li>{@link #DECIMAL} - Decimal number (BigDecimal)</li>
 * </ul>
 *
 * <h3>Temporal Types</h3>
 * <ul>
 *   <li>{@link #DATE} - ISO date</li>
 *   <li>{@link #TIME} - ISO time</li>
 *   <li>{@link #DATE_TIME} - ISO date-time</li>
 *   <li>{@link #DURATION} - Time-based duration</li>
 *   <li>{@link #PERIOD} - Date-based period</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * import static org.azertio.core.datatypes.CoreDataTypes.*;
 *
 * Integer count = (Integer) NUMBER.parse("1,234");
 * LocalDate date = (LocalDate) DATE.parse("2024-01-15");
 * }</pre>
 *
 * @see DataType
 * @see DataTypeProvider

 * @author Luis Iñesta Gelabert - luiinge@gmail.com */
@Extension(scope = Scope.SINGLETON)
public class CoreDataTypes implements DataTypeProvider {


	/** Single word without spaces. Pattern: {@code [\w-]+}. Example: {@code hello}, {@code my-word} */
	public static final DataType WORD = new RegexDataTypeAdapter<>(
		"word", "[\\w-]+", String.class, x -> x, "<word>"
	);

	/** URL/URI. Example: {@code https://example.com/path} */
	public static final DataType URL = new RegexDataTypeAdapter<> (
		"url",
		"\\w+:(\\/?\\/?)[^\\s]+",
		URI.class,
		URI::new,
		"<protocol://host/path>"
	);

	/** Java-like identifier. Pattern: {@code [a-zA-Z_$][\w$]*}. Example: {@code userId}, {@code my_var} */
	public static final DataType ID = new RegexDataTypeAdapter<>(
		"id", "[a-zA-Z_$][\\w$]*", String.class, x -> x, "<identifier>"
	);

	/** File path (quoted). Parses to {@link Path}. Example: {@code '/path/to/file.txt'} */
	public static final DataType FILE = new RegexDataTypeAdapter<>(
		"file",
		"\"([^\"\\\\]*(\\\\.[^\"\\\\]*)*)\"|'([^'\\\\]*(\\\\.[^'\\\\]*)*)'",
		Path.class,
		x -> Path.of(x.substring(1, x.length() - 1)),
		"local path to file/dir"
	);

	/** Quoted text string. Example: {@code 'hello world'}, {@code "text"} */
	public static final DataType TEXT = new RegexDataTypeAdapter<>(
		"text",
		"\"([^\"\\\\]*(\\\\.[^\"\\\\]*)*)\"|'([^'\\\\]*(\\\\.[^'\\\\]*)*)'",
		String.class,
		x -> x.substring(1, x.length()-1),
		"'text'"
	);

	/** Integer number. Supports grouping separators. Example: {@code 12345}, {@code 12,345} */
	public static final DataType NUMBER = new NumberDataTypeAdapter<>(
		"integer",
		Integer.class,
		false,
		false,
		Number::intValue
	);

	/** Large integer (long). Supports grouping separators. Example: {@code 9999999999} */
	public static final DataType BIG_NUMBER = new NumberDataTypeAdapter<>(
		"big-integer",
		Long.class,
		false,
		false,
		Number::longValue
	);

	/** Decimal number (BigDecimal). Example: {@code 12,345.67} */
	public static final DataType DECIMAL = new NumberDataTypeAdapter<>(
		"decimal",
		BigDecimal.class,
		true,
		true,
		BigDecimal.class::cast
	);

	/** ISO date. Format: {@code yyyy-MM-dd}. Example: {@code 2024-01-15} */
	public static final DataType DATE = new TemporalDataTypeAdapter<>(
		"date", LocalDate.class, true, false, LocalDate::from
	);

	/** ISO time. Format: {@code HH:mm:ss}. Example: {@code 14:30:00} */
	public static final DataType TIME = new TemporalDataTypeAdapter<>(
		"time", LocalTime.class, false, true, LocalTime::from
	);

	/** ISO date-time. Format: {@code yyyy-MM-ddTHH:mm:ss}. Example: {@code 2024-01-15T14:30:00} */
	public static final DataType DATE_TIME = new TemporalDataTypeAdapter<>(
		"date-time", LocalDateTime.class, true, true, LocalDateTime::from
	);

	/** Time-based duration. Format: {@code <hours>h <minutes>m <seconds>s <milliseconds>ms}. Example: {@code 2h 30m 15s} */
	public static final DataType DURATION = new DurationDataTypeAdapter("duration");

	/** Date-based period. Format: {@code <years>y <months>m <days>d}. Example: {@code 1y 6m 15d} */
	public static final DataType PERIOD = new PeriodDataTypeAdapter("period");

	/** Array of all core data types. */
	public static final DataType[] ALL = {
		WORD,
		URL,
		ID,
		FILE,
		TEXT,
		NUMBER,
		BIG_NUMBER,
		DECIMAL,
		DATE,
		TIME,
		DATE_TIME,
		DURATION,
		PERIOD
	};

	@Override
	public Stream<DataType> dataTypes() {
		return Stream.of(ALL);
	}


}
