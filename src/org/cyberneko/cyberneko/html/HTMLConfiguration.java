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

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Vector;

import org.apache.xerces.util.DefaultErrorHandler;
import org.apache.xerces.util.ParserConfigurationSettings;
import org.apache.xerces.xni.XMLDTDContentModelHandler;
import org.apache.xerces.xni.XMLDTDHandler;
import org.apache.xerces.xni.XMLDocumentHandler;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLConfigurationException;
import org.apache.xerces.xni.parser.XMLDocumentFilter;
import org.apache.xerces.xni.parser.XMLDocumentSource;
import org.apache.xerces.xni.parser.XMLEntityResolver;
import org.apache.xerces.xni.parser.XMLErrorHandler;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.apache.xerces.xni.parser.XMLParseException;
import org.apache.xerces.xni.parser.XMLPullParserConfiguration;
import org.cyberneko.html.filters.NamespaceBinder;
import org.cyberneko.html.xercesbridge.XercesBridge;
                                      
/**
 * An XNI-based parser configuration that can be used to parse HTML 
 * documents. This configuration can be used directly in order to
 * parse HTML documents or can be used in conjunction with any XNI
 * based tools, such as the Xerces2 implementation.
 * <p>
 * This configuration recognizes the following features:
 * <ul>
 * <li>http://cyberneko.org/html/features/augmentations
 * <li>http://cyberneko.org/html/features/report-errors
 * <li>http://cyberneko.org/html/features/report-errors/simple
 * <li>http://cyberneko.org/html/features/balance-tags
 * <li><i>and</i>
 * <li>the features supported by the scanner and tag balancer components.
 * </ul>
 * <p>
 * This configuration recognizes the following properties:
 * <ul>
 * <li>http://cyberneko.org/html/properties/names/elems
 * <li>http://cyberneko.org/html/properties/names/attrs
 * <li>http://cyberneko.org/html/properties/filters
 * <li>http://cyberneko.org/html/properties/error-reporter
 * <li><i>and</i>
 * <li>the properties supported by the scanner and tag balancer.
 * </ul>
 * <p>
 * For complete usage information, refer to the documentation.
 *
 * @see HTMLScanner
 * @see HTMLTagBalancer
 * @see HTMLErrorReporter
 *
 * @author Andy Clark
 *
 * @version $Id: HTMLConfiguration.java,v 1.9 2005/02/14 03:56:54 andyc Exp $
 */
public class HTMLConfiguration 
    extends ParserConfigurationSettings
    implements XMLPullParserConfiguration {

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

    /** Simple report format. */
    protected static final String SIMPLE_ERROR_FORMAT = "http://cyberneko.org/html/features/report-errors/simple";

    /** Balance tags. */
    protected static final String BALANCE_TAGS = "http://cyberneko.org/html/features/balance-tags";

    // properties

    /** Modify HTML element names: { "upper", "lower", "default" }. */
    protected static final String NAMES_ELEMS = "http://cyberneko.org/html/properties/names/elems";

    /** Modify HTML attribute names: { "upper", "lower", "default" }. */
    protected static final String NAMES_ATTRS = "http://cyberneko.org/html/properties/names/attrs";
    
    /** Pipeline filters. */
    protected static final String FILTERS = "http://cyberneko.org/html/properties/filters";

    /** Error reporter. */
    protected static final String ERROR_REPORTER = "http://cyberneko.org/html/properties/error-reporter";

    // other

    /** Error domain. */
    protected static final String ERROR_DOMAIN = "http://cyberneko.org/html";

    // private

    /** Document source class array. */
    private static final Class[] DOCSOURCE = { XMLDocumentSource.class };

    //
    // Data
    //

    // handlers

    /** Document handler. */
    protected XMLDocumentHandler fDocumentHandler;

    /** DTD handler. */
    protected XMLDTDHandler fDTDHandler;

    /** DTD content model handler. */
    protected XMLDTDContentModelHandler fDTDContentModelHandler;

    /** Error handler. */
    protected XMLErrorHandler fErrorHandler = new DefaultErrorHandler();

    // other settings

    /** Entity resolver. */
    protected XMLEntityResolver fEntityResolver;

    /** Locale. */
    protected Locale fLocale = Locale.getDefault();

    // state

    /** 
     * Stream opened by parser. Therefore, must close stream manually upon
     * termination of parsing.
     */
    protected boolean fCloseStream;

    // components

    /** Components. */
    protected final Vector fHTMLComponents = new Vector(2);

    // pipeline

    /** Document scanner. */
    protected final HTMLScanner fDocumentScanner = createDocumentScanner();

	/** HTML tag balancer. */
    protected final HTMLTagBalancer fTagBalancer = new HTMLTagBalancer();

    /** Namespace binder. */
    protected final NamespaceBinder fNamespaceBinder = new NamespaceBinder();

    // other components

    /** Error reporter. */
    protected final HTMLErrorReporter fErrorReporter = new ErrorReporter();

    // HACK: workarounds Xerces 2.0.x problems

    /** Parser version is Xerces 2.0.0. */
    protected static boolean XERCES_2_0_0 = false;

    /** Parser version is Xerces 2.0.1. */
    protected static boolean XERCES_2_0_1 = false;

    /** Parser version is XML4J 4.0.x. */
    protected static boolean XML4J_4_0_x = false;

    //
    // Static initializer
    //

    static {
        try {
            String VERSION = "org.apache.xerces.impl.Version";
            Object version = ObjectFactory.createObject(VERSION, VERSION);
            java.lang.reflect.Field field = version.getClass().getField("fVersion");
            String versionStr = String.valueOf(field.get(version));
            XERCES_2_0_0 = versionStr.equals("Xerces-J 2.0.0");
            XERCES_2_0_1 = versionStr.equals("Xerces-J 2.0.1");
            XML4J_4_0_x = versionStr.startsWith("XML4J 4.0.");
        }
        catch (Throwable e) {
            // ignore
        }
    } // <clinit>()

    //
    // Constructors
    //

    /** Default constructor. */
    public HTMLConfiguration() {

        // add components
        addComponent(fDocumentScanner);
        addComponent(fTagBalancer);
        addComponent(fNamespaceBinder);

        //
        // features
        //

        // recognized features
        String VALIDATION = "http://xml.org/sax/features/validation";
        String[] recognizedFeatures = {
            AUGMENTATIONS,
            NAMESPACES,
            VALIDATION,
            REPORT_ERRORS,
            SIMPLE_ERROR_FORMAT,
            BALANCE_TAGS,
        };
        addRecognizedFeatures(recognizedFeatures);
        setFeature(AUGMENTATIONS, false);
        setFeature(NAMESPACES, true);
        setFeature(VALIDATION, false);
        setFeature(REPORT_ERRORS, false);
        setFeature(SIMPLE_ERROR_FORMAT, false);
        setFeature(BALANCE_TAGS, true);

        // HACK: Xerces 2.0.0
        if (XERCES_2_0_0) {
            // NOTE: These features should not be required but it causes a
            //       problem if they're not there. This will be fixed in 
            //       subsequent releases of Xerces.
            recognizedFeatures = new String[] {
                "http://apache.org/xml/features/scanner/notify-builtin-refs",
            };
            addRecognizedFeatures(recognizedFeatures);
        }
        
        // HACK: Xerces 2.0.1
        if (XERCES_2_0_0 || XERCES_2_0_1 || XML4J_4_0_x) {
            // NOTE: These features should not be required but it causes a
            //       problem if they're not there. This should be fixed in 
            //       subsequent releases of Xerces.
            recognizedFeatures = new String[] {
                "http://apache.org/xml/features/validation/schema/normalized-value",
                "http://apache.org/xml/features/scanner/notify-char-refs",
            };
            addRecognizedFeatures(recognizedFeatures);
        }
        
        //
        // properties
        //

        // recognized properties
        String[] recognizedProperties = {
            NAMES_ELEMS,
            NAMES_ATTRS,
            FILTERS,
            ERROR_REPORTER,
        };
        addRecognizedProperties(recognizedProperties);
        setProperty(NAMES_ELEMS, "upper");
        setProperty(NAMES_ATTRS, "lower");
        setProperty(ERROR_REPORTER, fErrorReporter);
        
        // HACK: Xerces 2.0.0
        if (XERCES_2_0_0) {
            // NOTE: This is a hack to get around a problem in the Xerces 2.0.0
            //       AbstractSAXParser. If it uses a parser configuration that
            //       does not have a SymbolTable, then it will remove *all*
            //       attributes. This will be fixed in subsequent releases of 
            //       Xerces.
            String SYMBOL_TABLE = "http://apache.org/xml/properties/internal/symbol-table";
            recognizedProperties = new String[] {
                SYMBOL_TABLE,
            };
            addRecognizedProperties(recognizedProperties);
            Object symbolTable = ObjectFactory.createObject("org.apache.xerces.util.SymbolTable",
                                                            "org.apache.xerces.util.SymbolTable");
            setProperty(SYMBOL_TABLE, symbolTable);
        }

    } // <init>()

	protected HTMLScanner createDocumentScanner() {
		return new HTMLScanner();
	}

    //
    // Public methods
    //

    /** 
     * Pushes an input source onto the current entity stack. This 
     * enables the scanner to transparently scan new content (e.g. 
     * the output written by an embedded script). At the end of the
     * current entity, the scanner returns where it left off at the
     * time this entity source was pushed.
     * <p>
     * <strong>Hint:</strong>
     * To use this feature to insert the output of &lt;SCRIPT&gt;
     * tags, remember to buffer the <em>entire</em> output of the
     * processed instructions before pushing a new input source.
     * Otherwise, events may appear out of sequence.
     *
     * @param inputSource The new input source to start scanning.
     * @see #evaluateInputSource(XMLInputSource)
     */
    public void pushInputSource(XMLInputSource inputSource) {
        fDocumentScanner.pushInputSource(inputSource);
    } // pushInputSource(XMLInputSource)

    /**
     * <font color="red">EXPERIMENTAL: may change in next release</font><br/>
     * Immediately evaluates an input source and add the new content (e.g. 
     * the output written by an embedded script).
     *
     * @param inputSource The new input source to start scanning.
     * @see #pushInputSource(XMLInputSource)
     */
    public void evaluateInputSource(XMLInputSource inputSource) {
        fDocumentScanner.evaluateInputSource(inputSource);
    } // evaluateInputSource(XMLInputSource)

    // XMLParserConfiguration methods
    //

    /** Sets a feature. */
    public void setFeature(String featureId, boolean state)
        throws XMLConfigurationException {
        super.setFeature(featureId, state);
        int size = fHTMLComponents.size();
        for (int i = 0; i < size; i++) {
            HTMLComponent component = (HTMLComponent)fHTMLComponents.elementAt(i);
            component.setFeature(featureId, state);
        }
    } // setFeature(String,boolean)

    /** Sets a property. */
    public void setProperty(String propertyId, Object value)
        throws XMLConfigurationException {
        super.setProperty(propertyId, value);

        if (propertyId.equals(FILTERS)) {
            XMLDocumentFilter[] filters = (XMLDocumentFilter[])getProperty(FILTERS);
            if (filters != null) {
                for (int i = 0; i < filters.length; i++) {
                    XMLDocumentFilter filter = filters[i];
                    if (filter instanceof HTMLComponent) {
                        addComponent((HTMLComponent)filter);
                    }
                }
            }
        }

        int size = fHTMLComponents.size();
        for (int i = 0; i < size; i++) {
            HTMLComponent component = (HTMLComponent)fHTMLComponents.elementAt(i);
            component.setProperty(propertyId, value);
        }
    } // setProperty(String,Object)

    /** Sets the document handler. */
    public void setDocumentHandler(XMLDocumentHandler handler) {
        fDocumentHandler = handler;
        if (handler instanceof HTMLTagBalancingListener) {
        	fTagBalancer.setTagBalancingListener((HTMLTagBalancingListener) handler);
        }
    } // setDocumentHandler(XMLDocumentHandler)

    /** Returns the document handler. */
    public XMLDocumentHandler getDocumentHandler() {
        return fDocumentHandler;
    } // getDocumentHandler():XMLDocumentHandler

    /** Sets the DTD handler. */
    public void setDTDHandler(XMLDTDHandler handler) {
        fDTDHandler = handler;
    } // setDTDHandler(XMLDTDHandler)

    /** Returns the DTD handler. */
    public XMLDTDHandler getDTDHandler() {
        return fDTDHandler;
    } // getDTDHandler():XMLDTDHandler

    /** Sets the DTD content model handler. */
    public void setDTDContentModelHandler(XMLDTDContentModelHandler handler) {
        fDTDContentModelHandler = handler;
    } // setDTDContentModelHandler(XMLDTDContentModelHandler)

    /** Returns the DTD content model handler. */
    public XMLDTDContentModelHandler getDTDContentModelHandler() {
        return fDTDContentModelHandler;
    } // getDTDContentModelHandler():XMLDTDContentModelHandler

    /** Sets the error handler. */
    public void setErrorHandler(XMLErrorHandler handler) {
        fErrorHandler = handler;
    } // setErrorHandler(XMLErrorHandler)

    /** Returns the error handler. */
    public XMLErrorHandler getErrorHandler() {
        return fErrorHandler;
    } // getErrorHandler():XMLErrorHandler

    /** Sets the entity resolver. */
    public void setEntityResolver(XMLEntityResolver resolver) {
        fEntityResolver = resolver;
    } // setEntityResolver(XMLEntityResolver)

    /** Returns the entity resolver. */
    public XMLEntityResolver getEntityResolver() {
        return fEntityResolver;
    } // getEntityResolver():XMLEntityResolver

    /** Sets the locale. */
    public void setLocale(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        fLocale = locale;
    } // setLocale(Locale)

    /** Returns the locale. */
    public Locale getLocale() {
        return fLocale;
    } // getLocale():Locale

    /** Parses a document. */
    public void parse(XMLInputSource source) throws XNIException, IOException {
        setInputSource(source);
        parse(true);
    } // parse(XMLInputSource)

    //
    // XMLPullParserConfiguration methods
    //

    // parsing

    /**
     * Sets the input source for the document to parse.
     *
     * @param inputSource The document's input source.
     *
     * @exception XMLConfigurationException Thrown if there is a 
     *                        configuration error when initializing the
     *                        parser.
     * @exception IOException Thrown on I/O error.
     *
     * @see #parse(boolean)
     */
    public void setInputSource(XMLInputSource inputSource)
        throws XMLConfigurationException, IOException {
        reset();
        fCloseStream = inputSource.getByteStream() == null &&
                       inputSource.getCharacterStream() == null;
        fDocumentScanner.setInputSource(inputSource);
    } // setInputSource(XMLInputSource)

    /**
     * Parses the document in a pull parsing fashion.
     *
     * @param complete True if the pull parser should parse the
     *                 remaining document completely.
     *
     * @return True if there is more document to parse.
     *
     * @exception XNIException Any XNI exception, possibly wrapping 
     *                         another exception.
     * @exception IOException  An IO exception from the parser, possibly
     *                         from a byte stream or character stream
     *                         supplied by the parser.
     *
     * @see #setInputSource
     */
    public boolean parse(boolean complete) throws XNIException, IOException {
        try {
            boolean more = fDocumentScanner.scanDocument(complete);
            if (!more) {
                cleanup();
            }
            return more;
        }
        catch (XNIException e) {
            cleanup();
            throw e;
        }
        catch (IOException e) {
            cleanup();
            throw e;
        }
    } // parse(boolean):boolean

    /**
     * If the application decides to terminate parsing before the xml document
     * is fully parsed, the application should call this method to free any
     * resource allocated during parsing. For example, close all opened streams.
     */
    public void cleanup() {
        fDocumentScanner.cleanup(fCloseStream);
    } // cleanup()
    
    //
    // Protected methods
    //

    /** Adds a component. */
    protected void addComponent(HTMLComponent component) {

        // add component to list
        fHTMLComponents.addElement(component);

        // add recognized features and set default states
        String[] features = component.getRecognizedFeatures();
        addRecognizedFeatures(features);
        int featureCount = features != null ? features.length : 0;
        for (int i = 0; i < featureCount; i++) {
            Boolean state = component.getFeatureDefault(features[i]);
            if (state != null) {
                setFeature(features[i], state.booleanValue());
            }
        }

        // add recognized properties and set default values
        String[] properties = component.getRecognizedProperties();
        addRecognizedProperties(properties);
        int propertyCount = properties != null ? properties.length : 0;
        for (int i = 0; i < propertyCount; i++) {
            Object value = component.getPropertyDefault(properties[i]);
            if (value != null) {
                setProperty(properties[i], value);
            }
        }

    } // addComponent(HTMLComponent)

    /** Resets the parser configuration. */
    protected void reset() throws XMLConfigurationException {

        // reset components
        int size = fHTMLComponents.size();
        for (int i = 0; i < size; i++) {
            HTMLComponent component = (HTMLComponent)fHTMLComponents.elementAt(i);
            component.reset(this);
        }

        // configure pipeline
        XMLDocumentSource lastSource = fDocumentScanner;
        if (getFeature(NAMESPACES)) {
            lastSource.setDocumentHandler(fNamespaceBinder);
            fNamespaceBinder.setDocumentSource(fTagBalancer);
            lastSource = fNamespaceBinder;
        }
        if (getFeature(BALANCE_TAGS)) {
            lastSource.setDocumentHandler(fTagBalancer);
            fTagBalancer.setDocumentSource(fDocumentScanner);
            lastSource = fTagBalancer;
        }
        XMLDocumentFilter[] filters = (XMLDocumentFilter[])getProperty(FILTERS);
        if (filters != null) {
            for (int i = 0; i < filters.length; i++) {
                XMLDocumentFilter filter = filters[i];
                XercesBridge.getInstance().XMLDocumentFilter_setDocumentSource(filter, lastSource);
                lastSource.setDocumentHandler(filter);
                lastSource = filter;
            }
        }
        lastSource.setDocumentHandler(fDocumentHandler);

    } // reset()

    //
    // Interfaces
    //

    /**
     * Defines an error reporter for reporting HTML errors. There is no such 
     * thing as a fatal error in parsing HTML. I/O errors are fatal but should 
     * throw an <code>IOException</code> directly instead of reporting an error.
     * <p>
     * When used in a configuration, the error reporter instance should be
     * set as a property with the following property identifier:
     * <pre>
     * "http://cyberneko.org/html/internal/error-reporter" in the
     * </pre>
     * Components in the configuration can query the error reporter using this
     * property identifier.
     * <p>
     * <strong>Note:</strong>
     * All reported errors are within the domain "http://cyberneko.org/html". 
     *
     * @author Andy Clark
     */
    protected class ErrorReporter
        implements HTMLErrorReporter {

        //
        // Data
        //

        /** Last locale. */
        protected Locale fLastLocale;

        /** Error messages. */
        protected ResourceBundle fErrorMessages;

        //
        // HTMLErrorReporter methods
        //

        /** Format message without reporting error. */
        public String formatMessage(String key, Object[] args) {
            if (!getFeature(SIMPLE_ERROR_FORMAT)) {
                if (!fLocale.equals(fLastLocale)) {
                    fErrorMessages = null;
                    fLastLocale = fLocale;
                }
                if (fErrorMessages == null) {
                    fErrorMessages = 
                        ResourceBundle.getBundle("org/cyberneko/html/res/ErrorMessages",
                                                 fLocale);
                }
                try {
                    String value = fErrorMessages.getString(key);
                    String message = MessageFormat.format(value, args);
                    return message;
                }
                catch (MissingResourceException e) {
                    // ignore and return a simple format
                }
            }
            return formatSimpleMessage(key, args);
        } // formatMessage(String,Object[]):String

        /** Reports a warning. */
        public void reportWarning(String key, Object[] args)
            throws XMLParseException {
            if (fErrorHandler != null) {
                fErrorHandler.warning(ERROR_DOMAIN, key, createException(key, args));
            }
        } // reportWarning(String,Object[])

        /** Reports an error. */
        public void reportError(String key, Object[] args)
            throws XMLParseException {
            if (fErrorHandler != null) {
                fErrorHandler.error(ERROR_DOMAIN, key, createException(key, args));
            }
        } // reportError(String,Object[])

        //
        // Protected methods
        //

        /** Creates parse exception. */
        protected XMLParseException createException(String key, Object[] args) {
            String message = formatMessage(key, args);
            return new XMLParseException(fDocumentScanner, message);
        } // createException(String,Object[]):XMLParseException

        /** Format simple message. */
        protected String formatSimpleMessage(String key, Object[] args) {
            StringBuffer str = new StringBuffer();
            str.append(ERROR_DOMAIN);
            str.append('#');
            str.append(key);
            if (args != null && args.length > 0) {
                str.append('\t');
                for (int i = 0; i < args.length; i++) {
                    if (i > 0) {
                        str.append('\t');
                    }
                    str.append(String.valueOf(args[i]));
                }
            }
            return str.toString();
        } // formatSimpleMessage(String,

    } // class ErrorReporter

} // class HTMLConfiguration
