package org.azertio.plugins.rest.jdk;

import org.azertio.core.AzertioException;
import org.azertio.plugins.rest.RestEngine;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class JdkHttpEngine implements RestEngine {

    private String baseUrl;
    private Integer httpCodeThreshold;
    private Duration timeout = Duration.ofSeconds(10);
    private HttpRequest lastRequest;
    private String lastRequestBody;
    private HttpResponse<String> lastResponse;

    private final HttpClient client = HttpClient.newHttpClient();

    @Override
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public void setHttpCodeThreshold(Integer httpCode) {
        this.httpCodeThreshold = httpCode;
    }

    @Override
    public void setTimeout(Long milliseconds) {
        this.timeout = Duration.ofMillis(milliseconds);
    }

    @Override
    public int requestGET(String endpoint) {
        return send(builder(endpoint).GET().build(), null);
    }

    @Override
    public int requestPOST(String endpoint) {
        return send(builder(endpoint).POST(HttpRequest.BodyPublishers.noBody()).build(), null);
    }

    @Override
    public int requestPOST(String endpoint, String content) {
        return send(builder(endpoint).POST(HttpRequest.BodyPublishers.ofString(content)).build(), content);
    }

    @Override
    public int requestPUT(String endpoint, String content) {
        return send(builder(endpoint).PUT(HttpRequest.BodyPublishers.ofString(content)).build(), content);
    }

    @Override
    public int requestPATCH(String endpoint, String content) {
        return send(builder(endpoint).method("PATCH", HttpRequest.BodyPublishers.ofString(content)).build(), content);
    }

    @Override
    public int requestDELETE(String endpoint) {
        return send(builder(endpoint).DELETE().build(), null);
    }

    @Override
    public Integer responseHttpCode() {
        return lastResponse != null ? lastResponse.statusCode() : null;
    }

    @Override
    public String responseBody() {
        return lastResponse != null ? lastResponse.body() : null;
    }

    @Override
    public String responseContentType() {
        if (lastResponse == null) return null;
        return lastResponse.headers().firstValue("Content-Type")
            .map(this::translateContentType)
            .orElse(null);
    }

    private HttpRequest.Builder builder(String endpoint) {
        String url = (baseUrl == null || baseUrl.isBlank())
            ? endpoint
            : (baseUrl.endsWith("/") || endpoint.startsWith("/"))
                ? baseUrl + endpoint
                : baseUrl + "/" + endpoint;
        return HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(timeout);
    }

    @Override
    public String requestRaw() {
        if (lastRequest == null) return null;
        var sb = new StringBuilder();
        sb.append(lastRequest.method()).append(" ").append(lastRequest.uri()).append("\n");
        lastRequest.headers().map().forEach((name, values) ->
            values.forEach(value -> sb.append(name).append(": ").append(value).append("\n"))
        );
        if (lastRequestBody != null && !lastRequestBody.isBlank()) {
            sb.append("\n").append(lastRequestBody);
        }
        return sb.toString();
    }

    @Override
    public String responseRaw() {
        if (lastResponse == null) return null;
        var sb = new StringBuilder();
        sb.append("HTTP/1.1 ").append(lastResponse.statusCode()).append("\n");
        lastResponse.headers().map().forEach((name, values) ->
            values.forEach(value -> sb.append(name).append(": ").append(value).append("\n"))
        );
        String body = lastResponse.body();
        if (body != null && !body.isBlank()) {
            sb.append("\n").append(body);
        }
        return sb.toString();
    }

    private int send(HttpRequest request, String body) {
        lastRequest = request;
        lastRequestBody = body;
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AzertioException("HTTP request interrupted: {}",request.uri());
        } catch (IOException e) {
            String reason = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            throw new AzertioException("HTTP request failed [{}]: {}", request.uri(), reason);
        }
        lastResponse = response;
        checkThreshold(response.statusCode());
        return response.statusCode();
    }


    private void checkThreshold(int statusCode) {
        if (httpCodeThreshold != null && statusCode >= httpCodeThreshold) {
            throw new AssertionError(
                "HTTP response status " + statusCode +
                " exceeds threshold " + httpCodeThreshold
            );
        }
    }

    private String translateContentType(String contentType) {
        if (contentType == null) return null;
        // Strip parameters like "; charset=UTF-8"
        String base = contentType.split(";")[0].trim();
        if (base.contains("application/json")) return "json";
        if (base.contains("application/yaml") ||
            base.contains("text/yaml") ||
            base.contains("text/x-yaml")) return "yaml";
        if (base.contains("application/xml") || base.contains("text/xml")) return "xml";
        if (base.contains("text/html")) return "html";
        return base;
    }
}
