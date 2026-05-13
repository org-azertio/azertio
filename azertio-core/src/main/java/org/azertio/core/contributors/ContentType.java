package org.azertio.core.contributors;

import org.myjtools.jexten.ExtensionPoint;
import org.azertio.core.Assertion;

@ExtensionPoint(version = "1.0")
public interface ContentType extends Contributor {

	enum ComparisonMode {
		STRICT,
		ANY_ORDER,
		LOOSE
	}

	/**
	 * Determines if this comparator can handle the specified content type.
	 *
	 * @param contentType the content type to check (e.g., "application/json", "application/xml")
	 * @return true if this comparator can handle the specified content type, false otherwise
	 */
	boolean accepts(String contentType);

	/**
	 * Asserts that the actual content matches the expected content according to the specified comparison mode.
	 *
	 * @param expected the expected content
	 * @param actual the actual content
	 * @param mode the comparison mode to use for the assertion
	 * @throws AssertionError if the contents do not match according to the specified mode
	 */
	void assertContentEquals(String expected, String actual, ComparisonMode mode);


	/**
	 * Asserts that a specific fragment of the actual content matches the expected value
	 * according to the specified assertion. The fragment is identified by a path
	 * (e.g., JSONPath for JSON content, XPath for XML content).
	 * @param content the actual content to extract the fragment from
	 * @param fragmentPath the path to the fragment within the content (e.g., JSONPath, XPath)
	 * @param assertion the assertion to apply to the fragment
	 * @throws AssertionError if the fragment does not match according to the specified mode
	 */
	void assertFragmentEquals(String content, String fragmentPath, Assertion assertion);


	/**
	 * Asserts that the content complies with a given schema.
	 * Schema validation can be used to ensure that the content adheres to a specific structure and data types,
	 * which is especially useful for formats like JSON and XML.
	 * @param content the content to validate
	 * @param schema the schema to validate against (e.g., JSON Schema, XML Schema)
	 * @throws AssertionError if the content does not comply with the schema
	 */
	void assertComplyWithSchema(String content, String schema);


	/**
	 * Extracts a specific value from the content based on the provided fragment path.
	 * The fragment path is a way to specify the location of the value within the content (e.g., JSONPath for JSON, XPath for XML).
	 *
	 * @param content the content to extract the value from
	 * @param fragmentPath the path to the value within the content (e.g., JSONPath, XPath)
	 * @return the extracted value cast to the specified type
	 * @throws AssertionError if the extraction fails or if the extracted value cannot be cast to the specified type
	 */
	String extractValue(String content, String fragmentPath);

}
