package org.azertio.core.test.expressions;

import org.junit.jupiter.api.Test;
import org.azertio.core.expressions.ExpressionException;

import static org.junit.jupiter.api.Assertions.*;

class ExpressionExceptionTest {

	@Test
	void positionConstructor_containsTextAndPosition() {
		var ex = new ExpressionException("the expression", 5, "unexpected token");
		assertNotNull(ex.getMessage());
		assertTrue(ex.getMessage().contains("5"));
		assertTrue(ex.getMessage().contains("the expression"));
		assertTrue(ex.getMessage().contains("unexpected token"));
	}

	@Test
	void messageConstructor_formatsArgs() {
		var ex = new ExpressionException("invalid type: {}", "foo");
		assertEquals("invalid type: foo", ex.getMessage());
	}

	@Test
	void isRuntimeException() {
		assertInstanceOf(RuntimeException.class, new ExpressionException("msg"));
	}
}