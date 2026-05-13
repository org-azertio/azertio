/**
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 */
package org.azertio.core.test.datatypes;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.azertio.core.AzertioException;
import java.math.BigDecimal;
import static org.azertio.core.datatypes.CoreDataTypes.DECIMAL;
import static org.azertio.core.datatypes.CoreDataTypes.NUMBER;



class TestNumberType {


	@Test
	void testAttemptParseWithWrongValue() {
		Assertions.assertThrowsExactly(
			AzertioException.class,
			() -> NUMBER.parse("xxxx"),
			"Error parsing type int using language en: 'xxxxx'"
		);
	}


	@Test
	void testInteger() {
		var type = NUMBER;
		Assertions.assertTrue(type.matcher("12345").matches());
		Assertions.assertEquals(12345, type.parse("12345"));
		Assertions.assertTrue(type.matcher("12,345").matches());
		Assertions.assertEquals(12345, type.parse("12,345"));
		Assertions.assertFalse(type.matcher("12,345.54").matches());
		Assertions.assertFalse(type.matcher("xxxxx").matches());
	}


	@Test
	void testDecimal() {
		var type = DECIMAL;
		Assertions.assertFalse(type.matcher("12345").matches());
		Assertions.assertTrue(type.matcher("12345.0").matches());
		Assertions.assertEquals(BigDecimal.valueOf(12345.0),type.parse("12345.0"));
		Assertions.assertFalse(type.matcher("12,345").matches());
		Assertions.assertTrue(type.matcher("12,345.0").matches());
		Assertions.assertEquals(BigDecimal.valueOf(12345.0), type.parse("12,345.0"));
		Assertions.assertTrue(type.matcher("12,345.54").matches());
		Assertions.assertEquals(BigDecimal.valueOf(12345.54),type.parse("12,345.54"));
		Assertions.assertFalse(type.matcher("xxxxx").matches());
	}

}
