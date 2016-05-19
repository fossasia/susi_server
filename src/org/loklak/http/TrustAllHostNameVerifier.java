package org.loklak.http;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

public class TrustAllHostNameVerifier implements HostnameVerifier {

	public boolean verify(String hostname, SSLSession session) {
		return true;
	}
}
