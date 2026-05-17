package org.azertio.core.contenttypes;

import org.myjtools.jexten.Extension;
import org.azertio.core.Assertion;
import org.azertio.core.contributors.ContentType;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Extension
public class XMLContentType implements ContentType {

	@Override
	public boolean accepts(String contentType) {
		return "xml".equalsIgnoreCase(contentType) ||
			"application/xml".equalsIgnoreCase(contentType) ||
			"text/xml".equalsIgnoreCase(contentType) ||
			contentType.toLowerCase().endsWith("+xml");
	}


	@Override
	public void assertContentEquals(String expected, String actual, ComparisonMode mode) {
		Element expectedRoot = parseXml(expected, "expected").getDocumentElement();
		Element actualRoot   = parseXml(actual, "actual").getDocumentElement();
		stripWhitespace(expectedRoot);
		stripWhitespace(actualRoot);
		boolean matches = switch (mode) {
			case STRICT    -> expectedRoot.isEqualNode(actualRoot);
			case ANY_ORDER -> { sortChildren(expectedRoot); sortChildren(actualRoot); yield expectedRoot.isEqualNode(actualRoot); }
			case LOOSE     -> looseContains(expectedRoot, actualRoot);
		};
		if (!matches) {
			throw new AssertionError(
				"XML content mismatch [mode=" + mode + "]:\n" +
				"Expected:\n" + pretty(expectedRoot) + "\n" +
				"Actual:\n"   + pretty(actualRoot)
			);
		}
	}


	@Override
	public void assertFragmentEquals(String content, String fragmentPath, Assertion assertion) {
		String value = evaluateXPath(content, fragmentPath);
		if (!assertion.test(value)) {
			throw new AssertionError(
				"Fragment assertion failed at XPath '" + fragmentPath + "':\n" +
				assertion.describeFailure(value)
			);
		}
	}


	@Override
	public String extractValue(String content, String fragmentPath) {
		return evaluateXPath(content, fragmentPath);
	}


	private String evaluateXPath(String content, String fragmentPath) {
		Document doc = parseXml(content, "content");
		try {
			XPath xpath = XPathFactory.newInstance().newXPath();
			return (String) xpath.evaluate(fragmentPath, doc, XPathConstants.STRING);
		} catch (XPathExpressionException e) {
			throw new AssertionError("Invalid XPath expression '" + fragmentPath + "': " + e.getMessage());
		}
	}


	@Override
	public void assertComplyWithSchema(String content, String schema) {
		try {
			SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

			Schema xsdSchema = sf.newSchema(new StreamSource(new StringReader(schema)));
			sf.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			sf.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
			Validator validator = xsdSchema.newValidator();
			List<String> errors = new ArrayList<>();
			validator.setErrorHandler(new org.xml.sax.ErrorHandler() {
				@Override public void warning(SAXParseException e) { /* method intentionally empty */ }
				@Override public void error(SAXParseException e)      { errors.add(e.getMessage()); }
				@Override public void fatalError(SAXParseException e) { errors.add(e.getMessage()); }
			});
			validator.validate(new StreamSource(new StringReader(content)));
			if (!errors.isEmpty()) {
				String messages = errors.stream().collect(Collectors.joining("\n  - ", "  - ", ""));
				throw new AssertionError("XML Schema validation failed:\n" + messages);
			}
		} catch (AssertionError e) {
			throw e;
		} catch (Exception e) {
			throw new AssertionError("XML Schema validation error: " + e.getMessage(), e);
		}
	}


	// --- DOM helpers ---

	private Document parseXml(String xml, String label) {
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
			dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
			dbf.setNamespaceAware(true);
			dbf.setIgnoringElementContentWhitespace(true);
			DocumentBuilder db = dbf.newDocumentBuilder();
			return db.parse(new InputSource(new StringReader(xml)));
		} catch (Exception e) {
			throw new AssertionError("Invalid XML in " + label + ": " + e.getMessage());
		}
	}

	private void stripWhitespace(Node node) {
		List<Node> toRemove = new ArrayList<>();
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.TEXT_NODE && child.getNodeValue().isBlank()) {
				toRemove.add(child);
			} else {
				stripWhitespace(child);
			}
		}
		toRemove.forEach(node::removeChild);
	}

	private void sortChildren(Node node) {
		List<Node> elements = new ArrayList<>();
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				sortChildren(child);
				elements.add(child);
			}
		}
		elements.stream()
			.sorted(Comparator.comparing(Node::getNodeName).thenComparing(Node::getTextContent))
			.forEach(node::appendChild); // moves existing child to end, effectively sorting
	}

	private boolean looseContains(Element expected, Element actual) {
		if (!expected.getTagName().equals(actual.getTagName())) {
			return false;
		}
		NamedNodeMap expAttrs = expected.getAttributes();
		for (int i = 0; i < expAttrs.getLength(); i++) {
			Attr expAttr = (Attr) expAttrs.item(i);
			if (!expAttr.getValue().equals(actual.getAttribute(expAttr.getName()))) {
				return false;
			}
		}
		List<Element> expChildren = childElements(expected);
		List<Element> actChildren = childElements(actual);
		for (Element expChild : expChildren) {
			boolean found = actChildren.stream().anyMatch(actChild -> looseContains(expChild, actChild));
			if (!found) {
				return false;
			}
		}
		if (expChildren.isEmpty()) {
			String expText = expected.getTextContent().trim();
			if (!expText.isEmpty() && !actual.getTextContent().trim().contains(expText)) {
				return false;
			}
		}
		return true;
	}

	private List<Element> childElements(Element parent) {
		List<Element> result = new ArrayList<>();
		NodeList children = parent.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
				result.add((Element) children.item(i));
			}
		}
		return result;
	}

	private String pretty(Node node) {
		try {
			TransformerFactory tfactory = TransformerFactory.newInstance();
			tfactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			tfactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			tfactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
			Transformer tf = tfactory.newTransformer();
			tf.setOutputProperty(OutputKeys.INDENT, "yes");
			tf.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			StringWriter sw = new StringWriter();
			tf.transform(new DOMSource(node), new StreamResult(sw));
			return sw.toString();
		} catch (Exception e) {
			return node.toString();
		}
	}
}
