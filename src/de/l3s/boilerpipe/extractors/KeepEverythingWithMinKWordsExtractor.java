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
import de.l3s.boilerpipe.filters.heuristics.SimpleBlockFusionProcessor;
import de.l3s.boilerpipe.filters.simple.MarkEverythingContentFilter;
import de.l3s.boilerpipe.filters.simple.MinWordsFilter;

/**
 * A full-text extractor which extracts the largest text component of a page.
 * For news articles, it may perform better than the {@link DefaultExtractor},
 * but usually worse than {@link ArticleExtractor}.
 * 
 * @author Christian Kohlschütter
 */
public final class KeepEverythingWithMinKWordsExtractor extends ExtractorBase {

    private final MinWordsFilter filter;

    public KeepEverythingWithMinKWordsExtractor(final int kMin) {
        this.filter = new MinWordsFilter(kMin);
    }

    public boolean process(TextDocument doc)
            throws BoilerpipeProcessingException {
        return SimpleBlockFusionProcessor.INSTANCE.process(doc)
                | MarkEverythingContentFilter.INSTANCE.process(doc)
                | filter.process(doc);
    }

}
