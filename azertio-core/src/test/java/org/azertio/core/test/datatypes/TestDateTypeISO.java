/**
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 */
package org.azertio.core.test.datatypes;


import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.azertio.core.datatypes.CoreDataTypes.*;

class TestDateTypeISO {


	@Test
	void testISODate() {
		assertTrue(DATE.matcher("2018-05-30").matches());
		assertEquals(DATE.parse("2018-05-30"),LocalDate.of(2018, 5, 30));
	}


	@Test
	void testISOTime() {
		assertTrue(TIME.matcher("17:35").matches());
		assertEquals(TIME.parse("17:35"),LocalTime.of(17, 35));
		assertTrue(TIME.matcher("17:35:29").matches());
		assertEquals(TIME.parse("17:35:29"),LocalTime.of(17, 35, 29));
		assertTrue(TIME.matcher("17:35:29.743").matches());
		assertEquals(TIME.parse("17:35:29.743"),LocalTime.of(17, 35, 29, 743000000));
	}


	@Test
	void testISODateTime() {
		assertTrue(DATE_TIME.matcher("2018-05-30T17:35").matches());
		assertEquals(DATE_TIME.parse("2018-05-30T17:35"),LocalDateTime.of(2018, 5, 30, 17, 35));
		assertTrue(DATE_TIME.matcher("2018-05-30T17:35:29").matches());
		assertEquals(DATE_TIME.parse("2018-05-30T17:35:29"),LocalDateTime.of(2018, 5, 30, 17, 35, 29));
		assertTrue(DATE_TIME.matcher("2018-05-30T17:35:29.743").matches());
		assertEquals(DATE_TIME.parse("2018-05-30T17:35:29.743"),LocalDateTime.of(2018, 5, 30, 17, 35, 29, 743000000));
	 }

}
