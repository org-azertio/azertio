# REST Plugin

Provides steps to make HTTP requests against REST APIs and assert their responses.
Configured via `rest.*` properties in `azertio.yaml`. All request steps use a shared
engine instance per scenario execution; auth and headers set in `given` steps persist
for the duration of the scenario unless overridden.

---

## Authentication

### Requirement: Persistent header authentication
The plugin SHALL support setting arbitrary headers that are sent with every subsequent
request in the scenario via a two-column data table (header name / value).

#### Scenario: Custom headers sent on every request
- **WHEN** `rest.auth.headers` is called with a table containing `Authorization: Token abc`
- **THEN** every subsequent request in the scenario includes that header

### Requirement: Bearer token authentication
The plugin SHALL support setting a Bearer token that is sent as `Authorization: Bearer <token>`
with every subsequent request in the scenario.

#### Scenario: Bearer token injected into requests
- **WHEN** `rest.auth.bearer` is called with token `"my-token"`
- **THEN** every subsequent request includes `Authorization: Bearer my-token`

### Requirement: HTTP Basic authentication
The plugin SHALL support Basic auth by encoding `username:password` in Base64 and sending
it as `Authorization: Basic <encoded>` with every subsequent request.

#### Scenario: Basic credentials encoded correctly
- **WHEN** `rest.auth.basic` is called with username `"user"` and password `"pass"`
- **THEN** every subsequent request includes a correctly encoded Basic auth header

### Requirement: API key authentication via header
The plugin SHALL support sending an API key as a named request header with every subsequent request.

#### Scenario: API key sent as header
- **WHEN** `rest.auth.apikey.header` is called with key `"abc"` and header name `"X-API-Key"`
- **THEN** every subsequent request includes `X-API-Key: abc`

### Requirement: API key authentication via query parameter
The plugin SHALL support appending an API key as a named query parameter to every subsequent request URL.

#### Scenario: API key appended to URL
- **WHEN** `rest.auth.apikey.query` is called with key `"abc"` and parameter name `"api_key"`
- **THEN** every subsequent request URL includes `?api_key=abc` (or `&api_key=abc` if other params exist)

### Requirement: OAuth2 client credentials grant
The plugin SHALL obtain an OAuth2 access token using the client credentials grant (RFC 6749 §4.4),
sending credentials via HTTP Basic (CLIENT_SECRET_BASIC), and store the token in a named variable.

#### Scenario: Token obtained and stored
- **WHEN** `rest.auth.oauth2.client_credentials` is called with a valid token endpoint, client ID, secret, and variable name
- **THEN** the variable contains a non-empty access token string

#### Scenario: Token endpoint error fails the step
- **WHEN** the token endpoint returns a non-2xx response
- **THEN** the step fails with a descriptive error

### Requirement: OAuth2 Resource Owner Password Credentials grant
The plugin SHALL obtain an OAuth2 access token using the ROPC grant (RFC 6749 §4.3),
including resource owner credentials alongside client credentials, and store the token in a named variable.

#### Scenario: Token obtained with user credentials
- **WHEN** `rest.auth.oauth2.password` is called with endpoint, client ID, secret, username, password, and variable name
- **THEN** the variable contains a non-empty access token string

---

## HTTP Requests

### Requirement: Per-request headers
The plugin SHALL support setting headers for the next request only via a two-column data table;
these headers SHALL be cleared after the request is sent.

#### Scenario: Per-request header not sent on subsequent requests
- **WHEN** `rest.request.headers` is called and two requests are made
- **THEN** the header is present in the first request and absent in the second

### Requirement: GET request
The plugin SHALL make a GET request to the configured `rest.baseURL` + endpoint and store the response.

#### Scenario: GET request to relative endpoint
- **WHEN** `rest.request.GET` is called with `"users/1"`
- **THEN** a GET request is sent to `{baseURL}/users/1` and the response is stored

### Requirement: POST request with body
The plugin SHALL make a POST request with an inline body provided as a docstring block.
The default `Content-Type` is `application/json` unless overridden via `rest.contentType`.

#### Scenario: POST with JSON body
- **WHEN** `rest.request.POST.body` is called with endpoint `"users"` and body `{"name":"Ana"}`
- **THEN** a POST request is sent with that body and the response is stored

### Requirement: POST request with empty body
The plugin SHALL make a POST request with no body.

#### Scenario: POST with no body
- **WHEN** `rest.request.POST.empty` is called
- **THEN** a POST request is sent with an empty body

### Requirement: POST request from file
The plugin SHALL make a POST request with a body read from a local file path.

#### Scenario: POST body loaded from file
- **WHEN** `rest.request.POST.file` is called with a valid file path
- **THEN** the file contents are sent as the request body

### Requirement: POST request with URL-encoded form body
The plugin SHALL make a POST request with `application/x-www-form-urlencoded` body
built from a two-column data table of field names and values.

#### Scenario: Form fields URL-encoded
- **WHEN** `rest.request.POST.urlencoded` is called with a table containing `field=value`
- **THEN** the request body is `field=value` with proper URL encoding

### Requirement: POST request with multipart body
The plugin SHALL make a POST request with `multipart/form-data` body built from a two-column
data table of field names and values.

#### Scenario: Multipart body sent
- **WHEN** `rest.request.POST.multipart` is called with a table of fields
- **THEN** the request body is multipart/form-data with each row as a separate part

### Requirement: PUT request with body
The plugin SHALL make a PUT request with an inline body docstring.

#### Scenario: PUT with JSON body
- **WHEN** `rest.request.PUT.body` is called with endpoint and body
- **THEN** a PUT request is sent with that body

### Requirement: PUT request from file
The plugin SHALL make a PUT request with a body read from a local file path.

#### Scenario: PUT body loaded from file
- **WHEN** `rest.request.PUT.file` is called with a valid file path
- **THEN** the file contents are sent as the PUT request body

### Requirement: PATCH request with body
The plugin SHALL make a PATCH request with an inline body docstring.

#### Scenario: PATCH with body
- **WHEN** `rest.request.PATCH.body` is called with endpoint and body
- **THEN** a PATCH request is sent with that body

### Requirement: PATCH request from file
The plugin SHALL make a PATCH request with a body read from a local file path.

#### Scenario: PATCH body loaded from file
- **WHEN** `rest.request.PATCH.file` is called with a valid file path
- **THEN** the file contents are sent as the PATCH request body

### Requirement: DELETE request
The plugin SHALL make a DELETE request to the given endpoint.

#### Scenario: DELETE request sent
- **WHEN** `rest.request.DELETE` is called with an endpoint
- **THEN** a DELETE request is sent and the response is stored

### Requirement: Automatic failure on HTTP error threshold
Any response with a status code ≥ `rest.httpCodeThreshold` (default 500) SHALL cause the step
to fail immediately, without requiring an explicit status code assertion.

#### Scenario: 500 response fails automatically
- **WHEN** the server returns 500 and `rest.httpCodeThreshold` is 500
- **THEN** the request step fails with an error indicating the status code

#### Scenario: 404 does not fail automatically at default threshold
- **WHEN** the server returns 404 and `rest.httpCodeThreshold` is 500
- **THEN** the request step succeeds and the response is available for assertion

---

## Response Assertions

### Requirement: Status code assertion
The plugin SHALL assert that the last response's HTTP status code equals the expected value.

#### Scenario: Exact status code match
- **WHEN** the server returns 201 and `rest.response.statusCode` asserts 201
- **THEN** the assertion passes

#### Scenario: Status code mismatch fails
- **WHEN** the server returns 404 and `rest.response.statusCode` asserts 200
- **THEN** the assertion fails with actual vs expected status codes

### Requirement: Exact body assertion
The plugin SHALL assert that the last response body matches the expected value exactly (inline docstring).

#### Scenario: Exact body match
- **WHEN** the response body is `{"id":1}` and `rest.response.body` expects `{"id":1}`
- **THEN** the assertion passes

### Requirement: Exact body assertion from file
The plugin SHALL assert that the last response body matches the contents of a file exactly.

#### Scenario: Body matches file contents
- **WHEN** the response body equals the content of the referenced file
- **THEN** `rest.response.body.file` passes

### Requirement: Partial body assertion
The plugin SHALL assert that the last response body *contains* the expected value (subset match).

#### Scenario: JSON subset match
- **WHEN** the response body is `{"id":1,"name":"Ana"}` and the assertion expects `{"id":1}`
- **THEN** `rest.response.body.contains` passes

### Requirement: Response header assertion
The plugin SHALL assert that the last response includes the specified headers with the given values.
Additional headers not listed in the table SHALL be accepted.

#### Scenario: Expected headers present
- **WHEN** the response includes `Content-Type: application/json` and the assertion lists that header
- **THEN** the assertion passes

#### Scenario: Missing header fails
- **WHEN** an expected header is absent from the response
- **THEN** the assertion fails

### Requirement: Response cookie assertion
The plugin SHALL assert that the last response sets the specified cookies with the given values.
Only the cookie value is checked; attributes (Path, Domain, HttpOnly, Secure) are ignored.
Additional cookies not listed in the table SHALL be accepted.

#### Scenario: Expected cookie present with correct value
- **WHEN** the response sets `session=abc` and the assertion expects `session: abc`
- **THEN** the assertion passes

---

## Variable Extraction

### Requirement: Extract field from response body
The plugin SHALL extract the value at a JSON path in the last response body and store it
in a named scenario variable for use in subsequent steps.

#### Scenario: Field extracted and available as variable
- **WHEN** the response body is `{"user":{"id":42}}` and `rest.response.extracts.field` extracts `user.id` into `userId`
- **THEN** `${userId}` equals `"42"` in subsequent steps

### Requirement: Extract response header into variable
The plugin SHALL store the value of a named response header into a scenario variable.
The step SHALL fail if the header is not present in the response.

#### Scenario: Header extracted successfully
- **WHEN** the response includes `Location: /users/42` and the step extracts `Location` into `loc`
- **THEN** `${loc}` equals `"/users/42"`

#### Scenario: Missing header fails extraction
- **WHEN** the expected header is not present in the response
- **THEN** the step fails with a descriptive error

---

## Configuration

| Property | Type | Default | Description |
|---|---|---|---|
| `rest.baseURL` | text | — | Base URL prepended to all relative endpoint arguments |
| `rest.httpCodeThreshold` | integer | 500 | Responses with status ≥ this value fail the request step automatically |
| `rest.timeout` | integer (ms) | 60000 | Abort request if no response received within this duration |
| `rest.contentType` | text | `application/json` | Default `Content-Type` for request bodies |