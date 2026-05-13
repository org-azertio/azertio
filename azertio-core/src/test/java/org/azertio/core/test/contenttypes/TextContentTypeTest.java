package org.azertio.core.test.contenttypes;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.azertio.core.assertions.AssertionAdapter;
import org.azertio.core.contenttypes.TextContentType;
import org.azertio.core.contributors.ContentType.ComparisonMode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TextContentTypeTest {

	private final TextContentType comparator = new TextContentType();

	// --- accepts ---

	@Test
	void accepts_text_shorthand() {
		assertThat(comparator.accepts("text")).isTrue();
	}

	@Test
	void accepts_plain_shorthand() {
		assertThat(comparator.accepts("plain")).isTrue();
	}

	@Test
	void accepts_textPlain() {
		assertThat(comparator.accepts("text/plain")).isTrue();
	}

	@Test
	void accepts_json_returnsFalse() {
		assertThat(comparator.accepts("application/json")).isFalse();
	}


	// --- assertContentEquals STRICT ---

	@Test
	void contentEquals_strict_identical_passes() {
		String text = "Hello World\nLine two";
		assertThatCode(() -> comparator.assertContentEquals(text, text, ComparisonMode.STRICT))
			.doesNotThrowAnyException();
	}

	@Test
	void contentEquals_strict_differentLine_fails() {
		assertThatThrownBy(() -> comparator.assertContentEquals(
			"Hello\nWorld",
			"Hello\nJava",
			ComparisonMode.STRICT
		)).isInstanceOf(AssertionError.class)
			.hasMessageContaining("line 2")
			.hasMessageContaining("World")
			.hasMessageContaining("Java");
	}

	@Test
	void contentEquals_strict_extraActualLine_fails() {
		assertThatThrownBy(() -> comparator.assertContentEquals(
			"Hello",
			"Hello\nextra",
			ComparisonMode.STRICT
		)).isInstanceOf(AssertionError.class);
	}

	@Test
	void contentEquals_strict_missingActualLine_fails() {
		assertThatThrownBy(() -> comparator.assertContentEquals(
			"Hello\nWorld",
			"Hello",
			ComparisonMode.STRICT
		)).isInstanceOf(AssertionError.class).hasMessageContaining("<missing>");
	}


	// --- assertContentEquals ANY_ORDER ---

	@Test
	void contentEquals_anyOrder_differentLineOrder_passes() {
		assertThatCode(() -> comparator.assertContentEquals(
			"Apple\nBanana\nCherry",
			"Cherry\nApple\nBanana",
			ComparisonMode.ANY_ORDER
		)).doesNotThrowAnyException();
	}

	@Test
	void contentEquals_anyOrder_missingLine_fails() {
		assertThatThrownBy(() -> comparator.assertContentEquals(
			"Apple\nBanana\nCherry",
			"Apple\nBanana",
			ComparisonMode.ANY_ORDER
		)).isInstanceOf(AssertionError.class)
			.hasMessageContaining("Missing")
			.hasMessageContaining("Cherry");
	}

	@Test
	void contentEquals_anyOrder_extraLine_fails() {
		assertThatThrownBy(() -> comparator.assertContentEquals(
			"Apple\nBanana",
			"Apple\nBanana\nExtra",
			ComparisonMode.ANY_ORDER
		)).isInstanceOf(AssertionError.class)
			.hasMessageContaining("Unexpected")
			.hasMessageContaining("Extra");
	}


	// --- assertContentEquals LOOSE ---

	@Test
	void contentEquals_loose_subsetLines_passes() {
		assertThatCode(() -> comparator.assertContentEquals(
			"Apple\nCherry",
			"Apple\nBanana\nCherry",
			ComparisonMode.LOOSE
		)).doesNotThrowAnyException();
	}

	@Test
	void contentEquals_loose_missingExpectedLine_fails() {
		assertThatThrownBy(() -> comparator.assertContentEquals(
			"Apple\nMango",
			"Apple\nBanana",
			ComparisonMode.LOOSE
		)).isInstanceOf(AssertionError.class)
			.hasMessageContaining("Mango");
	}


	// --- assertFragmentEquals: line number ---

	@Test
	void fragmentEquals_lineNumber_passes() {
		assertThatCode(() -> comparator.assertFragmentEquals(
			"line one\nline two\nline three",
			"2",
			new AssertionAdapter(Matchers.equalTo("line two"))
		)).doesNotThrowAnyException();
	}

	@Test
	void fragmentEquals_lineNumber_wrongValue_fails() {
		assertThatThrownBy(() -> comparator.assertFragmentEquals(
			"line one\nline two",
			"1",
			new AssertionAdapter(Matchers.equalTo("line two"))
		)).isInstanceOf(AssertionError.class);
	}

	@Test
	void fragmentEquals_lineNumberOutOfBounds_fails() {
		assertThatThrownBy(() -> comparator.assertFragmentEquals(
			"only one line",
			"5",
			new AssertionAdapter(Matchers.anything())
		)).isInstanceOf(AssertionError.class).hasMessageContaining("does not exist");
	}


	// --- assertFragmentEquals: regex ---

	@Test
	void fragmentEquals_regex_fullMatch_passes() {
		assertThatCode(() -> comparator.assertFragmentEquals(
			"status: OK\ncode: 200",
			"code: (\\d+)",
			new AssertionAdapter(Matchers.equalTo("200"))
		)).doesNotThrowAnyException();
	}

	@Test
	void fragmentEquals_regex_noCapturingGroup_returnsFullMatch() {
		assertThatCode(() -> comparator.assertFragmentEquals(
			"status: OK",
			"status: OK",
			new AssertionAdapter(Matchers.equalTo("status: OK"))
		)).doesNotThrowAnyException();
	}

	@Test
	void fragmentEquals_regex_noMatch_fails() {
		assertThatThrownBy(() -> comparator.assertFragmentEquals(
			"no match here",
			"missing: (\\w+)",
			new AssertionAdapter(Matchers.anything())
		)).isInstanceOf(AssertionError.class).hasMessageContaining("No line matches");
	}

	@Test
	void fragmentEquals_invalidRegex_fails() {
		assertThatThrownBy(() -> comparator.assertFragmentEquals(
			"content",
			"[invalid",
			new AssertionAdapter(Matchers.anything())
		)).isInstanceOf(AssertionError.class).hasMessageContaining("Invalid fragment path");
	}


	// --- assertComplyWithSchema ---

	@Test
	void complyWithSchema_contentMatchesPattern_passes() {
		assertThatCode(() -> comparator.assertComplyWithSchema(
			"order-12345",
			"order-\\d+"
		)).doesNotThrowAnyException();
	}

	@Test
	void complyWithSchema_contentDoesNotMatchPattern_fails() {
		assertThatThrownBy(() -> comparator.assertComplyWithSchema(
			"invoice-12345",
			"order-\\d+"
		)).isInstanceOf(AssertionError.class).hasMessageContaining("pattern");
	}

	@Test
	void complyWithSchema_multilineContent_passes() {
		assertThatCode(() -> comparator.assertComplyWithSchema(
			"BEGIN\nsome content\nEND",
			"BEGIN.*END"
		)).doesNotThrowAnyException();
	}

	@Test
	void complyWithSchema_invalidPattern_throws() {
		assertThatThrownBy(() -> comparator.assertComplyWithSchema(
			"content",
			"[invalid"
		)).isInstanceOf(AssertionError.class).hasMessageContaining("Invalid pattern");
	}


	// --- extractValue ---

	@Test
	void extractValue_lineNumber_returnsString() {
		assertThat(comparator.extractValue("line one\nline two\nline three", "2"))
			.isEqualTo("line two");
	}

	@Test
	void extractValue_regexWithCaptureGroup_returnsMatch() {
		assertThat(comparator.extractValue("status: OK\ncode: 200", "code: (\\d+)"))
			.isEqualTo("200");
	}

	@Test
	void extractValue_regexNoCaptureGroup_returnsFullMatch() {
		assertThat(comparator.extractValue("status: OK", "status: OK"))
			.isEqualTo("status: OK");
	}

	@Test
	void extractValue_lineNumberOutOfBounds_throws() {
		assertThatThrownBy(() -> comparator.extractValue("only one line", "5"))
			.isInstanceOf(AssertionError.class).hasMessageContaining("does not exist");
	}

	@Test
	void extractValue_noRegexMatch_throws() {
		assertThatThrownBy(() -> comparator.extractValue("no match here", "missing: (\\w+)"))
			.isInstanceOf(AssertionError.class).hasMessageContaining("No line matches");
	}
}
