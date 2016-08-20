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
package de.l3s.boilerpipe.estimators;

import de.l3s.boilerpipe.BoilerpipeExtractor;
import de.l3s.boilerpipe.document.TextDocumentStatistics;
import de.l3s.boilerpipe.extractors.ArticleExtractor;
import de.l3s.boilerpipe.extractors.DefaultExtractor;

/**
 * Estimates the "goodness" of a {@link BoilerpipeExtractor} on a given document.
 * 
 * @author Christian Kohlschütter
 */
public final class SimpleEstimator {

	/**
	 * Returns the singleton instance of {@link SimpleEstimator}
	 */
    public static final SimpleEstimator INSTANCE = new SimpleEstimator();
    
    private SimpleEstimator() {
    }
    
    /**
     * Given the statistics of the document before and after applying the {@link BoilerpipeExtractor},
     * can we regard the extraction quality (too) low?
     * 
     * Works well with {@link DefaultExtractor}, {@link ArticleExtractor} and others.
     * 
     * @param dsBefore
     * @param dsAfter
     * @return true if low quality is to be expected. 
     */
    public boolean isLowQuality(final TextDocumentStatistics dsBefore, final TextDocumentStatistics dsAfter) {
        if (dsBefore.getNumWords() < 90 || dsAfter.getNumWords() < 70) {
            return true;
        }

        if (dsAfter.avgNumWords() < 25) {
            return true;
        }

        return false;
    }

}
