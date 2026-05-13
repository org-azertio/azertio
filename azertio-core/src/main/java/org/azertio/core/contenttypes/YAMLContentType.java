package org.azertio.core.contenttypes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersionDetector;
import com.networknt.schema.ValidationMessage;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.myjtools.jexten.Extension;
import org.azertio.core.Assertion;
import org.azertio.core.contributors.ContentType;

import java.util.Set;
import java.util.stream.Collectors;

@Extension
public class YAMLContentType extends JacksonContentTypeAdapter implements ContentType {

	private final ObjectMapper jsonMapper = new ObjectMapper();

	public YAMLContentType() {
		super(new YAMLMapper());
	}


	@Override
	public boolean accepts(String contentType) {
		return "yaml".equalsIgnoreCase(contentType) ||
			"application/yaml".equalsIgnoreCase(contentType) ||
			"text/yaml".equalsIgnoreCase(contentType) ||
			"application/x-yaml".equalsIgnoreCase(contentType);
	}


	/**
	 * Converts YAML to JSON so that Jayway JSONPath (which expects JSON strings) can be applied.
	 */
	@Override
	public void assertFragmentEquals(String content, String fragmentPath, Assertion assertion) {
		super.assertFragmentEquals(toJson(content), fragmentPath, assertion);
	}


	@Override
	public String extractValue(String content, String fragmentPath) {
		return super.extractValue(toJson(content), fragmentPath);
	}


	/**
	 * Parses YAML content and validates against a JSON Schema (schema must be JSON or YAML JSON Schema).
	 */
	@Override
	public void assertComplyWithSchema(String content, String schema) {
		try {
			JsonNode contentNode = mapper().readTree(content);
			JsonNode schemaNode  = jsonMapper.readTree(schema);
			JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersionDetector.detect(schemaNode));
			JsonSchema jsonSchema = factory.getSchema(schemaNode);
			Set<ValidationMessage> errors = jsonSchema.validate(contentNode);
			if (!errors.isEmpty()) {
				String messages = errors.stream()
					.map(ValidationMessage::getMessage)
					.collect(Collectors.joining("\n  - ", "  - ", ""));
				throw new AssertionError("YAML Schema validation failed:\n" + messages);
			}
		} catch (AssertionError e) {
			throw e;
		} catch (Exception e) {
			throw new AssertionError("YAML Schema validation error: " + e.getMessage(), e);
		}
	}


	private String toJson(String yaml) {
		try {
			JsonNode node = mapper().readTree(yaml);
			return jsonMapper.writeValueAsString(node);
		} catch (JsonProcessingException e) {
			throw new AssertionError("Invalid YAML: " + e.getMessage());
		}
	}
}
