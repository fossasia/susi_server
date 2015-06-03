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
 * A markdown link reference.
 *
 * @author René Jeschke <rene_jeschke@yahoo.de>
 */
class LinkRef
{
    /** The link. */
    public final String  link;
    /** The optional comment/title. */
    public String        title;
    /** Flag indicating that this is an abbreviation. */
    public final boolean isAbbrev;

    /**
     * Constructor.
     *
     * @param link
     *            The link.
     * @param title
     *            The title (may be <code>null</code>).
     */
    public LinkRef(final String link, final String title, final boolean isAbbrev)
    {
        this.link = link;
        this.title = title;
        this.isAbbrev = isAbbrev;
    }

    /** @see java.lang.Object#toString() */
    @Override
    public String toString()
    {
        return this.link + " \"" + this.title + "\"";
    }
}
