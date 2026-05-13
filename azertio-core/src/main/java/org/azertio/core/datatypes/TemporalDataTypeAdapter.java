package org.azertio.core.datatypes;


import org.azertio.core.DataType;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;


/**
 * Data type adapter for temporal values (date, time, date-time).
 *
 * <p>This adapter handles parsing of ISO-formatted temporal strings:</p>
 * <ul>
 *   <li>Date only: {@code yyyy-MM-dd} (e.g., "2024-01-15")</li>
 *   <li>Time only: {@code HH:mm:ss} (e.g., "14:30:00")</li>
 *   <li>Date-time: {@code yyyy-MM-ddTHH:mm:ss} (e.g., "2024-01-15T14:30:00")</li>
 * </ul>
 *
 * <p>The adapter uses standard ISO formatters from {@link DateTimeFormatter}.</p>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * DataType DATE = new TemporalDataTypeAdapter<>(
 *     "date", LocalDate.class, true, false, LocalDate::from
 * );
 *
 * DataType TIME = new TemporalDataTypeAdapter<>(
 *     "time", LocalTime.class, false, true, LocalTime::from
 * );
 * }</pre>
 *
 * @param <T> the target temporal type (LocalDate, LocalTime, LocalDateTime, etc.)
 * @see CoreDataTypes#DATE
 * @see CoreDataTypes#TIME
 * @see CoreDataTypes#DATE_TIME
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 */
public class TemporalDataTypeAdapter<T extends TemporalAccessor> extends DataTypeAdapter<T>
implements DataType {


	/**
	 * Creates a new temporal data type adapter.
	 *
	 * @param name          the unique name of this data type
	 * @param javaType      the target Java temporal class
	 * @param withDate      whether to include date component
	 * @param withTime      whether to include time component
	 * @param temporalQuery the query to extract the temporal value
	 */
	public TemporalDataTypeAdapter(
		String name,
		Class<T> javaType,
		boolean withDate,
		boolean withTime,
		TemporalQuery<T> temporalQuery
	) {
		super(
			name,
			javaType,
			regex(withDate, withTime),
			hint(withDate, withTime),
			dateTimeParser(withDate, withTime, temporalQuery)
		);
	}


	private static String regex(boolean withDate, boolean withTime) {
		if (withDate && withTime) {
			return "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(:\\d{2}(\\.\\d{3})?)?"; // Match ISO date-time format
		} else if (withDate) {
			return "\\d{4}-\\d{2}-\\d{2}"; // Match ISO date format
		} else {
			return "\\d{2}:\\d{2}(:\\d{2}(\\.\\d{3})?)?"; // Match time format
		}
	}


	private static String hint(boolean withDate, boolean withTime) {
		if (withDate && withTime) {
			return "yyyy-MM-ddTHH:mm:ss"; // Match ISO date-time format
		} else if (withDate) {
			return "yyyy-MM-dd"; // Match ISO date format
		} else {
			return "HH:mm:ss"; // Match time format
		}
	}




	public static <T extends TemporalAccessor> Function<String,T> dateTimeParser(
		boolean withDate,
		boolean withTime,
		TemporalQuery<T> temporalQuery
	) {
		return (String input) -> {
			List<DateTimeFormatter> formatters = new ArrayList<>();
			if (withDate && withTime) {
				return DateTimeFormatter.ISO_DATE_TIME.parse(input, temporalQuery);
			} else if (withDate) {
				return DateTimeFormatter.ISO_DATE.parse(input, temporalQuery);
			} else {
				return DateTimeFormatter.ISO_TIME.parse(input, temporalQuery);
			}
		};
	}

}
