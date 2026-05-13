package org.azertio.core.test.contenttypes;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.azertio.core.assertions.AssertionAdapter;
import org.azertio.core.contenttypes.XMLContentType;
import org.azertio.core.contributors.ContentType.ComparisonMode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class XMLContentTypeTest {

	private final XMLContentType comparator = new XMLContentType();

	// --- accepts ---

	@Test
	void accepts_xml_shorthand() {
		assertThat(comparator.accepts("xml")).isTrue();
	}

	@Test
	void accepts_applicationXml() {
		assertThat(comparator.accepts("application/xml")).isTrue();
	}

	@Test
	void accepts_textXml() {
		assertThat(comparator.accepts("text/xml")).isTrue();
	}

	@Test
	void accepts_customPlusXml() {
		assertThat(comparator.accepts("application/atom+xml")).isTrue();
	}

	@Test
	void accepts_json_returnsFalse() {
		assertThat(comparator.accepts("application/json")).isFalse();
	}


	// --- assertContentEquals STRICT ---

	@Test
	void contentEquals_strict_identical_passes() {
		String xml = "<person><name>Alice</name><age>30</age></person>";
		assertThatCode(() -> comparator.assertContentEquals(xml, xml, ComparisonMode.STRICT))
			.doesNotThrowAnyException();
	}

	@Test
	void contentEquals_strict_whitespaceOnly_passes() {
		assertThatCode(() -> comparator.assertContentEquals(
			"<root>  <child>A</child>  </root>",
			"<root><child>A</child></root>",
			ComparisonMode.STRICT
		)).doesNotThrowAnyException();
	}

	@Test
	void contentEquals_strict_differentValue_fails() {
		assertThatThrownBy(() -> comparator.assertContentEquals(
			"<person><name>Alice</name></person>",
			"<person><name>Bob</name></person>",
			ComparisonMode.STRICT
		)).isInstanceOf(AssertionError.class).hasMessageContaining("STRICT");
	}

	@Test
	void contentEquals_strict_differentOrder_fails() {
		assertThatThrownBy(() -> comparator.assertContentEquals(
			"<root><a/><b/></root>",
			"<root><b/><a/></root>",
			ComparisonMode.STRICT
		)).isInstanceOf(AssertionError.class);
	}


	// --- assertContentEquals ANY_ORDER ---

	@Test
	void contentEquals_anyOrder_differentChildOrder_passes() {
		assertThatCode(() -> comparator.assertContentEquals(
			"<root><a>1</a><b>2</b></root>",
			"<root><b>2</b><a>1</a></root>",
			ComparisonMode.ANY_ORDER
		)).doesNotThrowAnyException();
	}

	@Test
	void contentEquals_anyOrder_differentValues_fails() {
		assertThatThrownBy(() -> comparator.assertContentEquals(
			"<root><a>1</a><b>2</b></root>",
			"<root><a>1</a><b>9</b></root>",
			ComparisonMode.ANY_ORDER
		)).isInstanceOf(AssertionError.class);
	}


	// --- assertContentEquals LOOSE ---

	@Test
	void contentEquals_loose_subsetElements_passes() {
		assertThatCode(() -> comparator.assertContentEquals(
			"<person><name>Alice</name></person>",
			"<person><name>Alice</name><age>30</age><city>Madrid</city></person>",
			ComparisonMode.LOOSE
		)).doesNotThrowAnyException();
	}

	@Test
	void contentEquals_loose_missingExpectedElement_fails() {
		assertThatThrownBy(() -> comparator.assertContentEquals(
			"<person><name>Alice</name><role>admin</role></person>",
			"<person><name>Alice</name></person>",
			ComparisonMode.LOOSE
		)).isInstanceOf(AssertionError.class);
	}

	@Test
	void contentEquals_loose_subsetAttributes_passes() {
		assertThatCode(() -> comparator.assertContentEquals(
			"<item id=\"1\"/>",
			"<item id=\"1\" type=\"product\"/>",
			ComparisonMode.LOOSE
		)).doesNotThrowAnyException();
	}

	@Test
	void contentEquals_loose_wrongAttributeValue_fails() {
		assertThatThrownBy(() -> comparator.assertContentEquals(
			"<item id=\"1\"/>",
			"<item id=\"2\"/>",
			ComparisonMode.LOOSE
		)).isInstanceOf(AssertionError.class);
	}


	// --- assertFragmentEquals ---

	@Test
	void fragmentEquals_xpathText_passes() {
		assertThatCode(() -> comparator.assertFragmentEquals(
			"<person><name>Alice</name><age>30</age></person>",
			"/person/name",
			new AssertionAdapter(Matchers.equalTo("Alice"))
		)).doesNotThrowAnyException();
	}

	@Test
	void fragmentEquals_xpathAttribute_passes() {
		assertThatCode(() -> comparator.assertFragmentEquals(
			"<item id=\"42\"/>",
			"/item/@id",
			new AssertionAdapter(Matchers.equalTo("42"))
		)).doesNotThrowAnyException();
	}

	@Test
	void fragmentEquals_wrongValue_fails() {
		assertThatThrownBy(() -> comparator.assertFragmentEquals(
			"<person><name>Alice</name></person>",
			"/person/name",
			new AssertionAdapter(Matchers.equalTo("Bob"))
		)).isInstanceOf(AssertionError.class);
	}

	@Test
	void fragmentEquals_invalidXPath_fails() {
		assertThatThrownBy(() -> comparator.assertFragmentEquals(
			"<person/>",
			"[invalid xpath",
			new AssertionAdapter(Matchers.anything())
		)).isInstanceOf(AssertionError.class).hasMessageContaining("Invalid XPath");
	}


	// --- assertComplyWithSchema ---

	private static final String XSD = """
		<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
		  <xs:element name="person">
		    <xs:complexType>
		      <xs:sequence>
		        <xs:element name="name" type="xs:string"/>
		        <xs:element name="age"  type="xs:integer" minOccurs="0"/>
		      </xs:sequence>
		    </xs:complexType>
		  </xs:element>
		</xs:schema>
		""";

	@Test
	void complyWithSchema_validContent_passes() {
		assertThatCode(() -> comparator.assertComplyWithSchema(
			"<person><name>Alice</name><age>30</age></person>",
			XSD
		)).doesNotThrowAnyException();
	}

	@Test
	void complyWithSchema_missingRequiredElement_fails() {
		assertThatThrownBy(() -> comparator.assertComplyWithSchema(
			"<person><age>30</age></person>",
			XSD
		)).isInstanceOf(AssertionError.class).hasMessageContaining("Schema validation failed");
	}

	@Test
	void complyWithSchema_wrongElementType_fails() {
		assertThatThrownBy(() -> comparator.assertComplyWithSchema(
			"<person><name>Alice</name><age>not-a-number</age></person>",
			XSD
		)).isInstanceOf(AssertionError.class);
	}

	@Test
	void contentEquals_invalidXml_throws() {
		assertThatThrownBy(() -> comparator.assertContentEquals(
			"not-xml", "<root/>", ComparisonMode.STRICT
		)).isInstanceOf(AssertionError.class).hasMessageContaining("Invalid XML");
	}


	// --- extractValue ---

	@Test
	void extractValue_elementText_returnsString() {
		assertThat(comparator.extractValue(
			"<person><name>Alice</name><age>30</age></person>",
			"/person/name"
		)).isEqualTo("Alice");
	}

	@Test
	void extractValue_attribute_returnsString() {
		assertThat(comparator.extractValue(
			"<item id=\"42\" type=\"product\"/>",
			"/item/@id"
		)).isEqualTo("42");
	}

	@Test
	void extractValue_numericElement_returnsStringRepresentation() {
		assertThat(comparator.extractValue(
			"<person><name>Alice</name><age>30</age></person>",
			"/person/age"
		)).isEqualTo("30");
	}

	@Test
	void extractValue_decimalElement_returnsStringRepresentation() {
		assertThat(comparator.extractValue(
			"<product><price>19.99</price></product>",
			"/product/price"
		)).isEqualTo("19.99");
	}

	@Test
	void extractValue_invalidXPath_throws() {
		assertThatThrownBy(() -> comparator.extractValue(
			"<person/>",
			"[invalid xpath"
		)).isInstanceOf(AssertionError.class).hasMessageContaining("Invalid XPath");
	}
}
