/*
 * Copyright (C) 2011-2015 René Jeschke <rene_jeschke@yahoo.de>
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

// this package was migrated from com.github.rjeschke.txtmark
package org.loklak.visualization.txtmark;

import java.util.HashMap;
import java.util.HashSet;

/**
 * HTML utility class.
 *
 * @author René Jeschke <rene_jeschke@yahoo.de>
 */
class HTML
{
    /** List of valid HTML/XML entity names. */
    private final static String[] ENTITY_NAMES = {
        "&Acirc;", "&acirc;", "&acute;", "&AElig;", "&aelig;", "&Agrave;", "&agrave;", "&alefsym;",
        "&Alpha;", "&alpha;", "&amp;", "&and;", "&ang;", "&apos;", "&Aring;", "&aring;",
        "&asymp;", "&Atilde;", "&atilde;", "&Auml;", "&auml;", "&bdquo;", "&Beta;", "&beta;",
        "&brvbar;", "&bull;", "&cap;", "&Ccedil;", "&ccedil;", "&cedil;", "&cent;", "&Chi;",
        "&chi;", "&circ;", "&clubs;", "&cong;", "&copy;", "&crarr;", "&cup;", "&curren;",
        "&Dagger;", "&dagger;", "&dArr;", "&darr;", "&deg;", "&Delta;", "&delta;", "&diams;",
        "&divide;", "&Eacute;", "&eacute;", "&Ecirc;", "&ecirc;", "&Egrave;", "&egrave;", "&empty;",
        "&emsp;", "&ensp;", "&Epsilon;", "&epsilon;", "&equiv;", "&Eta;", "&eta;", "&ETH;",
        "&eth;", "&Euml;", "&euml;", "&euro;", "&exist;", "&fnof;", "&forall;", "&frac12;",
        "&frac14;", "&frac34;", "&frasl;", "&Gamma;", "&gamma;", "&ge;", "&gt;", "&hArr;",
        "&harr;", "&hearts;", "&hellip;", "&Iacute;", "&iacute;", "&Icirc;", "&icirc;", "&iexcl;",
        "&Igrave;", "&igrave;", "&image;", "&infin;", "&int;", "&Iota;", "&iota;", "&iquest;",
        "&isin;", "&Iuml;", "&iuml;", "&Kappa;", "&kappa;", "&Lambda;", "&lambda;", "&lang;",
        "&laquo;", "&lArr;", "&larr;", "&lceil;", "&ldquo;", "&le;", "&lfloor;", "&lowast;",
        "&loz;", "&lrm;", "&lsaquo;", "&lsquo;", "&lt;", "&macr;", "&mdash;", "&micro;",
        "&middot;", "&minus;", "&Mu;", "&mu;", "&nabla;", "&nbsp;", "&ndash;", "&ne;",
        "&ni;", "&not;", "&notin;", "&nsub;", "&Ntilde;", "&ntilde;", "&Nu;", "&nu;",
        "&Oacute;", "&oacute;", "&Ocirc;", "&ocirc;", "&OElig;", "&oelig;", "&Ograve;", "&ograve;",
        "&oline;", "&Omega;", "&omega;", "&Omicron;", "&omicron;", "&oplus;", "&or;", "&ordf;",
        "&ordm;", "&Oslash;", "&oslash;", "&Otilde;", "&otilde;", "&otimes;", "&Ouml;", "&ouml;",
        "&para;", "&part;", "&permil;", "&perp;", "&Phi;", "&phi;", "&Pi;", "&pi;",
        "&piv;", "&plusmn;", "&pound;", "&Prime;", "&prime;", "&prod;", "&prop;", "&Psi;",
        "&psi;", "&quot;", "&radic;", "&rang;", "&raquo;", "&rArr;", "&rarr;", "&rceil;",
        "&rdquo;", "&real;", "&reg;", "&rfloor;", "&Rho;", "&rho;", "&rlm;", "&rsaquo;",
        "&rsquo;", "&sbquo;", "&Scaron;", "&scaron;", "&sdot;", "&sect;", "&shy;", "&Sigma;",
        "&sigma;", "&sigmaf;", "&sim;", "&spades;", "&sub;", "&sube;", "&sum;", "&sup;",
        "&sup1;", "&sup2;", "&sup3;", "&supe;", "&szlig;", "&Tau;", "&tau;", "&there4;",
        "&Theta;", "&theta;", "&thetasym;", "&thinsp;", "&thorn;", "&tilde;", "&times;", "&trade;",
        "&Uacute;", "&uacute;", "&uArr;", "&uarr;", "&Ucirc;", "&ucirc;", "&Ugrave;", "&ugrave;",
        "&uml;", "&upsih;", "&Upsilon;", "&upsilon;", "&Uuml;", "&uuml;", "&weierp;", "&Xi;",
        "&xi;", "&Yacute;", "&yacute;", "&yen;", "&Yuml;", "&yuml;", "&Zeta;", "&zeta;",
        "&zwj;", "&zwnj;"
    };
    /** Characters corresponding to ENTITY_NAMES. */
    private final static char[] ENTITY_CHARS = {
        '\u00C2', '\u00E2', '\u00B4', '\u00C6', '\u00E6', '\u00C0', '\u00E0', '\u2135',
        '\u0391', '\u03B1', '\u0026', '\u2227', '\u2220', '\'', '\u00C5', '\u00E5',
        '\u2248', '\u00C3', '\u00E3', '\u00C4', '\u00E4', '\u201E', '\u0392', '\u03B2',
        '\u00A6', '\u2022', '\u2229', '\u00C7', '\u00E7', '\u00B8', '\u00A2', '\u03A7',
        '\u03C7', '\u02C6', '\u2663', '\u2245', '\u00A9', '\u21B5', '\u222A', '\u00A4',
        '\u2021', '\u2020', '\u21D3', '\u2193', '\u00B0', '\u0394', '\u03B4', '\u2666',
        '\u00F7', '\u00C9', '\u00E9', '\u00CA', '\u00EA', '\u00C8', '\u00E8', '\u2205',
        '\u2003', '\u2002', '\u0395', '\u03B5', '\u2261', '\u0397', '\u03B7', '\u00D0',
        '\u00F0', '\u00CB', '\u00EB', '\u20AC', '\u2203', '\u0192', '\u2200', '\u00BD',
        '\u00BC', '\u00BE', '\u2044', '\u0393', '\u03B3', '\u2265', '\u003E', '\u21D4',
        '\u2194', '\u2665', '\u2026', '\u00CD', '\u00ED', '\u00CE', '\u00EE', '\u00A1',
        '\u00CC', '\u00EC', '\u2111', '\u221E', '\u222B', '\u0399', '\u03B9', '\u00BF',
        '\u2208', '\u00CF', '\u00EF', '\u039A', '\u03BA', '\u039B', '\u03BB', '\u2329',
        '\u00AB', '\u21D0', '\u2190', '\u2308', '\u201C', '\u2264', '\u230A', '\u2217',
        '\u25CA', '\u200E', '\u2039', '\u2018', '\u003C', '\u00AF', '\u2014', '\u00B5',
        '\u00B7', '\u2212', '\u039C', '\u03BC', '\u2207', '\u00A0', '\u2013', '\u2260',
        '\u220B', '\u00AC', '\u2209', '\u2284', '\u00D1', '\u00F1', '\u039D', '\u03BD',
        '\u00D3', '\u00F3', '\u00D4', '\u00F4', '\u0152', '\u0153', '\u00D2', '\u00F2',
        '\u203E', '\u03A9', '\u03C9', '\u039F', '\u03BF', '\u2295', '\u2228', '\u00AA',
        '\u00BA', '\u00D8', '\u00F8', '\u00D5', '\u00F5', '\u2297', '\u00D6', '\u00F6',
        '\u00B6', '\u2202', '\u2030', '\u22A5', '\u03A6', '\u03C6', '\u03A0', '\u03C0',
        '\u03D6', '\u00B1', '\u00A3', '\u2033', '\u2032', '\u220F', '\u221D', '\u03A8',
        '\u03C8', '\u0022', '\u221A', '\u232A', '\u00BB', '\u21D2', '\u2192', '\u2309',
        '\u201D', '\u211C', '\u00AE', '\u230B', '\u03A1', '\u03C1', '\u200F', '\u203A',
        '\u2019', '\u201A', '\u0160', '\u0161', '\u22C5', '\u00A7', '\u00AD', '\u03A3',
        '\u03C3', '\u03C2', '\u223C', '\u2660', '\u2282', '\u2286', '\u2211', '\u2283',
        '\u00B9', '\u00B2', '\u00B3', '\u2287', '\u00DF', '\u03A4', '\u03C4', '\u2234',
        '\u0398', '\u03B8', '\u03D1', '\u00DE', '\u00FE', '\u02DC', '\u00D7', '\u2122',
        '\u00DA', '\u00FA', '\u21D1', '\u2191', '\u00DB', '\u00FB', '\u00D9', '\u00F9',
        '\u00A8', '\u03D2', '\u03A5', '\u03C5', '\u00DC', '\u00FC', '\u2118', '\u039E',
        '\u03BE', '\u00DD', '\u00FD', '\u00A5', '\u0178', '\u00FF', '\u0396', '\u03B6',
        '\u200D', '\u200C'
    };
    /** Valid markdown link prefixes for auto links. */
    private final static String[] LINK_PREFIXES = {
        "http", "https",
        "ftp", "ftps"
    };

    /** HTML block level elements. */
    private final static HTMLElement[] BLOCK_ELEMENTS = {
        HTMLElement.address,
        HTMLElement.blockquote,
        HTMLElement.del, HTMLElement.div, HTMLElement.dl,
        HTMLElement.fieldset, HTMLElement.form,
        HTMLElement.h1, HTMLElement.h2, HTMLElement.h3, HTMLElement.h4, HTMLElement.h5, HTMLElement.h6, HTMLElement.hr,
        HTMLElement.ins,
        HTMLElement.noscript,
        HTMLElement.ol,
        HTMLElement.p, HTMLElement.pre,
        HTMLElement.table,
        HTMLElement.ul
    };

    /** HTML unsafe elements. */
    private final static HTMLElement[] UNSAFE_ELEMENTS = {
        HTMLElement.applet,
        HTMLElement.head,
        HTMLElement.html,
        HTMLElement.body,
        HTMLElement.frame,
        HTMLElement.frameset,
        HTMLElement.iframe,
        HTMLElement.script,
        HTMLElement.object,
    };

    /** Character to entity encoding map. */
    private final static HashMap<Character, String> encodeMap           = new HashMap<Character, String>();
    /** Entity to character decoding map. */
    private final static HashMap<String, Character> decodeMap           = new HashMap<String, Character>();
    /** Set of valid HTML tags. */
    private final static HashSet<String>            HTML_ELEMENTS       = new HashSet<String>();
    /** Set of unsafe HTML tags. */
    private final static HashSet<String>            HTML_UNSAFE         = new HashSet<String>();
    /** Set of HTML block level tags. */
    private final static HashSet<String>            HTML_BLOCK_ELEMENTS = new HashSet<String>();
    /** Set of valid markdown link prefixes. */
    private final static HashSet<String>            LINK_PREFIX         = new HashSet<String>();

    static
    {
        for (final HTMLElement h : HTMLElement.values())
        {
            HTML_ELEMENTS.add(h.toString());
        }
        for (final HTMLElement h : UNSAFE_ELEMENTS)
        {
            HTML_UNSAFE.add(h.toString());
        }
        for (final HTMLElement h : BLOCK_ELEMENTS)
        {
            HTML_BLOCK_ELEMENTS.add(h.toString());
        }
        for (int i = 0; i < ENTITY_NAMES.length; i++)
        {
            encodeMap.put(ENTITY_CHARS[i], ENTITY_NAMES[i]);
            decodeMap.put(ENTITY_NAMES[i], ENTITY_CHARS[i]);
        }
        for (int i = 0; i < LINK_PREFIXES.length; i++)
        {
            LINK_PREFIX.add(LINK_PREFIXES[i]);
        }
    }

    /** Constructor. (Singleton) */
    private HTML()
    {
        //
    }

    /**
     * @param value
     *            String to check.
     * @return Returns <code>true</code> if the given String is a link prefix.
     */
    public final static boolean isLinkPrefix(final String value)
    {
        return LINK_PREFIX.contains(value);
    }

    /**
     * @param value
     *            String to check.
     * @return Returns <code>true</code> if the given String is an entity.
     */
    public final static boolean isEntity(final String value)
    {
        return decodeMap.containsKey(value);
    }

    /**
     * @param value
     *            String to check.
     * @return Returns <code>true</code> if the given String is a HTML tag.
     */
    public final static boolean isHtmlElement(final String value)
    {
        return HTML_ELEMENTS.contains(value);
    }

    /**
     * @param value
     *            String to check.
     * @return Returns <code>true</code> if the given String is a HTML block
     *         level tag.
     */
    public final static boolean isHtmlBlockElement(final String value)
    {
        return HTML_BLOCK_ELEMENTS.contains(value);
    }

    /**
     * @param value
     *            String to check.
     * @return Returns <code>true</code> if the given String is an unsafe HTML
     *         tag.
     */
    public final static boolean isUnsafeHtmlElement(final String value)
    {
        return HTML_UNSAFE.contains(value);
    }
}
