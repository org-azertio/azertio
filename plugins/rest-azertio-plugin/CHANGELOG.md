# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.0] - 2026-06-19

### Added
- `rest.auth.oauth2.client_credentials` step: obtain an OAuth2 client credentials token and store it in a variable
- `rest.auth.oauth2.password` step: obtain an OAuth2 password grant (ROPC) token
- `rest.request.post.form_data` step: POST with `multipart/form-data` body via DataTable
- `rest.request.post.urlencoded` step: POST with `application/x-www-form-urlencoded` body via DataTable
- `rest.response.cookies` step: assert `Set-Cookie` response headers
- DEBUG-level HTTP exchange logging (`[REST] -->` / `[REST] <--`) with body truncation and Authorization header masking

### Fixed
- OAuth2 client credentials now uses `Authorization: Basic` header (RFC 6749 §2.3.1) instead of body params — fixes compatibility with Spring Authorization Server and other servers that only accept Basic authentication
- `fetchOAuth2Token` no longer prepends `baseURL` to the token URL; the token URL is always used as-is (absolute URL)

## [1.0.0] - 2026-05-17

### Added
- Table-based steps for setting request headers
- Table-based steps for asserting response headers
- Authorization steps: Basic, Bearer token, API Key (header and query param), OAuth2
- Persistent headers support across multiple requests
- Response header extraction into variables
- Step and config help provider for VS Code integration

### Fixed
- Bearer token literals now quoted to match text type pattern
- NPE when using a variable in Bearer token step
- Typo in RestStepProvider causing import failure