/**
 *  LoklakErrorHandler
 *  Copyright 24.05.2016 by Sudheesh Singanamalla, @sudheesh001
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *  
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program in the file lgpl21.txt
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package org.loklak;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;
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
		if (code == 500) {
			JSONObject errorMessage = new JSONObject();
			errorMessage.put("Error", "Please check the input params which you had provided.");
			writer.write(errorMessage.toString());
			return;
		}	
		super.handleErrorPage(request, writer, code, message);
	}
}
