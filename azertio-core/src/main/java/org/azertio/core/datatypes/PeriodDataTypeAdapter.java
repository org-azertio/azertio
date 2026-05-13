package org.azertio.core.datatypes;


import org.azertio.core.AzertioException;
import org.azertio.core.DataType;
import org.azertio.core.util.Patterns;
import java.time.Period;
import java.util.function.Function;
import java.util.regex.Pattern;


/**
 * Data type adapter for date-based periods.
 *
 * <p>This adapter parses period strings in a human-readable format with
 * years, months, and days components.</p>
 *
 * <h2>Format</h2>
 * <pre>{@code <years>y <months>m <days>d}</pre>
 *
 * <p>All components are optional, but at least one must be present.</p>
 *
 * <h2>Examples</h2>
 * <ul>
 *   <li>{@code 1y} - 1 year</li>
 *   <li>{@code 6m} - 6 months</li>
 *   <li>{@code 1y 6m} - 1 year and 6 months</li>
 *   <li>{@code 2y 3m 15d} - 2 years, 3 months, and 15 days</li>
 * </ul>
 *
 * @see CoreDataTypes#PERIOD
 * @see Period
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 */
public class PeriodDataTypeAdapter extends DataTypeAdapter<Period>
implements DataType {

	private static final Pattern PATTERN = Patterns.of(
		"(?=.*\\d+(?:y|m|d))(?:(\\d+)y)?\\s*(?:(\\d+)m)?\\s*(?:(\\d+)d)?"
	);
	public static final String HINT = "<years>y <months>m <days>d";

	/**
	 * Creates a new period data type adapter.
	 *
	 * @param name the unique name of this data type
	 */
	public PeriodDataTypeAdapter(String name) {
		super(
			name,
			Period.class,
			PATTERN.pattern(),
			HINT, // Example hint for period format
			periodParser()
		);
	}

	private static Function<String,Period> periodParser() {
		return source -> {
			Period period = Period.ZERO;
			var matcher = PATTERN.matcher(source);
			if (matcher.matches()) {
				   if (matcher.group(1) != null) {
					   period = period.plusYears(Integer.parseInt(matcher.group(1)));
				}
				if (matcher.group(2) != null) {
					period = period.plusMonths(Integer.parseInt(matcher.group(2)));
				}
				if (matcher.group(3) != null) {
					period = period.plusDays(Integer.parseInt(matcher.group(3)));
				}
				return period;
			}
			throw new AzertioException("Invalid period format: {} , expected: {}", source, HINT);
		};
	}


}
