/* 
 * Copyright 2002-2009 Andy Clark, Marc Guillemot
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

package org.cyberneko.html.filters;

import java.util.Hashtable;

import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.NamespaceContext;
import org.apache.xerces.xni.QName;
import org.apache.xerces.xni.XMLAttributes;
import org.apache.xerces.xni.XMLLocator;
import org.apache.xerces.xni.XMLResourceIdentifier;
import org.apache.xerces.xni.XMLString;
import org.apache.xerces.xni.XNIException;

/**
 * This class is a document filter capable of removing specified
 * elements from the processing stream. There are two options for
 * processing document elements:
 * <ul>
 * <li>specifying those elements which should be accepted and,
 *     optionally, which attributes of that element should be
 *     kept; and
 * <li>specifying those elements whose tags and content should be
 *     completely removed from the event stream.
 * </ul>
 * <p>
 * The first option allows the application to specify which elements
 * appearing in the event stream should be accepted and, therefore,
 * passed on to the next stage in the pipeline. All elements 
 * <em>not</em> in the list of acceptable elements have their start 
 * and end tags stripped from the event stream <em>unless</em> those
 * elements appear in the list of elements to be removed. 
 * <p>
 * The second option allows the application to specify which elements
 * should be completely removed from the event stream. When an element
 * appears that is to be removed, the element's start and end tag as
 * well as all of that element's content is removed from the event
 * stream.
 * <p>
 * A common use of this filter would be to only allow rich-text
 * and linking elements as well as the character content to pass 
 * through the filter &mdash; all other elements would be stripped.
 * The following code shows how to configure this filter to perform
 * this task:
 * <pre>
 *  ElementRemover remover = new ElementRemover();
 *  remover.acceptElement("b", null);
 *  remover.acceptElement("i", null);
 *  remover.acceptElement("u", null);
 *  remover.acceptElement("a", new String[] { "href" });
 * </pre>
 * <p>
 * However, this would still allow the text content of other
 * elements to pass through, which may not be desirable. In order
 * to further "clean" the input, the <code>removeElement</code>
 * option can be used. The following piece of code adds the ability
 * to completely remove any &lt;SCRIPT&gt; tags and content 
 * from the stream.
 * <pre>
 *  remover.removeElement("script");
 * </pre>
 * <p>
 * <strong>Note:</strong> 
 * All text and accepted element children of a stripped element is 
 * retained. To completely remove an element's content, use the
 * <code>removeElement</code> method.
 * <p>
 * <strong>Note:</strong>
 * Care should be taken when using this filter because the output
 * may not be a well-balanced tree. Specifically, if the application
 * removes the &lt;HTML&gt; element (with or without retaining its
 * children), the resulting document event stream will no longer be
 * well-formed.
 *
 * @author Andy Clark
 *
 * @version $Id: ElementRemover.java,v 1.5 2005/02/14 03:56:54 andyc Exp $
 */
public class ElementRemover
    extends DefaultFilter {

    //
    // Constants
    //

    /** A "null" object. */
    protected static final Object NULL = new Object();

    //
    // Data
    //

    // information

    /** Accepted elements. */
    protected Hashtable fAcceptedElements = new Hashtable();

    /** Removed elements. */
    protected Hashtable fRemovedElements = new Hashtable();

    // state

    /** The element depth. */
    protected int fElementDepth;

    /** The element depth at element removal. */
    protected int fRemovalElementDepth;

    //
    // Public methods
    //

    /** 
     * Specifies that the given element should be accepted and, optionally,
     * which attributes of that element should be kept.
     *
     * @param element The element to accept.
     * @param attributes The list of attributes to be kept or null if no
     *                   attributes should be kept for this element.
     *
     * see #removeElement
     */
    public void acceptElement(String element, String[] attributes) {
        Object key = element.toLowerCase();
        Object value = NULL;
        if (attributes != null) {
            String[] newarray = new String[attributes.length];
            for (int i = 0; i < attributes.length; i++) {
                newarray[i] = attributes[i].toLowerCase();
            }
            value = attributes;
        }
        fAcceptedElements.put(key, value);
    } // acceptElement(String,String[])

    /** 
     * Specifies that the given element should be completely removed. If an
     * element is encountered during processing that is on the remove list, 
     * the element's start and end tags as well as all of content contained
     * within the element will be removed from the processing stream.
     *
     * @param element The element to completely remove.
     */
    public void removeElement(String element) {
        Object key = element.toLowerCase();
        Object value = NULL;
        fRemovedElements.put(key, value);
    } // removeElement(String)

    //
    // XMLDocumentHandler methods
    //

    // since Xerces-J 2.2.0

    /** Start document. */
    public void startDocument(XMLLocator locator, String encoding, 
                              NamespaceContext nscontext, Augmentations augs) 
        throws XNIException {
        fElementDepth = 0;
        fRemovalElementDepth = Integer.MAX_VALUE;
        super.startDocument(locator, encoding, nscontext, augs);
    } // startDocument(XMLLocator,String,NamespaceContext,Augmentations)

    // old methods

    /** Start document. */
    public void startDocument(XMLLocator locator, String encoding, Augmentations augs)
        throws XNIException {
        startDocument(locator, encoding, null, augs);
    } // startDocument(XMLLocator,String,Augmentations)

    /** Start prefix mapping. */
    public void startPrefixMapping(String prefix, String uri, Augmentations augs)
        throws XNIException {
        if (fElementDepth <= fRemovalElementDepth) {
            super.startPrefixMapping(prefix, uri, augs);
        }
    } // startPrefixMapping(String,String,Augmentations)

    /** Start element. */
    public void startElement(QName element, XMLAttributes attributes, Augmentations augs)
        throws XNIException {
        if (fElementDepth <= fRemovalElementDepth && handleOpenTag(element, attributes)) {
            super.startElement(element, attributes, augs);
        }
        fElementDepth++;
    } // startElement(QName,XMLAttributes,Augmentations)

    /** Empty element. */
    public void emptyElement(QName element, XMLAttributes attributes, Augmentations augs)
        throws XNIException {
        if (fElementDepth <= fRemovalElementDepth && handleOpenTag(element, attributes)) {
            super.emptyElement(element, attributes, augs);
        }
    } // emptyElement(QName,XMLAttributes,Augmentations)

    /** Comment. */
    public void comment(XMLString text, Augmentations augs)
        throws XNIException {
        if (fElementDepth <= fRemovalElementDepth) {
            super.comment(text, augs);
        }
    } // comment(XMLString,Augmentations)

    /** Processing instruction. */
    public void processingInstruction(String target, XMLString data, Augmentations augs)
        throws XNIException {
        if (fElementDepth <= fRemovalElementDepth) {
            super.processingInstruction(target, data, augs);
        }
    } // processingInstruction(String,XMLString,Augmentations)

    /** Characters. */
    public void characters(XMLString text, Augmentations augs) 
        throws XNIException {
        if (fElementDepth <= fRemovalElementDepth) {
            super.characters(text, augs);
        }
    } // characters(XMLString,Augmentations)

    /** Ignorable whitespace. */
    public void ignorableWhitespace(XMLString text, Augmentations augs) 
        throws XNIException {
        if (fElementDepth <= fRemovalElementDepth) {
            super.ignorableWhitespace(text, augs);
        }
    } // ignorableWhitespace(XMLString,Augmentations)

    /** Start general entity. */
    public void startGeneralEntity(String name, XMLResourceIdentifier id, String encoding, Augmentations augs)
        throws XNIException {
        if (fElementDepth <= fRemovalElementDepth) {
            super.startGeneralEntity(name, id, encoding, augs);
        }
    } // startGeneralEntity(String,XMLResourceIdentifier,String,Augmentations)

    /** Text declaration. */
    public void textDecl(String version, String encoding, Augmentations augs)
        throws XNIException {
        if (fElementDepth <= fRemovalElementDepth) {
            super.textDecl(version, encoding, augs);
        }
    } // textDecl(String,String,Augmentations)

    /** End general entity. */
    public void endGeneralEntity(String name, Augmentations augs)
        throws XNIException {
        if (fElementDepth <= fRemovalElementDepth) {
            super.endGeneralEntity(name, augs);
        }
    } // endGeneralEntity(String,Augmentations)

    /** Start CDATA section. */
    public void startCDATA(Augmentations augs) throws XNIException {
        if (fElementDepth <= fRemovalElementDepth) {
            super.startCDATA(augs);
        }
    } // startCDATA(Augmentations)

    /** End CDATA section. */
    public void endCDATA(Augmentations augs) throws XNIException {
        if (fElementDepth <= fRemovalElementDepth) {
            super.endCDATA(augs);
        }
    } // endCDATA(Augmentations)

    /** End element. */
    public void endElement(QName element, Augmentations augs)
        throws XNIException {
        if (fElementDepth <= fRemovalElementDepth && elementAccepted(element.rawname)) {
            super.endElement(element, augs);
        }
        fElementDepth--;
        if (fElementDepth == fRemovalElementDepth) {
            fRemovalElementDepth = Integer.MAX_VALUE;
        }
    } // endElement(QName,Augmentations)

    /** End prefix mapping. */
    public void endPrefixMapping(String prefix, Augmentations augs)
        throws XNIException {
        if (fElementDepth <= fRemovalElementDepth) {
            super.endPrefixMapping(prefix, augs);
        }
    } // endPrefixMapping(String,Augmentations)

    //
    // Protected methods
    //

    /** Returns true if the specified element is accepted. */
    protected boolean elementAccepted(String element) {
        Object key = element.toLowerCase();
        return fAcceptedElements.containsKey(key);
    } // elementAccepted(String):boolean

    /** Returns true if the specified element should be removed. */
    protected boolean elementRemoved(String element) {
        Object key = element.toLowerCase();
        return fRemovedElements.containsKey(key);
    } // elementRemoved(String):boolean

    /** Handles an open tag. */
    protected boolean handleOpenTag(QName element, XMLAttributes attributes) {
        if (elementAccepted(element.rawname)) {
            Object key = element.rawname.toLowerCase();
            Object value = fAcceptedElements.get(key);
            if (value != NULL) {
                String[] anames = (String[])value;
                int attributeCount = attributes.getLength();
                LOOP: for (int i = 0; i < attributeCount; i++) {
                    String aname = attributes.getQName(i).toLowerCase();
                    for (int j = 0; j < anames.length; j++) {
                        if (anames[j].equals(aname)) {
                            continue LOOP;
                        }
                    }
                    attributes.removeAttributeAt(i--);
                    attributeCount--;
                }
            }
            else {
                attributes.removeAllAttributes();
            }
            return true;
        }
        else if (elementRemoved(element.rawname)) {
            fRemovalElementDepth = fElementDepth;
        }
        return false;
    } // handleOpenTag(QName,XMLAttributes):boolean

} // class DefaultFilter
