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
import de.l3s.boilerpipe.filters.simple.MinClauseWordsFilter;
import de.l3s.boilerpipe.filters.simple.SplitParagraphBlocksFilter;

/**
 * A full-text extractor which is tuned towards extracting sentences from news articles.
 * 
 * @author Christian Kohlschütter
 */
public final class ArticleSentencesExtractor extends ExtractorBase {
    public static final ArticleSentencesExtractor INSTANCE = new ArticleSentencesExtractor();

    /**
     * Returns the singleton instance for {@link ArticleSentencesExtractor}.
     */
    public static ArticleSentencesExtractor getInstance() {
        return INSTANCE;
    }

    public boolean process(TextDocument doc)
            throws BoilerpipeProcessingException {
        return

        ArticleExtractor.INSTANCE.process(doc)
                | SplitParagraphBlocksFilter.INSTANCE.process(doc)
                | MinClauseWordsFilter.INSTANCE.process(doc);
    }

}
