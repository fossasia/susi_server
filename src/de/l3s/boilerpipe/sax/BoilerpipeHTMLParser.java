/**
 * boilerpipe
 *
 * Copyright (c) 2009 Christian Kohlschütter
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

import org.apache.xerces.parsers.AbstractSAXParser;
import org.cyberneko.html.HTMLConfiguration;

import de.l3s.boilerpipe.BoilerpipeDocumentSource;
import de.l3s.boilerpipe.document.TextBlock;
import de.l3s.boilerpipe.document.TextDocument;

/**
 * A simple SAX Parser, used by {@link BoilerpipeSAXInput}. The parser uses <a
 * href="http://nekohtml.sourceforge.net/">CyberNeko</a> to parse HTML content.
 * 
 * @author Christian Kohlschütter
 */
public class BoilerpipeHTMLParser extends AbstractSAXParser implements BoilerpipeDocumentSource {

    private BoilerpipeHTMLContentHandler contentHandler;

    /**
     * Constructs a {@link BoilerpipeHTMLParser} using a default HTML content handler.
     */
    public BoilerpipeHTMLParser() {
        this(new BoilerpipeHTMLContentHandler());
    }

    /**
     * Constructs a {@link BoilerpipeHTMLParser} using the given {@link BoilerpipeHTMLContentHandler}.
     *
     * @param contentHandler
     */
    public BoilerpipeHTMLParser(BoilerpipeHTMLContentHandler contentHandler) {
        super(new HTMLConfiguration());
        setContentHandler(contentHandler);
    }
    
    protected BoilerpipeHTMLParser(boolean ignore) {
    	super(new HTMLConfiguration());
    }

    public void setContentHandler(final BoilerpipeHTMLContentHandler contentHandler) {
    	this.contentHandler = contentHandler;
    	super.setContentHandler(contentHandler);
    }
    public void setContentHandler(final org.xml.sax.ContentHandler contentHandler) {
    	this.contentHandler = null;
    	super.setContentHandler(contentHandler);
    }
    
    /**
     * Returns a {@link TextDocument} containing the extracted {@link TextBlock}
     * s. NOTE: Only call this after {@link #parse(org.xml.sax.InputSource)}.
     * 
     * @return The {@link TextDocument}
     */
    public TextDocument toTextDocument() {
        return contentHandler.toTextDocument();
    }
}