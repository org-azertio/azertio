package org.azertio.plugins.rest;

import org.myjtools.imconfig.Config;
import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Inject;
import org.myjtools.jexten.Scope;
import org.azertio.core.Assertion;
import org.azertio.core.ContentTypes;
import org.azertio.core.ResourceFinder;
import org.azertio.core.backend.ExecutionContext;
import org.azertio.core.contributors.ContentType;
import org.azertio.core.contributors.StatisticsProvider;
import org.azertio.core.contributors.StepExpression;
import org.azertio.core.contributors.StepProvider;
import org.azertio.core.testplan.DataTable;
import org.azertio.core.testplan.Document;
import org.azertio.plugins.rest.jdk.JdkHttpEngine;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.IntSupplier;

@Extension(
	name = "REST steps provider",
	scope = Scope.TRANSIENT, // each test plan execution gets its own instance
	extensionPointVersion = "1.0"
)
public class RestStepProvider implements StepProvider  {

	@Inject
	ResourceFinder resourceFinder;

	@Inject
	ContentTypes contentTypes;

	private RestEngine restEngine;

	@Override
	public void init(Config config) {
		this.restEngine = new JdkHttpEngine();
		restEngine.setBaseUrl(config.getString("rest.baseURL").orElse(""));
		restEngine.setHttpCodeThreshold(config.getInteger("rest.httpCodeThreshold").orElse(500));
		restEngine.setTimeout(config.getLong("rest.timeout").orElse(10000L));
		restEngine.setContentType(config.getString("rest.contentType").orElse("application/json"));
	}


	// --- request methods ---

	private void request(IntSupplier request) {
		ExecutionContext.current().runWithinBenchmark(() ->	request.getAsInt() < 400);
		if (!ExecutionContext.current().isBenchmarkMode()) {
			storeHttpExchange();
		}
	}


	@StepExpression("rest.auth.headers")
	public void setPersistentHeaders(DataTable table) {
		var headers = new LinkedHashMap<String, String>();
		for (var row : ExecutionContext.current().interpolateDataTable(table).values()) {
			if (row.size() >= 2) {
				headers.put(row.get(0), row.get(1));
			}
		}
		restEngine.setPersistentHeaders(headers);
	}

	@StepExpression(value = "rest.auth.bearer", args = {"token:text"})
	public void setAuthBearer(String token) {
		restEngine.setPersistentHeaders(Map.of("Authorization", "Bearer " + interpolate(token)));
	}

	@StepExpression(value = "rest.auth.basic", args = {"username:text", "password:text"})
	public void setAuthBasic(String username, String password) {
		String credentials = Base64.getEncoder().encodeToString(
			(interpolate(username) + ":" + interpolate(password)).getBytes(StandardCharsets.UTF_8)
		);
		restEngine.setPersistentHeaders(Map.of("Authorization", "Basic " + credentials));
	}

	@StepExpression(value = "rest.auth.apikey.header", args = {"key:text", "name:text"})
	public void setAuthApiKeyHeader(String key, String name) {
		restEngine.setPersistentHeaders(Map.of(interpolate(name), interpolate(key)));
	}

	@StepExpression(value = "rest.auth.apikey.query", args = {"key:text", "name:text"})
	public void setAuthApiKeyQuery(String key, String name) {
		restEngine.addPersistentQueryParam(interpolate(name), interpolate(key));
	}

	@StepExpression(value = "rest.auth.oauth2.client_credentials", args = {"url:text", "clientId:text", "clientSecret:text", "variable:id"})
	public void fetchOAuth2ClientCredentials(String url, String clientId, String clientSecret, String variable) {
		String token = restEngine.fetchOAuth2Token(interpolate(url), interpolate(clientId), interpolate(clientSecret));
		ExecutionContext.current().setVariable(variable, token);
	}

	@StepExpression(value = "rest.auth.oauth2.password", args = {"url:text", "clientId:text", "clientSecret:text", "username:text", "password:text", "variable:id"})
	public void fetchOAuth2PasswordGrant(String url, String clientId, String clientSecret, String username, String password, String variable) {
		String token = restEngine.fetchOAuth2PasswordToken(
			interpolate(url), interpolate(clientId), interpolate(clientSecret),
			interpolate(username), interpolate(password)
		);
		ExecutionContext.current().setVariable(variable, token);
	}


	@StepExpression("rest.request.headers")
	public void setNextRequestHeaders(DataTable table) {
		var headers = new LinkedHashMap<String, String>();
		for (var row : ExecutionContext.current().interpolateDataTable(table).values()) {
			if (row.size() >= 2) {
				headers.put(row.get(0), row.get(1));
			}
		}
		restEngine.setNextRequestHeaders(headers);
	}


	@StatisticsProvider
	@StepExpression(value = "rest.request.GET", args = {"endpoint:text"})
	public void get(String endpoint) {
		request(() -> restEngine.requestGET(interpolate(endpoint)));
	}

	@StatisticsProvider
	@StepExpression(value = "rest.request.POST.empty", args = {"endpoint:text"})
	public void post(String endpoint) {
		request(() -> restEngine.requestPOST(interpolate(endpoint)));
	}

	@StatisticsProvider
	@StepExpression(value = "rest.request.POST.body", args = {"endpoint:text"})
	public void postWithBody(String endpoint, Document body) {
		request(() -> restEngine.requestPOST(interpolate(endpoint), interpolate(body.content())));
	}

	@StatisticsProvider
	@StepExpression(value = "rest.request.POST.file", args = {"endpoint:text", "file:text"})
	public void postWithFile(String endpoint, String file) {
		request(() -> restEngine.requestPOST(interpolate(endpoint), resourceFinder.readAsString(file)));
	}

	@StatisticsProvider
	@StepExpression(value = "rest.request.POST.urlencoded", args = {"endpoint:text"})
	public void postWithUrlEncodedForm(String endpoint, DataTable table) {
		var fields = new LinkedHashMap<String, String>();
		for (var row : ExecutionContext.current().interpolateDataTable(table).values()) {
			if (row.size() >= 2) fields.put(row.get(0), row.get(1));
		}
		request(() -> restEngine.requestPOSTUrlEncoded(interpolate(endpoint), fields));
	}

	@StatisticsProvider
	@StepExpression(value = "rest.request.POST.multipart", args = {"endpoint:text"})
	public void postWithMultipartForm(String endpoint, DataTable table) {
		var fields = new LinkedHashMap<String, String>();
		for (var row : ExecutionContext.current().interpolateDataTable(table).values()) {
			if (row.size() >= 2) fields.put(row.get(0), row.get(1));
		}
		request(() -> restEngine.requestPOSTMultipart(interpolate(endpoint), fields));
	}

	@StatisticsProvider
	@StepExpression(value = "rest.request.PUT.body", args = {"endpoint:text"})
	public void putWithBody(String endpoint, Document body) {
		request(() -> restEngine.requestPUT(interpolate(endpoint), interpolate(body.content())));
	}

	@StatisticsProvider
	@StepExpression(value = "rest.request.PUT.file", args = {"endpoint:text", "file:text"})
	public void putWithFile(String endpoint, String file) {
		request(() -> restEngine.requestPUT(interpolate(endpoint), resourceFinder.readAsString(file)));
	}

	@StatisticsProvider
	@StepExpression(value = "rest.request.PATCH.body", args = {"endpoint:text"})
	public void patchWithBody(String endpoint, Document body) {
		request(() -> restEngine.requestPATCH(interpolate(endpoint), interpolate(body.content())));
	}

	@StatisticsProvider
	@StepExpression(value = "rest.request.PATCH.file", args = {"endpoint:text", "file:text"})
	public void patchWithFile(String endpoint, String file) {
		request(() -> restEngine.requestPATCH(interpolate(endpoint), resourceFinder.readAsString(file)));
	}

	@StatisticsProvider
	@StepExpression(value = "rest.request.DELETE", args = {"endpoint:text"})
	public void delete(String endpoint) {
		request(() -> restEngine.requestDELETE(interpolate(endpoint)));
	}


	@StepExpression("rest.response.statusCode")
	public void checkStatusCode(Assertion assertion) {
		Assertion.assertThat(restEngine.responseHttpCode(), assertion);
	}

	@StepExpression("rest.response.body")
	public void checkResponseBody(Document body) {
		assertCompareContentType(body.content(), body.mimeType(), ContentType.ComparisonMode.STRICT);
	}

	@StepExpression(value = "rest.response.body.file", args = {"file:text"})
	public void checkResponseBodyFromFile(String file) {
		assertCompareContentType(resourceFinder.readAsString(file), null, ContentType.ComparisonMode.STRICT);
	}


	@StepExpression("rest.response.body.contains")
	public void checkResponseBodyContains(Document body) {
		assertCompareContentType(body.content(), body.mimeType(), ContentType.ComparisonMode.LOOSE);
	}

	@StepExpression("rest.response.headers")
	public void checkResponseHeaders(DataTable table) {
		var errors = new ArrayList<String>();
		for (var row : ExecutionContext.current().interpolateDataTable(table).values()) {
			if (row.size() < 2) continue;
			String name = row.get(0);
			String expected = row.get(1);
			String actual = restEngine.responseHeader(name);
			if (actual == null) {
				errors.add("Header '" + name + "' was not present in the response");
			} else if (!expected.equals(actual)) {
				errors.add("Header '" + name + "': expected '" + expected + "' but was '" + actual + "'");
			}
		}
		if (!errors.isEmpty()) {
			throw new AssertionError("Response headers mismatch:\n" + String.join("\n", errors));
		}
	}


	@StepExpression("rest.response.cookies")
	public void checkResponseCookies(DataTable table) {
		var errors = new ArrayList<String>();
		for (var row : ExecutionContext.current().interpolateDataTable(table).values()) {
			if (row.size() < 2) continue;
			String name = row.get(0);
			String expected = row.get(1);
			String actual = restEngine.responseCookie(name);
			if (actual == null) {
				errors.add("Cookie '" + name + "' was not set in the response");
			} else if (!expected.equals(actual)) {
				errors.add("Cookie '" + name + "': expected '" + expected + "' but was '" + actual + "'");
			}
		}
		if (!errors.isEmpty()) {
			throw new AssertionError("Response cookies mismatch:\n" + String.join("\n", errors));
		}
	}

	@StepExpression(value = "rest.response.extracts.field", args = {"field:text", "variable:id"})
	public void extractFieldFromResponse(String field, String variable) {
		String contentType = restEngine.responseContentType();
		String value = contentTypes.get(contentType).orElseThrow(
			() -> new IllegalStateException("Unsupported response content type: " + contentType)
		).extractValue(restEngine.responseBody(), field);
		ExecutionContext.current().setVariable(variable, value);
	}

	@StepExpression(value = "rest.response.extracts.header", args = {"header:text", "variable:id"})
	public void extractHeaderFromResponse(String header, String variable) {
		String value = restEngine.responseHeader(interpolate(header));
		if (value == null) {
			throw new AssertionError("Response header '" + header + "' was not present");
		}
		ExecutionContext.current().setVariable(variable, value);
	}


	private void assertCompareContentType(
		String expectedContent,
		String expectedContentType,
		ContentType.ComparisonMode comparisonMode
	) {
		String actualContentType = restEngine.responseContentType();
		if (expectedContentType == null) {
			expectedContentType = actualContentType;
		}
		assertEqualContentTypes(expectedContentType, actualContentType);
		contentTypes.get(expectedContentType).ifPresent(comparator ->
			comparator.assertContentEquals(interpolate(expectedContent), restEngine.responseBody(), comparisonMode)
		);
	}


	private void storeHttpExchange() {
		String content = restEngine.requestRaw() + "\n\n" + restEngine.responseRaw();
		ExecutionContext.current().storeAttachment(content.getBytes(), "text/plain");
	}

	protected String interpolate(String text) {
		return ExecutionContext.current().interpolateString(text);
	}

	private void assertEqualContentTypes(String expectedContentType, String actualContentType) {
		if (contentTypes.get(expectedContentType).map(it -> it.accepts(actualContentType)).isEmpty()) {
			throw new AssertionError(
				"Response content type mismatch:\n" +
				"Expected: " + expectedContentType + "\n" +
				"Actual: "   + actualContentType
			);
		}
	}

}
