package org.azertio.core.test.contenttypes;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.azertio.core.assertions.AssertionAdapter;
import org.azertio.core.contenttypes.YAMLContentType;
import org.azertio.core.contributors.ContentType.ComparisonMode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class YAMLContentTypeTest {

	private final YAMLContentType comparator = new YAMLContentType();

	// --- accepts ---

	@Test
	void accepts_yaml_shorthand() {
		assertThat(comparator.accepts("yaml")).isTrue();
	}

	@Test
	void accepts_applicationYaml() {
		assertThat(comparator.accepts("application/yaml")).isTrue();
	}

	@Test
	void accepts_textYaml() {
		assertThat(comparator.accepts("text/yaml")).isTrue();
	}

	@Test
	void accepts_applicationXYaml() {
		assertThat(comparator.accepts("application/x-yaml")).isTrue();
	}

	@Test
	void accepts_json_returnsFalse() {
		assertThat(comparator.accepts("application/json")).isFalse();
	}


	// --- assertContentEquals STRICT ---

	@Test
	void contentEquals_strict_identical_passes() {
		String yaml = """
			name: Alice
			age: 30
			""";
		assertThatCode(() -> comparator.assertContentEquals(yaml, yaml, ComparisonMode.STRICT))
			.doesNotThrowAnyException();
	}

	@Test
	void contentEquals_strict_differentValue_fails() {
		assertThatThrownBy(() -> comparator.assertContentEquals(
			"""
			name: Alice
			""",
			"""
			name: Bob
			""",
			ComparisonMode.STRICT
		)).isInstanceOf(AssertionError.class).hasMessageContaining("STRICT");
	}

	@Test
	void contentEquals_strict_extraField_fails() {
		assertThatThrownBy(() -> comparator.assertContentEquals(
			"""
			name: Alice
			""",
			"""
			name: Alice
			age: 30
			""",
			ComparisonMode.STRICT
		)).isInstanceOf(AssertionError.class);
	}


	// --- assertContentEquals ANY_ORDER ---

	@Test
	void contentEquals_anyOrder_differentListOrder_passes() {
		assertThatCode(() -> comparator.assertContentEquals(
			"""
			items:
			  - 1
			  - 2
			  - 3
			""",
			"""
			items:
			  - 3
			  - 1
			  - 2
			""",
			ComparisonMode.ANY_ORDER
		)).doesNotThrowAnyException();
	}

	@Test
	void contentEquals_anyOrder_differentValues_fails() {
		assertThatThrownBy(() -> comparator.assertContentEquals(
			"""
			items:
			  - 1
			  - 2
			""",
			"""
			items:
			  - 1
			  - 9
			""",
			ComparisonMode.ANY_ORDER
		)).isInstanceOf(AssertionError.class);
	}


	// --- assertContentEquals LOOSE ---

	@Test
	void contentEquals_loose_subsetFields_passes() {
		assertThatCode(() -> comparator.assertContentEquals(
			"""
			name: Alice
			""",
			"""
			name: Alice
			age: 30
			city: Madrid
			""",
			ComparisonMode.LOOSE
		)).doesNotThrowAnyException();
	}

	@Test
	void contentEquals_loose_missingExpectedField_fails() {
		assertThatThrownBy(() -> comparator.assertContentEquals(
			"""
			name: Alice
			role: admin
			""",
			"""
			name: Alice
			""",
			ComparisonMode.LOOSE
		)).isInstanceOf(AssertionError.class);
	}


	// --- assertFragmentEquals ---

	@Test
	void fragmentEquals_existingPath_passes() {
		assertThatCode(() -> comparator.assertFragmentEquals(
			"""
			person:
			  name: Alice
			  age: 30
			""",
			"$.person.name",
			new AssertionAdapter(Matchers.equalTo("Alice"))
		)).doesNotThrowAnyException();
	}

	@Test
	void fragmentEquals_listElement_passes() {
		assertThatCode(() -> comparator.assertFragmentEquals(
			"""
			items:
			  - 10
			  - 20
			  - 30
			""",
			"$.items[1]",
			new AssertionAdapter(Matchers.equalTo(20))
		)).doesNotThrowAnyException();
	}

	@Test
	void fragmentEquals_wrongValue_fails() {
		assertThatThrownBy(() -> comparator.assertFragmentEquals(
			"""
			name: Alice
			""",
			"$.name",
			new AssertionAdapter(Matchers.equalTo("Bob"))
		)).isInstanceOf(AssertionError.class);
	}

	@Test
	void fragmentEquals_missingPath_fails() {
		assertThatThrownBy(() -> comparator.assertFragmentEquals(
			"""
			name: Alice
			""",
			"$.missing",
			new AssertionAdapter(Matchers.anything())
		)).isInstanceOf(AssertionError.class).hasMessageContaining("$.missing");
	}


	// --- assertComplyWithSchema ---

	private static final String JSON_SCHEMA = """
		{
		  "$schema": "https://json-schema.org/draft/2020-12/schema",
		  "type": "object",
		  "properties": {
		    "name": {"type": "string"},
		    "age":  {"type": "integer"}
		  },
		  "required": ["name"]
		}
		""";

	@Test
	void complyWithSchema_validYaml_passes() {
		assertThatCode(() -> comparator.assertComplyWithSchema(
			"""
			name: Alice
			age: 30
			""",
			JSON_SCHEMA
		)).doesNotThrowAnyException();
	}

	@Test
	void complyWithSchema_missingRequiredField_fails() {
		assertThatThrownBy(() -> comparator.assertComplyWithSchema(
			"""
			age: 30
			""",
			JSON_SCHEMA
		)).isInstanceOf(AssertionError.class).hasMessageContaining("Schema validation failed");
	}

	@Test
	void complyWithSchema_wrongType_fails() {
		assertThatThrownBy(() -> comparator.assertComplyWithSchema(
			"""
			name: Alice
			age: not-a-number
			""",
			JSON_SCHEMA
		)).isInstanceOf(AssertionError.class);
	}

	@Test
	void contentEquals_invalidYaml_throws() {
		// YAML that references an undefined anchor
		assertThatThrownBy(() -> comparator.assertContentEquals(
			"key: *undefined_anchor", "{}", ComparisonMode.STRICT
		)).isInstanceOf(AssertionError.class);
	}


	// --- extractValue ---

	@Test
	void extractValue_stringField_returnsString() {
		assertThat(comparator.extractValue("""
			name: Alice
			age: 30
			""", "$.name")).isEqualTo("Alice");
	}

	@Test
	void extractValue_integerField_returnsStringRepresentation() {
		assertThat(comparator.extractValue("""
			name: Alice
			age: 30
			""", "$.age")).isEqualTo("30");
	}

	@Test
	void extractValue_decimalField_returnsStringRepresentation() {
		assertThat(comparator.extractValue("""
			price: 19.99
			""", "$.price")).isEqualTo("19.99");
	}

	@Test
	void extractValue_missingPath_throws() {
		assertThatThrownBy(() -> comparator.extractValue("""
			name: Alice
			""", "$.missing"))
			.isInstanceOf(AssertionError.class).hasMessageContaining("$.missing");
	}
}
