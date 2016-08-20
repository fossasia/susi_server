/**
 * boilerpipe
 *
 * Copyright (c) 2009, 2010 Christian Kohlschütter
 *
 * The author licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.l3s.boilerpipe.sax;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * Defines an action that is to be performed whenever a particular tag occurs
 * during HTML parsing.
 * 
 * @author Christian Kohlschütter
 */
public interface TagAction {

	boolean start(final BoilerpipeHTMLContentHandler instance,
			final String localName, final String qName, final Attributes atts)
			throws SAXException;

	boolean end(final BoilerpipeHTMLContentHandler instance,
			final String localName, final String qName) throws SAXException;
	
	boolean changesTagLevel();
}