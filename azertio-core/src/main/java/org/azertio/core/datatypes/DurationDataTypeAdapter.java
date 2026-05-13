package org.azertio.core.datatypes;


import org.azertio.core.AzertioException;
import org.azertio.core.DataType;
import org.azertio.core.util.Patterns;
import java.time.Duration;
import java.util.function.Function;
import java.util.regex.Pattern;


/**
 * Data type adapter for time-based durations.
 *
 * <p>This adapter parses duration strings in a human-readable format with
 * hours, minutes, seconds, and milliseconds components.</p>
 *
 * <h2>Format</h2>
 * <pre>{@code <hours>h <minutes>m <seconds>s <milliseconds>ms}</pre>
 *
 * <p>All components are optional, but at least one must be present.</p>
 *
 * <h2>Examples</h2>
 * <ul>
 *   <li>{@code 2h} - 2 hours</li>
 *   <li>{@code 30m} - 30 minutes</li>
 *   <li>{@code 2h 30m} - 2 hours and 30 minutes</li>
 *   <li>{@code 1h 15m 30s 250ms} - 1 hour, 15 minutes, 30 seconds, 250 milliseconds</li>
 * </ul>
 *
 * @see CoreDataTypes#DURATION
 * @see Duration
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 */
public class DurationDataTypeAdapter extends DataTypeAdapter<Duration>
implements DataType {

	private static final Pattern PATTERN = Patterns.of(
		"(?=.*\\d+(?:h|m|s|ms))(?:(\\d+)h)?\\s*(?:(\\d+)m)?\\s*(?:(\\d+)s)?\\s*(?:(\\d+)ms)?"
	);
	private static final String HINT = "<hours>h <minutes>m <seconds>s <milliseconds>ms";

	/**
	 * Creates a new duration data type adapter.
	 *
	 * @param name the unique name of this data type
	 */
	public DurationDataTypeAdapter(String name) {
		super(
			name,
			Duration.class,
			PATTERN.pattern(),
				HINT, // Example hint for period format
			durationParser()
		);
	}

	private static Function<String,Duration> durationParser() {
		return source -> {
			Duration duration = Duration.ZERO;
			var matcher = PATTERN.matcher(source);
			if (matcher.matches()) {
				if (matcher.group(1) != null) {
					duration = duration.plusHours(Integer.parseInt(matcher.group(1)));
				}
				if (matcher.group(2) != null) {
					duration = duration.plusMinutes(Integer.parseInt(matcher.group(2)));
				}
				if (matcher.group(3) != null) {
					duration = duration.plusSeconds(Integer.parseInt(matcher.group(3)));
				}
				if (matcher.group(4) != null) {
					duration = duration.plusMillis(Integer.parseInt(matcher.group(4)));
				}
				return duration;
			}
			throw new AzertioException("Invalid duration format: {}, expected: {}", source, HINT);
		};
	}


}
