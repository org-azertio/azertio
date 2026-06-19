package org.azertio.plugins.rest;

import java.util.Map;

public interface RestEngine {

	void setBaseUrl(String baseUrl);
	void setHttpCodeThreshold(Integer httpCode);
	void setTimeout(Long milliseconds);
	int requestGET(String endpoint);
	int requestPOST(String endpoint);
	int requestPOST(String endpoint, String content);
	int requestPOSTUrlEncoded(String endpoint, Map<String, String> fields);
	int requestPOSTMultipart(String endpoint, Map<String, String> fields);
	int requestPUT(String endpoint, String content);
	int requestPATCH(String endpoint, String content);
	void setContentType(String contentType);
	int requestDELETE(String endpoint);
	/** Sets headers for the next request only; cleared after it is sent. */
	void setNextRequestHeaders(Map<String, String> headers);
	/** Merges headers that will be sent with every subsequent request. */
	void setPersistentHeaders(Map<String, String> headers);
	/** Appends a query parameter to every subsequent request URL. */
	void addPersistentQueryParam(String name, String value);
	/** Calls an OAuth2 token endpoint (client_credentials grant) and returns the access token. */
	String fetchOAuth2Token(String tokenUrl, String clientId, String clientSecret);
	/** Calls an OAuth2 token endpoint (password grant / ROPC) and returns the access token. */
	String fetchOAuth2PasswordToken(String tokenUrl, String clientId, String clientSecret, String username, String password);
	Integer responseHttpCode();
	String responseBody();
	String responseContentType();
	String responseHeader(String name);
	String responseCookie(String name);
	String requestRaw();
	String responseRaw();

}
