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

		wireMock.stubFor(get("/secure")
			.withHeader("Authorization", equalTo("Bearer secret"))
			.withHeader("X-Tenant-Id", equalTo("acme"))
			.willReturn(ok()));

		wireMock.stubFor(get("/headers-test")
			.willReturn(ok()
				.withHeader("Content-Type", "application/json")
				.withHeader("X-Custom-Header", "my-value")
				.withBody("{}")));

		wireMock.stubFor(get("/bearer-protected")
			.withHeader("Authorization", equalTo("Bearer test-token"))
			.willReturn(ok()));

		// base64("user:pass") = dXNlcjpwYXNz
		wireMock.stubFor(get("/basic-protected")
			.withHeader("Authorization", equalTo("Basic dXNlcjpwYXNz"))
			.willReturn(ok()));

		wireMock.stubFor(get("/apikey-header-protected")
			.withHeader("X-API-Key", equalTo("secret-key"))
			.willReturn(ok()));

		wireMock.stubFor(get(urlPathEqualTo("/apikey-query-protected"))
			.withQueryParam("api_key", equalTo("secret-key"))
			.willReturn(ok()));

		// bXktY2xpZW50Om15LXNlY3JldA== = base64("my-client:my-secret")
		wireMock.stubFor(post("/oauth/token")
			.withHeader("Authorization", equalTo("Basic bXktY2xpZW50Om15LXNlY3JldA=="))
			.withRequestBody(containing("grant_type=client_credentials"))
			.willReturn(ok()
				.withHeader("Content-Type", "application/json")
				.withBody("{\"access_token\":\"oauth-token\",\"token_type\":\"Bearer\",\"expires_in\":3600}")));

		wireMock.stubFor(get("/oauth-protected")
			.withHeader("Authorization", equalTo("Bearer oauth-token"))
			.willReturn(ok()));

		// bXktY2xpZW50Om15LXNlY3JldA== = base64("my-client:my-secret")
		wireMock.stubFor(post("/oauth/token")
			.withHeader("Authorization", equalTo("Basic bXktY2xpZW50Om15LXNlY3JldA=="))
			.withRequestBody(containing("grant_type=password"))
			.withRequestBody(containing("username=admin"))
			.withRequestBody(containing("password=admin123"))
			.willReturn(ok()
				.withHeader("Content-Type", "application/json")
				.withBody("{\"access_token\":\"user-token\",\"token_type\":\"Bearer\",\"expires_in\":3600}")));

		wireMock.stubFor(get("/user-protected")
			.withHeader("Authorization", equalTo("Bearer user-token"))
			.willReturn(ok()));

		wireMock.stubFor(post("/form-login")
			.withHeader("Content-Type", containing("application/x-www-form-urlencoded"))
			.withRequestBody(containing("username=alice"))
			.withRequestBody(containing("password=s3cr3t"))
			.willReturn(aResponse().withStatus(200)));

		wireMock.stubFor(post("/form-upload")
			.withHeader("Content-Type", containing("multipart/form-data"))
			.withRequestBody(containing("name"))
			.withRequestBody(containing("Alice"))
			.willReturn(aResponse().withStatus(201)));

		wireMock.stubFor(get("/persistent-headers-test")
			.withHeader("X-Tenant-Id", equalTo("acme"))
			.withHeader("X-Version", equalTo("2"))
			.willReturn(ok()));

		wireMock.stubFor(get("/echo-header")
			.willReturn(ok()));

		wireMock.stubFor(post("/cookie-login")
			.willReturn(ok()
				.withHeader("Set-Cookie", "session_id=abc123; Path=/; HttpOnly")
				.withHeader("Set-Cookie", "theme=dark; Path=/")));
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

	@Test
	@FeatureDir("request-headers")
	void requestHeaders_areSentWithNextRequestOnly(JUnitAzertioPlan plan) {
		plan.withConfig("rest.baseURL", baseUrl()).execute().assertAllPassed();
	}

	@Test
	@FeatureDir("response-headers")
	void responseHeaders_passes(JUnitAzertioPlan plan) {
		plan.withConfig("rest.baseURL", baseUrl()).execute().assertAllPassed();
	}

	@Test
	@FeatureDir("wrong-response-headers")
	void wrongResponseHeaders_fails(JUnitAzertioPlan plan) {
		plan.withConfig("rest.baseURL", baseUrl()).execute().assertAllFailed();
	}

	@Test
	@FeatureDir("auth-bearer")
	void authBearer_passes(JUnitAzertioPlan plan) {
		plan.withConfig("rest.baseURL", baseUrl()).execute().assertAllPassed();
	}

	@Test
	@FeatureDir("auth-basic")
	void authBasic_passes(JUnitAzertioPlan plan) {
		plan.withConfig("rest.baseURL", baseUrl()).execute().assertAllPassed();
	}

	@Test
	@FeatureDir("auth-apikey-header")
	void authApiKeyHeader_passes(JUnitAzertioPlan plan) {
		plan.withConfig("rest.baseURL", baseUrl()).execute().assertAllPassed();
	}

	@Test
	@FeatureDir("auth-apikey-query")
	void authApiKeyQuery_passes(JUnitAzertioPlan plan) {
		plan.withConfig("rest.baseURL", baseUrl()).execute().assertAllPassed();
	}

	@Test
	@FeatureDir("auth-oauth2")
	void authOAuth2ClientCredentials_passes(JUnitAzertioPlan plan) {
		plan.withConfig("rest.baseURL", baseUrl()).execute().assertAllPassed();
	}

	@Test
	@FeatureDir("auth-oauth2-password")
	void authOAuth2PasswordGrant_passes(JUnitAzertioPlan plan) {
		plan.withConfig("rest.baseURL", baseUrl()).execute().assertAllPassed();
	}

	@Test
	@FeatureDir("post-urlencoded")
	void postUrlEncoded_passes(JUnitAzertioPlan plan) {
		plan.withConfig("rest.baseURL", baseUrl()).execute().assertAllPassed();
	}

	@Test
	@FeatureDir("post-multipart")
	void postMultipart_passes(JUnitAzertioPlan plan) {
		plan.withConfig("rest.baseURL", baseUrl()).execute().assertAllPassed();
	}

	@Test
	@FeatureDir("response-cookies")
	void responseCookies_passes(JUnitAzertioPlan plan) {
		plan.withConfig("rest.baseURL", baseUrl()).execute().assertAllPassed();
	}

	@Test
	@FeatureDir("auth-persistent-headers")
	void authPersistentHeaders_passes(JUnitAzertioPlan plan) {
		plan.withConfig("rest.baseURL", baseUrl()).execute().assertAllPassed();
	}

	@Test
	@FeatureDir("extract-header")
	void extractHeader_storesValueInVariable(JUnitAzertioPlan plan) {
		plan.withConfig("rest.baseURL", baseUrl()).execute().assertAllPassed();
	}
}
