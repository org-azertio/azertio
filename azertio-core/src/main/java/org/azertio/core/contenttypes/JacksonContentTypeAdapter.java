package org.azertio.core.contenttypes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.azertio.core.Assertion;
import org.azertio.core.contributors.ContentType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public abstract class JacksonContentTypeAdapter implements ContentType {

	private final ObjectMapper mapper;

	protected JacksonContentTypeAdapter(ObjectMapper mapper) {
		this.mapper = mapper;
		this.mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
	}

	protected ObjectMapper mapper() {
		return mapper;
	}


	@Override
	public void assertContentEquals(String expected, String actual, ComparisonMode mode) {
		JsonNode expectedNode = parse(expected, "expected");
		JsonNode actualNode   = parse(actual,   "actual");
		boolean matches = switch (mode) {
			case STRICT    -> expectedNode.equals(actualNode);
			case ANY_ORDER -> anyOrderEqual(expectedNode, actualNode);
			case LOOSE     -> looseContains(expectedNode, actualNode);
		};
		if (!matches) {
			throw new AssertionError(
				"JSON content mismatch [mode=" + mode + "]:\n" +
				"Expected:\n" + pretty(expectedNode) + "\n" +
				"Actual:\n"   + pretty(actualNode)
			);
		}
	}


	@Override
	public void assertFragmentEquals(String content, String fragmentPath, Assertion assertion) {
		Object value = readJsonPath(content, fragmentPath);
		if (!assertion.test(value)) {
			throw new AssertionError(
				"Fragment assertion failed at path '" + fragmentPath + "':\n" +
				assertion.describeFailure(value)
			);
		}
	}


	@Override
	public String extractValue(String content, String fragmentPath) {
		Object value = readJsonPath(content, fragmentPath);
		return value == null ? null : value.toString();
	}


	private Object readJsonPath(String content, String fragmentPath) {
		try {
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			Class<?> jsonPathClass = cl.loadClass("com.jayway.jsonpath.JsonPath");
			Method readMethod = findJsonPathReadMethod(jsonPathClass);
			Object emptyPredicates = java.lang.reflect.Array.newInstance(readMethod.getParameterTypes()[2].getComponentType(), 0);
			return readMethod.invoke(null, content, fragmentPath, emptyPredicates);
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			String causeType = cause.getClass().getSimpleName();
			if (causeType.contains("PathNotFound")) {
				throw new AssertionError("Fragment not found at path '" + fragmentPath + "': " + cause.getMessage());
			}
			throw new AssertionError("Invalid JSON or path '" + fragmentPath + "': " + cause.getMessage());
		} catch (Exception e) {
			throw new AssertionError("JSON path evaluation failed at '" + fragmentPath + "': " + e.getMessage(), e);
		}
	}



	// --- Comparison helpers ---

	/**
	 * Deep equality ignoring array element order at every level.
	 */
	private boolean anyOrderEqual(JsonNode a, JsonNode b) {
		if (a.getNodeType() != b.getNodeType()) {
			return false;
		}
		if (a.isArray()) {
			if (a.size() != b.size()) {
				return false;
			}
			List<String> aItems = canonicalList(a);
			List<String> bItems = canonicalList(b);
			Collections.sort(aItems);
			Collections.sort(bItems);
			return aItems.equals(bItems);
		}
		if (a.isObject()) {
			if (a.size() != b.size()) {
				return false;
			}
			Iterator<Map.Entry<String, JsonNode>> fields = a.fields();
			while (fields.hasNext()) {
				Map.Entry<String, JsonNode> entry = fields.next();
				if (!b.has(entry.getKey())) {
					return false;
				}
				if (!anyOrderEqual(entry.getValue(), b.get(entry.getKey()))) {
					return false;
				}
			}
			return true;
		}
		return a.equals(b);
	}

	private List<String> canonicalList(JsonNode arrayNode) {
		List<String> result = new ArrayList<>(arrayNode.size());
		for (JsonNode item : arrayNode) {
			result.add(item.toString());
		}
		return result;
	}

	/**
	 * Checks that every field/element in {@code expected} exists in {@code actual} with equal value.
	 * Extra fields in {@code actual} are allowed. Array elements in {@code expected} must all be
	 * present somewhere in {@code actual} (subset match, any order).
	 */
	private boolean looseContains(JsonNode expected, JsonNode actual) {
		if (expected.isObject()) {
			return looseObjectContains(expected, actual);
		}
		if (expected.isArray()) {
			return looseArrayContains(expected, actual);
		}
		return expected.equals(actual);
	}

	private boolean looseObjectContains(JsonNode expected, JsonNode actual) {
		Iterator<Map.Entry<String, JsonNode>> fields = expected.fields();
		while (fields.hasNext()) {
			Map.Entry<String, JsonNode> entry = fields.next();
			if (!actual.has(entry.getKey()) || !looseContains(entry.getValue(), actual.get(entry.getKey()))) {
				return false;
			}
		}
		return true;
	}

	private boolean looseArrayContains(JsonNode expected, JsonNode actual) {
		for (JsonNode expItem : expected) {
			boolean found = false;
			for (JsonNode actItem : actual) {
				if (looseContains(expItem, actItem)) {
					found = true;
					break;
				}
			}
			if (!found) {
				return false;
			}
		}
		return true;
	}


	private static Method findJsonPathReadMethod(Class<?> jsonPathClass) throws NoSuchMethodException {
		for (Method m : jsonPathClass.getMethods()) {
			if (m.getName().equals("read") && m.getParameterCount() == 3
					&& m.getParameterTypes()[0] == String.class
					&& m.getParameterTypes()[1] == String.class
					&& m.getParameterTypes()[2].isArray()) {
				return m;
			}
		}
		throw new NoSuchMethodException("JsonPath.read(String, String, Predicate[]) not found");
	}


	// --- Utility ---

	private JsonNode parse(String json, String label) {
		try {
			return mapper.readTree(json);
		} catch (JsonProcessingException e) {
			throw new AssertionError("Invalid JSON in " + label + ": " + e.getOriginalMessage());
		}
	}

	private String pretty(JsonNode node) {
		try {
			return mapper.writeValueAsString(node);
		} catch (JsonProcessingException e) {
			return node.toString();
		}
	}
}
