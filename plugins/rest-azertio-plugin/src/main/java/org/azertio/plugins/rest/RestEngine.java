package org.azertio.plugins.rest;

import java.util.Map;

public interface RestEngine {

	void setBaseUrl(String baseUrl);
	void setHttpCodeThreshold(Integer httpCode);
	void setTimeout(Long milliseconds);
	int requestGET(String endpoint);
	int requestPOST(String endpoint);
	int requestPOST(String endpoint, String content);
	int requestPUT(String endpoint, String content);
	int requestPATCH(String endpoint, String content);
	void setContentType(String contentType);
	int requestDELETE(String endpoint);
	void setNextRequestHeaders(Map<String, String> headers);
	Integer responseHttpCode();
	String responseBody();
	String responseContentType();
	String responseHeader(String name);
	String requestRaw();
	String responseRaw();

}
