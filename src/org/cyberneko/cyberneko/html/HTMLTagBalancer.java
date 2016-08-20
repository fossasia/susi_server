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

package org.cyberneko.html;

import java.util.ArrayList;
import java.util.List;

import org.apache.xerces.util.XMLAttributesImpl;
import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.NamespaceContext;
import org.apache.xerces.xni.QName;
import org.apache.xerces.xni.XMLAttributes;
import org.apache.xerces.xni.XMLDocumentHandler;
import org.apache.xerces.xni.XMLLocator;
import org.apache.xerces.xni.XMLResourceIdentifier;
import org.apache.xerces.xni.XMLString;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLComponentManager;
import org.apache.xerces.xni.parser.XMLConfigurationException;
import org.apache.xerces.xni.parser.XMLDocumentFilter;
import org.apache.xerces.xni.parser.XMLDocumentSource;
import org.cyberneko.html.HTMLElements.Element;
import org.cyberneko.html.filters.NamespaceBinder;
import org.cyberneko.html.xercesbridge.XercesBridge;
                      
/**
 * Balances tags in an HTML document. This component receives document events
 * and tries to correct many common mistakes that human (and computer) HTML
 * document authors make. This tag balancer can:
 * <ul>
 * <li>add missing parent elements;
 * <li>automatically close elements with optional end tags; and
 * <li>handle mis-matched inline element tags.
 * </ul>
 * <p>
 * This component recognizes the following features:
 * <ul>
 * <li>http://cyberneko.org/html/features/augmentations
 * <li>http://cyberneko.org/html/features/report-errors
 * <li>http://cyberneko.org/html/features/balance-tags/document-fragment
 * <li>http://cyberneko.org/html/features/balance-tags/ignore-outside-content
 * </ul>
 * <p>
 * This component recognizes the following properties:
 * <ul>
 * <li>http://cyberneko.org/html/properties/names/elems
 * <li>http://cyberneko.org/html/properties/names/attrs
 * <li>http://cyberneko.org/html/properties/error-reporter
 * <li>http://cyberneko.org/html/properties/balance-tags/current-stack
 * </ul>
 *
 * @see HTMLElements
 *
 * @author Andy Clark
 * @author Marc Guillemot
 *
 * @version $Id: HTMLTagBalancer.java,v 1.20 2005/02/14 04:06:22 andyc Exp $
 */
public class HTMLTagBalancer
    implements XMLDocumentFilter, HTMLComponent {

    //
    // Constants
    //

    // features

    /** Namespaces. */
    protected static final String NAMESPACES = "http://xml.org/sax/features/namespaces";

    /** Include infoset augmentations. */
    protected static final String AUGMENTATIONS = "http://cyberneko.org/html/features/augmentations";

    /** Report errors. */
    protected static final String REPORT_ERRORS = "http://cyberneko.org/html/features/report-errors";

    /** Document fragment balancing only (deprecated). */
    protected static final String DOCUMENT_FRAGMENT_DEPRECATED = "http://cyberneko.org/html/features/document-fragment";

    /** Document fragment balancing only. */
    protected static final String DOCUMENT_FRAGMENT = "http://cyberneko.org/html/features/balance-tags/document-fragment";

    /** Ignore outside content. */
    protected static final String IGNORE_OUTSIDE_CONTENT = "http://cyberneko.org/html/features/balance-tags/ignore-outside-content";

    /** Recognized features. */
    private static final String[] RECOGNIZED_FEATURES = {
        NAMESPACES,
        AUGMENTATIONS,
        REPORT_ERRORS,
        DOCUMENT_FRAGMENT_DEPRECATED,
        DOCUMENT_FRAGMENT,
        IGNORE_OUTSIDE_CONTENT,
    };

    /** Recognized features defaults. */
    private static final Boolean[] RECOGNIZED_FEATURES_DEFAULTS = {
        null,
        null,
        null,
        null,
        Boolean.FALSE,
        Boolean.FALSE,
    };

    // properties

    /** Modify HTML element names: { "upper", "lower", "default" }. */
    protected static final String NAMES_ELEMS = "http://cyberneko.org/html/properties/names/elems";

    /** Modify HTML attribute names: { "upper", "lower", "default" }. */
    protected static final String NAMES_ATTRS = "http://cyberneko.org/html/properties/names/attrs";
    
    /** Error reporter. */
    protected static final String ERROR_REPORTER = "http://cyberneko.org/html/properties/error-reporter";

    /**
     * <font color="red">EXPERIMENTAL: may change in next release</font><br/>
     * Name of the property holding the stack of elements in which context a document fragment should be parsed.
     **/
    public static final String FRAGMENT_CONTEXT_STACK = "http://cyberneko.org/html/properties/balance-tags/fragment-context-stack";

    /** Recognized properties. */
    private static final String[] RECOGNIZED_PROPERTIES = {
        NAMES_ELEMS,
        NAMES_ATTRS,
        ERROR_REPORTER,
        FRAGMENT_CONTEXT_STACK,
    };

    /** Recognized properties defaults. */
    private static final Object[] RECOGNIZED_PROPERTIES_DEFAULTS = {
        null,
        null,
        null,
        null,
    };

    // modify HTML names

    /** Don't modify HTML names. */
    protected static final short NAMES_NO_CHANGE = 0;

    /** Match HTML element names. */
    protected static final short NAMES_MATCH = 0;

    /** Uppercase HTML names. */
    protected static final short NAMES_UPPERCASE = 1;

    /** Lowercase HTML names. */
    protected static final short NAMES_LOWERCASE = 2;

    // static vars

    /** Synthesized event info item. */
    protected static final HTMLEventInfo SYNTHESIZED_ITEM = 
        new HTMLEventInfo.SynthesizedItem();

    //
    // Data
    //

    // features

    /** Namespaces. */
    protected boolean fNamespaces;

    /** Include infoset augmentations. */
    protected boolean fAugmentations;
    
    /** Report errors. */
    protected boolean fReportErrors;

    /** Document fragment balancing only. */
    protected boolean fDocumentFragment;

    /** Ignore outside content. */
    protected boolean fIgnoreOutsideContent;

    // properties

    /** Modify HTML element names. */
    protected short fNamesElems;

    /** Modify HTML attribute names. */
    protected short fNamesAttrs;

    /** Error reporter. */
    protected HTMLErrorReporter fErrorReporter;

    // connections

    /** The document source. */
    protected XMLDocumentSource fDocumentSource;

    /** The document handler. */
    protected XMLDocumentHandler fDocumentHandler;

    // state

    /** The element stack. */
    protected final InfoStack fElementStack = new InfoStack();

    /** The inline stack. */
    protected final InfoStack fInlineStack = new InfoStack();

    /** True if seen anything. Important for xml declaration. */
    protected boolean fSeenAnything;

    /** True if root element has been seen. */
    protected boolean fSeenDoctype;

    /** True if root element has been seen. */
    protected boolean fSeenRootElement;

    /** 
     * True if seen the end of the document element. In other words, 
     * this variable is set to false <em>until</em> the end &lt;/HTML&gt; 
     * tag is seen (or synthesized). This is used to ensure that 
     * extraneous events after the end of the document element do not 
     * make the document stream ill-formed.
     */
    protected boolean fSeenRootElementEnd;

    /** True if seen &lt;head&lt; element. */
    protected boolean fSeenHeadElement;

    /** True if seen &lt;body&lt; element. */
    protected boolean fSeenBodyElement;

    /** True if a form is in the stack (allow to discard opening of nested forms) */
    protected boolean fOpenedForm;

    // temp vars

    /** A qualified name. */
    private final QName fQName = new QName();

    /** Empty attributes. */
    private final XMLAttributes fEmptyAttrs = new XMLAttributesImpl();

    /** Augmentations. */
    private final HTMLAugmentations fInfosetAugs = new HTMLAugmentations();

    protected HTMLTagBalancingListener tagBalancingListener;
    private LostText lostText_ = new LostText();

    private boolean forcedStartElement_ = false;
    private boolean forcedEndElement_ = false;

    /**
     * Stack of elements determining the context in which a document fragment should be parsed
     */
	private QName[] fragmentContextStack_ = null;
	private int fragmentContextStackSize_ = 0; // not 0 only when a fragment is parsed and fragmentContextStack_ is set

    private List/*ElementEntry*/ endElementsBuffer_ = new ArrayList(); 

    //
    // HTMLComponent methods
    //

    /** Returns the default state for a feature. */
    public Boolean getFeatureDefault(String featureId) {
        int length = RECOGNIZED_FEATURES != null ? RECOGNIZED_FEATURES.length : 0;
        for (int i = 0; i < length; i++) {
            if (RECOGNIZED_FEATURES[i].equals(featureId)) {
                return RECOGNIZED_FEATURES_DEFAULTS[i];
            }
        }
        return null;
    } // getFeatureDefault(String):Boolean

    /** Returns the default state for a property. */
    public Object getPropertyDefault(String propertyId) {
        int length = RECOGNIZED_PROPERTIES != null ? RECOGNIZED_PROPERTIES.length : 0;
        for (int i = 0; i < length; i++) {
            if (RECOGNIZED_PROPERTIES[i].equals(propertyId)) {
                return RECOGNIZED_PROPERTIES_DEFAULTS[i];
            }
        }
        return null;
    } // getPropertyDefault(String):Object

    //
    // XMLComponent methods
    //

    /** Returns recognized features. */
    public String[] getRecognizedFeatures() {
        return RECOGNIZED_FEATURES;
    } // getRecognizedFeatures():String[]

    /** Returns recognized properties. */
    public String[] getRecognizedProperties() {
        return RECOGNIZED_PROPERTIES;
    } // getRecognizedProperties():String[]

    /** Resets the component. */
    public void reset(XMLComponentManager manager)
        throws XMLConfigurationException {

        // get features
        fNamespaces = manager.getFeature(NAMESPACES);
        fAugmentations = manager.getFeature(AUGMENTATIONS);
        fReportErrors = manager.getFeature(REPORT_ERRORS);
        fDocumentFragment = manager.getFeature(DOCUMENT_FRAGMENT) ||
                            manager.getFeature(DOCUMENT_FRAGMENT_DEPRECATED);
        fIgnoreOutsideContent = manager.getFeature(IGNORE_OUTSIDE_CONTENT);

        // get properties
        fNamesElems = getNamesValue(String.valueOf(manager.getProperty(NAMES_ELEMS)));
        fNamesAttrs = getNamesValue(String.valueOf(manager.getProperty(NAMES_ATTRS)));
        fErrorReporter = (HTMLErrorReporter)manager.getProperty(ERROR_REPORTER);
        
        fragmentContextStack_ = (QName[]) manager.getProperty(FRAGMENT_CONTEXT_STACK);

    } // reset(XMLComponentManager)

    /** Sets a feature. */
    public void setFeature(String featureId, boolean state)
        throws XMLConfigurationException {

        if (featureId.equals(AUGMENTATIONS)) {
            fAugmentations = state;
            return;
        }
        if (featureId.equals(REPORT_ERRORS)) {
            fReportErrors = state;
            return;
        }
        if (featureId.equals(IGNORE_OUTSIDE_CONTENT)) {
            fIgnoreOutsideContent = state;
            return;
        }

    } // setFeature(String,boolean)

    /** Sets a property. */
    public void setProperty(String propertyId, Object value)
        throws XMLConfigurationException {
    
        if (propertyId.equals(NAMES_ELEMS)) {
            fNamesElems = getNamesValue(String.valueOf(value));
            return;
        }

        if (propertyId.equals(NAMES_ATTRS)) {
            fNamesAttrs = getNamesValue(String.valueOf(value));
            return;
        }

    } // setProperty(String,Object)

    //
    // XMLDocumentSource methods
    //

    /** Sets the document handler. */
    public void setDocumentHandler(XMLDocumentHandler handler) {
        fDocumentHandler = handler;
    } // setDocumentHandler(XMLDocumentHandler)

    // @since Xerces 2.1.0

    /** Returns the document handler. */
    public XMLDocumentHandler getDocumentHandler() {
        return fDocumentHandler;
    } // getDocumentHandler():XMLDocumentHandler

    //
    // XMLDocumentHandler methods
    //

    // since Xerces-J 2.2.0

    /** Start document. */
    public void startDocument(XMLLocator locator, String encoding, 
                              NamespaceContext nscontext, Augmentations augs) 
        throws XNIException {

        // reset state
        fElementStack.top = 0;
        if (fragmentContextStack_ != null) {
        	fragmentContextStackSize_ = fragmentContextStack_.length;
        	for (int i=0; i<fragmentContextStack_.length; ++i) {
        		final QName name = fragmentContextStack_[i];
            	final Element elt = HTMLElements.getElement(name.localpart);
            	fElementStack.push(new Info(elt, name));
        	}
        	
        }
        else {
        	fragmentContextStackSize_ = 0;
        }
        fSeenAnything = false;
        fSeenDoctype = false;
        fSeenRootElement = false;
        fSeenRootElementEnd = false;
        fSeenHeadElement = false;
        fSeenBodyElement = false;
        

        // pass on event
        if (fDocumentHandler != null) {
        	XercesBridge.getInstance().XMLDocumentHandler_startDocument(fDocumentHandler, locator, encoding, nscontext, augs);
        }
    
    } // startDocument(XMLLocator,String,Augmentations)

    // old methods

    /** XML declaration. */
    public void xmlDecl(String version, String encoding, String standalone,
                        Augmentations augs) throws XNIException {
        if (!fSeenAnything && fDocumentHandler != null) {
            fDocumentHandler.xmlDecl(version, encoding, standalone, augs);
        }
    } // xmlDecl(String,String,String,Augmentations)

    /** Doctype declaration. */
    public void doctypeDecl(String rootElementName, String publicId, String systemId,
                            Augmentations augs) throws XNIException {
        fSeenAnything = true;
        if (fReportErrors) {
            if (fSeenRootElement) {
                fErrorReporter.reportError("HTML2010", null);
            }
            else if (fSeenDoctype) {
                fErrorReporter.reportError("HTML2011", null);
            }
        }
        if (!fSeenRootElement && !fSeenDoctype) {
            fSeenDoctype = true;
            if (fDocumentHandler != null) {
                fDocumentHandler.doctypeDecl(rootElementName, publicId, systemId, augs);
            }
        }
    } // doctypeDecl(String,String,String,Augmentations)

    /** End document. */
    public void endDocument(Augmentations augs) throws XNIException {

    	// </body> and </html> have been buffered to consider outside content
    	fIgnoreOutsideContent = true; // endElement should not ignore the elements passed from buffer
    	consumeBufferedEndElements();
    	
        // handle empty document
        if (!fSeenRootElement && !fDocumentFragment) {
            if (fReportErrors) {
                fErrorReporter.reportError("HTML2000", null);
            }
            if (fDocumentHandler != null) {
            	fSeenRootElementEnd = false;
                forceStartBody(); // will force <html> and <head></head>
                final String body = modifyName("body", fNamesElems);
                fQName.setValues(null, body, body, null);
                callEndElement(fQName, synthesizedAugs());

                final String ename = modifyName("html", fNamesElems);
                fQName.setValues(null, ename, ename, null);
                callEndElement(fQName, synthesizedAugs());
            }
        }

        // pop all remaining elements
        else {
            int length = fElementStack.top - fragmentContextStackSize_;
            for (int i = 0; i < length; i++) {
                Info info = fElementStack.pop();
                if (fReportErrors) {
                    String ename = info.qname.rawname;
                    fErrorReporter.reportWarning("HTML2001", new Object[]{ename});
                }
                if (fDocumentHandler != null) {
                    callEndElement(info.qname, synthesizedAugs());
                }
            }
        }

        // call handler
        if (fDocumentHandler != null) {
            fDocumentHandler.endDocument(augs);
        }

    } // endDocument(Augmentations)

    /**
     * Consume elements that have been buffered, like </body></html> that are first consumed
     * at the end of document
     */
	private void consumeBufferedEndElements() {
		final List toConsume = new ArrayList(endElementsBuffer_);
		endElementsBuffer_.clear();
		for (int i=0; i<toConsume.size(); ++i) {
    		final ElementEntry entry = (ElementEntry) toConsume.get(i);
    		forcedEndElement_ = true;
        	endElement(entry.name_, entry.augs_);
    	}
		endElementsBuffer_.clear();
	}

    /** Comment. */
    public void comment(XMLString text, Augmentations augs) throws XNIException {
        fSeenAnything = true;
        consumeEarlyTextIfNeeded();
        if (fDocumentHandler != null) {
            fDocumentHandler.comment(text, augs);
        }
    } // comment(XMLString,Augmentations)

	private void consumeEarlyTextIfNeeded() {
		if (!lostText_.isEmpty()) {
        	if (!fSeenBodyElement) {
        		forceStartBody();
        	}
            lostText_.refeed(this);
        }
	}

    /** Processing instruction. */
    public void processingInstruction(String target, XMLString data,
                                      Augmentations augs) throws XNIException {
        fSeenAnything = true;
        consumeEarlyTextIfNeeded();
        if (fDocumentHandler != null) {
            fDocumentHandler.processingInstruction(target, data, augs);
        }
    } // processingInstruction(String,XMLString,Augmentations)

    /** Start element. */
    public void startElement(final QName elem, XMLAttributes attrs, final Augmentations augs)
        throws XNIException {
        fSeenAnything = true;
        
        final boolean isForcedCreation = forcedStartElement_;
        forcedStartElement_ = false;

        // check for end of document
        if (fSeenRootElementEnd) {
        	notifyDiscardedStartElement(elem, attrs, augs);
            return;
        }

        // get element information
        final HTMLElements.Element element = getElement(elem);
        final short elementCode = element.code;

        // the creation of some elements like TABLE or SELECT can't be forced. Any others? 
        if (isForcedCreation && (elementCode == HTMLElements.TABLE || elementCode == HTMLElements.SELECT)) {
        	return; // don't accept creation
        }

        // ignore multiple html, head, body elements
		if (fSeenRootElement && elementCode == HTMLElements.HTML) {
        	notifyDiscardedStartElement(elem, attrs, augs);
            return;
        }
        if (elementCode == HTMLElements.HEAD) {
            if (fSeenHeadElement) {
            	notifyDiscardedStartElement(elem, attrs, augs);
                return;
            }
            fSeenHeadElement = true;
        }
        else if (elementCode == HTMLElements.FRAMESET) {
        	consumeBufferedEndElements(); // </head> (if any) has been buffered
        }
        else if (elementCode == HTMLElements.BODY) {
    		// create <head></head> if none was present
    		if (!fSeenHeadElement) {
    			final QName head = createQName("head");
    			forceStartElement(head, null, synthesizedAugs());
    			endElement(head, synthesizedAugs());
    		}
        	consumeBufferedEndElements(); // </head> (if any) has been buffered
    		
            if (fSeenBodyElement) {
            	notifyDiscardedStartElement(elem, attrs, augs);
                return;
            }
            fSeenBodyElement = true;
        }
        else if (elementCode == HTMLElements.FORM) {
        	if (fOpenedForm) {
            	notifyDiscardedStartElement(elem, attrs, augs);
        		return;
        	}
        	fOpenedForm = true;
        }
        else if (elementCode == HTMLElements.UNKNOWN) {
        	consumeBufferedEndElements();
        }

        // check proper parent
        if (element.parent != null) {
            final HTMLElements.Element preferedParent = element.parent[0];
        	if (fDocumentFragment && (preferedParent.code == HTMLElements.HEAD || preferedParent.code == HTMLElements.BODY)) {
        		// nothing, don't force HEAD or BODY creation for a document fragment
        	}
        	else if (!fSeenRootElement && !fDocumentFragment) {
                String pname = preferedParent.name;
                pname = modifyName(pname, fNamesElems);
                if (fReportErrors) {
                    String ename = elem.rawname;
                    fErrorReporter.reportWarning("HTML2002", new Object[]{ename,pname});
                }
                final QName qname = new QName(null, pname, pname, null);
                final boolean parentCreated = forceStartElement(qname, null, synthesizedAugs());
                if (!parentCreated) {
                	if (!isForcedCreation) {
                		notifyDiscardedStartElement(elem, attrs, augs);
                	}
            		return;
                }
            }
        	else {
                if (preferedParent.code != HTMLElements.HEAD || (!fSeenBodyElement && !fDocumentFragment)) {
                    int depth = getParentDepth(element.parent, element.bounds);
                    if (depth == -1) { // no parent found
                        final String pname = modifyName(preferedParent.name, fNamesElems);
                        final QName qname = new QName(null, pname, pname, null);
                        if (fReportErrors) {
                            String ename = elem.rawname;
                            fErrorReporter.reportWarning("HTML2004", new Object[]{ename,pname});
                        }
                        final boolean parentCreated = forceStartElement(qname, null, synthesizedAugs());
                        if (!parentCreated) {
                        	if (!isForcedCreation) {
                        		notifyDiscardedStartElement(elem, attrs, augs);
                        	}
                    		return;
                        }
                    }
                }
            }
        }

        // if block element, save immediate parent inline elements
        int depth = 0;
        if (element.flags == 0) {
            int length = fElementStack.top;
            fInlineStack.top = 0;
            for (int i = length - 1; i >= 0; i--) {
                Info info = fElementStack.data[i];
                if (!info.element.isInline()) {
                    break;
                }
                fInlineStack.push(info);
                endElement(info.qname, synthesizedAugs());
            }
            depth = fInlineStack.top;
        }

        // close previous elements
        // all elements close a <script>
        // in head, no element has children
        if ((fElementStack.top > 1 
        		&& (fElementStack.peek().element.code == HTMLElements.SCRIPT))
        		|| fElementStack.top > 2 && fElementStack.data[fElementStack.top-2].element.code == HTMLElements.HEAD) {
            final Info info = fElementStack.pop();
            if (fDocumentHandler != null) {
                callEndElement(info.qname, synthesizedAugs());
            }
        }
        if (element.closes != null) {
            int length = fElementStack.top;
            for (int i = length - 1; i >= 0; i--) {
                Info info = fElementStack.data[i];

                // does it close the element we're looking at?
                if (element.closes(info.element.code)) {
                    if (fReportErrors) {
                        String ename = elem.rawname;
                        String iname = info.qname.rawname;
                        fErrorReporter.reportWarning("HTML2005", new Object[]{ename,iname});
                    }
                    for (int j = length - 1; j >= i; j--) {
                        info = fElementStack.pop();
                        if (fDocumentHandler != null) {
                            // PATCH: Marc-Andr� Morissette
                            callEndElement(info.qname, synthesizedAugs());
                        }
                    }
                    length = i;
                    continue;
                }
                
                // should we stop searching?
                if (info.element.isBlock() || element.isParent(info.element)) {
                	break;
                }
            }
        }
        // TODO: investigate if only table is special here
        // table closes all opened inline elements
        else if (elementCode == HTMLElements.TABLE) {
            for (int i=fElementStack.top-1; i >= 0; i--) {
                final Info info = fElementStack.data[i];
                if (!info.element.isInline()) {
                    break;
                }
                endElement(info.qname, synthesizedAugs());
            }
        }

        // call handler
        fSeenRootElement = true;
        if (element != null && element.isEmpty()) {
            if (attrs == null) {
                attrs = emptyAttributes();
            }
            if (fDocumentHandler != null) {
                fDocumentHandler.emptyElement(elem, attrs, augs);
            }
        }
        else {
            boolean inline = element != null && element.isInline();
            fElementStack.push(new Info(element, elem, inline ? attrs : null));
            if (attrs == null) {
                attrs = emptyAttributes();
            }
            if (fDocumentHandler != null) {
                callStartElement(elem, attrs, augs);
            }
        }

        // re-open inline elements
        for (int i = 0; i < depth; i++) {
            Info info = fInlineStack.pop();
            forceStartElement(info.qname, info.attributes, synthesizedAugs());
        }

        if (elementCode == HTMLElements.BODY) {
        	lostText_.refeed(this);
        }
    } // startElement(QName,XMLAttributes,Augmentations)

    /**
     * Forces an element start, taking care to set the information to allow startElement to "see" that's
     * the element has been forced.
     * @return <code>true</code> if creation could be done (TABLE's creation for instance can't be forced)
     */
    private boolean forceStartElement(final QName elem, XMLAttributes attrs, final Augmentations augs)
    throws XNIException {
    	
    	forcedStartElement_ = true;
    	startElement(elem, attrs, augs);
    	
    	return fElementStack.top > 0 && elem.equals(fElementStack.peek().qname);
    }

    private QName createQName(String tagName) {
		tagName = modifyName(tagName, fNamesElems);
		return new QName(null, tagName, tagName, NamespaceBinder.XHTML_1_0_URI);
	}

	/** Empty element. */
    public void emptyElement(final QName element, XMLAttributes attrs, Augmentations augs)
        throws XNIException {
    	startElement(element, attrs, augs);
        // browser ignore the closing indication for non empty tags like <form .../> but not for unknown element
        final HTMLElements.Element elem = getElement(element);
        if (elem.isEmpty() || elem.code == HTMLElements.UNKNOWN) {
        	endElement(element, augs);
        }
    } // emptyElement(QName,XMLAttributes,Augmentations)

	/** Start entity. */
    public void startGeneralEntity(String name, 
                                   XMLResourceIdentifier id,
                                   String encoding,
                                   Augmentations augs) throws XNIException {
        fSeenAnything = true;

        // check for end of document
        if (fSeenRootElementEnd) {
            return;
        }

        // insert body, if needed
        if (!fDocumentFragment) {
            boolean insertBody = !fSeenRootElement;
            if (!insertBody) {
                Info info = fElementStack.peek();
                if (info.element.code == HTMLElements.HEAD ||
                    info.element.code == HTMLElements.HTML) {
                    String hname = modifyName("head", fNamesElems);
                    String bname = modifyName("body", fNamesElems);
                    if (fReportErrors) {
                        fErrorReporter.reportWarning("HTML2009", new Object[]{hname,bname});
                    }
                    fQName.setValues(null, hname, hname, null);
                    endElement(fQName, synthesizedAugs());
                    insertBody = true;
                }
            }
            if (insertBody) {
                forceStartBody();
            }
        }
        
        // call handler
        if (fDocumentHandler != null) {
            fDocumentHandler.startGeneralEntity(name, id, encoding, augs);
        }

    } // startGeneralEntity(String,XMLResourceIdentifier,String,Augmentations)

    /**
     * Generates a missing <body> (which creates missing <head> when needed)
     */
	private void forceStartBody() {
		final QName body = createQName("body");
		if (fReportErrors) {
		    fErrorReporter.reportWarning("HTML2006", new Object[]{body.localpart});
		}
		forceStartElement(body, null, synthesizedAugs());
	}

    /** Text declaration. */
    public void textDecl(String version, String encoding, Augmentations augs)
        throws XNIException {
        fSeenAnything = true;
        
        // check for end of document
        if (fSeenRootElementEnd) {
            return;
        }

        // call handler
        if (fDocumentHandler != null) {
            fDocumentHandler.textDecl(version, encoding, augs);
        }

    } // textDecl(String,String,Augmentations)

    /** End entity. */
    public void endGeneralEntity(String name, Augmentations augs) throws XNIException {
        
        // check for end of document
        if (fSeenRootElementEnd) {
            return;
        }

        // call handler
        if (fDocumentHandler != null) {
            fDocumentHandler.endGeneralEntity(name, augs);
        }

    } // endGeneralEntity(String,Augmentations)

    /** Start CDATA section. */
    public void startCDATA(Augmentations augs) throws XNIException {
        fSeenAnything = true;
        
        consumeEarlyTextIfNeeded();

        // check for end of document
        if (fSeenRootElementEnd) {
            return;
        }

        // call handler
        if (fDocumentHandler != null) {
            fDocumentHandler.startCDATA(augs);
        }

    } // startCDATA(Augmentations)

    /** End CDATA section. */
    public void endCDATA(Augmentations augs) throws XNIException {

        // check for end of document
        if (fSeenRootElementEnd) {
            return;
        }

        // call handler
        if (fDocumentHandler != null) {
            fDocumentHandler.endCDATA(augs);
        }

    } // endCDATA(Augmentations)

    /** Characters. */
    public void characters(final XMLString text, final Augmentations augs) throws XNIException {
        // check for end of document
        if (fSeenRootElementEnd) {
            return;
        }

    	if (fElementStack.top == 0 && !fDocumentFragment) {
    		// character before first opening tag
    		lostText_.add(text, augs);
    		return;
    	}

        // is this text whitespace?
        boolean whitespace = true;
        for (int i = 0; i < text.length; i++) {
            if (!Character.isWhitespace(text.ch[text.offset + i])) {
                whitespace = false;
                break;
            }
        }

        if (!fDocumentFragment) {
            // handle bare characters
            if (!fSeenRootElement) {
                if (whitespace) {
                    return;
                }
                forceStartBody();
            }
            
            if (whitespace && (fElementStack.top < 2 || endElementsBuffer_.size() == 1)) {
            	// ignore spaces directly within <html>
            	return;
            }

            // handle character content in head
            // NOTE: This frequently happens when the document looks like:
            //       <title>Title</title>
            //       And here's some text.
            else if (!whitespace) {
                Info info = fElementStack.peek();
                if (info.element.code == HTMLElements.HEAD ||
                    info.element.code == HTMLElements.HTML) {
                    String hname = modifyName("head", fNamesElems);
                    String bname = modifyName("body", fNamesElems);
                    if (fReportErrors) {
                        fErrorReporter.reportWarning("HTML2009", new Object[]{hname,bname});
                    }
                    forceStartBody();
                }
            }
        }

        // call handler
        if (fDocumentHandler != null) {
            fDocumentHandler.characters(text, augs);
        }

    } // characters(XMLString,Augmentations)

    /** Ignorable whitespace. */
    public void ignorableWhitespace(XMLString text, Augmentations augs)
        throws XNIException {
        characters(text, augs);
    } // ignorableWhitespace(XMLString,Augmentations)
    
    /** End element. */
    public void endElement(final QName element, final Augmentations augs) throws XNIException {
    	final boolean forcedEndElement = forcedEndElement_;
        // is there anything to do?
        if (fSeenRootElementEnd) {
        	notifyDiscardedEndElement(element, augs);
            return;
        }
        
        // get element information
        HTMLElements.Element elem = getElement(element);

        // if we consider outside content, just buffer </body> and </html> to consider them at the very end
        if (!fIgnoreOutsideContent &&
            (elem.code == HTMLElements.BODY || elem.code == HTMLElements.HTML)) {
        	endElementsBuffer_.add(new ElementEntry(element, augs));
            return;
        }

        // check for end of document
        if (elem.code == HTMLElements.HTML) {
            fSeenRootElementEnd = true;
        }
        else if (elem.code == HTMLElements.FORM) {
        	fOpenedForm = false;
        }
        else if (elem.code == HTMLElements.HEAD && !forcedEndElement) {
        	// consume </head> first when <body> is reached to retrieve content lost between </head> and <body>
        	endElementsBuffer_.add(new ElementEntry(element, augs));
        	return;
        }

        // empty element
        int depth = getElementDepth(elem);
        if (depth == -1) {
        	if (elem.code == HTMLElements.P) {
        		forceStartElement(element, emptyAttributes(), synthesizedAugs());
	            endElement(element, augs);
        	}
        	else if (!elem.isEmpty()) {
            	notifyDiscardedEndElement(element, augs);
        	}
            return;
        }

        // find unbalanced inline elements
        if (depth > 1 && elem.isInline()) {
            final int size = fElementStack.top;
            fInlineStack.top = 0;
            for (int i = 0; i < depth - 1; i++) {
                final Info info = fElementStack.data[size - i - 1];
                final HTMLElements.Element pelem = info.element;
                if (pelem.isInline() || pelem.code == HTMLElements.FONT) { // TODO: investigate if only FONT
                    // NOTE: I don't have to make a copy of the info because
                    //       it will just be popped off of the element stack
                    //       as soon as we close it, anyway.
                    fInlineStack.push(info);
                }
            }
        }

        // close children up to appropriate element
        for (int i = 0; i < depth; i++) {
            Info info = fElementStack.pop();
            if (fReportErrors && i < depth - 1) {
                String ename = modifyName(element.rawname, fNamesElems);
                String iname = info.qname.rawname;
                fErrorReporter.reportWarning("HTML2007", new Object[]{ename,iname});
            }
            if (fDocumentHandler != null) {
                // PATCH: Marc-AndrMorissette
                callEndElement(info.qname, i < depth - 1 ? synthesizedAugs() : augs);
            }
        }

        // re-open inline elements
        if (depth > 1) {
            int size = fInlineStack.top;
            for (int i = 0; i < size; i++) {
                Info info = (Info)fInlineStack.pop();
                XMLAttributes attributes = info.attributes;
                if (fReportErrors) {
                    String iname = info.qname.rawname;
                    fErrorReporter.reportWarning("HTML2008", new Object[]{iname});
                }
                forceStartElement(info.qname, attributes, synthesizedAugs());
            }
        }

    } // endElement(QName,Augmentations)

    // @since Xerces 2.1.0

	/** Sets the document source. */
    public void setDocumentSource(XMLDocumentSource source) {
        fDocumentSource = source;
    } // setDocumentSource(XMLDocumentSource)

    /** Returns the document source. */
    public XMLDocumentSource getDocumentSource() {
        return fDocumentSource;
    } // getDocumentSource():XMLDocumentSource

    // removed since Xerces-J 2.3.0

    /** Start document. */
    public void startDocument(XMLLocator locator, String encoding, Augmentations augs) 
        throws XNIException {
        startDocument(locator, encoding, null, augs);
    } // startDocument(XMLLocator,String,Augmentations)

    /** Start prefix mapping. */
    public void startPrefixMapping(String prefix, String uri, Augmentations augs)
        throws XNIException {
        
        // check for end of document
        if (fSeenRootElementEnd) {
            return;
        }

        // call handler
        if (fDocumentHandler != null) {
        	XercesBridge.getInstance().XMLDocumentHandler_startPrefixMapping(fDocumentHandler, prefix, uri, augs);
        }
    
    } // startPrefixMapping(String,String,Augmentations)

    /** End prefix mapping. */
    public void endPrefixMapping(String prefix, Augmentations augs)
        throws XNIException {
        
        // check for end of document
        if (fSeenRootElementEnd) {
            return;
        }

        // call handler
        if (fDocumentHandler != null) {
        	XercesBridge.getInstance().XMLDocumentHandler_endPrefixMapping(fDocumentHandler, prefix, augs);
        }
    
    } // endPrefixMapping(String,Augmentations)

    //
    // Protected methods
    //

    /** Returns an HTML element. */
    protected HTMLElements.Element getElement(final QName elementName) {
    	String name = elementName.rawname;
        if (fNamespaces && NamespaceBinder.XHTML_1_0_URI.equals(elementName.uri)) {
            int index = name.indexOf(':');
            if (index != -1) {
                name = name.substring(index+1);
            }
        }
        return HTMLElements.getElement(name);
    } // getElement(String):HTMLElements.Element

    /** Call document handler start element. */
    protected final void callStartElement(QName element, XMLAttributes attrs,
                                          Augmentations augs) 
        throws XNIException {
        fDocumentHandler.startElement(element, attrs, augs);
    } // callStartElement(QName,XMLAttributes,Augmentations)

    /** Call document handler end element. */
    protected final void callEndElement(QName element, Augmentations augs) 
        throws XNIException {
        fDocumentHandler.endElement(element, augs);
    } // callEndElement(QName,Augmentations)

    /**
     * Returns the depth of the open tag associated with the specified
     * element name or -1 if no matching element is found.
     *
     * @param element The element.
     */
    protected final int getElementDepth(HTMLElements.Element element) {
        final boolean container = element.isContainer();
        final short elementCode = element.code;
        final boolean tableBodyOrHtml = (elementCode == HTMLElements.TABLE)
			|| (elementCode == HTMLElements.BODY) || (elementCode == HTMLElements.HTML);
        int depth = -1;
        for (int i = fElementStack.top - 1; i >=fragmentContextStackSize_; i--) {
            Info info = fElementStack.data[i];
            if (info.element.code == element.code) {
                depth = fElementStack.top - i;
                break;
            }
            if (!container && info.element.isBlock()) {
                break;
            }
            if (info.element.code == HTMLElements.TABLE && !tableBodyOrHtml) {
            	return -1; // current element not allowed to close a table
            }
        }
        return depth;
    } // getElementDepth(HTMLElements.Element)

    /**
     * Returns the depth of the open tag associated with the specified
     * element parent names or -1 if no matching element is found.
     *
     * @param parents The parent elements.
     */
    protected int getParentDepth(HTMLElements.Element[] parents, short bounds) {
        if (parents != null) {
            for (int i = fElementStack.top - 1; i >= 0; i--) {
                Info info = fElementStack.data[i];
                if (info.element.code == bounds) {
                    break;
                }
                for (int j = 0; j < parents.length; j++) {
                    if (info.element.code == parents[j].code) {
                        return fElementStack.top - i;
                    }
                }
            }
        }
        return -1;
    } // getParentDepth(HTMLElements.Element[],short):int

    /** Returns a set of empty attributes. */
    protected final XMLAttributes emptyAttributes() {
        fEmptyAttrs.removeAllAttributes();
        return fEmptyAttrs;
    } // emptyAttributes():XMLAttributes

    /** Returns an augmentations object with a synthesized item added. */
    protected final Augmentations synthesizedAugs() {
        HTMLAugmentations augs = null;
        if (fAugmentations) {
            augs = fInfosetAugs;
            augs.removeAllItems();
            augs.putItem(AUGMENTATIONS, SYNTHESIZED_ITEM);
        }
        return augs;
    } // synthesizedAugs():Augmentations

    //
    // Protected static methods
    //

    /** Modifies the given name based on the specified mode. */
    protected static final String modifyName(String name, short mode) {
        switch (mode) {
            case NAMES_UPPERCASE: return name.toUpperCase();
            case NAMES_LOWERCASE: return name.toLowerCase();
        }
        return name;
    } // modifyName(String,short):String

    /**
     * Converts HTML names string value to constant value. 
     *
     * @see #NAMES_NO_CHANGE
     * @see #NAMES_LOWERCASE
     * @see #NAMES_UPPERCASE
     */
    protected static final short getNamesValue(String value) {
        if (value.equals("lower")) {
            return NAMES_LOWERCASE;
        }
        if (value.equals("upper")) {
            return NAMES_UPPERCASE;
        }
        return NAMES_NO_CHANGE;
    } // getNamesValue(String):short

    //
    // Classes
    //

    /**
     * Element info for each start element. This information is used when
     * closing unbalanced inline elements. For example:
     * <pre>
     * &lt;i>unbalanced &lt;b>HTML&lt;/i> content&lt;/b>
     * </pre>
     * <p>
     * It seems that it is a waste of processing and memory to copy the 
     * attributes for every start element even if there are no unbalanced 
     * inline elements in the document. However, if the attributes are
     * <em>not</em> saved, then important attributes such as style
     * information would be lost.
     *
     * @author Andy Clark
     */
    public static class Info {

        //
        // Data
        //

        /** The element. */
        public HTMLElements.Element element;

        /** The element qualified name. */
        public QName qname;

        /** The element attributes. */
        public XMLAttributes attributes;

        //
        // Constructors
        //

        /**
         * Creates an element information object.
         * <p>
         * <strong>Note:</strong>
         * This constructor makes a copy of the element information.
         *
         * @param element The element qualified name.
         */
        public Info(HTMLElements.Element element, QName qname) {
            this(element, qname, null);
        } // <init>(HTMLElements.Element,QName)

        /**
         * Creates an element information object.
         * <p>
         * <strong>Note:</strong>
         * This constructor makes a copy of the element information.
         *
         * @param element The element qualified name.
         * @param attributes The element attributes.
         */
        public Info(HTMLElements.Element element,
                    QName qname, XMLAttributes attributes) {
            this.element = element;
            this.qname = new QName(qname);
            if (attributes != null) {
                int length = attributes.getLength();
                if (length > 0) {
                    QName aqname = new QName();
                    XMLAttributes newattrs = new XMLAttributesImpl();
                    for (int i = 0; i < length; i++) {
                        attributes.getName(i, aqname);
                        String type = attributes.getType(i);
                        String value = attributes.getValue(i);
                        String nonNormalizedValue = attributes.getNonNormalizedValue(i);
                        boolean specified = attributes.isSpecified(i);
                        newattrs.addAttribute(aqname, type, value);
                        newattrs.setNonNormalizedValue(i, nonNormalizedValue);
                        newattrs.setSpecified(i, specified);
                    }
                    this.attributes = newattrs;
                }
            }
        } // <init>(HTMLElements.Element,QName,XMLAttributes)

        /**
         * Simple representation to make debugging easier
         */
        public String toString() {
        	return super.toString() + qname;
        }
    } // class Info

    /** Unsynchronized stack of element information. */
    public static class InfoStack {

        //
        // Data
        //

        /** The top of the stack. */
        public int top;

        /** The stack data. */
        public Info[] data = new Info[10];

        //
        // Public methods
        //

        /** Pushes element information onto the stack. */
        public void push(Info info) {
            if (top == data.length) {
                Info[] newarray = new Info[top + 10];
                System.arraycopy(data, 0, newarray, 0, top);
                data = newarray;
            }
            data[top++] = info;
        } // push(Info)

        /** Peeks at the top of the stack. */
        public Info peek() {
            return data[top-1];
        } // peek():Info

        /** Pops the top item off of the stack. */
        public Info pop() {
            return data[--top];
        } // pop():Info
        
        /**
         * Simple representation to make debugging easier
         */
        public String toString() {
        	final StringBuffer sb = new StringBuffer("InfoStack(");
        	for (int i=top-1; i>=0; --i) {
        		sb.append(data[i]);
        		if (i != 0)
        			sb.append(", ");
        	}
        	sb.append(")");
        	return sb.toString();
        }


    } // class InfoStack

	void setTagBalancingListener(final HTMLTagBalancingListener tagBalancingListener) {
		this.tagBalancingListener = tagBalancingListener;
	}

	/**
	 * Notifies the tagBalancingListener (if any) of an ignored start element
	 */
    private void notifyDiscardedStartElement(final QName elem, final XMLAttributes attrs,
    		final Augmentations augs) {
    	if (tagBalancingListener != null)
    		tagBalancingListener.ignoredStartElement(elem, attrs, augs);
	}

	/**
	 * Notifies the tagBalancingListener (if any) of an ignored end element
	 */
    private void notifyDiscardedEndElement(final QName element, final Augmentations augs) {
    	if (tagBalancingListener != null)
    		tagBalancingListener.ignoredEndElement(element, augs);
	}

    /**
     * Structure to hold information about an element placed in buffer to be comsumed later
     */
    static class ElementEntry {
    	private final QName name_;
    	private final Augmentations augs_;
    	ElementEntry(final QName element, final Augmentations augs) {
    		name_ = new QName(element);
    		augs_ = (augs == null) ? null : new HTMLAugmentations(augs);
    	}
    }
} // class HTMLTagBalancer
