package org.azertio.core.test.util;

import org.junit.jupiter.api.Test;
import org.azertio.core.util.TokenParser;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TokenParserTest {

	@Test
	void parsesLiterals() {
		var parser = new TokenParser("helloworld", List.of("hello", "world"), List.of());
		assertTrue(parser.hasMoreTokens());
		assertEquals("hello", parser.nextToken());
		assertTrue(parser.hasMoreTokens());
		assertEquals("world", parser.nextToken());
		assertFalse(parser.hasMoreTokens());
	}

	@Test
	void parsesRegex() {
		var parser = new TokenParser("abc123", List.of(), List.of("[a-z]+", "[0-9]+"));
		assertEquals("abc", parser.nextToken());
		assertEquals("123", parser.nextToken());
		assertFalse(parser.hasMoreTokens());
	}

	@Test
	void prefersLongestMatch() {
		var parser = new TokenParser("foobar", List.of("foo", "foobar"), List.of());
		assertEquals("foobar", parser.nextToken());
		assertFalse(parser.hasMoreTokens());
	}

	@Test
	void emptyStringHasNoTokens() {
		var parser = new TokenParser("", List.of("x"), List.of());
		assertFalse(parser.hasMoreTokens());
	}

	@Test
	void noMatchHasNoTokens() {
		var parser = new TokenParser("xyz", List.of("abc"), List.of());
		assertFalse(parser.hasMoreTokens());
	}

	@Test
	void literalWithSpecialRegexChars() {
		var parser = new TokenParser("a.b+c", List.of("a.b+c"), List.of());
		assertTrue(parser.hasMoreTokens());
		assertEquals("a.b+c", parser.nextToken());
		assertFalse(parser.hasMoreTokens());
	}

	@Test
	void mixedLiteralsAndRegex() {
		var parser = new TokenParser("hello42world", List.of("hello", "world"), List.of("[0-9]+"));
		assertEquals("hello", parser.nextToken());
		assertEquals("42", parser.nextToken());
		assertEquals("world", parser.nextToken());
		assertFalse(parser.hasMoreTokens());
	}
}
