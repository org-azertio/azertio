package org.azertio.core.test.util;

import org.junit.jupiter.api.Test;
import org.azertio.core.util.Either;

import static org.junit.jupiter.api.Assertions.*;

class EitherTest {

	@Test
	void valuePresent_returnsValue() {
		var either = Either.of("hello", "fallback");
		assertTrue(either.value().isPresent());
		assertEquals("hello", either.value().get());
	}

	@Test
	void valueNull_returnsEmptyOptional() {
		var either = Either.<String, String>of((String) null, "fallback");
		assertTrue(either.value().isEmpty());
	}

	@Test
	void fallback_returnsFallbackValue() {
		var either = Either.of("hello", "fallback");
		assertEquals("fallback", either.fallback());
	}

	@Test
	void valueWithMapper_usesValueWhenPresent() {
		var either = Either.of("hello", "fallback");
		assertEquals("hello", either.value(f -> "from-fallback"));
	}

	@Test
	void valueWithMapper_usesMappedFallbackWhenValueAbsent() {
		var either = Either.<String, String>of((String) null, "fallback");
		assertEquals("FALLBACK", either.value(f -> f.toUpperCase()));
	}

	@Test
	void fallbackWithMapper_usesFallbackWhenPresent() {
		var either = Either.of("hello", "fallback");
		assertEquals("fallback", either.fallback(v -> "from-value"));
	}

	@Test
	void fallbackWithMapper_usesMappedValueWhenFallbackAbsent() {
		var either = Either.<String, String>of("hello");
		assertNull(either.fallback());
		assertEquals("HELLO", either.fallback(v -> v.toUpperCase()));
	}

	@Test
	void ofWithSuppliers_evaluatesLazily() {
		var called = new boolean[]{false};
		var either = Either.of(() -> { called[0] = true; return "v"; }, () -> "fb");
		assertFalse(called[0]);
		either.value();
		assertTrue(called[0]);
	}

	@Test
	void fallbackFactory_onlyFallback() {
		var either = Either.<String, String>fallback("only-fallback");
		assertTrue(either.value().isEmpty());
		assertEquals("only-fallback", either.fallback());
	}

	@Test
	void fallbackSupplierFactory() {
		var either = Either.<String, String>fallback(() -> "lazy-fallback");
		assertEquals("lazy-fallback", either.fallback());
	}
}