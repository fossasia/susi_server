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

import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.BitSet;
import java.util.Stack;

import org.apache.xerces.util.EncodingMap;
import org.apache.xerces.util.NamespaceSupport;
import org.apache.xerces.util.URI;
import org.apache.xerces.util.XMLAttributesImpl;
import org.apache.xerces.util.XMLResourceIdentifierImpl;
import org.apache.xerces.util.XMLStringBuffer;
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
import org.apache.xerces.xni.parser.XMLDocumentScanner;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.cyberneko.html.xercesbridge.XercesBridge;

/**
 * A simple HTML scanner. This scanner makes no attempt to balance tags
 * or fix other problems in the source document &mdash; it just scans what 
 * it can and generates XNI document "events", ignoring errors of all 
 * kinds.
 * <p>
 * This component recognizes the following features:
 * <ul>
 * <li>http://cyberneko.org/html/features/augmentations
 * <li>http://cyberneko.org/html/features/report-errors
 * <li>http://apache.org/xml/features/scanner/notify-char-refs
 * <li>http://apache.org/xml/features/scanner/notify-builtin-refs
 * <li>http://cyberneko.org/html/features/scanner/notify-builtin-refs
 * <li>http://cyberneko.org/html/features/scanner/fix-mswindows-refs
 * <li>http://cyberneko.org/html/features/scanner/script/strip-cdata-delims
 * <li>http://cyberneko.org/html/features/scanner/script/strip-comment-delims
 * <li>http://cyberneko.org/html/features/scanner/style/strip-cdata-delims
 * <li>http://cyberneko.org/html/features/scanner/style/strip-comment-delims
 * <li>http://cyberneko.org/html/features/scanner/ignore-specified-charset
 * <li>http://cyberneko.org/html/features/scanner/cdata-sections
 * <li>http://cyberneko.org/html/features/override-doctype
 * <li>http://cyberneko.org/html/features/insert-doctype
 * <li>http://cyberneko.org/html/features/parse-noscript-content
 * </ul>
 * <p>
 * This component recognizes the following properties:
 * <ul>
 * <li>http://cyberneko.org/html/properties/names/elems
 * <li>http://cyberneko.org/html/properties/names/attrs
 * <li>http://cyberneko.org/html/properties/default-encoding
 * <li>http://cyberneko.org/html/properties/error-reporter
 * <li>http://cyberneko.org/html/properties/doctype/pubid
 * <li>http://cyberneko.org/html/properties/doctype/sysid
 * </ul>
 *
 * @see HTMLElements
 * @see HTMLEntities
 *
 * @author Andy Clark
 * @author Marc Guillemot
 * @author Ahmed Ashour
 *
 * @version $Id: HTMLScanner.java,v 1.19 2005/06/14 05:52:37 andyc Exp $
 */
public class HTMLScanner 
    implements XMLDocumentScanner, XMLLocator, HTMLComponent {

    //
    // Constants
    //

    // doctype info: HTML 4.01 strict

    /** HTML 4.01 strict public identifier ("-//W3C//DTD HTML 4.01//EN"). */
    public static final String HTML_4_01_STRICT_PUBID = "-//W3C//DTD HTML 4.01//EN";

    /** HTML 4.01 strict system identifier ("http://www.w3.org/TR/html4/strict.dtd"). */
    public static final String HTML_4_01_STRICT_SYSID = "http://www.w3.org/TR/html4/strict.dtd";

    // doctype info: HTML 4.01 loose

    /** HTML 4.01 transitional public identifier ("-//W3C//DTD HTML 4.01 Transitional//EN"). */
    public static final String HTML_4_01_TRANSITIONAL_PUBID = "-//W3C//DTD HTML 4.01 Transitional//EN";

    /** HTML 4.01 transitional system identifier ("http://www.w3.org/TR/html4/loose.dtd"). */
    public static final String HTML_4_01_TRANSITIONAL_SYSID = "http://www.w3.org/TR/html4/loose.dtd";

    // doctype info: HTML 4.01 frameset

    /** HTML 4.01 frameset public identifier ("-//W3C//DTD HTML 4.01 Frameset//EN"). */
    public static final String HTML_4_01_FRAMESET_PUBID = "-//W3C//DTD HTML 4.01 Frameset//EN";

    /** HTML 4.01 frameset system identifier ("http://www.w3.org/TR/html4/frameset.dtd"). */
    public static final String HTML_4_01_FRAMESET_SYSID = "http://www.w3.org/TR/html4/frameset.dtd";

    // features

    /** Include infoset augmentations. */
    protected static final String AUGMENTATIONS = "http://cyberneko.org/html/features/augmentations";

    /** Report errors. */
    protected static final String REPORT_ERRORS = "http://cyberneko.org/html/features/report-errors";

    /** Notify character entity references (e.g. &amp;#32;, &amp;#x20;, etc). */
    public static final String NOTIFY_CHAR_REFS = "http://apache.org/xml/features/scanner/notify-char-refs";

    /** 
     * Notify handler of built-in entity references (e.g. &amp;amp;, 
     * &amp;lt;, etc).
     * <p>
     * <strong>Note:</strong>
     * This only applies to the five pre-defined XML general entities.
     * Specifically, "amp", "lt", "gt", "quot", and "apos". This is done 
     * for compatibility with the Xerces feature.
     * <p>
     * To be notified of the built-in entity references in HTML, set the 
     * <code>http://cyberneko.org/html/features/scanner/notify-builtin-refs</code> 
     * feature to <code>true</code>.
     */
    public static final String NOTIFY_XML_BUILTIN_REFS = "http://apache.org/xml/features/scanner/notify-builtin-refs";

    /** 
     * Notify handler of built-in entity references (e.g. &amp;nobr;, 
     * &amp;copy;, etc).
     * <p>
     * <strong>Note:</strong>
     * This <em>includes</em> the five pre-defined XML general entities.
     */
    public static final String NOTIFY_HTML_BUILTIN_REFS = "http://cyberneko.org/html/features/scanner/notify-builtin-refs";

    /** Fix Microsoft Windows&reg; character entity references. */
    public static final String FIX_MSWINDOWS_REFS = "http://cyberneko.org/html/features/scanner/fix-mswindows-refs";

    /** 
     * Strip HTML comment delimiters ("&lt;!&minus;&minus;" and 
     * "&minus;&minus;&gt;") from SCRIPT tag contents.
     */
    public static final String SCRIPT_STRIP_COMMENT_DELIMS = "http://cyberneko.org/html/features/scanner/script/strip-comment-delims";

    /** 
     * Strip XHTML CDATA delimiters ("&lt;![CDATA[" and "]]&gt;") from 
     * SCRIPT tag contents.
     */
    public static final String SCRIPT_STRIP_CDATA_DELIMS = "http://cyberneko.org/html/features/scanner/script/strip-cdata-delims";

    /** 
     * Strip HTML comment delimiters ("&lt;!&minus;&minus;" and 
     * "&minus;&minus;&gt;") from STYLE tag contents.
     */
    public static final String STYLE_STRIP_COMMENT_DELIMS = "http://cyberneko.org/html/features/scanner/style/strip-comment-delims";

    /** 
     * Strip XHTML CDATA delimiters ("&lt;![CDATA[" and "]]&gt;") from 
     * STYLE tag contents.
     */
    public static final String STYLE_STRIP_CDATA_DELIMS = "http://cyberneko.org/html/features/scanner/style/strip-cdata-delims";

    /**
     * Ignore specified charset found in the &lt;meta equiv='Content-Type'
     * content='text/html;charset=&hellip;'&gt; tag or in the &lt;?xml &hellip; encoding='&hellip;'&gt; processing instruction
     */
    public static final String IGNORE_SPECIFIED_CHARSET = "http://cyberneko.org/html/features/scanner/ignore-specified-charset";

    /** Scan CDATA sections. */
    public static final String CDATA_SECTIONS = "http://cyberneko.org/html/features/scanner/cdata-sections";

    /** Override doctype declaration public and system identifiers. */
    public static final String OVERRIDE_DOCTYPE = "http://cyberneko.org/html/features/override-doctype";

    /** Insert document type declaration. */
    public static final String INSERT_DOCTYPE = "http://cyberneko.org/html/features/insert-doctype";
    
    /** Parse &lt;noscript&gt;...&lt;/noscript&gt; content */
    public static final String PARSE_NOSCRIPT_CONTENT = "http://cyberneko.org/html/features/parse-noscript-content";

    /** Normalize attribute values. */
    protected static final String NORMALIZE_ATTRIBUTES = "http://cyberneko.org/html/features/scanner/normalize-attrs";

    /** Recognized features. */
    private static final String[] RECOGNIZED_FEATURES = {
        AUGMENTATIONS,
        REPORT_ERRORS,
        NOTIFY_CHAR_REFS,
        NOTIFY_XML_BUILTIN_REFS,
        NOTIFY_HTML_BUILTIN_REFS,
        FIX_MSWINDOWS_REFS,
        SCRIPT_STRIP_CDATA_DELIMS,
        SCRIPT_STRIP_COMMENT_DELIMS,
        STYLE_STRIP_CDATA_DELIMS,
        STYLE_STRIP_COMMENT_DELIMS,
        IGNORE_SPECIFIED_CHARSET,
        CDATA_SECTIONS,
        OVERRIDE_DOCTYPE,
        INSERT_DOCTYPE,
        NORMALIZE_ATTRIBUTES,
        PARSE_NOSCRIPT_CONTENT,
    };

    /** Recognized features defaults. */
    private static final Boolean[] RECOGNIZED_FEATURES_DEFAULTS = {
        null,
        null,
        Boolean.FALSE,
        Boolean.FALSE,
        Boolean.FALSE,
        Boolean.FALSE,
        Boolean.FALSE,
        Boolean.FALSE,
        Boolean.FALSE,
        Boolean.FALSE,
        Boolean.FALSE,
        Boolean.FALSE,
        Boolean.FALSE,
        Boolean.FALSE,
        Boolean.FALSE,
        Boolean.TRUE,
    };

    // properties

    /** Modify HTML element names: { "upper", "lower", "default" }. */
    protected static final String NAMES_ELEMS = "http://cyberneko.org/html/properties/names/elems";

    /** Modify HTML attribute names: { "upper", "lower", "default" }. */
    protected static final String NAMES_ATTRS = "http://cyberneko.org/html/properties/names/attrs";
    
    /** Default encoding. */
    protected static final String DEFAULT_ENCODING = "http://cyberneko.org/html/properties/default-encoding";
    
    /** Error reporter. */
    protected static final String ERROR_REPORTER = "http://cyberneko.org/html/properties/error-reporter";

    /** Doctype declaration public identifier. */
    protected static final String DOCTYPE_PUBID = "http://cyberneko.org/html/properties/doctype/pubid";

    /** Doctype declaration system identifier. */
    protected static final String DOCTYPE_SYSID = "http://cyberneko.org/html/properties/doctype/sysid";

    /** Recognized properties. */
    private static final String[] RECOGNIZED_PROPERTIES = {
        NAMES_ELEMS,
        NAMES_ATTRS,
        DEFAULT_ENCODING,
        ERROR_REPORTER,
        DOCTYPE_PUBID,
        DOCTYPE_SYSID,
    };

    /** Recognized properties defaults. */
    private static final Object[] RECOGNIZED_PROPERTIES_DEFAULTS = {
        null,
        null,
        "Windows-1252",
        null,
        HTML_4_01_TRANSITIONAL_PUBID,
        HTML_4_01_TRANSITIONAL_SYSID,
    };

    // states

    /** State: content. */
    protected static final short STATE_CONTENT = 0;

    /** State: markup bracket. */
    protected static final short STATE_MARKUP_BRACKET = 1;

    /** State: start document. */
    protected static final short STATE_START_DOCUMENT = 10;

    /** State: end document. */
    protected static final short STATE_END_DOCUMENT = 11;

    // modify HTML names

    /** Don't modify HTML names. */
    protected static final short NAMES_NO_CHANGE = 0;

    /** Uppercase HTML names. */
    protected static final short NAMES_UPPERCASE = 1;

    /** Lowercase HTML names. */
    protected static final short NAMES_LOWERCASE = 2;

    // defaults

    /** Default buffer size. */
    protected static final int DEFAULT_BUFFER_SIZE = 2048;

    // debugging

    /** Set to true to debug changes in the scanner. */
    private static final boolean DEBUG_SCANNER = false;

    /** Set to true to debug changes in the scanner state. */
    private static final boolean DEBUG_SCANNER_STATE = false;

    /** Set to true to debug the buffer. */
    private static final boolean DEBUG_BUFFER = false;

    /** Set to true to debug character encoding handling. */
    private static final boolean DEBUG_CHARSET = false;

    /** Set to true to debug callbacks. */
    protected static final boolean DEBUG_CALLBACKS = false;
    
    // static vars

    /** Synthesized event info item. */
    protected static final HTMLEventInfo SYNTHESIZED_ITEM = 
        new HTMLEventInfo.SynthesizedItem();
        
    private final static BitSet ENTITY_CHARS = new BitSet();
    static {
    	final String str = "-.0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcdefghijklmnopqrstuvwxyz";
    	for (int i = 0; i < str.length(); ++i) {
    		char c = str.charAt(i);
    		ENTITY_CHARS.set(c);
    	}
    }
    //
    // Data
    //

    // features

    /** Augmentations. */
    protected boolean fAugmentations;

    /** Report errors. */
    protected boolean fReportErrors;

    /** Notify character entity references. */
    protected boolean fNotifyCharRefs;

    /** Notify XML built-in general entity references. */
    protected boolean fNotifyXmlBuiltinRefs;

    /** Notify HTML built-in general entity references. */
    protected boolean fNotifyHtmlBuiltinRefs;

    /** Fix Microsoft Windows&reg; character entity references. */
    protected boolean fFixWindowsCharRefs;

    /** Strip CDATA delimiters from SCRIPT tags. */
    protected boolean fScriptStripCDATADelims;

    /** Strip comment delimiters from SCRIPT tags. */
    protected boolean fScriptStripCommentDelims;

    /** Strip CDATA delimiters from STYLE tags. */
    protected boolean fStyleStripCDATADelims;

    /** Strip comment delimiters from STYLE tags. */
    protected boolean fStyleStripCommentDelims;

    /** Ignore specified character set. */
    protected boolean fIgnoreSpecifiedCharset;

    /** CDATA sections. */
    protected boolean fCDATASections;

    /** Override doctype declaration public and system identifiers. */
    protected boolean fOverrideDoctype;

    /** Insert document type declaration. */
    protected boolean fInsertDoctype;

    /** Normalize attribute values. */
    protected boolean fNormalizeAttributes;
    
    /** Parse noscript content. */
    protected boolean fParseNoScriptContent;

    /** Parse noframes content. */
    protected boolean fParseNoFramesContent;

    // properties

    /** Modify HTML element names. */
    protected short fNamesElems;

    /** Modify HTML attribute names. */
    protected short fNamesAttrs;

    /** Default encoding. */
    protected String fDefaultIANAEncoding;

    /** Error reporter. */
    protected HTMLErrorReporter fErrorReporter;

    /** Doctype declaration public identifier. */
    protected String fDoctypePubid;

    /** Doctype declaration system identifier. */
    protected String fDoctypeSysid;

    // boundary locator information

    /** Beginning line number. */
    protected int fBeginLineNumber;

    /** Beginning column number. */
    protected int fBeginColumnNumber;

    /** Beginning character offset in the file. */
    protected int fBeginCharacterOffset;

    /** Ending line number. */
    protected int fEndLineNumber;

    /** Ending column number. */
    protected int fEndColumnNumber;

    /** Ending character offset in the file. */
    protected int fEndCharacterOffset;

    // state

    /** The playback byte stream. */
    protected PlaybackInputStream fByteStream;

    /** Current entity. */
    protected CurrentEntity fCurrentEntity;
    
    /** The current entity stack. */
    protected final Stack fCurrentEntityStack = new Stack();

    /** The current scanner. */
    protected Scanner fScanner;

    /** The current scanner state. */
    protected short fScannerState;

    /** The document handler. */
    protected XMLDocumentHandler fDocumentHandler;

    /** Auto-detected IANA encoding. */
    protected String fIANAEncoding;

    /** Auto-detected Java encoding. */
    protected String fJavaEncoding;

    /** True if the encoding matches "ISO-8859-*". */
    protected boolean fIso8859Encoding;

    /** Element count. */
    protected int fElementCount;

    /** Element depth. */
    protected int fElementDepth;

    // scanners

    /** Content scanner. */
    protected Scanner fContentScanner = new ContentScanner();

    /** 
     * Special scanner used for elements whose content needs to be scanned 
     * as plain text, ignoring markup such as elements and entity references.
     * For example: &lt;SCRIPT&gt; and &lt;COMMENT&gt;.
     */
    protected SpecialScanner fSpecialScanner = new SpecialScanner();

    // temp vars

    /** String buffer. */
    protected final XMLStringBuffer fStringBuffer = new XMLStringBuffer(1024);

    /** String buffer. */
    private final XMLStringBuffer fStringBuffer2 = new XMLStringBuffer(1024);

    /** Non-normalized attribute string buffer. */
    private final XMLStringBuffer fNonNormAttr = new XMLStringBuffer(128);

    /** Augmentations. */
    private final HTMLAugmentations fInfosetAugs = new HTMLAugmentations();

    /** Location infoset item. */
    private final LocationItem fLocationItem = new LocationItem();

    /** Single boolean array. */
    private final boolean[] fSingleBoolean = { false };

    /** Resource identifier. */
    private final XMLResourceIdentifierImpl fResourceId = new XMLResourceIdentifierImpl();

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
     * <strong>Note:</strong>
     * This functionality is experimental at this time and is
     * subject to change in future releases of NekoHTML.
     *
     * @param inputSource The new input source to start scanning.
     * @see #evaluateInputSource(XMLInputSource)
     */
    public void pushInputSource(XMLInputSource inputSource) {
    	final Reader reader = getReader(inputSource);

    	fCurrentEntityStack.push(fCurrentEntity);
        String encoding = inputSource.getEncoding();
        String publicId = inputSource.getPublicId();
        String baseSystemId = inputSource.getBaseSystemId();
        String literalSystemId = inputSource.getSystemId();
        String expandedSystemId = expandSystemId(literalSystemId, baseSystemId);
        fCurrentEntity = new CurrentEntity(reader, encoding, 
                                           publicId, baseSystemId,
                                           literalSystemId, expandedSystemId);
    } // pushInputSource(XMLInputSource)

    private Reader getReader(final XMLInputSource inputSource) {
        Reader reader = inputSource.getCharacterStream();
        if (reader == null) {
        	try {
				return new InputStreamReader(inputSource.getByteStream(), fJavaEncoding);
			}
        	catch (final UnsupportedEncodingException e) {
				// should not happen as this encoding is already used to parse the "main" source
			}
        }
        return reader;
	}

	/** 
     * Immediately evaluates an input source and add the new content (e.g. 
     * the output written by an embedded script).
     *
     * @param inputSource The new input source to start evaluating.
     * @see #pushInputSource(XMLInputSource)
     */
    public void evaluateInputSource(XMLInputSource inputSource) {
        final Scanner previousScanner = fScanner;
        final short previousScannerState = fScannerState;
        final CurrentEntity previousEntity = fCurrentEntity;
        final Reader reader = getReader(inputSource);

        String encoding = inputSource.getEncoding();
        String publicId = inputSource.getPublicId();
        String baseSystemId = inputSource.getBaseSystemId();
        String literalSystemId = inputSource.getSystemId();
        String expandedSystemId = expandSystemId(literalSystemId, baseSystemId);
        fCurrentEntity = new CurrentEntity(reader, encoding, 
                                           publicId, baseSystemId,
                                           literalSystemId, expandedSystemId);
        setScanner(fContentScanner);
        setScannerState(STATE_CONTENT);
        try {
            do {
                fScanner.scan(false);
            } while (fScannerState != STATE_END_DOCUMENT);
        }
        catch (final IOException e) {
            // ignore
        }
        setScanner(previousScanner);
        setScannerState(previousScannerState);
        fCurrentEntity = previousEntity;
    } // evaluateInputSource(XMLInputSource)

    /**
     * Cleans up used resources. For example, if scanning is terminated
     * early, then this method ensures all remaining open streams are
     * closed.
     *
     * @param closeall Close all streams, including the original.
     *                 This is used in cases when the application has
     *                 opened the original document stream and should
     *                 be responsible for closing it.
     */
    public void cleanup(boolean closeall) {
        int size = fCurrentEntityStack.size();
        if (size > 0) {
            // current entity is not the original, so close it
            if (fCurrentEntity != null) {
            	fCurrentEntity.closeQuietly();
            }
            // close remaining streams
            for (int i = closeall ? 0 : 1; i < size; i++) {
                fCurrentEntity = (CurrentEntity) fCurrentEntityStack.pop();
                fCurrentEntity.closeQuietly();
            }
        }
        else if (closeall && fCurrentEntity != null) {
        	fCurrentEntity.closeQuietly();
        }
    } // cleanup(boolean)

    //
    // XMLLocator methods
    //

    /** Returns the encoding. */
    public String getEncoding() {
        return fCurrentEntity != null ? fCurrentEntity.encoding : null;
    } // getEncoding():String

    /** Returns the public identifier. */
    public String getPublicId() { 
        return fCurrentEntity != null ? fCurrentEntity.publicId : null; 
    } // getPublicId():String

    /** Returns the base system identifier. */
    public String getBaseSystemId() { 
        return fCurrentEntity != null ? fCurrentEntity.baseSystemId : null; 
    } // getBaseSystemId():String

    /** Returns the literal system identifier. */
    public String getLiteralSystemId() { 
        return fCurrentEntity != null ? fCurrentEntity.literalSystemId : null; 
    } // getLiteralSystemId():String

    /** Returns the expanded system identifier. */
    public String getExpandedSystemId() { 
        return fCurrentEntity != null ? fCurrentEntity.expandedSystemId : null; 
    } // getExpandedSystemId():String

    /** Returns the current line number. */
    public int getLineNumber() { 
        return fCurrentEntity != null ? fCurrentEntity.getLineNumber() : -1; 
    } // getLineNumber():int

    /** Returns the current column number. */
    public int getColumnNumber() { 
        return fCurrentEntity != null ? fCurrentEntity.getColumnNumber() : -1; 
    } // getColumnNumber():int
    
    /** Returns the XML version. */
    public String getXMLVersion() {
		return fCurrentEntity != null ? fCurrentEntity.version : null; 
    } // getXMLVersion():String
    
    /** Returns the character offset. */
    public int getCharacterOffset() {
		return fCurrentEntity != null ? fCurrentEntity.getCharacterOffset() : -1; 
    } // getCharacterOffset():int

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
        fAugmentations = manager.getFeature(AUGMENTATIONS);
        fReportErrors = manager.getFeature(REPORT_ERRORS);
        fNotifyCharRefs = manager.getFeature(NOTIFY_CHAR_REFS);
        fNotifyXmlBuiltinRefs = manager.getFeature(NOTIFY_XML_BUILTIN_REFS);
        fNotifyHtmlBuiltinRefs = manager.getFeature(NOTIFY_HTML_BUILTIN_REFS);
        fFixWindowsCharRefs = manager.getFeature(FIX_MSWINDOWS_REFS);
        fScriptStripCDATADelims = manager.getFeature(SCRIPT_STRIP_CDATA_DELIMS);
        fScriptStripCommentDelims = manager.getFeature(SCRIPT_STRIP_COMMENT_DELIMS);
        fStyleStripCDATADelims = manager.getFeature(STYLE_STRIP_CDATA_DELIMS);
        fStyleStripCommentDelims = manager.getFeature(STYLE_STRIP_COMMENT_DELIMS);
        fIgnoreSpecifiedCharset = manager.getFeature(IGNORE_SPECIFIED_CHARSET);
        fCDATASections = manager.getFeature(CDATA_SECTIONS);
        fOverrideDoctype = manager.getFeature(OVERRIDE_DOCTYPE);
        fInsertDoctype = manager.getFeature(INSERT_DOCTYPE);
        fNormalizeAttributes = manager.getFeature(NORMALIZE_ATTRIBUTES);
        fParseNoScriptContent = manager.getFeature(PARSE_NOSCRIPT_CONTENT);

        // get properties
        fNamesElems = getNamesValue(String.valueOf(manager.getProperty(NAMES_ELEMS)));
        fNamesAttrs = getNamesValue(String.valueOf(manager.getProperty(NAMES_ATTRS)));
        fDefaultIANAEncoding = String.valueOf(manager.getProperty(DEFAULT_ENCODING));
        fErrorReporter = (HTMLErrorReporter)manager.getProperty(ERROR_REPORTER);
        fDoctypePubid = String.valueOf(manager.getProperty(DOCTYPE_PUBID));
        fDoctypeSysid = String.valueOf(manager.getProperty(DOCTYPE_SYSID));
    
    } // reset(XMLComponentManager)

    /** Sets a feature. */
    public void setFeature(String featureId, boolean state)
        throws XMLConfigurationException {

        if (featureId.equals(AUGMENTATIONS)) { 
            fAugmentations = state; 
        }
        else if (featureId.equals(IGNORE_SPECIFIED_CHARSET)) { 
            fIgnoreSpecifiedCharset = state; 
        }
        else if (featureId.equals(NOTIFY_CHAR_REFS)) { 
            fNotifyCharRefs = state; 
        }
        else if (featureId.equals(NOTIFY_XML_BUILTIN_REFS)) { 
            fNotifyXmlBuiltinRefs = state; 
        }
        else if (featureId.equals(NOTIFY_HTML_BUILTIN_REFS)) { 
            fNotifyHtmlBuiltinRefs = state; 
        }
        else if (featureId.equals(FIX_MSWINDOWS_REFS)) { 
            fFixWindowsCharRefs = state; 
        }
        else if (featureId.equals(SCRIPT_STRIP_CDATA_DELIMS)) { 
            fScriptStripCDATADelims = state; 
        }
        else if (featureId.equals(SCRIPT_STRIP_COMMENT_DELIMS)) { 
            fScriptStripCommentDelims = state; 
        }
        else if (featureId.equals(STYLE_STRIP_CDATA_DELIMS)) { 
            fStyleStripCDATADelims = state; 
        }
        else if (featureId.equals(STYLE_STRIP_COMMENT_DELIMS)) { 
            fStyleStripCommentDelims = state; 
        }
        else if (featureId.equals(IGNORE_SPECIFIED_CHARSET)) { 
            fIgnoreSpecifiedCharset = state; 
        }
        else if (featureId.equals(PARSE_NOSCRIPT_CONTENT)) { 
            fParseNoScriptContent = state; 
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

        if (propertyId.equals(DEFAULT_ENCODING)) {
            fDefaultIANAEncoding = String.valueOf(value);
            return;
        }

    } // setProperty(String,Object)

    //
    // XMLDocumentScanner methods
    //

    /** Sets the input source. */
    public void setInputSource(XMLInputSource source) throws IOException {

        // reset state
        fElementCount = 0;
        fElementDepth = -1;
        fByteStream = null;
        fCurrentEntityStack.removeAllElements();

        fBeginLineNumber = 1;
        fBeginColumnNumber = 1;
        fBeginCharacterOffset = 0;
        fEndLineNumber = fBeginLineNumber;
        fEndColumnNumber = fBeginColumnNumber;
        fEndCharacterOffset = fBeginCharacterOffset;

        // reset encoding information
        fIANAEncoding = fDefaultIANAEncoding;
        fJavaEncoding = fIANAEncoding;

        // get location information
        String encoding = source.getEncoding();
        String publicId = source.getPublicId();
        String baseSystemId = source.getBaseSystemId();
        String literalSystemId = source.getSystemId();
        String expandedSystemId = expandSystemId(literalSystemId, baseSystemId);

        // open stream
        Reader reader = source.getCharacterStream();
        if (reader == null) {
            InputStream inputStream = source.getByteStream();
            if (inputStream == null) {
                URL url = new URL(expandedSystemId);
                inputStream = url.openStream();
            }
            fByteStream = new PlaybackInputStream(inputStream);
            String[] encodings = new String[2];
            if (encoding == null) {
                fByteStream.detectEncoding(encodings);
            }
            else {
                encodings[0] = encoding;
            }
            if (encodings[0] == null) {
                encodings[0] = fDefaultIANAEncoding;
                if (fReportErrors) {
                    fErrorReporter.reportWarning("HTML1000", null);
                }
            }
            if (encodings[1] == null) {
                encodings[1] = EncodingMap.getIANA2JavaMapping(encodings[0].toUpperCase());
                if (encodings[1] == null) {
                    encodings[1] = encodings[0];
                    if (fReportErrors) {
                        fErrorReporter.reportWarning("HTML1001", new Object[]{encodings[0]});
                    }
                }
            }
            fIANAEncoding = encodings[0];
            fJavaEncoding = encodings[1];
            /* PATCH: Asgeir Asgeirsson */
            fIso8859Encoding = fIANAEncoding == null 
                            || fIANAEncoding.toUpperCase().startsWith("ISO-8859")
                            || fIANAEncoding.equalsIgnoreCase(fDefaultIANAEncoding);
            encoding = fIANAEncoding;
            reader = new InputStreamReader(fByteStream, fJavaEncoding);
        }
        fCurrentEntity = new CurrentEntity(reader, encoding,
                                           publicId, baseSystemId,
                                           literalSystemId, expandedSystemId);

        // set scanner and state
        setScanner(fContentScanner);
        setScannerState(STATE_START_DOCUMENT);

    } // setInputSource(XMLInputSource)

    /** Scans the document. */
    public boolean scanDocument(boolean complete) throws XNIException, IOException {
        do {
            if (!fScanner.scan(complete)) {
                return false;
            }
        } while (complete);
        return true;
    } // scanDocument(boolean):boolean

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
    // Protected static methods
    //

    /** Returns the value of the specified attribute, ignoring case. */
    protected static String getValue(XMLAttributes attrs, String aname) {
        int length = attrs != null ? attrs.getLength() : 0;
        for (int i = 0; i < length; i++) {
            if (attrs.getQName(i).equalsIgnoreCase(aname)) {
                return attrs.getValue(i);
            }
        }
        return null;
    } // getValue(XMLAttributes,String):String

    /**
     * Expands a system id and returns the system id as a URI, if
     * it can be expanded. A return value of null means that the
     * identifier is already expanded. An exception thrown
     * indicates a failure to expand the id.
     *
     * @param systemId The systemId to be expanded.
     *
     * @return Returns the URI string representing the expanded system
     *         identifier. A null value indicates that the given
     *         system identifier is already expanded.
     *
     */
    public static String expandSystemId(String systemId, String baseSystemId) {

        // check for bad parameters id
        if (systemId == null || systemId.length() == 0) {
            return systemId;
        }
        // if id already expanded, return
        try {
            URI uri = new URI(systemId);
            if (uri != null) {
                return systemId;
            }
        }
        catch (URI.MalformedURIException e) {
            // continue on...
        }
        // normalize id
        String id = fixURI(systemId);

        // normalize base
        URI base = null;
        URI uri = null;
        try {
            if (baseSystemId == null || baseSystemId.length() == 0 ||
                baseSystemId.equals(systemId)) {
                String dir;
                try {
                    dir = fixURI(System.getProperty("user.dir"));
                }
                catch (SecurityException se) {
                    dir = "";
                }
                if (!dir.endsWith("/")) {
                    dir = dir + "/";
                }
                base = new URI("file", "", dir, null, null);
            }
            else {
                try {
                    base = new URI(fixURI(baseSystemId));
                }
                catch (URI.MalformedURIException e) {
                    String dir;
                    try {
                        dir = fixURI(System.getProperty("user.dir"));
                    }
                    catch (SecurityException se) {
                        dir = "";
                    }
                    if (baseSystemId.indexOf(':') != -1) {
                        // for xml schemas we might have baseURI with
                        // a specified drive
                        base = new URI("file", "", fixURI(baseSystemId), null, null);
                    }
                    else {
                        if (!dir.endsWith("/")) {
                            dir = dir + "/";
                        }
                        dir = dir + fixURI(baseSystemId);
                        base = new URI("file", "", dir, null, null);
                    }
                }
             }
             // expand id
             uri = new URI(base, id);
        }
        catch (URI.MalformedURIException e) {
            // let it go through
        }

        if (uri == null) {
            return systemId;
        }
        return uri.toString();

    } // expandSystemId(String,String):String

    /**
     * Fixes a platform dependent filename to standard URI form.
     *
     * @param str The string to fix.
     *
     * @return Returns the fixed URI string.
     */
    protected static String fixURI(String str) {

        // handle platform dependent strings
        str = str.replace(java.io.File.separatorChar, '/');

        // Windows fix
        if (str.length() >= 2) {
            char ch1 = str.charAt(1);
            // change "C:blah" to "/C:blah"
            if (ch1 == ':') {
                char ch0 = Character.toUpperCase(str.charAt(0));
                if (ch0 >= 'A' && ch0 <= 'Z') {
                    str = "/" + str;
                }
            }
            // change "//blah" to "file://blah"
            else if (ch1 == '/' && str.charAt(0) == '/') {
                str = "file:" + str;
            }
        }

        // done
        return str;

    } // fixURI(String):String

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

    /**
     * Fixes Microsoft Windows&reg; specific characters.
     * <p>
     * Details about this common problem can be found at 
     * <a href='http://www.cs.tut.fi/~jkorpela/www/windows-chars.html'>http://www.cs.tut.fi/~jkorpela/www/windows-chars.html</a>
     */
    protected int fixWindowsCharacter(int origChar) {
        /* PATCH: Asgeir Asgeirsson */
        switch(origChar) {
            case 130: return 8218;
            case 131: return 402;
            case 132: return 8222;
            case 133: return 8230;
            case 134: return 8224;
            case 135: return 8225;
            case 136: return 710;
            case 137: return 8240;
            case 138: return 352;
            case 139: return 8249;
            case 140: return 338;
            case 145: return 8216;
            case 146: return 8217;
            case 147: return 8220;
            case 148: return 8221;
            case 149: return 8226;
            case 150: return 8211;
            case 151: return 8212;
            case 152: return 732;
            case 153: return 8482;
            case 154: return 353;
            case 155: return 8250;
            case 156: return 339;
            case 159: return 376;
        }
        return origChar;
    } // fixWindowsCharacter(int):int

    //
    // Protected methods
    //

    // i/o
    /** Reads a single character. */
    protected int read() throws IOException {
    	return fCurrentEntity.read();
    }


    // debugging

    /** Sets the scanner. */
    protected void setScanner(Scanner scanner) {
        fScanner = scanner;
        if (DEBUG_SCANNER) {
            System.out.print("$$$ setScanner(");
            System.out.print(scanner!=null?scanner.getClass().getName():"null");
            System.out.println(");");
        }
    } // setScanner(Scanner)
    
    /** Sets the scanner state. */
    protected void setScannerState(short state) {
        fScannerState = state;
        if (DEBUG_SCANNER_STATE) {
            System.out.print("$$$ setScannerState(");
            switch (fScannerState) {
                case STATE_CONTENT: { System.out.print("STATE_CONTENT"); break; }
                case STATE_MARKUP_BRACKET: { System.out.print("STATE_MARKUP_BRACKET"); break; }
                case STATE_START_DOCUMENT: { System.out.print("STATE_START_DOCUMENT"); break; }
                case STATE_END_DOCUMENT: { System.out.print("STATE_END_DOCUMENT"); break; }
            }
            System.out.println(");");
        }
    } // setScannerState(short)

    // scanning

    /** Scans a DOCTYPE line. */
    protected void scanDoctype() throws IOException {
        String root = null;
        String pubid = null;
        String sysid = null;

        if (skipSpaces()) {
            root = scanName();
            if (root == null) {
                if (fReportErrors) {
                    fErrorReporter.reportError("HTML1014", null);
                }
            }
            else {
                root = modifyName(root, fNamesElems);
            }
            if (skipSpaces()) {
                if (skip("PUBLIC", false)) {
                    skipSpaces();
                    pubid = scanLiteral();
                    if (skipSpaces()) {
                        sysid = scanLiteral();
                    }
                }
                else if (skip("SYSTEM", false)) {
                    skipSpaces();
                    sysid = scanLiteral();
                }
            }
        }
        int c;
        while ((c = fCurrentEntity.read()) != -1) {
            if (c == '<') {
            	fCurrentEntity.rewind();
                break;
            }
            if (c == '>') {
                break;
            }
            if (c == '[') {
                skipMarkup(true);
                break;
            }
        }

        if (fDocumentHandler != null) {
            if (fOverrideDoctype) {
                pubid = fDoctypePubid;
                sysid = fDoctypeSysid;
            }
            fEndLineNumber = fCurrentEntity.getLineNumber();
            fEndColumnNumber = fCurrentEntity.getColumnNumber();
            fEndCharacterOffset = fCurrentEntity.getCharacterOffset();
            fDocumentHandler.doctypeDecl(root, pubid, sysid, locationAugs());
        }

    } // scanDoctype()

    /** Scans a quoted literal. */
    protected String scanLiteral() throws IOException {
        int quote = fCurrentEntity.read();
        if (quote == '\'' || quote == '"') {
            StringBuffer str = new StringBuffer();
            int c;
            while ((c = fCurrentEntity.read()) != -1) {
                if (c == quote) {
                    break;
                }
                if (c == '\r' || c == '\n') {
                	fCurrentEntity.rewind();
                    // NOTE: This collapses newlines to a single space.
                    //       [Q] Is this the right thing to do here? -Ac
                    skipNewlines();
                    str.append(' ');
                }
                else if (c == '<') {
                	fCurrentEntity.rewind();
                    break;
                }
                else {
                    str.append((char)c);
                }
            }
            if (c == -1) {
                if (fReportErrors) {
                    fErrorReporter.reportError("HTML1007", null);
                }
                throw new EOFException();
            }
            return str.toString();
        }
        else {
        	fCurrentEntity.rewind();
        }
        return null;
    } // scanLiteral():String

    /** Scans a name. */
    protected String scanName() throws IOException {
        fCurrentEntity.debugBufferIfNeeded("(scanName: ");
        if (fCurrentEntity.offset == fCurrentEntity.length) {
            if (fCurrentEntity.load(0) == -1) {
                fCurrentEntity.debugBufferIfNeeded(")scanName: ");
                return null;
            }
        }
        int offset = fCurrentEntity.offset;
        while (true) {
            while (fCurrentEntity.hasNext()) {
                char c = fCurrentEntity.getNextChar();
                if (!Character.isLetterOrDigit(c) &&
                    !(c == '-' || c == '.' || c == ':' || c == '_')) {
                	fCurrentEntity.rewind();
                    break;
                }
            }
            if (fCurrentEntity.offset == fCurrentEntity.length) {
                int length = fCurrentEntity.length - offset;
                System.arraycopy(fCurrentEntity.buffer, offset, fCurrentEntity.buffer, 0, length);
                int count = fCurrentEntity.load(length);
                offset = 0;
                if (count == -1) {
                    break;
                }
            }
            else {
                break;
            }
        }
        int length = fCurrentEntity.offset - offset;
        String name = length > 0 ? new String(fCurrentEntity.buffer, offset, length) : null;
        fCurrentEntity.debugBufferIfNeeded(")scanName: ", " -> \"" + name + '"');
        return name;
    } // scanName():String

    /** Scans an entity reference. */
    protected int scanEntityRef(final XMLStringBuffer str, final boolean content) 
        throws IOException {
        str.clear();
        str.append('&');
        boolean endsWithSemicolon = false;
        while (true) {
            int c = fCurrentEntity.read();
            if (c == ';') {
                str.append(';');
                endsWithSemicolon = true;
                break;
            }
            else if (c == -1) {
            	break;
            }
            else if (!ENTITY_CHARS.get(c) && c != '#') {
            	fCurrentEntity.rewind();
                break;
            }
            str.append((char)c);
        }

        if (!endsWithSemicolon) {
            if (fReportErrors) {
                fErrorReporter.reportWarning("HTML1004", null);
            }
        }
        if (str.length == 1) {
            if (content && fDocumentHandler != null && fElementCount >= fElementDepth) {
                fEndLineNumber = fCurrentEntity.getLineNumber();
                fEndColumnNumber = fCurrentEntity.getColumnNumber();
                fEndCharacterOffset = fCurrentEntity.getCharacterOffset();
                fDocumentHandler.characters(str, locationAugs());
            }
            return -1;
        }

        final String name;
        if (endsWithSemicolon)
        	name = str.toString().substring(1, str.length -1);
        else
        	name = str.toString().substring(1);

        if (name.startsWith("#")) {
            int value = -1;
            try {
                if (name.startsWith("#x") || name.startsWith("#X")) {
                    value = Integer.parseInt(name.substring(2), 16);
                }
                else {
                    value = Integer.parseInt(name.substring(1));
                }
                /* PATCH: Asgeir Asgeirsson */
                if (fFixWindowsCharRefs && fIso8859Encoding) {
                    value = fixWindowsCharacter(value);
                }
                if (content && fDocumentHandler != null && fElementCount >= fElementDepth) {
                    fEndLineNumber = fCurrentEntity.getLineNumber();
                    fEndColumnNumber = fCurrentEntity.getColumnNumber();
                    fEndCharacterOffset = fCurrentEntity.getCharacterOffset();
                    if (fNotifyCharRefs) {
                        XMLResourceIdentifier id = resourceId();
                        String encoding = null;
                        fDocumentHandler.startGeneralEntity(name, id, encoding, locationAugs());
                    }
                    str.clear();
                    str.append((char)value);
                    fDocumentHandler.characters(str, locationAugs());
                    if (fNotifyCharRefs) {
                        fDocumentHandler.endGeneralEntity(name, locationAugs());
                    }
                }
            }
            catch (NumberFormatException e) {
                if (fReportErrors) {
                    fErrorReporter.reportError("HTML1005", new Object[]{name});
                }
                if (content && fDocumentHandler != null && fElementCount >= fElementDepth) {
                    fEndLineNumber = fCurrentEntity.getLineNumber();
                    fEndColumnNumber = fCurrentEntity.getColumnNumber();
                    fEndCharacterOffset = fCurrentEntity.getCharacterOffset();
                    fDocumentHandler.characters(str, locationAugs());
                }
            }
            return value;
        }

        int c = HTMLEntities.get(name);
        // in attributes, some incomplete entities should be recognized, not all
        // TODO: investigate to find which ones (there are differences between browsers)
        // in a first time, consider only those that behave the same in FF and IE 
        final boolean invalidEntityInAttribute = !content && !endsWithSemicolon && c > 256;
        if (c == -1 || invalidEntityInAttribute) {
            if (fReportErrors) {
                fErrorReporter.reportWarning("HTML1006", new Object[]{name});
            }
            if (content && fDocumentHandler != null && fElementCount >= fElementDepth) {
                fEndLineNumber = fCurrentEntity.getLineNumber();
                fEndColumnNumber = fCurrentEntity.getColumnNumber();
                fEndCharacterOffset = fCurrentEntity.getCharacterOffset();
                fDocumentHandler.characters(str, locationAugs());
            }
            return -1;
        }
        if (content && fDocumentHandler != null && fElementCount >= fElementDepth) {
            fEndLineNumber = fCurrentEntity.getLineNumber();
            fEndColumnNumber = fCurrentEntity.getColumnNumber();
            fEndCharacterOffset = fCurrentEntity.getCharacterOffset();
            boolean notify = fNotifyHtmlBuiltinRefs || (fNotifyXmlBuiltinRefs && builtinXmlRef(name));
            if (notify) {
                XMLResourceIdentifier id = resourceId();
                String encoding = null;
                fDocumentHandler.startGeneralEntity(name, id, encoding, locationAugs());
            }
            str.clear();
            str.append((char)c);
            fDocumentHandler.characters(str, locationAugs());
            if (notify) {
                fDocumentHandler.endGeneralEntity(name, locationAugs());
            }
        }
        return c;

    } // scanEntityRef(XMLStringBuffer,boolean):int

    /** Returns true if the specified text is present and is skipped. */
    protected boolean skip(String s, boolean caseSensitive) throws IOException {
        int length = s != null ? s.length() : 0;
        for (int i = 0; i < length; i++) {
            if (fCurrentEntity.offset == fCurrentEntity.length) {
                System.arraycopy(fCurrentEntity.buffer, fCurrentEntity.offset - i, fCurrentEntity.buffer, 0, i);
                if (fCurrentEntity.load(i) == -1) {
                    fCurrentEntity.offset = 0;
                    return false;
                }
            }
            char c0 = s.charAt(i);
            char c1 = fCurrentEntity.getNextChar();
            if (!caseSensitive) {
                c0 = Character.toUpperCase(c0);
                c1 = Character.toUpperCase(c1);
            }
            if (c0 != c1) {
            	fCurrentEntity.rewind(i + 1);
                return false;
            }
        }
        return true;
    } // skip(String):boolean

    /** Skips markup. */
    protected boolean skipMarkup(boolean balance) throws IOException {
        fCurrentEntity.debugBufferIfNeeded("(skipMarkup: ");
        int depth = 1;
        boolean slashgt = false;
        OUTER: while (true) {
            if (fCurrentEntity.offset == fCurrentEntity.length) {
                if (fCurrentEntity.load(0) == -1) {
                    break OUTER;
                }
            }
            while (fCurrentEntity.hasNext()) {
                char c = fCurrentEntity.getNextChar();
                if (balance && c == '<') {
                    depth++;
                }
                else if (c == '>') {
                    depth--;
                    if (depth == 0) {
                        break OUTER;
                    }
                }
                else if (c == '/') {
                    if (fCurrentEntity.offset == fCurrentEntity.length) {
                        if (fCurrentEntity.load(0) == -1) {
                            break OUTER;
                        }
                    }
                    c = fCurrentEntity.getNextChar();
                    if (c == '>') {
                        slashgt = true;
                        depth--;
                        if (depth == 0) {
                            break OUTER;
                        }
                    }
                    else {
                    	fCurrentEntity.rewind();
                    }
                }
                else if (c == '\r' || c == '\n') {
                	fCurrentEntity.rewind();
                    skipNewlines();
                }
            }
        }
        fCurrentEntity.debugBufferIfNeeded(")skipMarkup: ", " -> "+slashgt);
        return slashgt;
    } // skipMarkup():boolean

    /** Skips whitespace. */
    protected boolean skipSpaces() throws IOException {
        fCurrentEntity.debugBufferIfNeeded("(skipSpaces: ");
        boolean spaces = false;
        while (true) {
            if (fCurrentEntity.offset == fCurrentEntity.length) {
                if (fCurrentEntity.load(0) == -1) {
                    break;
                }
            }
            char c = fCurrentEntity.getNextChar();
            if (!Character.isWhitespace(c)) {
            	fCurrentEntity.rewind();
                break;
            }
            spaces = true;
            if (c == '\r' || c == '\n') {
            	fCurrentEntity.rewind();
                skipNewlines();
                continue;
            }
        }
        fCurrentEntity.debugBufferIfNeeded(")skipSpaces: ", " -> " + spaces);
        return spaces;
    } // skipSpaces()

    /** Skips newlines and returns the number of newlines skipped. */
    protected int skipNewlines() throws IOException {
        fCurrentEntity.debugBufferIfNeeded("(skipNewlines: ");

        if (!fCurrentEntity.hasNext()) {
            if (fCurrentEntity.load(0) == -1) {
                fCurrentEntity.debugBufferIfNeeded(")skipNewlines: ");
                return 0;
            }
        }
        char c = fCurrentEntity.getCurrentChar();
        int newlines = 0;
        int offset = fCurrentEntity.offset;
        if (c == '\n' || c == '\r') {
            do {
                c = fCurrentEntity.getNextChar();
                if (c == '\r') {
                    newlines++;
                    if (fCurrentEntity.offset == fCurrentEntity.length) {
                        offset = 0;
                        fCurrentEntity.offset = newlines;
                        if (fCurrentEntity.load(newlines) == -1) {
                            break;
                        }
                    }
                    if (fCurrentEntity.getCurrentChar() == '\n') {
                        fCurrentEntity.offset++;
                        fCurrentEntity.characterOffset_++;
                        offset++;
                    }
                }
                else if (c == '\n') {
                    newlines++;
                    if (fCurrentEntity.offset == fCurrentEntity.length) {
                        offset = 0;
                        fCurrentEntity.offset = newlines;
                        if (fCurrentEntity.load(newlines) == -1) {
                            break;
                        }
                    }
                }
                else {
                    fCurrentEntity.rewind();
                    break;
                }
            } while (fCurrentEntity.offset < fCurrentEntity.length - 1);
            fCurrentEntity.incLine(newlines);
        }
        fCurrentEntity.debugBufferIfNeeded(")skipNewlines: ", " -> " + newlines);
        return newlines;
    } // skipNewlines(int):int

    // infoset utility methods

    /** Returns an augmentations object with a location item added. */
    protected final Augmentations locationAugs() {
        HTMLAugmentations augs = null;
        if (fAugmentations) {
            fLocationItem.setValues(fBeginLineNumber, fBeginColumnNumber, 
                                    fBeginCharacterOffset, fEndLineNumber,
                                    fEndColumnNumber, fEndCharacterOffset);
            augs = fInfosetAugs;
            augs.removeAllItems();
            augs.putItem(AUGMENTATIONS, fLocationItem);
        }
        return augs;
    } // locationAugs():Augmentations

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

    /** Returns an empty resource identifier. */
    protected final XMLResourceIdentifier resourceId() {
        /***/
        fResourceId.clear();
        return fResourceId;
        /***
        // NOTE: Unfortunately, the Xerces DOM parser classes expect a
        //       non-null resource identifier object to be passed to
        //       startGeneralEntity. -Ac
        return null;
        /***/
    } // resourceId():XMLResourceIdentifier

    //
    // Protected static methods
    //

    /** Returns true if the name is a built-in XML general entity reference. */
    protected static boolean builtinXmlRef(String name) {
        return name.equals("amp") || name.equals("lt") || name.equals("gt") ||
               name.equals("quot") || name.equals("apos");
    } // builtinXmlRef(String):boolean

    //
    // Private methods
    //

    //
    // Interfaces
    //

    /**
     * Basic scanner interface.
     *
     * @author Andy Clark
     */
    public interface Scanner {

        //
        // Scanner methods
        //

        /** 
         * Scans part of the document. This interface allows scanning to
         * be performed in a pulling manner.
         *
         * @param complete True if the scanner should not return until
         *                 scanning is complete.
         *
         * @return True if additional scanning is required.
         *
         * @throws IOException Thrown if I/O error occurs.
         */
        public boolean scan(boolean complete) throws IOException;

    } // interface Scanner

    //
    // Classes
    //

    /**
     * Current entity.
     *
     * @author Andy Clark
     */
    public static class CurrentEntity {

        //
        // Data
        //

        /** Character stream. */
        private Reader stream_;

        /** Encoding. */
        public final String encoding;

        /** Public identifier. */
        public final String publicId;

        /** Base system identifier. */
        public final String baseSystemId;

        /** Literal system identifier. */
        public final String literalSystemId;

        /** Expanded system identifier. */
        public final String expandedSystemId;

		/** XML version. */
		public final String version = "1.0";

        /** Line number. */
        private int lineNumber_ = 1;

        /** Column number. */
        private int columnNumber_ = 1;
        
        /** Character offset in the file. */
        public int characterOffset_ = 0;

        // buffer

        /** Character buffer. */
        public char[] buffer = new char[DEFAULT_BUFFER_SIZE];

        /** Offset into character buffer. */
        public int offset = 0;

        /** Length of characters read into character buffer. */
        public int length = 0;
        
        private boolean endReached_ = false;

        //
        // Constructors
        //

        /** Constructs an entity from the specified stream. */
        public CurrentEntity(Reader stream, String encoding, 
                             String publicId, String baseSystemId,
                             String literalSystemId, String expandedSystemId) {
            stream_ = stream;
            this.encoding = encoding;
            this.publicId = publicId;
            this.baseSystemId = baseSystemId;
            this.literalSystemId = literalSystemId;
            this.expandedSystemId = expandedSystemId;
        } // <init>(Reader,String,String,String,String)

		private char getCurrentChar() {
        	return buffer[offset];
        }

        /**
         * Gets the current character and moves to next one.
         * @return
         */
        private char getNextChar() {
	        characterOffset_++;
	        columnNumber_++;
        	return buffer[offset++];
        }
        private void closeQuietly() {
            try {
                stream_.close();
            }
            catch (IOException e) {
                // ignore
            }
		}

		/**
         * Indicates if there are characters left.
         */
        boolean hasNext() {
        	return offset < length;        	
        }

        /** 
         * Loads a new chunk of data into the buffer and returns the number of
         * characters loaded or -1 if no additional characters were loaded.
         *
         * @param offset The offset at which new characters should be loaded.
         */
        protected int load(int offset) throws IOException {
            debugBufferIfNeeded("(load: ");
            // resize buffer, if needed
            if (offset == buffer.length) {
                int adjust = buffer.length / 4;
                char[] array = new char[buffer.length + adjust];
                System.arraycopy(buffer, 0, array, 0, length);
                buffer = array;
            }
            // read a block of characters
            int count = stream_.read(buffer, offset, buffer.length - offset);
            if (count == -1) {
            	endReached_ = true;
            }
            length = count != -1 ? count + offset : offset;
            this.offset = offset;
            debugBufferIfNeeded(")load: ", " -> " + count);
            return count;
        } // load():int

        /** Reads a single character. */
        protected int read() throws IOException {
            debugBufferIfNeeded("(read: ");
            if (offset == length) {
                if (endReached_) {
                	return -1;
                }
                if (load(0) == -1) {
                    if (DEBUG_BUFFER) { 
                        System.out.println(")read: -> -1");
                    }
                    return -1;
                }
            }
            final char c = buffer[offset++];
	        characterOffset_++;
	        columnNumber_++;

        	debugBufferIfNeeded(")read: ", " -> " + c);
            return c;
        } // read():int

        /** Prints the contents of the character buffer to standard out. */
        private void debugBufferIfNeeded(final String prefix) {
        	debugBufferIfNeeded(prefix, "");
        }
        /** Prints the contents of the character buffer to standard out. */
        private void debugBufferIfNeeded(final String prefix, final String suffix) {
            if (DEBUG_BUFFER) {
                System.out.print(prefix);
                System.out.print('[');
                System.out.print(length);
                System.out.print(' ');
                System.out.print(offset);
                if (length > 0) {
                    System.out.print(" \"");
                    for (int i = 0; i < length; i++) {
                        if (i == offset) {
                            System.out.print('^');
                        }
                        char c = buffer[i];
                        switch (c) {
                            case '\r': {
                                System.out.print("\\r");
                                break;
                            }
                            case '\n': {
                                System.out.print("\\n");
                                break;
                            }
                            case '\t': {
                                System.out.print("\\t");
                                break;
                            }
                            case '"': {
                                System.out.print("\\\"");
                                break;
                            }
                            default: {
                                System.out.print(c);
                            }
                        }
                    }
                    if (offset == length) {
                        System.out.print('^');
                    }
                    System.out.print('"');
                }
                System.out.print(']');
                System.out.print(suffix);
                System.out.println();
            }
        } // printBuffer()

		private void setStream(final InputStreamReader inputStreamReader) {
            stream_ = inputStreamReader;
            offset = length = characterOffset_ = 0;
            lineNumber_ = columnNumber_ = 1;
		}
		
		/**
		 * Goes back, cancelling the effect of the previous read() call.
		 */
		private void rewind() {
	        offset--;
	        characterOffset_--;
	        columnNumber_--;
		}
        private void rewind(int i) {
            offset -= i;
            characterOffset_ -= i;
            columnNumber_ -= i;
		}

		private void incLine() {
            lineNumber_++;
            columnNumber_ = 1;
		}

		private void incLine(int nbLines) {
            lineNumber_ += nbLines;
            columnNumber_ = 1;
		}

		public int getLineNumber() {
			return lineNumber_;
		}

		private void resetBuffer(final XMLStringBuffer buffer, final int lineNumber,
				final int columnNumber, final int characterOffset) {
        	lineNumber_ = lineNumber;
        	columnNumber_ = columnNumber;
        	this.characterOffset_ = characterOffset;
        	this.buffer = buffer.ch;
        	this.offset = buffer.offset;
        	this.length = buffer.length;
		}

		private int getColumnNumber() {
			return columnNumber_;
		}

		private void restorePosition(int originalOffset,
				int originalColumnNumber, int originalCharacterOffset) {
	        this.offset = originalOffset;
	        this.columnNumber_ = originalColumnNumber;
	        this.characterOffset_ = originalCharacterOffset;
		}

		private int getCharacterOffset() {
			return characterOffset_;
		}
    } // class CurrentEntity

    /**
     * The primary HTML document scanner.
     *
     * @author Andy Clark
     */
    public class ContentScanner 
        implements Scanner {

        //
        // Data
        //

        // temp vars

        /** A qualified name. */
        private final QName fQName = new QName();

        /** Attributes. */
        private final XMLAttributesImpl fAttributes = new XMLAttributesImpl();

        //
        // Scanner methods
        //

        /** Scan. */
        public boolean scan(boolean complete) throws IOException {
            boolean next;
            do {
                try {
                    next = false;
                    switch (fScannerState) {
                        case STATE_CONTENT: {
                            fBeginLineNumber = fCurrentEntity.getLineNumber();
                            fBeginColumnNumber = fCurrentEntity.getColumnNumber();
                            fBeginCharacterOffset = fCurrentEntity.getCharacterOffset();
                            int c = fCurrentEntity.read();
                            if (c == '<') {
                                setScannerState(STATE_MARKUP_BRACKET);
                                next = true;
                            }
                            else if (c == '&') {
                                scanEntityRef(fStringBuffer, true);
                            }
                            else if (c == -1) {
                                throw new EOFException();
                            }
                            else {
                            	fCurrentEntity.rewind();
                                scanCharacters();
                            }
                            break;
                        }
                        case STATE_MARKUP_BRACKET: {
                            int c = fCurrentEntity.read();
                            if (c == '!') {
                                if (skip("--", false)) {
                                    scanComment();
                                }
                                else if (skip("[CDATA[", false)) {
                                    scanCDATA();
                                }
                                else if (skip("DOCTYPE", false)) {
                                    scanDoctype();
                                }
                                else {
                                    if (fReportErrors) {
                                        fErrorReporter.reportError("HTML1002", null);
                                    }
                                    skipMarkup(true);
                                }
                            }
                            else if (c == '?') {
                                scanPI();
                            }
                            else if (c == '/') {
                                scanEndElement();
                            }
                            else if (c == -1) {
                                if (fReportErrors) {
                                    fErrorReporter.reportError("HTML1003", null);
                                }
                                if (fDocumentHandler != null && fElementCount >= fElementDepth) {
                                    fStringBuffer.clear();
                                    fStringBuffer.append('<');
                                    fDocumentHandler.characters(fStringBuffer, null);
                                }
                                throw new EOFException();
                            }
                            else {
                            	fCurrentEntity.rewind();
                                fElementCount++;
                                fSingleBoolean[0] = false;
                                final String ename = scanStartElement(fSingleBoolean);
                                fBeginLineNumber = fCurrentEntity.getLineNumber();
                                fBeginColumnNumber = fCurrentEntity.getColumnNumber();
                                fBeginCharacterOffset = fCurrentEntity.getCharacterOffset();
                                if ("script".equalsIgnoreCase(ename)) {
                                	scanScriptContent();
                                }
                                else if (!fParseNoScriptContent && "noscript".equalsIgnoreCase(ename)) {
                                	scanNoXxxContent("noscript");
                                }
                                else if (!fParseNoFramesContent && "noframes".equalsIgnoreCase(ename)) {
                                	scanNoXxxContent("noframes");
                                }
                                else if (ename != null && !fSingleBoolean[0] 
                                    && HTMLElements.getElement(ename).isSpecial() 
                                    && (!ename.equalsIgnoreCase("TITLE") || isEnded(ename))) {
                                    setScanner(fSpecialScanner.setElementName(ename));
                                    setScannerState(STATE_CONTENT);
                                    return true;
                                }
                            }
                            setScannerState(STATE_CONTENT);
                            break;
                        }
                        case STATE_START_DOCUMENT: {
                            if (fDocumentHandler != null && fElementCount >= fElementDepth) {
                                if (DEBUG_CALLBACKS) {
                                    System.out.println("startDocument()");
                                }
                                XMLLocator locator = HTMLScanner.this;
                                String encoding = fIANAEncoding;
                                Augmentations augs = locationAugs();
                                NamespaceContext nscontext = new NamespaceSupport();
                                XercesBridge.getInstance().XMLDocumentHandler_startDocument(fDocumentHandler, locator, encoding, nscontext, augs);
                            }
                            if (fInsertDoctype && fDocumentHandler != null) {
                                String root = HTMLElements.getElement(HTMLElements.HTML).name;
                                root = modifyName(root, fNamesElems);
                                String pubid = fDoctypePubid;
                                String sysid = fDoctypeSysid;
                                fDocumentHandler.doctypeDecl(root, pubid, sysid,
                                                             synthesizedAugs());
                            }
                            setScannerState(STATE_CONTENT);
                            break;
                        }
                        case STATE_END_DOCUMENT: {
                            if (fDocumentHandler != null && fElementCount >= fElementDepth && complete) {
                                if (DEBUG_CALLBACKS) {
                                    System.out.println("endDocument()");
                                }
                                fEndLineNumber = fCurrentEntity.getLineNumber();
                                fEndColumnNumber = fCurrentEntity.getColumnNumber();
                                fEndCharacterOffset = fCurrentEntity.getCharacterOffset();
                                fDocumentHandler.endDocument(locationAugs());
                            }
                            return false;
                        }
                        default: {
                            throw new RuntimeException("unknown scanner state: "+fScannerState);
                        }
                    }
                }
                catch (EOFException e) {
                    if (fCurrentEntityStack.empty()) {
                        setScannerState(STATE_END_DOCUMENT);
                    }
                    else {
                        fCurrentEntity = (CurrentEntity)fCurrentEntityStack.pop();
                    }
                    next = true;
                }
            } while (next || complete);
            return true;
        } // scan(boolean):boolean

        /**
         * Scans the content of <noscript>: it doesn't get parsed but is considered as plain text
         * when feature {@link HTMLScanner#PARSE_NOSCRIPT_CONTENT} is set to false.
         * @param the tag for which content is scanned (one of "noscript" or "noframes")
         * @throws IOException
         */
        private void scanNoXxxContent(final String tagName) throws IOException {
        	final XMLStringBuffer buffer = new XMLStringBuffer();
        	final String end = "/" + tagName;
        	
            while (true) {
                int c = fCurrentEntity.read();
                if (c == -1) {
                    break;
                }
                if (c == '<') {
                	final String next = nextContent(10) + " ";
                	if (next.length() >= 10 && end.equalsIgnoreCase(next.substring(0, end.length()))
            			&& ('>' == next.charAt(9) || Character.isWhitespace(next.charAt(9)))) {
                		fCurrentEntity.rewind();
	                    break;
                	}
            	}
            	if (c == '\r' || c == '\n') {
            		fCurrentEntity.rewind();
                    int newlines = skipNewlines();
                    for (int i = 0; i < newlines; i++) {
                        buffer.append('\n');
                    }
                }
                else {
                    buffer.append((char)c);
                }
            }
            if (buffer.length > 0 && fDocumentHandler != null) {
                fEndLineNumber = fCurrentEntity.getLineNumber();
                fEndColumnNumber = fCurrentEntity.getColumnNumber();
                fEndCharacterOffset = fCurrentEntity.getCharacterOffset();
                fDocumentHandler.characters(buffer, locationAugs());
            }
        }
        
        private void scanScriptContent() throws IOException {

        	final XMLStringBuffer buffer = new XMLStringBuffer();
            boolean waitForEndComment = false;
            while (true) {
                int c = fCurrentEntity.read();
                if (c == -1) {
                    break;
                }
                else if (c == '-' && endsWith(buffer, "<!-"))
            	{
            		waitForEndComment = endCommentAvailable();
            	}
                else if (!waitForEndComment && c == '<') {
                	final String next = nextContent(8) + " ";
                	if (next.length() >= 8 && "/script".equalsIgnoreCase(next.substring(0, 7))
                			&& ('>' == next.charAt(7) || Character.isWhitespace(next.charAt(7)))) {
                		fCurrentEntity.rewind();
                        break;
                	}
                }
                else if (c == '>' && endsWith(buffer, "--"))  {
               		waitForEndComment = false;
                }

                if (c == '\r' || c == '\n') {
                	fCurrentEntity.rewind();
                    int newlines = skipNewlines();
                    for (int i = 0; i < newlines; i++) {
                        buffer.append('\n');
                    }
                }
                else {
                    buffer.append((char)c);
                }
            }

            if (fScriptStripCommentDelims) {
            	reduceToContent(buffer, "<!--", "-->");
            }
            if (fScriptStripCDATADelims) {
            	reduceToContent(buffer, "<![CDATA[", "]]>");
            }

            if (buffer.length > 0 && fDocumentHandler != null && fElementCount >= fElementDepth) {
                if (DEBUG_CALLBACKS) {
                    System.out.println("characters("+buffer+")");
                }
                fEndLineNumber = fCurrentEntity.getLineNumber();
                fEndColumnNumber = fCurrentEntity.getColumnNumber();
                fEndCharacterOffset = fCurrentEntity.getCharacterOffset();
                fDocumentHandler.characters(buffer, locationAugs());
            }
        }

        
        /**
         * Reads the next characters WITHOUT impacting the buffer content
         * up to current offset.
         * @param len the number of characters to read
         * @return the read string (length may be smaller if EOF is encountered)
         */
        protected String nextContent(int len) throws IOException {
            final int originalOffset = fCurrentEntity.offset;
            final int originalColumnNumber = fCurrentEntity.getColumnNumber();
            final int originalCharacterOffset = fCurrentEntity.getCharacterOffset();
            
            char[] buff = new char[len];
            int nbRead = 0;
            for (nbRead=0; nbRead<len; ++nbRead) {
    			// read() should not clear the buffer
    	        if (fCurrentEntity.offset == fCurrentEntity.length) {
    	        	if (fCurrentEntity.length == fCurrentEntity.buffer.length) {
    	        		fCurrentEntity.load(fCurrentEntity.buffer.length);
    	        	}
    	        	else { // everything was already loaded
    	        		break;
    	        	}
    	        }
    	        
    	        int c = fCurrentEntity.read();
    	        if (c == -1) {
    	        	break;
    	        }
    	        else {
    	        	buff[nbRead] = (char) c;
    	        }
    		}
	        fCurrentEntity.restorePosition(originalOffset, originalColumnNumber, originalCharacterOffset);
	        return new String(buff, 0, nbRead);
    	}

		//
        // Protected methods
        //

        /** Scans characters. */
        protected void scanCharacters() throws IOException {
            fCurrentEntity.debugBufferIfNeeded("(scanCharacters: ");
            fStringBuffer.clear();  
            while(true) { 
               int newlines = skipNewlines();
               if (newlines == 0 && fCurrentEntity.offset == fCurrentEntity.length) {
                    fCurrentEntity.debugBufferIfNeeded(")scanCharacters: ");
                    break;
                }
                char c;
                int offset = fCurrentEntity.offset - newlines;
                for (int i = offset; i < fCurrentEntity.offset; i++) {
                    fCurrentEntity.buffer[i] = '\n';
                }
                while (fCurrentEntity.hasNext()) {
                    c = fCurrentEntity.getNextChar();
                    if (c == '<' || c == '&' || c == '\n' || c == '\r') {
                    	fCurrentEntity.rewind();
                        break;
                    }
                }
                if (fCurrentEntity.offset > offset && 
                    fDocumentHandler != null && fElementCount >= fElementDepth) {
                    if (DEBUG_CALLBACKS) {
                    	final XMLString xmlString = new XMLString(fCurrentEntity.buffer, offset, fCurrentEntity.offset - offset);
                        System.out.println("characters(" + xmlString + ")");
                    }
                    fEndLineNumber = fCurrentEntity.getLineNumber();
                    fEndColumnNumber = fCurrentEntity.getColumnNumber();
                    fEndCharacterOffset = fCurrentEntity.getCharacterOffset();
                    fStringBuffer.append(fCurrentEntity.buffer, offset, fCurrentEntity.offset - offset);
                }
                fCurrentEntity.debugBufferIfNeeded(")scanCharacters: ");

                boolean hasNext = fCurrentEntity.offset  < fCurrentEntity.buffer.length;
                int next = hasNext ? fCurrentEntity.getCurrentChar() : -1; 
                
                if(next == '&' || next == '<' || next == -1) {
                     break;
                 }

            } //end while

            if(fStringBuffer.length != 0) {
                fDocumentHandler.characters(fStringBuffer, locationAugs());
            }

        } // scanCharacters()

        /** Scans a CDATA section. */
        protected void scanCDATA() throws IOException {
            fCurrentEntity.debugBufferIfNeeded("(scanCDATA: ");
            fStringBuffer.clear();
            if (fCDATASections) {
                if (fDocumentHandler != null && fElementCount >= fElementDepth) {
                    fEndLineNumber = fCurrentEntity.getLineNumber();
                    fEndColumnNumber = fCurrentEntity.getColumnNumber();
                    fEndCharacterOffset = fCurrentEntity.getCharacterOffset();
                    if (DEBUG_CALLBACKS) {
                        System.out.println("startCDATA()");
                    }
                    fDocumentHandler.startCDATA(locationAugs());
                }
            }
            else {
                fStringBuffer.append("[CDATA[");
            }
            boolean eof = scanMarkupContent(fStringBuffer, ']');
            if (!fCDATASections) {
                fStringBuffer.append("]]");
            }
            if (fDocumentHandler != null && fElementCount >= fElementDepth) {
                fEndLineNumber = fCurrentEntity.getLineNumber();
                fEndColumnNumber = fCurrentEntity.getColumnNumber();
                fEndCharacterOffset = fCurrentEntity.getCharacterOffset();
                if (fCDATASections) {
                    if (DEBUG_CALLBACKS) {
                        System.out.println("characters("+fStringBuffer+")");
                    }
                    fDocumentHandler.characters(fStringBuffer, locationAugs());
                    if (DEBUG_CALLBACKS) {
                        System.out.println("endCDATA()");
                    }
                    fDocumentHandler.endCDATA(locationAugs());
                }
                else {
                    if (DEBUG_CALLBACKS) {
                        System.out.println("comment("+fStringBuffer+")");
                    }
                    fDocumentHandler.comment(fStringBuffer, locationAugs());
                }
            }
            fCurrentEntity.debugBufferIfNeeded(")scanCDATA: ");
            if (eof) {
                throw new EOFException();
            }
        } // scanCDATA()
        
        /** Scans a comment. */
        protected void scanComment() throws IOException {
            fCurrentEntity.debugBufferIfNeeded("(scanComment: ");
            fEndLineNumber = fCurrentEntity.getLineNumber();
            fEndColumnNumber = fCurrentEntity.getColumnNumber();
            fEndCharacterOffset = fCurrentEntity.getCharacterOffset();
        	XMLStringBuffer buffer = new XMLStringBuffer();
            boolean eof = scanMarkupContent(buffer, '-');
            // no --> found, comment with end only with >
            if (eof) {
            	fCurrentEntity.resetBuffer(buffer, fEndLineNumber, fEndColumnNumber, fEndCharacterOffset);
            	buffer = new XMLStringBuffer(); // take a new one to avoid interactions
            	while (true) {
            		int c = fCurrentEntity.read();
                    if (c == -1) {
                        if (fReportErrors) {
                            fErrorReporter.reportError("HTML1007", null);
                        }
                        eof = true;
                        break;
                    }
                    else if (c != '>') {
            			buffer.append((char)c);
                        continue;
            		}
            		else if (c == '\n' || c == '\r') {
            			fCurrentEntity.rewind();
	                    int newlines = skipNewlines();
	                    for (int i = 0; i < newlines; i++) {
	                    	buffer.append('\n');
	                    }
	                    continue;
	                }
                    eof = false;
            		break;
            	}
            }
            if (fDocumentHandler != null && fElementCount >= fElementDepth) {
                if (DEBUG_CALLBACKS) {
                    System.out.println("comment(" + buffer + ")");
                }
                fEndLineNumber = fCurrentEntity.getLineNumber();
                fEndColumnNumber = fCurrentEntity.getColumnNumber();
                fEndCharacterOffset = fCurrentEntity.getCharacterOffset();
                fDocumentHandler.comment(buffer, locationAugs());
            }
            fCurrentEntity.debugBufferIfNeeded(")scanComment: ");
            if (eof) {
                throw new EOFException();
            }
        } // scanComment()

        /** Scans markup content. */
        protected boolean scanMarkupContent(XMLStringBuffer buffer, 
                                            char cend) throws IOException {
            int c = -1;
            OUTER: while (true) {
                c = fCurrentEntity.read();
                if (c == cend) {
                    int count = 1;
                    while (true) {
                        c = fCurrentEntity.read();
                        if (c == cend) {
                            count++;
                            continue;
                        }
                        break;
                    }
                    if (c == -1) {
                        if (fReportErrors) {
                            fErrorReporter.reportError("HTML1007", null);
                        }
                        break OUTER;
                    }
                    if (count < 2) {
                        buffer.append(cend);
                        //if (c != -1) {
                        fCurrentEntity.rewind();
                        //}
                        continue;
                    }
                    if (c != '>') {
                        for (int i = 0; i < count; i++) {
                            buffer.append(cend);
                        }
                        fCurrentEntity.rewind();
                        continue;
                    }
                    for (int i = 0; i < count - 2; i++) {
                        buffer.append(cend);
                    }
                    break;
                }
                else if (c == '\n' || c == '\r') {
                	fCurrentEntity.rewind();
                    int newlines = skipNewlines();
                    for (int i = 0; i < newlines; i++) {
                        buffer.append('\n');
                    }
                    continue;
                }
                else if (c == -1) {
                    if (fReportErrors) {
                        fErrorReporter.reportError("HTML1007", null);
                    }
                    break;
                }
                buffer.append((char)c);
            }
            return c == -1;
        } // scanMarkupContent(XMLStringBuffer,char):boolean

        /** Scans a processing instruction. */
        protected void scanPI() throws IOException {
            fCurrentEntity.debugBufferIfNeeded("(scanPI: ");
            if (fReportErrors) {
                fErrorReporter.reportWarning("HTML1008", null);
            }

            // scan processing instruction
            String target = scanName();
            if (target != null && !target.equalsIgnoreCase("xml")) {
                while (true) {
                    int c = fCurrentEntity.read();
                    if (c == '\r' || c == '\n') {
                        if (c == '\r') {
                            c = fCurrentEntity.read();
                            if (c != '\n') {
                                fCurrentEntity.offset--;
                                fCurrentEntity.characterOffset_--;
                            }
                        }
                        fCurrentEntity.incLine();
                        continue;
                    }
                    if (c == -1) {
                        break;
                    }
                    if (c != ' ' && c != '\t') {
                    	fCurrentEntity.rewind();
                        break;
                    }
                }
                fStringBuffer.clear();
                while (true) {
                    int c = fCurrentEntity.read();
                    if (c == '?' || c == '/') {
                        char c0 = (char)c;
                        c = fCurrentEntity.read();
                        if (c == '>') {
                            break;
                        }
                        else {
                            fStringBuffer.append(c0);
                            fCurrentEntity.rewind();
                            continue;
                        }
                    }
                    else if (c == '\r' || c == '\n') {
                        fStringBuffer.append('\n');
                        if (c == '\r') {
                            c = fCurrentEntity.read();
                            if (c != '\n') {
                                fCurrentEntity.offset--;
                                fCurrentEntity.characterOffset_--;
                            }
                        }
                        fCurrentEntity.incLine();
                        continue;
                    }
                    else if (c == -1) {
                        break;
                    }
                    else {
                        fStringBuffer.append((char)c);
                    }
                }
                XMLString data = fStringBuffer;
                if (fDocumentHandler != null) {
                    fEndLineNumber = fCurrentEntity.getLineNumber();
                    fEndColumnNumber = fCurrentEntity.getColumnNumber();
                    fEndCharacterOffset = fCurrentEntity.getCharacterOffset();
                    fDocumentHandler.processingInstruction(target, data, locationAugs());
                }
            }

            // scan xml/text declaration
            else {
                int beginLineNumber = fBeginLineNumber;
                int beginColumnNumber = fBeginColumnNumber;
                int beginCharacterOffset = fBeginCharacterOffset;
                fAttributes.removeAllAttributes();
                int aindex = 0;
                while (scanPseudoAttribute(fAttributes)) {
                	// if we haven't scanned a value, remove the entry as values have special signification
                	if (fAttributes.getValue(aindex).length() == 0) {
                		fAttributes.removeAttributeAt(aindex); 
                	}
                	else {
	                    fAttributes.getName(aindex,fQName);
	                    fQName.rawname = fQName.rawname.toLowerCase();
	                    fAttributes.setName(aindex,fQName);
	                    aindex++;
                	}
                }
                if (fDocumentHandler != null) {
                    String version = fAttributes.getValue("version");
                    String encoding = fAttributes.getValue("encoding");
                    String standalone = fAttributes.getValue("standalone");

                    // if the encoding is successfully changed, the stream will be processed again
                    // with the right encoding an we will come here again but without need to change the encoding
                    final boolean xmlDeclNow = fIgnoreSpecifiedCharset || !changeEncoding(encoding);
                    if (xmlDeclNow) {
	                    fBeginLineNumber = beginLineNumber;
	                    fBeginColumnNumber = beginColumnNumber;
	                    fBeginCharacterOffset = beginCharacterOffset;
	                    fEndLineNumber = fCurrentEntity.getLineNumber();
	                    fEndColumnNumber = fCurrentEntity.getColumnNumber();
	                    fEndCharacterOffset = fCurrentEntity.getCharacterOffset();
	                    fDocumentHandler.xmlDecl(version, encoding, standalone,
	                                             locationAugs());
                    }
                }
            }

            fCurrentEntity.debugBufferIfNeeded(")scanPI: ");
        } // scanPI()

        /** 
         * Scans a start element. 
         *
         * @param empty Is used for a second return value to indicate whether
         *              the start element tag is empty (e.g. "/&gt;").
         */
        protected String scanStartElement(boolean[] empty) throws IOException {
            String ename = scanName();
            int length = ename != null ? ename.length() : 0;
            int c = length > 0 ? ename.charAt(0) : -1;
            if (length == 0 || !((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'))) {
                if (fReportErrors) {
                    fErrorReporter.reportError("HTML1009", null);
                }
                if (fDocumentHandler != null && fElementCount >= fElementDepth) {
                    fStringBuffer.clear();
                    fStringBuffer.append('<');
                    if (length > 0) {
                        fStringBuffer.append(ename);
                    }
                    fDocumentHandler.characters(fStringBuffer, null);
                }
                return null;
            }
            ename = modifyName(ename, fNamesElems);
            fAttributes.removeAllAttributes();
            int beginLineNumber = fBeginLineNumber;
            int beginColumnNumber = fBeginColumnNumber;
            int beginCharacterOffset = fBeginCharacterOffset;
            while (scanAttribute(fAttributes, empty)) {
                // do nothing
            }
            fBeginLineNumber = beginLineNumber;
            fBeginColumnNumber = beginColumnNumber;
            fBeginCharacterOffset = beginCharacterOffset;
            if (fByteStream != null && fElementDepth == -1) {
                if (ename.equalsIgnoreCase("META")) {
                    if (DEBUG_CHARSET) {
                        System.out.println("+++ <META>");
                    }
                    String httpEquiv = getValue(fAttributes, "http-equiv");
                    if (httpEquiv != null && httpEquiv.equalsIgnoreCase("content-type")) {
                        if (DEBUG_CHARSET) {
                            System.out.println("+++ @content-type: \""+httpEquiv+'"');
                        }
                        String content = getValue(fAttributes, "content");
                        if (content != null) {
                        	content = removeSpaces(content);
                            int index1 = content.toLowerCase().indexOf("charset=");
                            if (index1 != -1 && !fIgnoreSpecifiedCharset) {
                                final int index2 = content.indexOf(';', index1);
                                final String charset = index2 != -1 ? content.substring(index1+8, index2) : content.substring(index1+8);
                                changeEncoding(charset);
                            }
                        }
                    }
                }
                else if (ename.equalsIgnoreCase("BODY")) {
                    fByteStream.clear();
                    fByteStream = null;
                }
                else {
                     HTMLElements.Element element = HTMLElements.getElement(ename);
                     if (element.parent != null && element.parent.length > 0) {
                         if (element.parent[0].code == HTMLElements.BODY) {
                             fByteStream.clear();
                             fByteStream = null;
                         }
                     }
                }
            }
            if (fDocumentHandler != null && fElementCount >= fElementDepth) {
                fQName.setValues(null, ename, ename, null);
                if (DEBUG_CALLBACKS) {
                    System.out.println("startElement("+fQName+','+fAttributes+")");
                }
                fEndLineNumber = fCurrentEntity.getLineNumber();
                fEndColumnNumber = fCurrentEntity.getColumnNumber();
                fEndCharacterOffset = fCurrentEntity.getCharacterOffset();
                if (empty[0]) {
                    fDocumentHandler.emptyElement(fQName, fAttributes, locationAugs());
                }
                else {
                    fDocumentHandler.startElement(fQName, fAttributes, locationAugs());
                }
            }
            return ename;
        } // scanStartElement():ename

        /**
         * Removes all spaces for the string (remember: JDK 1.3!)
         */
        private String removeSpaces(final String content) {
        	StringBuffer sb = null;
        	for (int i=content.length()-1; i>=0; --i) {
        		if (Character.isWhitespace(content.charAt(i))) {
        			if (sb == null) {
        				sb = new StringBuffer(content);
        			}
        			sb.deleteCharAt(i);
        		}
        	}
			return (sb == null) ? content : sb.toString();
		}

		/**
         * Tries to change the encoding used to read the input stream to the specified one
         * @param charset the charset that should be used
         * @return <code>true</code> when the encoding has been changed
         */
		private boolean changeEncoding(String charset) {
			if (charset == null || fByteStream == null) {
				return false;
			}
			charset = charset.trim();
			boolean encodingChanged = false;
			try {
			    String ianaEncoding = charset;
			    String javaEncoding = EncodingMap.getIANA2JavaMapping(ianaEncoding.toUpperCase());
			    if (DEBUG_CHARSET) {
			        System.out.println("+++ ianaEncoding: "+ianaEncoding);
			        System.out.println("+++ javaEncoding: "+javaEncoding);
			    }
			    if (javaEncoding == null) {
			        javaEncoding = ianaEncoding;
			        if (fReportErrors) {
			            fErrorReporter.reportError("HTML1001", new Object[]{ianaEncoding});
			        }
			    }
			    // patch: Marc Guillemot
			    if (!javaEncoding.equals(fJavaEncoding)) { 
			      	if (!isEncodingCompatible(javaEncoding, fJavaEncoding)) {
			            if (fReportErrors) {
			                fErrorReporter.reportError("HTML1015", new Object[]{javaEncoding,fJavaEncoding});
			            }
			     	}
			  		// change the charset
			     	else {
			            fIso8859Encoding = ianaEncoding == null 
			                    || ianaEncoding.toUpperCase().startsWith("ISO-8859")
			                    || ianaEncoding.equalsIgnoreCase(fDefaultIANAEncoding);
			            fJavaEncoding = javaEncoding;
			            fCurrentEntity.setStream(new InputStreamReader(fByteStream, javaEncoding));
			            fByteStream.playback();
			            fElementDepth = fElementCount;
			            fElementCount = 0;
	                    encodingChanged = true;
			     	}
			     }
			}
			catch (UnsupportedEncodingException e) {
			    if (fReportErrors) {
			        fErrorReporter.reportError("HTML1010", new Object[]{charset});
			    }
			    // NOTE: If the encoding change doesn't work, 
			    //       then there's no point in continuing to 
			    //       buffer the input stream.
			    fByteStream.clear();
			    fByteStream = null;
			}
			return encodingChanged;
		}

        /** 
         * Scans a real attribute. 
         *
         * @param attributes The list of attributes.
         * @param empty      Is used for a second return value to indicate 
         *                   whether the start element tag is empty 
         *                   (e.g. "/&gt;").
         */
        protected boolean scanAttribute(XMLAttributesImpl attributes,
                                        boolean[] empty)
            throws IOException {
            return scanAttribute(attributes,empty,'/');
        } // scanAttribute(XMLAttributesImpl,boolean[]):boolean

        /** 
         * Scans a pseudo attribute. 
         *
         * @param attributes The list of attributes.
         */
        protected boolean scanPseudoAttribute(XMLAttributesImpl attributes)
            throws IOException {
            return scanAttribute(attributes,fSingleBoolean,'?');
        } // scanPseudoAttribute(XMLAttributesImpl):boolean

        /** 
         * Scans an attribute, pseudo or real. 
         *
         * @param attributes The list of attributes.
         * @param empty      Is used for a second return value to indicate 
         *                   whether the start element tag is empty 
         *                   (e.g. "/&gt;").
         * @param endc       The end character that appears before the
         *                   closing angle bracket ('>').
         */
        protected boolean scanAttribute(XMLAttributesImpl attributes,
                                        boolean[] empty, char endc)
            throws IOException {
            boolean skippedSpaces = skipSpaces();
            fBeginLineNumber = fCurrentEntity.getLineNumber();
            fBeginColumnNumber = fCurrentEntity.getColumnNumber();
            fBeginCharacterOffset = fCurrentEntity.getCharacterOffset();
            int c = fCurrentEntity.read();
            if (c == -1) {
                if (fReportErrors) {
                    fErrorReporter.reportError("HTML1007", null);
                }
                return false;
            }
            else if (c == '>') {
                return false;
            }
            else if (c == '<') {
            	fCurrentEntity.rewind();
            	return false;
            }
            fCurrentEntity.rewind();
            String aname = scanName();
            if (aname == null) {
                if (fReportErrors) {
                    fErrorReporter.reportError("HTML1011", null);
                }
                empty[0] = skipMarkup(false);
                return false;
            }
            if (!skippedSpaces && fReportErrors) {
                fErrorReporter.reportError("HTML1013", new Object[] { aname });
            }
            aname = modifyName(aname, fNamesAttrs);
            skipSpaces();
            c = fCurrentEntity.read();
            if (c == -1) {
                if (fReportErrors) {
                    fErrorReporter.reportError("HTML1007", null);
                }
                throw new EOFException();
            }
            if (c == '/' || c == '>') {
                fQName.setValues(null, aname, aname, null);
                attributes.addAttribute(fQName, "CDATA", "");
                attributes.setSpecified(attributes.getLength()-1, true);
                if (fAugmentations) {
                    addLocationItem(attributes, attributes.getLength() - 1);
                }
                if (c == '/') {
                	fCurrentEntity.rewind();
                    empty[0] = skipMarkup(false);
                }
                return false;
            }
            /***
            // REVISIT: [Q] Why is this still here? -Ac
            if (c == '/' || c == '>') {
                if (c == '/') {
                    fCurrentEntity.offset--;
                    fCurrentEntity.columnNumber--;
                    empty[0] = skipMarkup(false);
                }
                fQName.setValues(null, aname, aname, null);
                attributes.addAttribute(fQName, "CDATA", "");
                attributes.setSpecified(attributes.getLength()-1, true);
                if (fAugmentations) {
                    addLocationItem(attributes, attributes.getLength() - 1);
                }
                return false;
            }
            /***/
            if (c == '=') {
                skipSpaces();
                c = fCurrentEntity.read();
                if (c == -1) {
                    if (fReportErrors) {
                        fErrorReporter.reportError("HTML1007", null);
                    }
                    throw new EOFException();
                }
                // Xiaowei/Ac: Fix for <a href=/cgi-bin/myscript>...</a>
                if (c == '>') {
                    fQName.setValues(null, aname, aname, null);
                    attributes.addAttribute(fQName, "CDATA", "");
                    attributes.setSpecified(attributes.getLength()-1, true);
                    if (fAugmentations) {
                        addLocationItem(attributes, attributes.getLength() - 1);
                    }
                    return false;
                }
                fStringBuffer.clear();
                fNonNormAttr.clear();
                if (c != '\'' && c != '"') {
                	fCurrentEntity.rewind();
                    while (true) {
                        c = fCurrentEntity.read();
                        // Xiaowei/Ac: Fix for <a href=/broken/>...</a>
                        if (Character.isWhitespace((char)c) || c == '>') {
                            //fCharOffset--;
                        	fCurrentEntity.rewind();
                            break;
                        }
                        if (c == -1) {
                            if (fReportErrors) {
                                fErrorReporter.reportError("HTML1007", null);
                            }
                            throw new EOFException();
                        }
                        if (c == '&') {
                            int ce = scanEntityRef(fStringBuffer2, false);
                            if (ce != -1) {
                                fStringBuffer.append((char)ce);
                            }
                            else {
                                fStringBuffer.append(fStringBuffer2);
                            }
                            fNonNormAttr.append(fStringBuffer2);
                        }
                        else {
                            fStringBuffer.append((char)c);
                            fNonNormAttr.append((char)c);
                        }
                    }
                    fQName.setValues(null, aname, aname, null);
                    String avalue = fStringBuffer.toString();
                    attributes.addAttribute(fQName, "CDATA", avalue);

                    int lastattr = attributes.getLength()-1;
                    attributes.setSpecified(lastattr, true);
                    attributes.setNonNormalizedValue(lastattr, fNonNormAttr.toString());
                    if (fAugmentations) {
                        addLocationItem(attributes, attributes.getLength() - 1);
                    }
                    return true;
                }
                char quote = (char)c;
                boolean isStart = true;
                boolean prevSpace = false;
                do {
                	boolean acceptSpace = !fNormalizeAttributes || (!isStart && !prevSpace);
                    c = fCurrentEntity.read();
                    if (c == -1) {
                        if (fReportErrors) {
                            fErrorReporter.reportError("HTML1007", null);
                        }
                        break;
//                        throw new EOFException();
                    }
                    if (c == '&') {
                    	isStart = false;
                        int ce = scanEntityRef(fStringBuffer2, false);
                        if (ce != -1) {
                            fStringBuffer.append((char)ce);
                        }
                        else {
                            fStringBuffer.append(fStringBuffer2);
                        }
                        fNonNormAttr.append(fStringBuffer2);
                    }
                    else if (c == ' ' || c == '\t') {
                    	if (acceptSpace) {
	                        fStringBuffer.append(fNormalizeAttributes ? ' ' : (char)c);
	                    }
                        fNonNormAttr.append((char)c);
                    }
                    else if (c == '\r' || c == '\n') {
                        if (c == '\r') {
                            int c2 = fCurrentEntity.read();
                            if (c2 != '\n') {
                            	fCurrentEntity.rewind();
                            }
                            else {
                                fNonNormAttr.append('\r');
                                c = c2;
                            }
                        }
                        if (acceptSpace) {
	                        fStringBuffer.append(fNormalizeAttributes ? ' ' : '\n');
	                    }
                        fCurrentEntity.incLine();
                        fNonNormAttr.append((char)c);
                    }
                    else if (c != quote) {
                    	isStart = false;
                        fStringBuffer.append((char)c);
                        fNonNormAttr.append((char)c);
                    }
                    prevSpace = c == ' ' || c == '\t' || c == '\r' || c == '\n';
                    isStart = isStart && prevSpace;
                } while (c != quote);
                
                if (fNormalizeAttributes && fStringBuffer.length > 0) {
                	// trailing whitespace already normalized to single space
       	        	if (fStringBuffer.ch[fStringBuffer.length - 1] == ' ') {
           	    		fStringBuffer.length--;
               		}
    	        }

                fQName.setValues(null, aname, aname, null);
                String avalue = fStringBuffer.toString();
                attributes.addAttribute(fQName, "CDATA", avalue);

                int lastattr = attributes.getLength()-1;
                attributes.setSpecified(lastattr, true);
                attributes.setNonNormalizedValue(lastattr, fNonNormAttr.toString());
                if (fAugmentations) {
                    addLocationItem(attributes, attributes.getLength() - 1);
                }
            }
            else {
                fQName.setValues(null, aname, aname, null);
                attributes.addAttribute(fQName, "CDATA", "");
                attributes.setSpecified(attributes.getLength()-1, true);
                fCurrentEntity.rewind();
                if (fAugmentations) {
                    addLocationItem(attributes, attributes.getLength() - 1);
                }
            }
            return true;
        } // scanAttribute(XMLAttributesImpl):boolean

        /** Adds location augmentations to the specified attribute. */
        protected void addLocationItem(XMLAttributes attributes, int index) {
            fEndLineNumber = fCurrentEntity.getLineNumber();
            fEndColumnNumber = fCurrentEntity.getColumnNumber();
            fEndCharacterOffset = fCurrentEntity.getCharacterOffset();
            LocationItem locationItem = new LocationItem();
            locationItem.setValues(fBeginLineNumber, fBeginColumnNumber,
                                   fBeginCharacterOffset, fEndLineNumber,
                                   fEndColumnNumber, fEndCharacterOffset);
            Augmentations augs = attributes.getAugmentations(index);
            augs.putItem(AUGMENTATIONS, locationItem);
        } // addLocationItem(XMLAttributes,int)

        /** Scans an end element. */
        protected void scanEndElement() throws IOException {
            String ename = scanName();
            if (fReportErrors && ename == null) {
                fErrorReporter.reportError("HTML1012", null);
            }
            skipMarkup(false);
            if (ename != null) {
                ename = modifyName(ename, fNamesElems);
                if (fDocumentHandler != null && fElementCount >= fElementDepth) {
                    fQName.setValues(null, ename, ename, null);
                    if (DEBUG_CALLBACKS) {
                        System.out.println("endElement("+fQName+")");
                    }
                    fEndLineNumber = fCurrentEntity.getLineNumber();
                    fEndColumnNumber = fCurrentEntity.getColumnNumber();
                    fEndCharacterOffset = fCurrentEntity.getCharacterOffset();
                    fDocumentHandler.endElement(fQName, locationAugs());
                }
            }
        } // scanEndElement()

        //
        // Private methods
        //

        /**
         * Returns true if the given element has an end-tag.
         */
        private boolean isEnded(String ename) {
            String content = new String(fCurrentEntity.buffer, fCurrentEntity.offset,
                fCurrentEntity.length - fCurrentEntity.offset);
            return content.toLowerCase().indexOf("</" + ename.toLowerCase() + ">") != -1;
        }

    } // class ContentScanner

    /**
     * Special scanner used for elements whose content needs to be scanned 
     * as plain text, ignoring markup such as elements and entity references.
     * For example: &lt;SCRIPT&gt; and &lt;COMMENT&gt;.
     *
     * @author Andy Clark
     */
    public class SpecialScanner
        implements Scanner {

        //
        // Data
        //

        /** Name of element whose content needs to be scanned as text. */
        protected String fElementName;

        /** True if &lt;style&gt; element. */
        protected boolean fStyle;

        /** True if &lt;textarea&gt; element. */
        protected boolean fTextarea;

        /** True if &lt;title&gt; element. */
        protected boolean fTitle;

        // temp vars

        /** A qualified name. */
        private final QName fQName = new QName();

        /** A string buffer. */
        private final XMLStringBuffer fStringBuffer = new XMLStringBuffer();

        //
        // Public methods
        //

        /** Sets the element name. */
        public Scanner setElementName(String ename) {
            fElementName = ename;
            fStyle = fElementName.equalsIgnoreCase("STYLE");
            fTextarea = fElementName.equalsIgnoreCase("TEXTAREA");
            fTitle = fElementName.equalsIgnoreCase("TITLE");
            return this;
        } // setElementName(String):Scanner

        //
        // Scanner methods
        //

        /** Scan. */
        public boolean scan(boolean complete) throws IOException {
            boolean next;
            do {
                try {
                    next = false;
                    switch (fScannerState) {
                        case STATE_CONTENT: {
                            fBeginLineNumber = fCurrentEntity.getLineNumber();
                            fBeginColumnNumber = fCurrentEntity.getColumnNumber();
                            fBeginCharacterOffset = fCurrentEntity.getCharacterOffset();
                            int c = fCurrentEntity.read();
                            if (c == '<') {
                                setScannerState(STATE_MARKUP_BRACKET);
                                continue;
                            }
                            if (c == '&') {
                                if (fTextarea || fTitle) {
                                    scanEntityRef(fStringBuffer, true);
                                    continue;
                                }
                                fStringBuffer.clear();
                                fStringBuffer.append('&');
                            }
                            else if (c == -1) {
                                if (fReportErrors) {
                                    fErrorReporter.reportError("HTML1007", null);
                                }
                                throw new EOFException();
                            }
                            else {
                            	fCurrentEntity.rewind();
                                fStringBuffer.clear();
                            }
                            scanCharacters(fStringBuffer, -1);
                            break;
                        } // case STATE_CONTENT
                        case STATE_MARKUP_BRACKET: {
                            int delimiter = -1;
                            int c = fCurrentEntity.read();
                            if (c == '/') {
                                String ename = scanName();
                                if (ename != null) {
                                    if (ename.equalsIgnoreCase(fElementName)) {
                                        if (fCurrentEntity.read() == '>') {
                                            ename = modifyName(ename, fNamesElems);
                                            if (fDocumentHandler != null && fElementCount >= fElementDepth) {
                                                fQName.setValues(null, ename, ename, null);
                                                if (DEBUG_CALLBACKS) {
                                                    System.out.println("endElement("+fQName+")");
                                                }
                                                fEndLineNumber = fCurrentEntity.getLineNumber();
                                                fEndColumnNumber = fCurrentEntity.getColumnNumber();
                                                fEndCharacterOffset = fCurrentEntity.getCharacterOffset();
                                                fDocumentHandler.endElement(fQName, locationAugs());
                                            }
                                            setScanner(fContentScanner);
                                            setScannerState(STATE_CONTENT);
                                            return true;
                                        }
                                        else {
                                        	fCurrentEntity.rewind();
                                        }
                                    }
                                    fStringBuffer.clear();
                                    fStringBuffer.append("</");
                                    fStringBuffer.append(ename);
                                }
                                else {
                                    fStringBuffer.clear();
                                    fStringBuffer.append("</");
                                }
                            }
                            else {
                                fStringBuffer.clear();
                                fStringBuffer.append('<');
                                fStringBuffer.append((char)c);
                            }
                            scanCharacters(fStringBuffer, delimiter);
                            setScannerState(STATE_CONTENT);
                            break;
                        } // case STATE_MARKUP_BRACKET
                    } // switch
                } // try
                catch (EOFException e) {
                    setScanner(fContentScanner);
                    if (fCurrentEntityStack.empty()) {
                        setScannerState(STATE_END_DOCUMENT);
                    }
                    else {
                        fCurrentEntity = (CurrentEntity)fCurrentEntityStack.pop();
                        setScannerState(STATE_CONTENT);
                    }
                    return true;
                }
            } // do
            while (next || complete);
            return true;
        } // scan(boolean):boolean

        //
        // Protected methods
        //

        /** Scan characters. */
        protected void scanCharacters(XMLStringBuffer buffer,
                                      int delimiter) throws IOException {
            fCurrentEntity.debugBufferIfNeeded("(scanCharacters, delimiter="+delimiter+": ");
            
            while (true) {
                int c = fCurrentEntity.read();

                if (c == -1 || (c == '<' || c == '&')) {
                    if (c != -1) {
                    	fCurrentEntity.rewind();
                    }
                    break;
                }
                // Patch supplied by Jonathan Baxter
                else if (c == '\r' || c == '\n') {
                	fCurrentEntity.rewind();
                    int newlines = skipNewlines();
                    for (int i = 0; i < newlines; i++) {
                        buffer.append('\n');
                    }
                }
                else {
                    buffer.append((char)c);
                    if (c == '\n') {
                        fCurrentEntity.incLine();
                    }
                }
            }

            if (fStyle) {
            	if (fStyleStripCommentDelims) {
            		reduceToContent(buffer, "<!--", "-->");
            	}
            	if (fStyleStripCDATADelims) {
                	reduceToContent(buffer, "<![CDATA[", "]]>");
            	}
            }

            if (buffer.length > 0 && fDocumentHandler != null && fElementCount >= fElementDepth) {
                if (DEBUG_CALLBACKS) {
                    System.out.println("characters("+buffer+")");
                }
                fEndLineNumber = fCurrentEntity.getLineNumber();
                fEndColumnNumber = fCurrentEntity.getColumnNumber();
                fEndCharacterOffset = fCurrentEntity.getCharacterOffset();
                fDocumentHandler.characters(buffer, locationAugs());
            }
            fCurrentEntity.debugBufferIfNeeded(")scanCharacters: ");
        } // scanCharacters(StringBuffer)
    } // class SpecialScanner

    /**
     * A playback input stream. This class has the ability to save the bytes
     * read from the underlying input stream and play the bytes back later.
     * This class is used by the HTML scanner to switch encodings when a 
     * &lt;meta&gt; tag is detected that specifies a different encoding. 
     * <p>
     * If the encoding is changed, then the scanner calls the 
     * <code>playback</code> method and re-scans the beginning of the HTML
     * document again. This should not be too much of a performance problem
     * because the &lt;meta&gt; tag appears at the beginning of the document.
     * <p>
     * If the &lt;body&gt; tag is reached without playing back the bytes,
     * then the buffer can be cleared by calling the <code>clear</code>
     * method. This stops the buffering of bytes and allows the memory used
     * by the buffer to be reclaimed. 
     * <p>
     * <strong>Note:</strong> 
     * If the buffer is never played back or cleared, this input stream
     * will continue to buffer the entire stream. Therefore, it is very
     * important to use this stream correctly.
     *
     * @author Andy Clark
     */
    public static class PlaybackInputStream
        extends FilterInputStream {

        //
        // Constants
        //

        /** Set to true to debug playback. */
        private static final boolean DEBUG_PLAYBACK = false;

        //
        // Data
        //

        // state

        /** Playback mode. */
        protected boolean fPlayback = false;

        /** Buffer cleared. */
        protected boolean fCleared = false;

        /** Encoding detected. */
        protected boolean fDetected = false;

        // buffer info

        /** Byte buffer. */
        protected byte[] fByteBuffer = new byte[1024];

        /** Offset into byte buffer during playback. */
        protected int fByteOffset = 0;

        /** Length of bytes read into byte buffer. */
        protected int fByteLength = 0;

        /** Pushback offset. */
        public int fPushbackOffset = 0;

        /** Pushback length. */
        public int fPushbackLength = 0;

        //
        // Constructors
        //

        /** Constructor. */
        public PlaybackInputStream(InputStream in) {
            super(in);
        } // <init>(InputStream)

        //
        // Public methods
        //

        /** Detect encoding. */
        public void detectEncoding(String[] encodings) throws IOException {
            if (fDetected) {
                throw new IOException("Should not detect encoding twice.");
            }
            fDetected = true;
            int b1 = read();
            if (b1 == -1) {
                return;
            }
            int b2 = read();
            if (b2 == -1) {
                fPushbackLength = 1;
                return;
            }
            // UTF-8 BOM: 0xEFBBBF
            if (b1 == 0xEF && b2 == 0xBB) {
                int b3 = read();
                if (b3 == 0xBF) {
                    fPushbackOffset = 3;
                    encodings[0] = "UTF-8";
                    encodings[1] = "UTF8";
                    return;
                }
                fPushbackLength = 3;
            }
            // UTF-16 LE BOM: 0xFFFE
            if (b1 == 0xFF && b2 == 0xFE) {
                encodings[0] = "UTF-16";
                encodings[1] = "UnicodeLittleUnmarked";
                return;
            }
            // UTF-16 BE BOM: 0xFEFF
            else if (b1 == 0xFE && b2 == 0xFF) {
                encodings[0] = "UTF-16";
                encodings[1] = "UnicodeBigUnmarked";
                return;
            }
            // unknown
            fPushbackLength = 2;
        } // detectEncoding()

        /** Playback buffer contents. */
        public void playback() {
            fPlayback = true;
        } // playback()

        /** 
         * Clears the buffer.
         * <p>
         * <strong>Note:</strong>
         * The buffer cannot be cleared during playback. Therefore, calling
         * this method during playback will not do anything. However, the
         * buffer will be cleared automatically at the end of playback.
         */
        public void clear() {
            if (!fPlayback) {
                fCleared = true;
                fByteBuffer = null;
            }
        } // clear()

        //
        // InputStream methods
        //

        /** Read a byte. */
        public int read() throws IOException {
            if (DEBUG_PLAYBACK) {
                System.out.println("(read");
            }
            if (fPushbackOffset < fPushbackLength) {
                return fByteBuffer[fPushbackOffset++];
            }
            if (fCleared) {
                return in.read();
            }
            if (fPlayback) {
                int c = fByteBuffer[fByteOffset++];
                if (fByteOffset == fByteLength) {
                    fCleared = true;
                    fByteBuffer = null;
                }
                if (DEBUG_PLAYBACK) {
                    System.out.println(")read -> "+(char)c);
                }
                return c;
            }
            int c = in.read();
            if (c != -1) {
                if (fByteLength == fByteBuffer.length) {
                    byte[] newarray = new byte[fByteLength + 1024];
                    System.arraycopy(fByteBuffer, 0, newarray, 0, fByteLength);
                    fByteBuffer = newarray;
                }
                fByteBuffer[fByteLength++] = (byte)c;
            }
            if (DEBUG_PLAYBACK) {
                System.out.println(")read -> "+(char)c);
            }
            return c;
        } // read():int

        /** Read an array of bytes. */
        public int read(byte[] array) throws IOException {
            return read(array, 0, array.length);
        } // read(byte[]):int

        /** Read an array of bytes. */
        public int read(byte[] array, int offset, int length) throws IOException {
            if (DEBUG_PLAYBACK) {
                System.out.println(")read("+offset+','+length+')');
            }
            if (fPushbackOffset < fPushbackLength) {
                int count = fPushbackLength - fPushbackOffset;
                if (count > length) {
                    count = length;
                }
                System.arraycopy(fByteBuffer, fPushbackOffset, array, offset, count);
                fPushbackOffset += count;
                return count;
            }
            if (fCleared) {
                return in.read(array, offset, length);
            }
            if (fPlayback) {
                if (fByteOffset + length > fByteLength) {
                    length = fByteLength - fByteOffset;
                }
                System.arraycopy(fByteBuffer, fByteOffset, array, offset, length);
                fByteOffset += length;
                if (fByteOffset == fByteLength) {
                    fCleared = true;
                    fByteBuffer = null;
                }
                return length;
            }
            int count = in.read(array, offset, length);
            if (count != -1) {
                if (fByteLength + count > fByteBuffer.length) {
                    byte[] newarray = new byte[fByteLength + count + 512];
                    System.arraycopy(fByteBuffer, 0, newarray, 0, fByteLength);
                    fByteBuffer = newarray;
                }
                System.arraycopy(array, offset, fByteBuffer, fByteLength, count);
                fByteLength += count;
            }
            if (DEBUG_PLAYBACK) {
                System.out.println(")read("+offset+','+length+") -> "+count);
            }
            return count;
        } // read(byte[]):int

    } // class PlaybackInputStream

    /**
     * Location infoset item. 
     *
     * @author Andy Clark
     */
    protected static class LocationItem implements HTMLEventInfo, Cloneable {

        //
        // Data
        //

        /** Beginning line number. */
        protected int fBeginLineNumber;

        /** Beginning column number. */
        protected int fBeginColumnNumber;

        /** Beginning character offset. */
        protected int fBeginCharacterOffset;

        /** Ending line number. */
        protected int fEndLineNumber;

        /** Ending column number. */
        protected int fEndColumnNumber;

        /** Ending character offset. */
        protected int fEndCharacterOffset;

        //
        // Public methods
        //
        public LocationItem() {
        	// nothing
        }

        LocationItem(final LocationItem other) {
			setValues(other.fBeginLineNumber, other.fBeginColumnNumber, other.fBeginCharacterOffset,
					other.fEndLineNumber, other.fEndColumnNumber, other.fEndCharacterOffset);
		}

        /** Sets the values of this item. */
        public void setValues(int beginLine, int beginColumn, int beginOffset,
                              int endLine, int endColumn, int endOffset) {
            fBeginLineNumber = beginLine;
            fBeginColumnNumber = beginColumn;
            fBeginCharacterOffset = beginOffset;
            fEndLineNumber = endLine;
            fEndColumnNumber = endColumn;
            fEndCharacterOffset = endOffset;
        } // setValues(int,int,int,int)

        //
        // HTMLEventInfo methods
        //

        // location information

        /** Returns the line number of the beginning of this event.*/
        public int getBeginLineNumber() {
            return fBeginLineNumber;
        } // getBeginLineNumber():int

        /** Returns the column number of the beginning of this event.*/
        public int getBeginColumnNumber() { 
            return fBeginColumnNumber;
        } // getBeginColumnNumber():int

        /** Returns the character offset of the beginning of this event.*/
        public int getBeginCharacterOffset() { 
            return fBeginCharacterOffset;
        } // getBeginCharacterOffset():int

        /** Returns the line number of the end of this event.*/
        public int getEndLineNumber() {
            return fEndLineNumber;
        } // getEndLineNumber():int

        /** Returns the column number of the end of this event.*/
        public int getEndColumnNumber() {
            return fEndColumnNumber;
        } // getEndColumnNumber():int

        /** Returns the character offset of the end of this event.*/
        public int getEndCharacterOffset() { 
            return fEndCharacterOffset;
        } // getEndCharacterOffset():int

        // other information

        /** Returns true if this corresponding event was synthesized. */
        public boolean isSynthesized() {
            return false;
        } // isSynthesize():boolean

        //
        // Object methods
        //

        /** Returns a string representation of this object. */
        public String toString() {
            StringBuffer str = new StringBuffer();
            str.append(fBeginLineNumber);
            str.append(':');
            str.append(fBeginColumnNumber);
            str.append(':');
            str.append(fBeginCharacterOffset);
            str.append(':');
            str.append(fEndLineNumber);
            str.append(':');
            str.append(fEndColumnNumber);
            str.append(':');
            str.append(fEndCharacterOffset);
            return str.toString();
        } // toString():String

    } // class LocationItem

    /**
     * To detect if 2 encoding are compatible, both must be able to read the meta tag specifying
     * the new encoding. This means that the byte representation of some minimal html markup must
     * be the same in both encodings
     */ 
    boolean isEncodingCompatible(final String encoding1, final String encoding2) {
		final String reference = "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html;charset=";
		try {
			final byte[] bytesEncoding1 = reference.getBytes(encoding1);
			final String referenceWithEncoding2 = new String(bytesEncoding1, encoding2);
			return reference.equals(referenceWithEncoding2);
		}
		catch (final UnsupportedEncodingException e) {
			return false;
		}
    }

    private boolean endsWith(final XMLStringBuffer buffer, final String string) {
		final int l = string.length();
		if (buffer.length < l) {
			return false;
		}
		else {
			final String s = new String(buffer.ch, buffer.length-l, l);
			return string.equals(s);
		}
	}

     /** Reads a single character, preserving the old buffer content */
     protected int readPreservingBufferContent() throws IOException {
         fCurrentEntity.debugBufferIfNeeded("(read: ");
         if (fCurrentEntity.offset == fCurrentEntity.length) {
             if (fCurrentEntity.load(fCurrentEntity.length) < 1) {
                 if (DEBUG_BUFFER) { 
                     System.out.println(")read: -> -1");
                 }
                 return -1;
             }
         }
         final char c = fCurrentEntity.getNextChar();
         fCurrentEntity.debugBufferIfNeeded(")read: ", " -> " + c);
         return c;
     } // readPreservingBufferContent():int

     /**
     * Indicates if the end comment --> is available, loading further data if needed, without to reset the buffer
     */
	private boolean endCommentAvailable() throws IOException {
		int nbCaret = 0;
        final int originalOffset = fCurrentEntity.offset;
        final int originalColumnNumber = fCurrentEntity.getColumnNumber();
        final int originalCharacterOffset = fCurrentEntity.getCharacterOffset();

		while (true) {
	        int c = readPreservingBufferContent();
	        if (c == -1) {
		        fCurrentEntity.restorePosition(originalOffset, originalColumnNumber, originalCharacterOffset);
	        	return false;
	        }
	        else if (c == '>' && nbCaret >= 2) {
		        fCurrentEntity.restorePosition(originalOffset, originalColumnNumber, originalCharacterOffset);
	        	return true;
	        }
	        else if (c == '-') {
	        	nbCaret++;
	        }
	        else {
	        	nbCaret = 0;
	        }
		}
	}

	/**
     * Reduces the buffer to the content between start and end marker when
     * only whitespaces are found before the startMarker as well as after the end marker
     */
	static void reduceToContent(final XMLStringBuffer buffer, final String startMarker, final String endMarker) {
		int i = 0;
		int startContent = -1;
		final int l1 = startMarker.length();
		final int l2 = endMarker.length();
		while (i < buffer.length - l1 - l2) {
			final char c = buffer.ch[buffer.offset+i];
			if (Character.isWhitespace(c)) {
				++i;
			}
			else if (c == startMarker.charAt(0)
				&& startMarker.equals(new String(buffer.ch, buffer.offset+i, l1))) {
				startContent = buffer.offset + i + l1;
				break;
			}
			else {
				return; // start marker not found
			}
		}
		if (startContent == -1) { // start marker not found
			return;
		}
		
		i = buffer.length - 1;
		while (i > startContent + l2) {
			final char c = buffer.ch[buffer.offset+i];
			if (Character.isWhitespace(c)) {
				--i;
			}
			else if (c == endMarker.charAt(l2-1)
				&& endMarker.equals(new String(buffer.ch, buffer.offset+i-l2+1, l2))) {
				
				buffer.length = buffer.offset + i - startContent - 2;
				buffer.offset = startContent;
				return;
			}
			else {
				return; // start marker not found
			}
		}
	}
} // class HTMLScanner
