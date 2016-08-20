/* 
 * Copyright Marc Guillemot
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cyberneko.html;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.XMLDocumentHandler;
import org.apache.xerces.xni.XMLString;

/**
 * Container for text that should be hold and re-feed later like text before &lt;html&gt; that will be re-feed
 * in &lt;body&gt;
 * @author Marc Guillemot
 *
 * @version $Id: LostText.java 226 2009-02-09 20:48:44Z mguillem $
 */
class LostText
{
	/**
	 * Pair of (text, augmentation)
	 */
	static class Entry
	{
    	private XMLString text_;
    	private Augmentations augs_;

    	public Entry(final XMLString text, final Augmentations augs)
    	{
    		final char[] chars = new char[text.length];
    		System.arraycopy(text.ch, text.offset, chars, 0, text.length);
    		text_ = new XMLString(chars, 0, chars.length);
    		if (augs != null)
    			augs_ = new HTMLAugmentations(augs);
    	}
	}
	private final List entries = new ArrayList();

	/**
	 * Adds some text that need to be re-feed later. The information gets copied.
	 */
	public void add(final XMLString text, final Augmentations augs)
	{
		if (!entries.isEmpty() || text.toString().trim().length() > 0)
			entries.add(new Entry(text, augs));
	}
	
	/**
	 * Pushes the characters into the {@link XMLDocumentHandler}
	 * @param tagBalancer the tag balancer that will receive the events
	 */
	public void refeed(final XMLDocumentHandler tagBalancer) {
		for (final Iterator iter = entries.iterator(); iter.hasNext();) {
			final LostText.Entry entry = (LostText.Entry) iter.next();
			tagBalancer.characters(entry.text_, entry.augs_);
		}
		// not needed anymore once it has been used -> clear to free memory
		entries.clear();
	}
	
	/**
	 * Indicates if this container contains something
	 * @return <code>true</code> if no lost text has been collected
	 */
	public boolean isEmpty() {
		return entries.isEmpty();
	}
}