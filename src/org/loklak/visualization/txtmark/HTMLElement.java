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

/**
 * Enum of HTML tags.
 *
 * @author René Jeschke <rene_jeschke@yahoo.de>
 */
enum HTMLElement
{
    // TODO add new HTML5 elements
    NONE,
    a, abbr, acronym, address, applet, area,
    b, base, basefont, bdo, big, blockquote, body, br, button,
    caption, cite, code, col, colgroup,
    dd, del, dfn, div, dl, dt,
    em,
    fieldset, font, form, frame, frameset,
    h1, h2, h3, h4, h5, h6, head, hr, html,
    i, iframe, img, input, ins,
    kbd,
    label, legend, li, link,
    map, meta,
    noscript,
    object, ol, optgroup, option,
    p, param, pre,
    q,
    s, samp, script, select, small, span, strike, strong, style, sub, sup,
    table, tbody, td, textarea, tfoot, th, thead, title, tr, tt,
    u, ul,
    var
}
