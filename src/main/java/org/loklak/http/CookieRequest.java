package org.loklak.http;

public interface CookieRequest {
	CookieRequest makeRequest();
	String body();
	String cookie();
}
