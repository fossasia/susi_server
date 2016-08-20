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

import java.util.HashMap;

/**
 * Base class for definition a set of {@link TagAction}s that are to be used for the
 * HTML parsing process.
 * 
 * @see DefaultTagActionMap
 * @author Christian Kohlschütter
 */
public abstract class TagActionMap extends HashMap<String, TagAction> {
    private static final long serialVersionUID = 1L;

    /**
     * Sets a particular {@link TagAction} for a given tag. Any existing TagAction for that tag
     * will be removed and overwritten.
     * 
     * @param tag The tag (will be stored internally 1. as it is, 2. lower-case, 3. upper-case)
     * @param action The {@link TagAction}
     */
    protected void setTagAction(final String tag, final TagAction action) {
        put(tag.toUpperCase(), action);
        put(tag.toLowerCase(), action);
        put(tag, action);
    }

    /**
     * Adds a particular {@link TagAction} for a given tag. If a TagAction already exists for that tag,
     * a chained action, consisting of the previous and the new {@link TagAction} is created.
     * 
     * @param tag The tag (will be stored internally 1. as it is, 2. lower-case, 3. upper-case)
     * @param action The {@link TagAction}
     */
    protected void addTagAction(final String tag, final TagAction action) {
        TagAction previousAction = get(tag);
        if(previousAction == null) {
            setTagAction(tag, action);
        } else {
            setTagAction(tag, new CommonTagActions.Chained(previousAction, action));
        }
    }
}
