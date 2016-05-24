package org.loklak;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.server.handler.ErrorHandler;

public class LoklakErrorHandler extends ErrorHandler {
	@Override
	protected void handleErrorPage(HttpServletRequest request,
			Writer writer, int code, String message) throws IOException {
		if (code == 404) {
			writer.write("<script>window.setTimeout(function() { window.location.href = '/ERROR/404.html'; }, 500);</script>");
			// return a page here instead
			return;
		}
		
		super.handleErrorPage(request, writer, code, message);
	}
}
