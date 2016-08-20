/* 
 * Copyright 2004-2008 Andy Clark
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

import java.util.Enumeration;
import java.util.Vector;

import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.NamespaceContext;
import org.apache.xerces.xni.QName;
import org.apache.xerces.xni.XMLAttributes;
import org.apache.xerces.xni.XMLLocator;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLComponentManager;
import org.apache.xerces.xni.parser.XMLConfigurationException;
import org.cyberneko.html.HTMLElements;
import org.cyberneko.html.xercesbridge.XercesBridge;

/**
 * This filter binds namespaces if namespace processing is turned on
 * by setting the feature "http://xml.org/sax/features/namespaces" is
 * set to <code>true</code>.
 * <p>
 * This configuration recognizes the following features:
 * <ul>
 * <li>http://xml.org/sax/features/namespaces
 * </ul>
 * 
 * @author Andy Clark
 * 
 * @version $Id: NamespaceBinder.java,v 1.8 2005/05/30 00:19:28 andyc Exp $
 */
public class NamespaceBinder
    extends DefaultFilter {

    //
    // Constants
    //

    // namespace uris

    /** XHTML 1.0 namespace URI (http://www.w3.org/1999/xhtml). */
    public static final String XHTML_1_0_URI = "http://www.w3.org/1999/xhtml";

    /** XML namespace URI (http://www.w3.org/XML/1998/namespace). */
    public static final String XML_URI = "http://www.w3.org/XML/1998/namespace";

    /** XMLNS namespace URI (http://www.w3.org/2000/xmlns/). */
    public static final String XMLNS_URI = "http://www.w3.org/2000/xmlns/";

    // features

    /** Namespaces. */
    protected static final String NAMESPACES = "http://xml.org/sax/features/namespaces";

    /** Override namespace binding URI. */
    protected static final String OVERRIDE_NAMESPACES = "http://cyberneko.org/html/features/override-namespaces";

    /** Insert namespace binding URIs. */
    protected static final String INSERT_NAMESPACES = "http://cyberneko.org/html/features/insert-namespaces";

    /** Recognized features. */
    private static final String[] RECOGNIZED_FEATURES = { 
        NAMESPACES, 
        OVERRIDE_NAMESPACES,
        INSERT_NAMESPACES,
    };

    /** Feature defaults. */
    private static final Boolean[] FEATURE_DEFAULTS = {
        null,
        Boolean.FALSE,
        Boolean.FALSE,
    };

    // properties

    /** Modify HTML element names: { "upper", "lower", "default" }. */
    protected static final String NAMES_ELEMS = "http://cyberneko.org/html/properties/names/elems";

    /** Modify HTML attribute names: { "upper", "lower", "default" }. */
    protected static final String NAMES_ATTRS = "http://cyberneko.org/html/properties/names/attrs";

    /** Namespaces URI. */
    protected static final String NAMESPACES_URI = "http://cyberneko.org/html/properties/namespaces-uri";

    /** Recognized properties. */
    private static final String[] RECOGNIZED_PROPERTIES = new String[] {
        NAMES_ELEMS,
        NAMES_ATTRS,
        NAMESPACES_URI,
    };

    /** Property defaults. */
    private static final Object[] PROPERTY_DEFAULTS = {
        null,
        null,
        XHTML_1_0_URI,
    };

    // modify HTML names

    /** Don't modify HTML names. */
    protected static final short NAMES_NO_CHANGE = 0;

    /** Uppercase HTML names. */
    protected static final short NAMES_UPPERCASE = 1;

    /** Lowercase HTML names. */
    protected static final short NAMES_LOWERCASE = 2;

    //
    // Data
    //

    // features

    /** Namespaces. */
    protected boolean fNamespaces;

    /** Namespace prefixes. */
    protected boolean fNamespacePrefixes;

    /** Override namespaces. */
    protected boolean fOverrideNamespaces;

    /** Insert namespaces. */
    protected boolean fInsertNamespaces;

    // properties

    /** Modify HTML element names. */
    protected short fNamesElems;

    /** Modify HTML attribute names. */
    protected short fNamesAttrs;

    /** Namespaces URI. */
    protected String fNamespacesURI;

    // state

    /** Namespace context. */
    protected final NamespaceSupport fNamespaceContext = new NamespaceSupport();

    // temp vars

    /** QName. */
    private final QName fQName = new QName();

    //
    // HTMLComponent methods
    //

    /**
     * Returns a list of feature identifiers that are recognized by
     * this component. This method may return null if no features
     * are recognized by this component.
     */
    public String[] getRecognizedFeatures() {
        return merge(super.getRecognizedFeatures(), RECOGNIZED_FEATURES);
    } // getRecognizedFeatures():String[]

    /**
     * Returns the default state for a feature, or null if this
     * component does not want to report a default value for this
     * feature.
     */
    public Boolean getFeatureDefault(String featureId) {
        for (int i = 0; i < RECOGNIZED_FEATURES.length; i++) {
            if (RECOGNIZED_FEATURES[i].equals(featureId)) {
                return FEATURE_DEFAULTS[i];
            }
        }
        return super.getFeatureDefault(featureId);
    } // getFeatureDefault(String):Boolean

    /**
     * Returns a list of property identifiers that are recognized by
     * this component. This method may return null if no properties
     * are recognized by this component.
     */
    public String[] getRecognizedProperties() {
        return merge(super.getRecognizedProperties(), RECOGNIZED_PROPERTIES);
    } // getRecognizedProperties():String[]

    /**
     * Returns the default value for a property, or null if this
     * component does not want to report a default value for this
     * property.
     */
    public Object getPropertyDefault(String propertyId) {
        for (int i = 0; i < RECOGNIZED_PROPERTIES.length; i++) {
            if (RECOGNIZED_PROPERTIES[i].equals(propertyId)) {
                return PROPERTY_DEFAULTS[i];
            }
        }
        return super.getPropertyDefault(propertyId);
    } // getPropertyDefault(String):Object

    /**
     * Resets the component. The component can query the component manager
     * about any features and properties that affect the operation of the
     * component.
     *
     * @param manager The component manager.
     *
     * @throws XNIException Thrown by component on initialization error.
     */
    public void reset(XMLComponentManager manager) 
        throws XMLConfigurationException {
        super.reset(manager);

        // features
        fNamespaces = manager.getFeature(NAMESPACES);
        fOverrideNamespaces = manager.getFeature(OVERRIDE_NAMESPACES);
        fInsertNamespaces = manager.getFeature(INSERT_NAMESPACES);

        // get properties
        fNamesElems = getNamesValue(String.valueOf(manager.getProperty(NAMES_ELEMS)));
        fNamesAttrs = getNamesValue(String.valueOf(manager.getProperty(NAMES_ATTRS)));
        fNamespacesURI = String.valueOf(manager.getProperty(NAMESPACES_URI));
    
        // initialize state
        fNamespaceContext.reset();

    } // reset(XMLComponentManager)

    //
    // XMLDocumentHandler methods
    //

    /** Start document. */
    public void startDocument(XMLLocator locator, String encoding,
                              NamespaceContext nscontext, Augmentations augs)
        throws XNIException {
        
        // perform default handling
        // NOTE: using own namespace context
        super.startDocument(locator,encoding,fNamespaceContext,augs);

    } // startDocument(XMLLocator,String,NamespaceContext,Augmentations)

    /** Start element. */
    public void startElement(QName element, XMLAttributes attrs,
                             Augmentations augs) throws XNIException {
        
        // bind namespaces, if needed
        if (fNamespaces) {
            fNamespaceContext.pushContext();
            bindNamespaces(element, attrs);

            int dcount = fNamespaceContext.getDeclaredPrefixCount();
            if (fDocumentHandler != null && dcount > 0) {
                for (int i = 0; i < dcount; i++) {
                    String prefix = fNamespaceContext.getDeclaredPrefixAt(i);
                    String uri = fNamespaceContext.getURI(prefix);
                    XercesBridge.getInstance().XMLDocumentHandler_startPrefixMapping(fDocumentHandler, prefix, uri, augs);
                }
            }
        }

        // perform default handling
        super.startElement(element, attrs, augs);

    } // startElement(QName,XMLAttributes,Augmentations)

    /** Empty element. */
    public void emptyElement(QName element, XMLAttributes attrs,
                             Augmentations augs) throws XNIException {
        
        // bind namespaces, if needed
        if (fNamespaces) {
            fNamespaceContext.pushContext();
            bindNamespaces(element, attrs);

            int dcount = fNamespaceContext.getDeclaredPrefixCount();
            if (fDocumentHandler != null && dcount > 0) {
                for (int i = 0; i < dcount; i++) {
                    String prefix = fNamespaceContext.getDeclaredPrefixAt(i);
                    String uri = fNamespaceContext.getURI(prefix);
                    XercesBridge.getInstance().XMLDocumentHandler_startPrefixMapping(fDocumentHandler, prefix, uri, augs);
                }
            }
        }

        // perform default handling
        super.emptyElement(element, attrs, augs);

        // pop context
        if (fNamespaces) {
            int dcount = fNamespaceContext.getDeclaredPrefixCount();
            if (fDocumentHandler != null && dcount > 0) {
                for (int i = dcount-1; i >= 0; i--) {
                    String prefix = fNamespaceContext.getDeclaredPrefixAt(i);
                    XercesBridge.getInstance().XMLDocumentHandler_endPrefixMapping(fDocumentHandler, prefix, augs);
                }
            }
            
            fNamespaceContext.popContext();
        }

    } // startElement(QName,XMLAttributes,Augmentations)

    /** End element. */
    public void endElement(QName element, Augmentations augs)
        throws XNIException {
        
        // bind namespaces, if needed
        if (fNamespaces) {
            bindNamespaces(element, null);
        }

        // perform default handling
        super.endElement(element, augs);

        // pop context
        if (fNamespaces) {
            int dcount = fNamespaceContext.getDeclaredPrefixCount();
            if (fDocumentHandler != null && dcount > 0) {
                for (int i = dcount-1; i >= 0; i--) {
                    String prefix = fNamespaceContext.getDeclaredPrefixAt(i);
                    XercesBridge.getInstance().XMLDocumentHandler_endPrefixMapping(fDocumentHandler, prefix, augs);
                }
            }
            
            fNamespaceContext.popContext();
        }

    } // endElement(QName,Augmentations)

    //
    // Protected static methods
    //

    /** Splits a qualified name. */
    protected static void splitQName(QName qname) {
        int index = qname.rawname.indexOf(':');
        if (index != -1) {
            qname.prefix = qname.rawname.substring(0,index);
            qname.localpart  = qname.rawname.substring(index+1);
        }
    } // splitQName(QName)

    /**
     * Converts HTML names string value to constant value. 
     *
     * @see #NAMES_NO_CHANGE
     * @see #NAMES_LOWERCASE
     * @see #NAMES_UPPERCASE
     */
    protected static final short getNamesValue(String value) {
        if (value.equals("lower")) { return NAMES_LOWERCASE; }
        if (value.equals("upper")) { return NAMES_UPPERCASE; }
        return NAMES_NO_CHANGE;
    } // getNamesValue(String):short

    /** Modifies the given name based on the specified mode. */
    protected static final String modifyName(String name, short mode) {
        switch (mode) {
            case NAMES_UPPERCASE: return name.toUpperCase();
            case NAMES_LOWERCASE: return name.toLowerCase();
        }
        return name;
    } // modifyName(String,short):String

    //
    // Protected methods
    //

    /** Binds namespaces. */
    protected void bindNamespaces(QName element, XMLAttributes attrs) {

        // split element qname
        splitQName(element);

        // declare namespace prefixes
        int attrCount = attrs != null ? attrs.getLength() : 0;
        for (int i = attrCount - 1; i >= 0; i--) {
            attrs.getName(i, fQName);
            String aname = fQName.rawname;
            String ANAME = aname.toUpperCase();
            if (ANAME.startsWith("XMLNS:") || ANAME.equals("XMLNS")) {
                int anamelen = aname.length();

                // get parts
                String aprefix = anamelen > 5 ? aname.substring(0,5) : null;
                String alocal = anamelen > 5 ? aname.substring(6) : aname;
                String avalue = attrs.getValue(i);
                
                // re-case parts and set them back into attributes
                if (anamelen > 5) {
                    aprefix = modifyName(aprefix, NAMES_LOWERCASE);
                    alocal = modifyName(alocal, fNamesElems);
                    aname = aprefix + ':' + alocal;
                }
                else {
                    alocal = modifyName(alocal, NAMES_LOWERCASE);
                    aname = alocal;
                }
                fQName.setValues(aprefix, alocal, aname, null);
                attrs.setName(i, fQName);

                // declare prefix
                String prefix = alocal != aname ? alocal : "";
                String uri = avalue.length() > 0 ? avalue : null;
                if (fOverrideNamespaces && 
                    prefix.equals(element.prefix) &&
                    HTMLElements.getElement(element.localpart, null) != null) {
                    uri = fNamespacesURI;
                }
                fNamespaceContext.declarePrefix(prefix, uri);
            }
        }

        // bind element
        String prefix = element.prefix != null ? element.prefix : "";
        element.uri = fNamespaceContext.getURI(prefix);
        // REVISIT: The prefix of a qualified element name that is
        //          bound to a namespace is passed (as recent as
        //          Xerces 2.4.0) as "" for start elements and null
        //          for end elements. Why? One of them is a bug,
        //          clearly. -Ac
        if (element.uri != null && element.prefix == null) {
            element.prefix = "";
        }

        // do we need to insert namespace bindings?
        if (fInsertNamespaces && attrs != null &&
            HTMLElements.getElement(element.localpart,null) != null) {
            if (element.prefix == null || 
                fNamespaceContext.getURI(element.prefix) == null) {
                String xmlns = "xmlns" + ((element.prefix != null)
                             ? ":"+element.prefix : "");
                fQName.setValues(null, xmlns, xmlns, null);
                attrs.addAttribute(fQName, "CDATA", fNamespacesURI);
                bindNamespaces(element, attrs);
                return;
            }
        }

        // bind attributes
        attrCount = attrs != null ? attrs.getLength() : 0;
        for (int i = 0; i < attrCount; i++) {
            attrs.getName(i, fQName);
            splitQName(fQName);
            prefix = !fQName.rawname.equals("xmlns")
                   ? (fQName.prefix != null ? fQName.prefix : "") : "xmlns";
            // PATCH: Joseph Walton
            if (!prefix.equals("")) {
                fQName.uri = prefix.equals("xml") ? XML_URI : fNamespaceContext.getURI(prefix);
            }
            // NOTE: You would think the xmlns namespace would be handled
            //       by NamespaceSupport but it's not. -Ac 
            if (prefix.equals("xmlns") && fQName.uri == null) {
                fQName.uri = XMLNS_URI;
            }
            attrs.setName(i, fQName);
        }

    } // bindNamespaces(QName,XMLAttributes)

    //
    // Classes
    //

    /**
     * This namespace context object implements the old and new XNI 
     * <code>NamespaceContext</code> interface methods so that it can
     * be used across all versions of Xerces2.
     */
    public static class NamespaceSupport
        implements NamespaceContext {

        //
        // Data
        //

        /** Top of the levels list. */
        protected int fTop = 0;

        /** The levels of the entries. */
        protected int[] fLevels = new int[10];

        /** The entries. */
        protected Entry[] fEntries = new Entry[10];

        //
        // Constructors
        //

        /** Default constructor. */
        public NamespaceSupport() {
            pushContext();
            declarePrefix("xml", NamespaceContext.XML_URI);
            declarePrefix("xmlns", NamespaceContext.XMLNS_URI);
        } // <init>()

        //
        // NamespaceContext methods
        //

        // since Xerces 2.0.0-beta2 (old XNI namespaces)

        /** Get URI. */
        public String getURI(String prefix) {
            for (int i = fLevels[fTop]-1; i >= 0; i--) {
                Entry entry = (Entry)fEntries[i];
                if (entry.prefix.equals(prefix)) {
                    return entry.uri;
                }
            }
            return null;
        } // getURI(String):String

        /** Get declared prefix count. */
        public int getDeclaredPrefixCount() {
            return fLevels[fTop] - fLevels[fTop-1];
        } // getDeclaredPrefixCount():int

        /** Get declared prefix at. */
        public String getDeclaredPrefixAt(int index) {
            return fEntries[fLevels[fTop-1] + index].prefix;
        } // getDeclaredPrefixAt(int):String

        /** Get parent context. */
        public NamespaceContext getParentContext() {
            return this;
        } // getParentContext():NamespaceContext

        // since Xerces #.#.# (new XNI namespaces)

        /** Reset. */
        public void reset() {
            fLevels[fTop = 1] = fLevels[fTop-1];
        } // reset()

        /** Push context. */
        public void pushContext() {
            if (++fTop == fLevels.length) {
                int[] iarray = new int[fLevels.length + 10];
                System.arraycopy(fLevels, 0, iarray, 0, fLevels.length);
                fLevels = iarray;
            }
            fLevels[fTop] = fLevels[fTop-1];
        } // pushContext()

        /** Pop context. */
        public void popContext() {
            if (fTop > 1) {
				fTop--;
			}
        } // popContext()

        /** Declare prefix. */
        public boolean declarePrefix(String prefix, String uri) {
            int count = getDeclaredPrefixCount();
            for (int i = 0; i < count; i++) {
                String dprefix = getDeclaredPrefixAt(i);
                if (dprefix.equals(prefix)) {
                    return false;
                }
            }
            Entry entry = new Entry(prefix, uri);
            if (fLevels[fTop] == fEntries.length) {
                Entry[] earray = new Entry[fEntries.length + 10];
                System.arraycopy(fEntries, 0, earray, 0, fEntries.length);
                fEntries = earray;
            }
            fEntries[fLevels[fTop]++] = entry;
            return true;
        } // declarePrefix(String,String):boolean

        /** Get prefix. */
        public String getPrefix(String uri) {
            for (int i = fLevels[fTop]-1; i >= 0; i--) {
                Entry entry = (Entry)fEntries[i];
                if (entry.uri.equals(uri)) {
                    return entry.prefix;
                }
            }
            return null;
        } // getPrefix(String):String

        /** Get all prefixes. */
        public Enumeration getAllPrefixes() {
            Vector prefixes = new Vector();
            for (int i = fLevels[1]; i < fLevels[fTop]; i++) {
                String prefix = fEntries[i].prefix;
                if (!prefixes.contains(prefix)) {
                    prefixes.addElement(prefix);
                }
            }
            return prefixes.elements();
        } // getAllPrefixes():Enumeration

        //
        // Classes
        //

        /** A namespace binding entry. */
        static class Entry {

            //
            // Data
            //

            /** Prefix. */
            public String prefix;

            /** URI. */
            public String uri;

            //
            // Constructors
            //

            /** Constructs an entry. */
            public Entry(String prefix, String uri) {
                this.prefix = prefix;
                this.uri = uri;
            } // <init>(String,String)

        } // class Entry

    } // class NamespaceSupport

} // class NamespaceBinder
