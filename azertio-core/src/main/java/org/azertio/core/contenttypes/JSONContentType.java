package org.azertio.core.contenttypes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersionDetector;
import com.networknt.schema.ValidationMessage;
import org.myjtools.jexten.Extension;
import org.azertio.core.contributors.ContentType;

import java.util.Set;
import java.util.stream.Collectors;

@Extension
public class JSONContentType extends JacksonContentTypeAdapter implements ContentType {

	public JSONContentType() {
		super(new ObjectMapper());
	}

	@Override
	public boolean accepts(String contentType) {
		return "json".equalsIgnoreCase(contentType) ||
				"application/json".equalsIgnoreCase(contentType) ||
				contentType.toLowerCase().endsWith("+json");
	}


	@Override
	public void assertComplyWithSchema(String content, String schema) {
		try {
			JsonNode schemaNode  = mapper().readTree(schema);
			JsonNode contentNode = mapper().readTree(content);
			JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersionDetector.detect(schemaNode));
			JsonSchema jsonSchema = factory.getSchema(schemaNode);
			Set<ValidationMessage> errors = jsonSchema.validate(contentNode);
			if (!errors.isEmpty()) {
				String messages = errors.stream()
					.map(ValidationMessage::getMessage)
					.collect(Collectors.joining("\n  - ", "  - ", ""));
				throw new AssertionError("JSON Schema validation failed:\n" + messages);
			}
		} catch (AssertionError e) {
			throw e;
		} catch (Exception e) {
			throw new AssertionError("JSON Schema validation error: " + e.getMessage(), e);
		}
	}

}
