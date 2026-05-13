package org.azertio.plugins.rest;

public interface RestEngine {

	void setBaseUrl(String baseUrl);
	void setHttpCodeThreshold(Integer httpCode);
	void setTimeout(Long milliseconds);
	int requestGET(String endpoint);
	int requestPOST(String endpoint);
	int requestPOST(String endpoint, String content);
	int requestPUT(String endpoint, String content);
	int requestPATCH(String endpoint, String content);
	int requestDELETE(String endpoint);
	Integer responseHttpCode();
	String responseBody();
	String responseContentType();
	String requestRaw();
	String responseRaw();

}
