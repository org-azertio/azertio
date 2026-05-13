/**
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 */
package org.azertio.core.test.datatypes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.Duration;
import java.time.Period;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.azertio.core.datatypes.CoreDataTypes.DURATION;
import static org.azertio.core.datatypes.CoreDataTypes.PERIOD;


class TestDurationType {


	public static Stream<Arguments> durations() {
		return Stream.of(
			Arguments.of("1h 2m 3s 4ms", Duration.ofHours(1).plusMinutes(2).plusSeconds(3).plusMillis(4)),
			Arguments.of("1h 2m 3s", Duration.ofHours(1).plusMinutes(2).plusSeconds(3)),
			Arguments.of("1h 2m", Duration.ofHours(1).plusMinutes(2)),
			Arguments.of("1h", Duration.ofHours(1)),
			Arguments.of("2m 3s 4ms", Duration.ofMinutes(2).plusSeconds(3).plusMillis(4)),
			Arguments.of("3s 4ms", Duration.ofSeconds(3).plusMillis(4)),
			Arguments.of("4ms", Duration.ofMillis(4)),
			Arguments.of("0ms", Duration.ZERO)
		);
	}



	@ParameterizedTest
	@MethodSource("durations")
	void testDuration(String value, Duration actual) {
		assertTrue(DURATION.matcher(value).matches());
		assertEquals(DURATION.parse(value), actual);
	}

	@Test
	void testEmptyDuration() {
		assertFalse(DURATION.matcher("").matches());
	}


	public static Stream<Arguments> periods() {
		return Stream.of(
			Arguments.of("1y 2m 3d", Period.of(1, 2, 3)),
			Arguments.of("1y 2m", Period.of(1, 2, 0)),
			Arguments.of("1y", Period.ofYears(1)),
			Arguments.of("2m 3d", Period.of(0,2,3)),
			Arguments.of("3d", Period.ofDays(3)),
			Arguments.of("0d", Period.ZERO)
		);
	}


	@ParameterizedTest
	@MethodSource("periods")
	void testPeriods(String value, Period actual) {
		assertTrue(PERIOD.matcher(value).matches());
		assertEquals(((Period)PERIOD.parse(value)).getDays(), actual.getDays());
	}

	@Test
	void testEmptyPeriod() {
		assertFalse(PERIOD.matcher("").matches());
	}


}
