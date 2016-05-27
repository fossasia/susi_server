/**
 *  LoklakEmailHandler
 *  Copyright 25.05.2016 by Shiven Mian, @shivenmian
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

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.jetty.util.log.Log;

public class LoklakEmailHandler {
	
	private String addressTo;
	private String addressFrom;
	private String subject;
	private String text;
	private Pattern pattern;
	private static final String EMAIL_PATTERN = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@" + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
	
	public LoklakEmailHandler (String addressTo, String addressFrom, String subject, String text) throws PatternSyntaxException, IllegalStateException{
		pattern = Pattern.compile(EMAIL_PATTERN);
		this.addressTo = addressTo;
		this.addressFrom = addressFrom;
		this.subject = subject;
		this.text = text;
		if(pattern.matcher(addressTo).matches() && pattern.matcher(addressFrom).matches()){
			//send to server
		} else {
			Log.getLog().info("Invalid email address");
		}
	}
}
