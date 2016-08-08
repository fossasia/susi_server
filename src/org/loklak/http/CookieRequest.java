package org.loklak.http;

public interface CookieRequest {
	public CookieRequest makeRequest();
	public String body();
	public String cookie();
}
