package org.azertio.plugins.webui.test;

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
class TestWebUiSteps {

    private static final String BASIC_PAGE = """
        <!DOCTYPE html>
        <html>
        <head><title>Basic Page</title></head>
        <body>
          <h1>Hello World</h1>
          <p id="visible-element">I am visible</p>
          <p id="hidden-element" style="display:none">I am hidden</p>
          <p id="code">ABC-123</p>
        </body>
        </html>
        """;

    private static final String FORM_PAGE = """
        <!DOCTYPE html>
        <html>
        <head><title>Form Page</title></head>
        <body>
          <input id="name" type="text" oninput="document.getElementById('name-display').textContent=this.value">
          <span id="name-display"></span>
          <select id="country" onchange="document.getElementById('selected').textContent=this.options[this.selectedIndex].text">
            <option value="">Select...</option>
            <option value="es">Spain</option>
            <option value="fr">France</option>
          </select>
          <span id="selected"></span>
          <input id="agree" type="checkbox" onchange="document.getElementById('agree-status').textContent=this.checked?'Checked':'Unchecked'">
          <span id="agree-status">Unchecked</span>
          <button id="submit-btn" onclick="var r=document.getElementById('result');r.style.display='block';r.textContent='Submitted'">Submit</button>
          <div id="result" style="display:none"></div>
        </body>
        </html>
        """;

    private static final String WAIT_VISIBLE_PAGE = """
        <!DOCTYPE html>
        <html>
        <head><title>Wait Visible Page</title></head>
        <body>
          <button id="load-btn" onclick="setTimeout(function(){document.getElementById('content').style.display='block'},300)">Load</button>
          <div id="content" style="display:none">Content loaded</div>
        </body>
        </html>
        """;

    private static final String WAIT_HIDDEN_PAGE = """
        <!DOCTYPE html>
        <html>
        <head><title>Wait Hidden Page</title></head>
        <body>
          <div id="spinner">Loading...</div>
          <script>setTimeout(function(){document.getElementById('spinner').style.display='none'},300)</script>
        </body>
        </html>
        """;

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort())
        .build();

    @BeforeEach
    void stubHtmlPages() {
        wireMock.stubFor(get(urlPathEqualTo("/basic"))
            .willReturn(ok()
                .withHeader("Content-Type", "text/html; charset=utf-8")
                .withBody(BASIC_PAGE)));

        wireMock.stubFor(get(urlPathEqualTo("/form"))
            .willReturn(ok()
                .withHeader("Content-Type", "text/html; charset=utf-8")
                .withBody(FORM_PAGE)));

        wireMock.stubFor(get(urlPathEqualTo("/wait-visible"))
            .willReturn(ok()
                .withHeader("Content-Type", "text/html; charset=utf-8")
                .withBody(WAIT_VISIBLE_PAGE)));

        wireMock.stubFor(get(urlPathEqualTo("/wait-hidden"))
            .willReturn(ok()
                .withHeader("Content-Type", "text/html; charset=utf-8")
                .withBody(WAIT_HIDDEN_PAGE)));
    }

    private String baseUrl() {
        return "http://localhost:" + wireMock.getPort();
    }

    @Test
    @FeatureDir("navigate")
    void navigate_assertsUrlAndTitle(JUnitAzertioPlan plan) {
        plan.withConfig("webui.baseURL", baseUrl()).execute().assertAllPassed();
    }

    @Test
    @FeatureDir("assert-visible")
    void assertVisible_passes(JUnitAzertioPlan plan) {
        plan.withConfig("webui.baseURL", baseUrl()).execute().assertAllPassed();
    }

    @Test
    @FeatureDir("assert-hidden")
    void assertHidden_passes(JUnitAzertioPlan plan) {
        plan.withConfig("webui.baseURL", baseUrl()).execute().assertAllPassed();
    }

    @Test
    @FeatureDir("assert-text")
    void assertText_passes(JUnitAzertioPlan plan) {
        plan.withConfig("webui.baseURL", baseUrl()).execute().assertAllPassed();
    }

    @Test
    @FeatureDir("assert-text-fails")
    void assertText_whenWrongValue_fails(JUnitAzertioPlan plan) {
        plan.withConfig("webui.baseURL", baseUrl()).execute().assertAllFailed();
    }

    @Test
    @FeatureDir("assert-url")
    void assertUrl_passes(JUnitAzertioPlan plan) {
        plan.withConfig("webui.baseURL", baseUrl()).execute().assertAllPassed();
    }

    @Test
    @FeatureDir("assert-title")
    void assertTitle_passes(JUnitAzertioPlan plan) {
        plan.withConfig("webui.baseURL", baseUrl()).execute().assertAllPassed();
    }

    @Test
    @FeatureDir("fill")
    void fill_typesIntoInputAndDisplayUpdates(JUnitAzertioPlan plan) {
        plan.withConfig("webui.baseURL", baseUrl()).execute().assertAllPassed();
    }

    @Test
    @FeatureDir("click")
    void click_triggersAction(JUnitAzertioPlan plan) {
        plan.withConfig("webui.baseURL", baseUrl()).execute().assertAllPassed();
    }

    @Test
    @FeatureDir("select")
    void select_choosesOption(JUnitAzertioPlan plan) {
        plan.withConfig("webui.baseURL", baseUrl()).execute().assertAllPassed();
    }

    @Test
    @FeatureDir("check")
    void check_marksCheckbox(JUnitAzertioPlan plan) {
        plan.withConfig("webui.baseURL", baseUrl()).execute().assertAllPassed();
    }

    @Test
    @FeatureDir("uncheck")
    void uncheck_unmarksCheckbox(JUnitAzertioPlan plan) {
        plan.withConfig("webui.baseURL", baseUrl()).execute().assertAllPassed();
    }

    @Test
    @FeatureDir("extract-text")
    void extractText_storesValueInVariable(JUnitAzertioPlan plan) {
        plan.withConfig("webui.baseURL", baseUrl()).execute().assertAllPassed();
    }

    @Test
    @FeatureDir("assert-visible-fails")
    void assertVisible_whenHidden_fails(JUnitAzertioPlan plan) {
        plan.withConfig("webui.baseURL", baseUrl()).execute().assertAllFailed();
    }

    @Test
    @FeatureDir("assert-hidden-fails")
    void assertHidden_whenVisible_fails(JUnitAzertioPlan plan) {
        plan.withConfig("webui.baseURL", baseUrl()).execute().assertAllFailed();
    }

    @Test
    @FeatureDir("wait-visible")
    void waitForVisible_blocksUntilElementAppears(JUnitAzertioPlan plan) {
        plan.withConfig("webui.baseURL", baseUrl()).execute().assertAllPassed();
    }

    @Test
    @FeatureDir("wait-hidden")
    void waitForHidden_blocksUntilElementDisappears(JUnitAzertioPlan plan) {
        plan.withConfig("webui.baseURL", baseUrl()).execute().assertAllPassed();
    }
}