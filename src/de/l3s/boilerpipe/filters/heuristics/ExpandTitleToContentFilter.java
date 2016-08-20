/**
 * boilerpipe
 *
 * Copyright (c) 2009 Christian Kohlschütter
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
package de.l3s.boilerpipe.filters.heuristics;

import de.l3s.boilerpipe.BoilerpipeFilter;
import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.document.TextBlock;
import de.l3s.boilerpipe.document.TextDocument;
import de.l3s.boilerpipe.labels.DefaultLabels;

/**
 * Marks all {@link TextBlock}s "content" which are between the headline and the part that
 * has already been marked content, if they are marked {@link DefaultLabels#MIGHT_BE_CONTENT}.
 * 
 * This filter is quite specific to the news domain.
 * 
 * @author Christian Kohlschütter
 */
public final class ExpandTitleToContentFilter implements BoilerpipeFilter {
    public static final ExpandTitleToContentFilter INSTANCE = new ExpandTitleToContentFilter();

    /**
     * Returns the singleton instance for ExpandTitleToContentFilter.
     */
    public static ExpandTitleToContentFilter getInstance() {
        return INSTANCE;
    }

    public boolean process(TextDocument doc)
            throws BoilerpipeProcessingException {
        int i = 0;
        int title = -1;
        int contentStart = -1;
        for (TextBlock tb : doc.getTextBlocks()) {
            if (contentStart == -1 && tb.hasLabel(DefaultLabels.TITLE)) {
                title = i;
                contentStart = -1;
            }
            if (contentStart == -1 && tb.isContent()) {
                contentStart = i;
            }
            
            i++;
        }

        if (contentStart <= title || title == -1) {
            return false;
        }
        boolean changes = false;
        for (TextBlock tb : doc.getTextBlocks().subList(title, contentStart)) {
            if (tb.hasLabel(DefaultLabels.MIGHT_BE_CONTENT)) {
                changes = tb.setIsContent(true) | changes;
            }
        }
        return changes;
    }

}
