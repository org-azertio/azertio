package org.myjtools.openbbt.plugins.rest;

import org.myjtools.imconfig.Config;
import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Inject;
import org.myjtools.jexten.Scope;
import org.myjtools.openbbt.core.Assertion;
import org.myjtools.openbbt.core.ContentTypes;
import org.myjtools.openbbt.core.ResourceFinder;
import org.myjtools.openbbt.core.backend.ExecutionContext;
import org.myjtools.openbbt.core.contributors.ContentType;
import org.myjtools.openbbt.core.contributors.StepExpression;
import org.myjtools.openbbt.core.contributors.StepProvider;
import org.myjtools.openbbt.core.testplan.Document;
import org.myjtools.openbbt.plugins.rest.jdk.JdkHttpEngine;
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
	}


	// --- request methods ---

	private void request(IntSupplier request) {
		ExecutionContext.current().runWithinBenchmark(() ->	request.getAsInt() < 400);
		if (!ExecutionContext.current().isBenchmarkMode()) {
			storeHttpExchange();
		}
	}


	@StepExpression(value = "rest.request.GET", args = {"endpoint:text"})
	public void get(String endpoint) {
		request(() -> restEngine.requestGET(interpolate(endpoint)));
	}

	@StepExpression(value = "rest.request.POST.empty", args = {"endpoint:text"})
	public void post(String endpoint) {
		request(() -> restEngine.requestPOST(interpolate(endpoint)));
	}

	@StepExpression(value = "rest.request.POST.body", args = {"endpoint:text"})
	public void postWithBody(String endpoint, Document body) {
		request(() -> restEngine.requestPOST(interpolate(endpoint), interpolate(body.content())));
	}

	@StepExpression(value = "rest.request.POST.file", args = {"endpoint:text", "file:text"})
	public void postWithFile(String endpoint, String file) {
		request(() -> restEngine.requestPOST(interpolate(endpoint), resourceFinder.readAsString(file)));
	}

	@StepExpression(value = "rest.request.PUT.body", args = {"endpoint:text"})
	public void putWithBody(String endpoint, Document body) {
		request(() -> restEngine.requestPUT(interpolate(endpoint), interpolate(body.content())));
	}

	@StepExpression(value = "rest.request.PUT.file", args = {"endpoint:text", "file:text"})
	public void putWithFile(String endpoint, String file) {
		request(() -> restEngine.requestPUT(interpolate(endpoint), resourceFinder.readAsString(file)));
	}

	@StepExpression(value = "rest.request.PATCH.body", args = {"endpoint:text"})
	public void patchWithBody(String endpoint, Document body) {
		request(() -> restEngine.requestPATCH(interpolate(endpoint), interpolate(body.content())));
	}

	@StepExpression(value = "rest.request.PATCH.file", args = {"endpoint:text", "file:text"})
	public void patchWithFile(String endpoint, String file) {
		request(() -> restEngine.requestPATCH(interpolate(endpoint), resourceFinder.readAsString(file)));
	}

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

	@StepExpression(value = "rest.response.extracts.field", args = {"field:text", "variable:id"})
	public void extractFieldFromResponse(String field, String variable) {
		String contentType = restEngine.responseContentType();
		String value = contentTypes.get(contentType).orElseThrow(
			() -> new IllegalStateException("Unsupported response content type: " + contentType)
		).extractValue(restEngine.responseBody(), field);
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
