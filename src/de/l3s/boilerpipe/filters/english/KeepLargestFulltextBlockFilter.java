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
package de.l3s.boilerpipe.filters.english;

import java.util.List;

import de.l3s.boilerpipe.BoilerpipeFilter;
import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.document.TextBlock;
import de.l3s.boilerpipe.document.TextDocument;
import de.l3s.boilerpipe.filters.heuristics.KeepLargestBlockFilter;
import de.l3s.boilerpipe.labels.DefaultLabels;

/**
 * Keeps the largest {@link TextBlock} only (by the number of words). In case of
 * more than one block with the same number of words, the first block is chosen.
 * All discarded blocks are marked "not content" and flagged as
 * {@link DefaultLabels#MIGHT_BE_CONTENT}.
 * 
 * As opposed to {@link KeepLargestBlockFilter}, the number of words are
 * computed using {@link HeuristicFilterBase#getNumFullTextWords(TextBlock)}, which only counts
 * words that occur in text elements with at least 9 words and are thus believed to be full text.
 * 
 * NOTE: Without language-specific fine-tuning (i.e., running the default instance), this filter
 * may lead to suboptimal results. You better use {@link KeepLargestBlockFilter} instead, which
 * works at the level of number-of-words instead of text densities.
 * 
 * @author Christian Kohlschütter
 */
public final class KeepLargestFulltextBlockFilter extends HeuristicFilterBase implements BoilerpipeFilter {
    public static final KeepLargestFulltextBlockFilter INSTANCE = new KeepLargestFulltextBlockFilter();

    public boolean process(final TextDocument doc)
            throws BoilerpipeProcessingException {
        List<TextBlock> textBlocks = doc.getTextBlocks();
        if (textBlocks.size() < 2) {
            return false;
        }

        int max = -1;
        TextBlock largestBlock = null;
        for (TextBlock tb : textBlocks) {
            if (!tb.isContent()) {
                continue;
            }
            int numWords = getNumFullTextWords(tb);
            if (numWords > max) {
                largestBlock = tb;
                max = numWords;
            }
        }
        
        if (largestBlock == null) {
            return false;
        }

        for (TextBlock tb : textBlocks) {
            if (tb == largestBlock) {
                tb.setIsContent(true);
            } else {
                tb.setIsContent(false);
                tb.addLabel(DefaultLabels.MIGHT_BE_CONTENT);
            }
        }

        return true;
    }
}
