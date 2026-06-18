package org.azertio.plugins.rest.jdk;

import org.azertio.core.AzertioException;
import org.azertio.core.util.Log;
import org.azertio.plugins.rest.RestEngine;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class JdkHttpEngine implements RestEngine {

    private static final Log log = Log.of("rest");
    private static final int MAX_LOG_BODY_LENGTH = 1000;

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
        return doFetchOAuth2Token(tokenUrl, "grant_type=client_credentials", clientId, clientSecret);
    }

    @Override
    public String fetchOAuth2PasswordToken(String tokenUrl, String clientId, String clientSecret, String username, String password) {
        String grantParams = "grant_type=password"
            + "&username=" + URLEncoder.encode(username, StandardCharsets.UTF_8)
            + "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8);
        return doFetchOAuth2Token(tokenUrl, grantParams, clientId, clientSecret);
    }

    // #131: uses CLIENT_SECRET_BASIC (RFC 6749 §2.3.1 recommended method)
    // #132: uses tokenUrl directly — it is always an absolute URL, must not go through buildUrl
    private String doFetchOAuth2Token(String tokenUrl, String grantParams, String clientId, String clientSecret) {
        String credentials = Base64.getEncoder().encodeToString(
            (clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8)
        );
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(tokenUrl))
            .timeout(timeout)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Authorization", "Basic " + credentials)
            .POST(HttpRequest.BodyPublishers.ofString(grantParams))
            .build();
        HttpResponse<String> response = executeRequest(request, grantParams);
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
        lastResponse = executeRequest(request, body);
        checkThreshold(lastResponse.statusCode());
        return lastResponse.statusCode();
    }

    private HttpResponse<String> executeRequest(HttpRequest request, String body) {
        logRequest(request, body);
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AzertioException("HTTP request interrupted: {}", request.uri());
        } catch (IOException e) {
            String reason = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            throw new AzertioException("HTTP request failed [{}]: {}", request.uri(), reason);
        }
        logResponse(response);
        return response;
    }

    private void logRequest(HttpRequest request, String body) {
        log.debug("[REST] --> {} {}", request.method(), request.uri());
        request.headers().map().forEach((name, values) ->
            values.forEach(value -> log.debug("[REST]     {}: {}", name, maskSensitiveHeader(name, value)))
        );
        if (body != null && !body.isBlank()) {
            log.debug("[REST]     Body: {}", truncate(body));
        }
    }

    private void logResponse(HttpResponse<String> response) {
        log.debug("[REST] <-- {}", response.statusCode());
        response.headers().map().forEach((name, values) ->
            values.forEach(value -> log.debug("[REST]     {}: {}", name, value))
        );
        String body = response.body();
        if (body != null && !body.isBlank()) {
            log.debug("[REST]     Body: {}", truncate(body));
        }
    }

    private String maskSensitiveHeader(String name, String value) {
        if ("Authorization".equalsIgnoreCase(name)) {
            int spaceIdx = value.indexOf(' ');
            return spaceIdx >= 0 ? value.substring(0, spaceIdx + 1) + "***" : "***";
        }
        return value;
    }

    private String truncate(String s) {
        return s.length() <= MAX_LOG_BODY_LENGTH ? s : s.substring(0, MAX_LOG_BODY_LENGTH) + "... [truncated]";
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