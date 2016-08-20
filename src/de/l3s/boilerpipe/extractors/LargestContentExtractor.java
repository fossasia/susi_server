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
import de.l3s.boilerpipe.filters.english.NumWordsRulesClassifier;
import de.l3s.boilerpipe.filters.heuristics.BlockProximityFusion;
import de.l3s.boilerpipe.filters.heuristics.KeepLargestBlockFilter;

/**
 * A full-text extractor which extracts the largest text component of a page.
 * For news articles, it may perform better than the {@link DefaultExtractor},
 * but usually worse than {@link ArticleExtractor}.
 * 
 * @author Christian Kohlschütter
 */
public final class LargestContentExtractor extends ExtractorBase {
    public static final LargestContentExtractor INSTANCE = new LargestContentExtractor();

    private LargestContentExtractor() {
    }

    /**
     * Returns the singleton instance for {@link LargestContentExtractor}.
     */
    public static LargestContentExtractor getInstance() {
        return INSTANCE;
    }

    public boolean process(TextDocument doc)
            throws BoilerpipeProcessingException {
        return NumWordsRulesClassifier.INSTANCE.process(doc)
                | BlockProximityFusion.MAX_DISTANCE_1.process(doc)
                | KeepLargestBlockFilter.INSTANCE.process(doc);
    }

}
