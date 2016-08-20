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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import de.l3s.boilerpipe.document.TextBlock;
import de.l3s.boilerpipe.labels.LabelAction;

/**
 * Defines an action that is to be performed whenever a particular tag occurs during HTML parsing.
 * 
 * @author Christian Kohlschütter
 */
public abstract class CommonTagActions {

	private CommonTagActions() {
	}

    public static final class Chained implements TagAction {

        private final TagAction t1;
        private final TagAction t2;

        public Chained(final TagAction t1, final TagAction t2) {
            this.t1 = t1;
            this.t2 = t2;
        }

        public boolean start(BoilerpipeHTMLContentHandler instance,
                String localName, String qName, Attributes atts)
                throws SAXException {
            return t1.start(instance, localName, qName, atts)
                    | t2.start(instance, localName, qName, atts);
        }

        public boolean end(BoilerpipeHTMLContentHandler instance,
                String localName, String qName) throws SAXException {
            return t1.end(instance, localName, qName)
                    | t2.end(instance, localName, qName);
        }

        public boolean changesTagLevel() {
        	return t1.changesTagLevel() || t2.changesTagLevel();
        }
    }

    /**
     * Marks this tag as "ignorable", i.e. all its inner content is silently skipped.
     */
    public static final TagAction TA_IGNORABLE_ELEMENT = new TagAction() {

        public boolean start(final BoilerpipeHTMLContentHandler instance,
                final String localName, final String qName,
                final Attributes atts) {
            instance.inIgnorableElement++;
            return true;
        }

        public boolean end(final BoilerpipeHTMLContentHandler instance,
                final String localName, final String qName) {
            instance.inIgnorableElement--;
            return true;
        }
        
        public boolean changesTagLevel() {
        	return true;
        }
    };
    
    /**
     * Marks this tag as "anchor" (this should usually only be set for the <code>&lt;A&gt;</code> tag).
     * Anchor tags may not be nested.
     * 
     * There is a bug in certain versions of NekoHTML which still allows nested tags.
     * If boilerpipe encounters such nestings, a SAXException is thrown.
     */
    public static final TagAction TA_ANCHOR_TEXT = new TagAction() {

        public boolean start(BoilerpipeHTMLContentHandler instance,
                final String localName, final String qName,
                final Attributes atts) throws SAXException {
            if (instance.inAnchor++ > 0) {
                // as nested A elements are not allowed per specification, we
                // are probably reaching this branch due to a bug in the XML
                // parser
            	System.err.println("Warning: SAX input contains nested A elements -- You have probably hit a bug in your HTML parser (e.g., NekoHTML bug #2909310). Please clean the HTML externally and feed it to boilerpipe again. Trying to recover somehow...");
            	
            	end(instance, localName, qName);
            }
            if (instance.inIgnorableElement == 0) {
                instance.addWhitespaceIfNecessary();
                instance.tokenBuffer
                        .append(BoilerpipeHTMLContentHandler.ANCHOR_TEXT_START);
                instance.tokenBuffer.append(' ');
                instance.sbLastWasWhitespace = true;
            }
            return false;
        }

        public boolean end(BoilerpipeHTMLContentHandler instance,
                final String localName, final String qName) {
            if (--instance.inAnchor == 0) {
                if (instance.inIgnorableElement == 0) {
                    instance.addWhitespaceIfNecessary();
                    instance.tokenBuffer
                            .append(BoilerpipeHTMLContentHandler.ANCHOR_TEXT_END);
                    instance.tokenBuffer.append(' ');
                    instance.sbLastWasWhitespace = true;
                }
            }
            return false;
        }

        public boolean changesTagLevel() {
        	return true;
        }
    };
    
    /**
     * Marks this tag the body element (this should usually only be set for the <code>&lt;BODY&gt;</code> tag).
     */
    public static final TagAction TA_BODY = new TagAction() {
        public boolean start(final BoilerpipeHTMLContentHandler instance,
                final String localName, final String qName,
                final Attributes atts) {
            instance.flushBlock();
            instance.inBody++;
            return false;
        }

        public boolean end(final BoilerpipeHTMLContentHandler instance,
                final String localName, final String qName) {
            instance.flushBlock();
            instance.inBody--;
            return false;
        }
        
        public boolean changesTagLevel() {
        	return true;
        }
    };

    /**
     * Marks this tag a simple "inline" element, which generates whitespace, but no new block.
     */
    public static final TagAction TA_INLINE_WHITESPACE = new TagAction() {

        public boolean start(BoilerpipeHTMLContentHandler instance,
                final String localName, final String qName,
                final Attributes atts) {
            instance.addWhitespaceIfNecessary();
            return false;
        }

        public boolean end(BoilerpipeHTMLContentHandler instance,
                final String localName, final String qName) {
            instance.addWhitespaceIfNecessary();
            return false;
        }
        
        public boolean changesTagLevel() {
        	return false;
        }
    };
    
    /**
     * @deprecated Use {@link #TA_INLINE_WHITESPACE} instead
     */
    @Deprecated
    public static final TagAction TA_INLINE = TA_INLINE_WHITESPACE;
    
    /**
     * Marks this tag a simple "inline" element, which neither generates whitespace, nor a new block.
     */
    public static final TagAction TA_INLINE_NO_WHITESPACE = new TagAction() {

        public boolean start(BoilerpipeHTMLContentHandler instance,
                final String localName, final String qName,
                final Attributes atts) {
            return false;
        }

        public boolean end(BoilerpipeHTMLContentHandler instance,
                final String localName, final String qName) {
            return false;
        }

        public boolean changesTagLevel() {
        	return false;
        }
    };
    private static final Pattern PAT_FONT_SIZE = Pattern
            .compile("([\\+\\-]?)([0-9])");
    
    /**
     * Explicitly marks this tag a simple "block-level" element, which always generates whitespace
     */
    public static final TagAction TA_BLOCK_LEVEL = new TagAction() {

        public boolean start(BoilerpipeHTMLContentHandler instance,
                final String localName, final String qName,
                final Attributes atts) {
            return true;
        }

        public boolean end(BoilerpipeHTMLContentHandler instance,
                final String localName, final String qName) {
            return true;
        }
        
        public boolean changesTagLevel() {
        	return true;
        }
    };    
    
    /**
     * Special TagAction for the <code>&lt;FONT&gt;</code> tag, which keeps track of the
     * absolute and relative font size.
     */
    public static final TagAction TA_FONT = new TagAction() {

        public boolean start(final BoilerpipeHTMLContentHandler instance,
                final String localName, final String qName,
                final Attributes atts) {

            String sizeAttr = atts.getValue("size");
            if (sizeAttr != null) {
                Matcher m = PAT_FONT_SIZE.matcher(sizeAttr);
                if (m.matches()) {
                    String rel = m.group(1);
                    final int val = Integer.parseInt(m.group(2));
                    final int size;
                    if (rel.length() == 0) {
                        // absolute
                        size = val;
                    } else {
                        // relative
                        int prevSize;
                        if (instance.fontSizeStack.isEmpty()) {
                            prevSize = 3;
                        } else {
                            prevSize = 3;
                            for (Integer s : instance.fontSizeStack) {
                                if (s != null) {
                                    prevSize = s;
                                    break;
                                }
                            }
                        }
                        if (rel.charAt(0) == '+') {
                            size = prevSize + val;
                        } else {
                            size = prevSize - val;
                        }

                    }
                    instance.fontSizeStack.add(0, size);
                } else {
                    instance.fontSizeStack.add(0, null);
                }
            } else {
                instance.fontSizeStack.add(0, null);
            }
            return false;
        }

        public boolean end(final BoilerpipeHTMLContentHandler instance,
                final String localName, final String qName) {
            instance.fontSizeStack.removeFirst();
            return false;
        }
        
        public boolean changesTagLevel() {
        	return false;
        }
    };

    /**
     * {@link CommonTagActions} for inline elements, which triggers some {@link LabelAction} on the generated
     * {@link TextBlock}.
     */
    public static final class InlineTagLabelAction implements TagAction {

        private final LabelAction action;

        public InlineTagLabelAction(final LabelAction action) {
            this.action = action;
        }

        public boolean start(BoilerpipeHTMLContentHandler instance,
                final String localName, final String qName,
                final Attributes atts) {
            instance.addWhitespaceIfNecessary();
            instance.addLabelAction(action);
            return false;
        }

        public boolean end(BoilerpipeHTMLContentHandler instance,
                final String localName, final String qName) {
            instance.addWhitespaceIfNecessary();
            return false;
        }
        
        public boolean changesTagLevel() {
        	return false;
        }
    }

    /**
     * {@link CommonTagActions} for block-level elements, which triggers some {@link LabelAction} on the generated
     * {@link TextBlock}.
     */
    public static final class BlockTagLabelAction implements TagAction {

        private final LabelAction action;

        public BlockTagLabelAction(final LabelAction action) {
            this.action = action;
        }

        public boolean start(BoilerpipeHTMLContentHandler instance,
                final String localName, final String qName,
                final Attributes atts) {
            instance.addLabelAction(action);
            return true;
        }

        public boolean end(BoilerpipeHTMLContentHandler instance,
                final String localName, final String qName) {
            return true;
        }
        
        public boolean changesTagLevel() {
        	return true;
        }
    }
}