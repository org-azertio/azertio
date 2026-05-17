package org.azertio.plugins.rest.jdk;

import org.azertio.core.AzertioException;
import org.azertio.plugins.rest.RestEngine;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class JdkHttpEngine implements RestEngine {

    private String baseUrl;
    private Integer httpCodeThreshold;
    private Duration timeout = Duration.ofSeconds(10);
    private String contentType;
    private final Map<String, String> persistentHeaders = new LinkedHashMap<>();
    private final Map<String, String> persistentQueryParams = new LinkedHashMap<>();
    private final Map<String, String> pendingRequestHeaders = new LinkedHashMap<>();
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
        return send(bodyBuilder(endpoint).POST(HttpRequest.BodyPublishers.ofString(content)).build(), content);
    }

    @Override
    public int requestPUT(String endpoint, String content) {
        return send(bodyBuilder(endpoint).PUT(HttpRequest.BodyPublishers.ofString(content)).build(), content);
    }

    @Override
    public int requestPATCH(String endpoint, String content) {
        return send(bodyBuilder(endpoint).method("PATCH", HttpRequest.BodyPublishers.ofString(content)).build(), content);
    }

    @Override
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public void setNextRequestHeaders(Map<String, String> headers) {
        pendingRequestHeaders.putAll(headers);
    }

    @Override
    public void setPersistentHeaders(Map<String, String> headers) {
        persistentHeaders.putAll(headers);
    }

    @Override
    public void addPersistentQueryParam(String name, String value) {
        persistentQueryParams.put(name, value);
    }

    @Override
    public String fetchOAuth2Token(String tokenUrl, String clientId, String clientSecret) {
        String url = buildUrl(tokenUrl);
        String body = "grant_type=client_credentials"
            + "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
            + "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(timeout)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AzertioException("OAuth2 token request interrupted: {}", url);
        } catch (IOException e) {
            String reason = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            throw new AzertioException("OAuth2 token request failed [{}]: {}", url, reason);
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new AzertioException("OAuth2 token endpoint returned status {}: {}", response.statusCode(), response.body());
        }
        var matcher = Pattern.compile("\"access_token\"\\s*:\\s*\"([^\"]+)\"").matcher(response.body());
        if (!matcher.find()) {
            throw new AzertioException("No 'access_token' field found in OAuth2 response: {}", response.body());
        }
        return matcher.group(1);
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

    @Override
    public String responseHeader(String name) {
        if (lastResponse == null) return null;
        return lastResponse.headers().firstValue(name).orElse(null);
    }

    private String buildUrl(String endpoint) {
        String url = (baseUrl == null || baseUrl.isBlank())
            ? endpoint
            : (baseUrl.endsWith("/") || endpoint.startsWith("/"))
                ? baseUrl + endpoint
                : baseUrl + "/" + endpoint;
        if (!persistentQueryParams.isEmpty()) {
            String qs = persistentQueryParams.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)
                        + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
            url = url.contains("?") ? url + "&" + qs : url + "?" + qs;
        }
        return url;
    }

    private HttpRequest.Builder builder(String endpoint) {
        HttpRequest.Builder b = HttpRequest.newBuilder()
            .uri(URI.create(buildUrl(endpoint)))
            .timeout(timeout);
        persistentHeaders.forEach(b::header);
        pendingRequestHeaders.forEach(b::header);
        pendingRequestHeaders.clear();
        return b;
    }

    private HttpRequest.Builder bodyBuilder(String endpoint) {
        HttpRequest.Builder b = builder(endpoint);
        if (contentType != null && !contentType.isBlank()) {
            b.header("Content-Type", contentType);
        }
        return b;
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
