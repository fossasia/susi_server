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

/**
 * Collection of HTML element information.
 *
 * @author Andy Clark
 * @author Ahmed Ashour
 * @author Marc Guillemot
 *
 * @version $Id: HTMLElements.java,v 1.12 2005/02/14 07:16:59 andyc Exp $
 */
public class HTMLElements {

    //
    // Constants
    //
    
    // element codes

    // NOTE: The element codes *must* start with 0 and increment in
    //       sequence. The parent and closes references depends on 
    //       this assumption. -Ac

    public static final short A = 0;
    public static final short ABBR = A+1;
    public static final short ACRONYM = ABBR+1;
    public static final short ADDRESS = ACRONYM+1;
    public static final short APPLET = ADDRESS+1;
    public static final short AREA = APPLET+1;
    public static final short B = AREA+1;
    public static final short BASE = B+1;
    public static final short BASEFONT = BASE+1;
    public static final short BDO = BASEFONT+1;
    public static final short BGSOUND = BDO+1;
    public static final short BIG = BGSOUND+1;
    public static final short BLINK = BIG+1;
    public static final short BLOCKQUOTE = BLINK+1;
    public static final short BODY = BLOCKQUOTE+1;
    public static final short BR = BODY+1;
    public static final short BUTTON = BR+1;
    public static final short CAPTION = BUTTON+1;
    public static final short CENTER = CAPTION+1;
    public static final short CITE = CENTER+1;
    public static final short CODE = CITE+1;
    public static final short COL = CODE+1;
    public static final short COLGROUP = COL+1;
    public static final short COMMENT = COLGROUP+1;
    public static final short DEL = COMMENT+1;
    public static final short DFN = DEL+1;
    public static final short DIR = DFN+1;
    public static final short DIV = DIR+1;
    public static final short DD = DIV+1;
    public static final short DL = DD+1;
    public static final short DT = DL+1;
    public static final short EM = DT+1;
    public static final short EMBED = EM+1;
    public static final short FIELDSET = EMBED+1;
    public static final short FONT = FIELDSET+1;
    public static final short FORM = FONT+1;
    public static final short FRAME = FORM+1;
    public static final short FRAMESET = FRAME+1;
    public static final short H1 = FRAMESET+1;
    public static final short H2 = H1+1;
    public static final short H3 = H2+1;
    public static final short H4 = H3+1;
    public static final short H5 = H4+1;
    public static final short H6 = H5+1;
    public static final short HEAD = H6+1;
    public static final short HR = HEAD+1;
    public static final short HTML = HR+1;
    public static final short I = HTML+1;
    public static final short IFRAME = I+1;
    public static final short ILAYER = IFRAME+1;
    public static final short IMG = ILAYER+1;
    public static final short INPUT = IMG+1;
    public static final short INS = INPUT+1;
    public static final short ISINDEX = INS+1;
    public static final short KBD = ISINDEX+1;
    public static final short KEYGEN = KBD+1;
    public static final short LABEL = KEYGEN+1;
    public static final short LAYER = LABEL+1;
    public static final short LEGEND = LAYER+1;
    public static final short LI = LEGEND+1;
    public static final short LINK = LI+1;
    public static final short LISTING = LINK+1;
    public static final short MAP = LISTING+1;
    public static final short MARQUEE = MAP+1;
    public static final short MENU = MARQUEE+1;
    public static final short META = MENU+1;
    public static final short MULTICOL = META+1;
    public static final short NEXTID = MULTICOL+1;
    public static final short NOBR = NEXTID+1;
    public static final short NOEMBED = NOBR+1;
    public static final short NOFRAMES = NOEMBED+1;
    public static final short NOLAYER = NOFRAMES+1;
    public static final short NOSCRIPT = NOLAYER+1;
    public static final short OBJECT = NOSCRIPT+1;
    public static final short OL = OBJECT+1;
    public static final short OPTION = OL+1;
    public static final short OPTGROUP = OPTION+1;
    public static final short P = OPTGROUP+1;
    public static final short PARAM = P+1;
    public static final short PLAINTEXT = PARAM+1;
    public static final short PRE = PLAINTEXT+1;
    public static final short Q = PRE+1;
    public static final short RB = Q+1;
    public static final short RBC = RB+1;
    public static final short RP = RBC+1;
    public static final short RT = RP+1;
    public static final short RTC = RT+1;
    public static final short RUBY = RTC+1;
    public static final short S = RUBY+1;
    public static final short SAMP = S+1;
    public static final short SCRIPT = SAMP+1;
    public static final short SELECT = SCRIPT+1;
    public static final short SMALL = SELECT+1;
    public static final short SOUND = SMALL+1;
    public static final short SPACER = SOUND+1;
    public static final short SPAN = SPACER+1;
    public static final short STRIKE = SPAN+1;
    public static final short STRONG = STRIKE+1;
    public static final short STYLE = STRONG+1;
    public static final short SUB = STYLE+1;
    public static final short SUP = SUB+1;
    public static final short TABLE = SUP+1;
    public static final short TBODY = TABLE+1;
    public static final short TD = TBODY+1;
    public static final short TEXTAREA = TD+1;
    public static final short TFOOT = TEXTAREA+1;
    public static final short TH = TFOOT+1;
    public static final short THEAD = TH+1;
    public static final short TITLE = THEAD+1;
    public static final short TR = TITLE+1;
    public static final short TT = TR+1;
    public static final short U = TT+1;
    public static final short UL = U+1;
    public static final short VAR = UL+1;
    public static final short WBR = VAR+1;
    public static final short XML = WBR+1;
    public static final short XMP = XML+1;
    public static final short UNKNOWN = XMP+1;

    // information

    /** Element information organized by first letter. */
    protected static final Element[][] ELEMENTS_ARRAY = new Element[26][];

    /** Element information as a contiguous list. */
    protected static final ElementList ELEMENTS = new ElementList();

    /** No such element. */
    public static final Element NO_SUCH_ELEMENT = new Element(UNKNOWN, "",  Element.CONTAINER, new short[]{BODY,HEAD}/*HTML*/, null);

    //
    // Static initializer
    //

    /**
     * Initializes the element information.
     * <p>
     * <strong>Note:</strong>
     * The <code>getElement</code> method requires that the HTML elements
     * are added to the list in alphabetical order. If new elements are
     * added, then they <em>must</em> be inserted in alphabetical order.
     */
    static {
        // <!ENTITY % heading "H1|H2|H3|H4|H5|H6">
        // <!ENTITY % fontstyle "TT | I | B | BIG | SMALL">
        // <!ENTITY % phrase "EM | STRONG | DFN | CODE | SAMP | KBD | VAR | CITE | ABBR | ACRONYM" >
        // <!ENTITY % special "A | IMG | OBJECT | BR | SCRIPT | MAP | Q | SUB | SUP | SPAN | BDO">
        // <!ENTITY % formctrl "INPUT | SELECT | TEXTAREA | LABEL | BUTTON">
        // <!ENTITY % inline "#PCDATA | %fontstyle; | %phrase; | %special; | %formctrl;">
        // <!ENTITY % block "P | %heading; | %list; | %preformatted; | DL | DIV | NOSCRIPT | BLOCKQUOTE | FORM | HR | TABLE | FIELDSET | ADDRESS">
        // <!ENTITY % flow "%block; | %inline;">

        // initialize array of element information
        ELEMENTS_ARRAY['A'-'A'] = new Element[] {
            // A - - (%inline;)* -(A)
            new Element(A, "A", Element.INLINE, BODY, new short[] {A}),
            // ABBR - - (%inline;)*
            new Element(ABBR, "ABBR", Element.INLINE, BODY, null),
            // ACRONYM - - (%inline;)*
            new Element(ACRONYM, "ACRONYM", Element.INLINE, BODY, null),
            // ADDRESS - - (%inline;)*
            new Element(ADDRESS, "ADDRESS", Element.BLOCK, BODY, null),
            // APPLET
            new Element(APPLET, "APPLET", 0, BODY, null),
            // AREA - O EMPTY
            new Element(AREA, "AREA", Element.EMPTY, MAP, null),
        };
        ELEMENTS_ARRAY['B'-'A'] = new Element[] {
            // B - - (%inline;)*
            new Element(B, "B", Element.INLINE, BODY, null),
            // BASE - O EMPTY
            new Element(BASE, "BASE", Element.EMPTY, HEAD, null),
            // BASEFONT
            new Element(BASEFONT, "BASEFONT", 0, HEAD, null),
            // BDO - - (%inline;)*
            new Element(BDO, "BDO", Element.INLINE, BODY, null),
            // BGSOUND
            new Element(BGSOUND, "BGSOUND", Element.EMPTY, HEAD, null),
            // BIG - - (%inline;)*
            new Element(BIG, "BIG", Element.INLINE, BODY, null),
            // BLINK
            new Element(BLINK, "BLINK", Element.INLINE, BODY, null),
            // BLOCKQUOTE - - (%block;|SCRIPT)+
            new Element(BLOCKQUOTE, "BLOCKQUOTE", Element.BLOCK, BODY, new short[]{P}),
            // BODY O O (%block;|SCRIPT)+ +(INS|DEL)
            new Element(BODY, "BODY", Element.CONTAINER, HTML, new short[]{HEAD}),
            // BR - O EMPTY
            new Element(BR, "BR", Element.EMPTY, BODY, null),
            // BUTTON - - (%flow;)* -(A|%formctrl;|FORM|FIELDSET)
            new Element(BUTTON, "BUTTON", 0, BODY, null),
        };
        ELEMENTS_ARRAY['C'-'A'] = new Element[] {
            // CAPTION - - (%inline;)*
            new Element(CAPTION, "CAPTION", Element.INLINE, TABLE, null),
            // CENTER, 
            new Element(CENTER, "CENTER", 0, BODY, null),
            // CITE - - (%inline;)*
            new Element(CITE, "CITE", Element.INLINE, BODY, null),
            // CODE - - (%inline;)*
            new Element(CODE, "CODE", Element.INLINE, BODY, null),
            // COL - O EMPTY
            new Element(COL, "COL", Element.EMPTY, TABLE, null),
            // COLGROUP - O (COL)*
            new Element(COLGROUP, "COLGROUP", 0, TABLE, new short[]{COL,COLGROUP}),
            // COMMENT
            new Element(COMMENT, "COMMENT", Element.SPECIAL, HTML, null),
        };
        ELEMENTS_ARRAY['D'-'A'] = new Element[] {
            // DEL - - (%flow;)*
            new Element(DEL, "DEL", 0, BODY, null),
            // DFN - - (%inline;)*
            new Element(DFN, "DFN", Element.INLINE, BODY, null),
            // DIR
            new Element(DIR, "DIR", 0, BODY, null),
            // DIV - - (%flow;)*
            new Element(DIV, "DIV", Element.BLOCK, BODY, new short[]{P}),
            // DD - O (%flow;)*
            new Element(DD, "DD", 0, DL, new short[]{DT,DD}),
            // DL - - (DT|DD)+
            new Element(DL, "DL", Element.BLOCK, BODY, null),
            // DT - O (%inline;)*
            new Element(DT, "DT", 0, DL, new short[]{DT,DD}),
        };
        ELEMENTS_ARRAY['E'-'A'] = new Element[] {
            // EM - - (%inline;)*
            new Element(EM, "EM", Element.INLINE, BODY, null),
            // EMBED
            new Element(EMBED, "EMBED", 0, BODY, null),
        };
        ELEMENTS_ARRAY['F'-'A'] = new Element[] {
            // FIELDSET - - (#PCDATA,LEGEND,(%flow;)*)
            new Element(FIELDSET, "FIELDSET", 0, BODY, null),
            // FONT
            new Element(FONT, "FONT", Element.CONTAINER, BODY, null),
            // FORM - - (%block;|SCRIPT)+ -(FORM)
            new Element(FORM, "FORM", Element.CONTAINER, new short[]{BODY,TD,DIV}, new short[]{BUTTON,P}),
            // FRAME - O EMPTY
            new Element(FRAME, "FRAME", Element.EMPTY, FRAMESET, null),
            // FRAMESET - - ((FRAMESET|FRAME)+ & NOFRAMES?)
            new Element(FRAMESET, "FRAMESET", 0, HTML, null),
        };
        ELEMENTS_ARRAY['H'-'A'] = new Element[] {
            // (H1|H2|H3|H4|H5|H6) - - (%inline;)*
            new Element(H1, "H1", Element.BLOCK, new short[]{BODY,A}, new short[]{H1,H2,H3,H4,H5,H6,P}),
            new Element(H2, "H2", Element.BLOCK, new short[]{BODY,A}, new short[]{H1,H2,H3,H4,H5,H6,P}),
            new Element(H3, "H3", Element.BLOCK, new short[]{BODY,A}, new short[]{H1,H2,H3,H4,H5,H6,P}),
            new Element(H4, "H4", Element.BLOCK, new short[]{BODY,A}, new short[]{H1,H2,H3,H4,H5,H6,P}),
            new Element(H5, "H5", Element.BLOCK, new short[]{BODY,A}, new short[]{H1,H2,H3,H4,H5,H6,P}),
            new Element(H6, "H6", Element.BLOCK, new short[]{BODY,A}, new short[]{H1,H2,H3,H4,H5,H6,P}),
            // HEAD O O (%head.content;) +(%head.misc;)
            new Element(HEAD, "HEAD", 0, HTML, null),
            // HR - O EMPTY
            new Element(HR, "HR", Element.EMPTY, BODY, new short[]{P}),
            // HTML O O (%html.content;)
            new Element(HTML, "HTML", 0, null, null),
        };
        ELEMENTS_ARRAY['I'-'A'] = new Element[] {
            // I - - (%inline;)*
            new Element(I, "I", Element.INLINE, BODY, null),
            // IFRAME
            new Element(IFRAME, "IFRAME", Element.BLOCK, BODY, null),
            // ILAYER
            new Element(ILAYER, "ILAYER", Element.BLOCK, BODY, null),
            // IMG - O EMPTY
            new Element(IMG, "IMG", Element.EMPTY, BODY, null),
            // INPUT - O EMPTY
            new Element(INPUT, "INPUT", Element.EMPTY, BODY, null),
            // INS - - (%flow;)*
            new Element(INS, "INS", 0, BODY, null),
            // ISINDEX
            new Element(ISINDEX, "ISINDEX", 0, HEAD, null),
        };
        ELEMENTS_ARRAY['K'-'A'] = new Element[] {
            // KBD - - (%inline;)*
            new Element(KBD, "KBD", Element.INLINE, BODY, null),
            // KEYGEN
            new Element(KEYGEN, "KEYGEN", 0, BODY, null),
        };
        ELEMENTS_ARRAY['L'-'A'] = new Element[] {
            // LABEL - - (%inline;)* -(LABEL)
            new Element(LABEL, "LABEL", 0, BODY, null),
            // LAYER
            new Element(LAYER, "LAYER", Element.BLOCK, BODY, null),
            // LEGEND - - (%inline;)*
            new Element(LEGEND, "LEGEND", Element.INLINE, FIELDSET, null),
            // LI - O (%flow;)*
            new Element(LI, "LI", 0, new short[]{BODY,UL,OL}, new short[]{LI}),
            // LINK - O EMPTY
            new Element(LINK, "LINK", Element.EMPTY, HEAD, null),
            // LISTING
            new Element(LISTING, "LISTING", 0, BODY, null),
        };
        ELEMENTS_ARRAY['M'-'A'] = new Element[] {
            // MAP - - ((%block;) | AREA)+
            new Element(MAP, "MAP", Element.INLINE, BODY, null),
            // MARQUEE
            new Element(MARQUEE, "MARQUEE", 0, BODY, null),
            // MENU
            new Element(MENU, "MENU", 0, BODY, null),
            // META - O EMPTY
            new Element(META, "META", Element.EMPTY, HEAD, new short[]{STYLE,TITLE}),
            // MULTICOL
            new Element(MULTICOL, "MULTICOL", 0, BODY, null),
        };
        ELEMENTS_ARRAY['N'-'A'] = new Element[] {
            // NEXTID
            new Element(NEXTID, "NEXTID", Element.EMPTY, BODY, null),
            // NOBR
            new Element(NOBR, "NOBR", Element.INLINE, BODY, null),
            // NOEMBED
            new Element(NOEMBED, "NOEMBED", 0, BODY, null),
            // NOFRAMES - - (BODY) -(NOFRAMES)
            new Element(NOFRAMES, "NOFRAMES", 0, null, null),
            // NOLAYER
            new Element(NOLAYER, "NOLAYER", 0, BODY, null),
            // NOSCRIPT - - (%block;)+
            new Element(NOSCRIPT, "NOSCRIPT", 0, new short[]{BODY}, null),
        };
        ELEMENTS_ARRAY['O'-'A'] = new Element[] {
            // OBJECT - - (PARAM | %flow;)*
            new Element(OBJECT, "OBJECT", 0, BODY, null),
            // OL - - (LI)+
            new Element(OL, "OL", Element.BLOCK, BODY, null),
            // OPTGROUP - - (OPTION)+
            new Element(OPTGROUP, "OPTGROUP", 0, SELECT, new short[]{OPTION}),
            // OPTION - O (#PCDATA)
            new Element(OPTION, "OPTION", 0, SELECT, new short[]{OPTION}),
        };
        ELEMENTS_ARRAY['P'-'A'] = new Element[] {
            // P - O (%inline;)*
            new Element(P, "P", Element.CONTAINER, BODY, new short[]{P}),
            // PARAM - O EMPTY
            new Element(PARAM, "PARAM", Element.EMPTY, new short[]{OBJECT,APPLET}, null),
            // PLAINTEXT
            new Element(PLAINTEXT, "PLAINTEXT", Element.SPECIAL, BODY, null),
            // PRE - - (%inline;)* -(%pre.exclusion;)
            new Element(PRE, "PRE", 0, BODY, null),
        };
        ELEMENTS_ARRAY['Q'-'A'] = new Element[] {
            // Q - - (%inline;)*
            new Element(Q, "Q", Element.INLINE, BODY, null),
        };
        ELEMENTS_ARRAY['R'-'A'] = new Element[] {
            // RB
            new Element(RB, "RB", Element.INLINE, RUBY, new short[]{RB}),
            // RBC
            new Element(RBC, "RBC", 0, RUBY, null),
            // RP
            new Element(RP, "RP", Element.INLINE, RUBY, new short[]{RB}),
            // RT
            new Element(RT, "RT", Element.INLINE, RUBY, new short[]{RB,RP}),
            // RTC
            new Element(RTC, "RTC", 0, RUBY, new short[]{RBC}),
            // RUBY
            new Element(RUBY, "RUBY", 0, BODY, new short[]{RUBY}),
        };
        ELEMENTS_ARRAY['S'-'A'] = new Element[] {
            // S
            new Element(S, "S", 0, BODY, null),
            // SAMP - - (%inline;)*
            new Element(SAMP, "SAMP", Element.INLINE, BODY, null),
            // SCRIPT - - %Script;
            new Element(SCRIPT, "SCRIPT", Element.SPECIAL, new short[]{HEAD,BODY}, null),
            // SELECT - - (OPTGROUP|OPTION)+
            new Element(SELECT, "SELECT", Element.CONTAINER, BODY, new short[]{SELECT}),
            // SMALL - - (%inline;)*
            new Element(SMALL, "SMALL", Element.INLINE, BODY, null),
            // SOUND
            new Element(SOUND, "SOUND", Element.EMPTY, HEAD, null),
            // SPACER
            new Element(SPACER, "SPACER", Element.EMPTY, BODY, null),
            // SPAN - - (%inline;)*
            new Element(SPAN, "SPAN", Element.CONTAINER, BODY, null),
            // STRIKE
            new Element(STRIKE, "STRIKE", Element.INLINE, BODY, null),
            // STRONG - - (%inline;)*
            new Element(STRONG, "STRONG", Element.INLINE, BODY, null),
            // STYLE - - %StyleSheet;
            new Element(STYLE, "STYLE", Element.SPECIAL, new short[]{HEAD,BODY}, new short[]{STYLE,TITLE,META}),
            // SUB - - (%inline;)*
            new Element(SUB, "SUB", Element.INLINE, BODY, null),
            // SUP - - (%inline;)*
            new Element(SUP, "SUP", Element.INLINE, BODY, null),
        };
        ELEMENTS_ARRAY['T'-'A'] = new Element[] {
            // TABLE - - (CAPTION?, (COL*|COLGROUP*), THEAD?, TFOOT?, TBODY+)
            new Element(TABLE, "TABLE", Element.BLOCK|Element.CONTAINER, BODY, null),
            // TBODY O O (TR)+
            new Element(TBODY, "TBODY", 0, TABLE, new short[]{THEAD,TBODY,TFOOT,TD,TH,TR,COLGROUP}),
            // TD - O (%flow;)*
            new Element(TD, "TD", Element.CONTAINER, TR, TABLE, new short[]{TD,TH}),
            // TEXTAREA - - (#PCDATA)
            new Element(TEXTAREA, "TEXTAREA", Element.SPECIAL, BODY, null),
            // TFOOT - O (TR)+
            new Element(TFOOT, "TFOOT", 0, TABLE, new short[]{THEAD,TBODY,TFOOT,TD,TH,TR}),
            // TH - O (%flow;)*
            new Element(TH, "TH", Element.CONTAINER, TR, TABLE, new short[]{TD,TH}),
            // THEAD - O (TR)+
            new Element(THEAD, "THEAD", 0, TABLE, new short[]{THEAD,TBODY,TFOOT,TD,TH,TR,COLGROUP}),
            // TITLE - - (#PCDATA) -(%head.misc;)
            new Element(TITLE, "TITLE", Element.SPECIAL, new short[]{HEAD,BODY}, null),
            // TR - O (TH|TD)+
            new Element(TR, "TR", Element.BLOCK, new short[]{TBODY, THEAD, TFOOT}, TABLE, new short[]{TD,TH,TR,COLGROUP}),
            // TT - - (%inline;)*
            new Element(TT, "TT", Element.INLINE, BODY, null),
        };
        ELEMENTS_ARRAY['U'-'A'] = new Element[] {
            // U, 
            new Element(U, "U", Element.INLINE, BODY, null),
            // UL - - (LI)+
            new Element(UL, "UL", Element.BLOCK, BODY, null),
        };
        ELEMENTS_ARRAY['V'-'A'] = new Element[] {
            // VAR - - (%inline;)*
            new Element(VAR, "VAR", Element.INLINE, BODY, null),
        };
        ELEMENTS_ARRAY['W'-'A'] = new Element[] {
            // WBR
            new Element(WBR, "WBR", Element.EMPTY, BODY, null),
        };
        ELEMENTS_ARRAY['X'-'A'] = new Element[] {
            // XML
            new Element(XML, "XML", 0, BODY, null),
            // XMP
            new Element(XMP, "XMP", Element.SPECIAL, BODY, null),
        };

        // keep contiguous list of elements for lookups by code
        for (int i = 0; i < ELEMENTS_ARRAY.length; i++) {
            Element[] elements = ELEMENTS_ARRAY[i];
            if (elements != null) {
                for (int j = 0; j < elements.length; j++) {
                    Element element = elements[j];
                    ELEMENTS.addElement(element);
                }
            }
        }
        ELEMENTS.addElement(NO_SUCH_ELEMENT);

        // initialize cross references to parent elements
        for (int i = 0; i < ELEMENTS.size; i++) {
            Element element = ELEMENTS.data[i];
            if (element.parentCodes != null) {
                element.parent = new Element[element.parentCodes.length];
                for (int j = 0; j < element.parentCodes.length; j++) {
                    element.parent[j] = ELEMENTS.data[element.parentCodes[j]];
                }
                element.parentCodes = null;
            }
        }

    } // <clinit>()

    //
    // Public static methods
    //

    /**
     * Returns the element information for the specified element code.
     *
     * @param code The element code.
     */
    public static final Element getElement(short code) {
        return ELEMENTS.data[code];
    } // getElement(short):Element

    /**
     * Returns the element information for the specified element name.
     *
     * @param ename The element name.
     */
    public static final Element getElement(String ename) {
        return getElement(ename, NO_SUCH_ELEMENT);
    } // getElement(String):Element

    /**
     * Returns the element information for the specified element name.
     *
     * @param ename The element name.
     * @param element The default element to return if not found.
     */
    public static final Element getElement(String ename, Element element) {

        if (ename.length() > 0) {
            int c = ename.charAt(0);
            if (c >= 'a' && c <= 'z') {
                c = 'A' + c - 'a';
            }
            if (c >= 'A' && c <= 'Z') {
                Element[] elements = ELEMENTS_ARRAY[c - 'A'];
                if (elements != null) {
                    for (int i = 0; i < elements.length; i++) {
                        Element elem = elements[i];
                        if (elem.name.equalsIgnoreCase(ename)) {
                            return elem;
                        }
                    }
                }
            }
        }
        return element;

    } // getElement(String):Element

    //
    // Classes
    //

    /**
     * Element information.
     *
     * @author Andy Clark
     */
    public static class Element {

        //
        // Constants
        //

        /** Inline element. */
        public static final int INLINE = 0x01;

        /** Block element. */
        public static final int BLOCK = 0x02;

        /** Empty element. */
        public static final int EMPTY = 0x04;

        /** Container element. */
        public static final int CONTAINER = 0x08;

        /** Special element. */
        public static final int SPECIAL = 0x10;

        //
        // Data
        //

        /** The element code. */
        public short code;

        /** The element name. */
        public String name;

        /** Informational flags. */
        public int flags;

        /** Parent elements. */
        public short[] parentCodes;

        /** Parent elements. */
        public Element[] parent;

        /** The bounding element code. */
        public short bounds;

        /** List of elements this element can close. */
        public short[] closes;

        //
        // Constructors
        //

        /** 
         * Constructs an element object.
         *
         * @param code The element code.
         * @param name The element name.
         * @param flags Informational flags
         * @param parent Natural closing parent name.
         * @param closes List of elements this element can close.
         */
        public Element(short code, String name, int flags, 
                       short parent, short[] closes) {
            this(code, name, flags, new short[]{parent}, (short)-1, closes);
        } // <init>(short,String,int,short,short[]);

        /** 
         * Constructs an element object.
         *
         * @param code The element code.
         * @param name The element name.
         * @param flags Informational flags
         * @param parent Natural closing parent name.
         * @param closes List of elements this element can close.
         */
        public Element(short code, String name, int flags, 
                       short parent, short bounds, short[] closes) {
            this(code, name, flags, new short[]{parent}, bounds, closes);
        } // <init>(short,String,int,short,short,short[])

        /** 
         * Constructs an element object.
         *
         * @param code The element code.
         * @param name The element name.
         * @param flags Informational flags
         * @param parents Natural closing parent names.
         * @param closes List of elements this element can close.
         */
        public Element(short code, String name, int flags, 
                       short[] parents, short[] closes) {
            this(code, name, flags, parents, (short)-1, closes);
        } // <init>(short,String,int,short[],short[])

        /** 
         * Constructs an element object.
         *
         * @param code The element code.
         * @param name The element name.
         * @param flags Informational flags
         * @param parents Natural closing parent names.
         * @param closes List of elements this element can close.
         */
        public Element(short code, String name, int flags, 
                       short[] parents, short bounds, short[] closes) {
            this.code = code;
            this.name = name;
            this.flags = flags;
            this.parentCodes = parents;
            this.parent = null;
            this.bounds = bounds;
            this.closes = closes;
        } // <init>(short,String,int,short[],short,short[])

        //
        // Public methods
        //

        /** Returns true if this element is an inline element. */
        public final boolean isInline() {
            return (flags & INLINE) != 0;
        } // isInline():boolean

        /** Returns true if this element is a block element. */
        public final boolean isBlock() {
            return (flags & BLOCK) != 0;
        } // isBlock():boolean

        /** Returns true if this element is an empty element. */
        public final boolean isEmpty() {
            return (flags & EMPTY) != 0;
        } // isEmpty():boolean

        /** Returns true if this element is a container element. */
        public final boolean isContainer() {
            return (flags & CONTAINER) != 0;
        } // isContainer():boolean

        /** 
         * Returns true if this element is special -- if its content
         * should be parsed ignoring markup.
         */
        public final boolean isSpecial() {
            return (flags & SPECIAL) != 0;
        } // isSpecial():boolean

        /**
         * Returns true if this element can close the specified Element.
         *
         * @param tag The element.
         */
        public boolean closes(short tag) {

            if (closes != null) {
                for (int i = 0; i < closes.length; i++) {
                    if (closes[i] == tag) {
                        return true;
                    }
                }
            }
            return false;

        } // closes(short):boolean

        //
        // Object methods
        //

        /** Returns a hash code for this object. */
        public int hashCode() {
            return name.hashCode();
        } // hashCode():int

        /** Returns true if the objects are equal. */
        public boolean equals(Object o) {
            return name.equals(o);
        } // equals(Object):boolean

        /**
         * Provides a simple representation to make debugging easier
         */
        public String toString() {
        	return super.toString() + "(name=" + name + ")";
        }

        /**
         * Indicates if the provided element is an accepted parent of current element
         * @param element the element to test for "paternity"
         * @return <code>true</code> if <code>element</code> belongs to the {@link #parent}
         */
		public boolean isParent(final Element element) {
			if (parent == null)
				return false;
			else {
				for (int i=0; i<parent.length; ++i) {
					if (element.code == parent[i].code)
						return true;
				}
			}
			return false;
		}
    } // class Element

    /** Unsynchronized list of elements. */
    public static class ElementList {

        //
        // Data
        //

        /** The size of the list. */
        public int size;

        /** The data in the list. */
        public Element[] data = new Element[120];

        //
        // Public methods
        //

        /** Adds an element to list, resizing if necessary. */
        public void addElement(Element element) {
            if (size == data.length) {
                Element[] newarray = new Element[size + 20];
                System.arraycopy(data, 0, newarray, 0, size);
                data = newarray;
            }
            data[size++] = element;
        } // addElement(Element)

    } // class Element

} // class HTMLElements
