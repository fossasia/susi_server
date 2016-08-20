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
package de.l3s.boilerpipe.extractors;

import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.document.TextDocument;
import de.l3s.boilerpipe.filters.english.IgnoreBlocksAfterContentFilter;
import de.l3s.boilerpipe.filters.english.NumWordsRulesClassifier;
import de.l3s.boilerpipe.filters.english.TerminatingBlocksFinder;
import de.l3s.boilerpipe.filters.heuristics.BlockProximityFusion;
import de.l3s.boilerpipe.filters.heuristics.DocumentTitleMatchClassifier;
import de.l3s.boilerpipe.filters.heuristics.ExpandTitleToContentFilter;
import de.l3s.boilerpipe.filters.heuristics.KeepLargestBlockFilter;
import de.l3s.boilerpipe.filters.heuristics.LargeBlockSameTagLevelToContentFilter;
import de.l3s.boilerpipe.filters.heuristics.ListAtEndFilter;
import de.l3s.boilerpipe.filters.heuristics.TrailingHeadlineToBoilerplateFilter;
import de.l3s.boilerpipe.filters.simple.BoilerplateBlockFilter;

/**
 * A full-text extractor which is tuned towards news articles. In this scenario
 * it achieves higher accuracy than {@link DefaultExtractor}.
 * 
 * @author Christian Kohlschütter
 */
public final class ArticleExtractor extends ExtractorBase {
    public static final ArticleExtractor INSTANCE = new ArticleExtractor();

    /**
     * Returns the singleton instance for {@link ArticleExtractor}.
     */
    public static ArticleExtractor getInstance() {
        return INSTANCE;
    }
    
    public boolean process(TextDocument doc)
            throws BoilerpipeProcessingException {
        return

        TerminatingBlocksFinder.INSTANCE.process(doc)
                | new DocumentTitleMatchClassifier(doc.getTitle()).process(doc)
                | NumWordsRulesClassifier.INSTANCE.process(doc)
                | IgnoreBlocksAfterContentFilter.DEFAULT_INSTANCE.process(doc)
                | TrailingHeadlineToBoilerplateFilter.INSTANCE.process(doc)
                | BlockProximityFusion.MAX_DISTANCE_1.process(doc)
                | BoilerplateBlockFilter.INSTANCE_KEEP_TITLE.process(doc)
                | BlockProximityFusion.MAX_DISTANCE_1_CONTENT_ONLY_SAME_TAGLEVEL.process(doc)
                | KeepLargestBlockFilter.INSTANCE_EXPAND_TO_SAME_TAGLEVEL_MIN_WORDS.process(doc)
                | ExpandTitleToContentFilter.INSTANCE.process(doc)
                | LargeBlockSameTagLevelToContentFilter.INSTANCE.process(doc)
                | ListAtEndFilter.INSTANCE.process(doc)
        ;
    }
}
