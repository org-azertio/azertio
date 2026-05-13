package org.azertio.plugins.rest.test;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.azertio.test.FeatureDir;
import org.azertio.test.JUnitAzertioPlan;
import org.azertio.test.AzertioExtension;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@ExtendWith(AzertioExtension.class)
class TestRestSteps {

	@RegisterExtension
	static WireMockExtension wireMock = WireMockExtension.newInstance()
		.options(wireMockConfig().dynamicPort())
		.build();

	@BeforeEach
	void stubEndpoints() {
		wireMock.stubFor(get("/users")
			.willReturn(ok()
				.withHeader("Content-Type", "application/json")
				.withBody("[]")));

		wireMock.stubFor(post("/users")
			.willReturn(aResponse().withStatus(201)
				.withHeader("Content-Type", "application/json")
				.withBody("{\"name\":\"Alice\"}")));

		wireMock.stubFor(delete("/users/1")
			.willReturn(noContent()));

		wireMock.stubFor(get("/users/1")
			.willReturn(ok()
				.withHeader("Content-Type", "application/json")
				.withBody("{\"name\":\"Alice\"}")));

		wireMock.stubFor(get("/missing")
			.willReturn(notFound()));

		wireMock.stubFor(get(urlPathEqualTo("/users"))
			.withQueryParam("name", equalTo("Alice"))
			.willReturn(ok()
				.withHeader("Content-Type", "application/json")
				.withBody("[{\"name\":\"Alice\"}]")));
	}

	private String baseUrl() {
		return "http://localhost:" + wireMock.getPort();
	}


	@Test
	@FeatureDir("get-200")
	void get200_passes(JUnitAzertioPlan plan) {
		plan.withConfig("rest.baseURL", baseUrl()).execute().assertAllPassed();
	}

	@Test
	@FeatureDir("post-201")
	void post201_passes(JUnitAzertioPlan plan) {
		plan.withConfig("rest.baseURL", baseUrl()).execute().assertAllPassed();
	}

	@Test
	@FeatureDir("delete-204")
	void delete204_passes(JUnitAzertioPlan plan) {
		plan.withConfig("rest.baseURL", baseUrl()).execute().assertAllPassed();
	}

	@Test
	@FeatureDir("wrong-status")
	void wrongStatus_fails(JUnitAzertioPlan plan) {
		plan.withConfig("rest.baseURL", baseUrl()).execute().assertAllFailed();
	}

	@Test
	@FeatureDir("wrong-body")
	void wrongBody_fails(JUnitAzertioPlan plan) {
		plan.withConfig("rest.baseURL", baseUrl()).execute().assertAllFailed();
	}

	// --- DSL language ---

	@Test
	@FeatureDir("dsl-get-200")
	void dsl_get200_passes(JUnitAzertioPlan plan) {
		plan.withConfig("rest.baseURL", baseUrl()).execute().assertAllPassed();
	}

	@Test
	@FeatureDir("dsl-post-201")
	void dsl_post201_passes(JUnitAzertioPlan plan) {
		plan.withConfig("rest.baseURL", baseUrl()).execute().assertAllPassed();
	}

	@Test
	@FeatureDir("dsl-delete-204")
	void dsl_delete204_passes(JUnitAzertioPlan plan) {
		plan.withConfig("rest.baseURL", baseUrl()).execute().assertAllPassed();
	}

	@Test
	@FeatureDir("extract-field")
	void extractField_storesValueAndUsesItInSubsequentRequest(JUnitAzertioPlan plan) {
		plan.withConfig("rest.baseURL", baseUrl()).execute().assertAllPassed();
	}

	@Test
	@FeatureDir("dsl-extract-field")
	void dsl_extractField_storesValueAndUsesItInSubsequentRequest(JUnitAzertioPlan plan) {
		plan.withConfig("rest.baseURL", baseUrl()).execute().assertAllPassed();
	}
}
