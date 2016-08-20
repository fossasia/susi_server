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
package de.l3s.boilerpipe.document;

/**
 * Provides shallow statistics on a given TextDocument
 * 
 * @author Christian Kohlschuetter
 */
public final class TextDocumentStatistics {
    private int numWords = 0;
    private int numBlocks = 0;

    /**
     * Computes statistics on a given {@link TextDocument}.
     *
     * @param doc The {@link TextDocument}.
     * @param contentOnly if true then o
     */
    public TextDocumentStatistics(final TextDocument doc, final boolean contentOnly) {
        for (TextBlock tb : doc.getTextBlocks()) {
            if (contentOnly && !tb.isContent()) {
                continue;
            }

            numWords += tb.getNumWords();
            numBlocks++;
        }
    }

    /**
     * Returns the average number of words at block-level (= overall number of words divided by
     * the number of blocks).
     * 
     * @return Average
     */
    public float avgNumWords() {
        return numWords / (float) numBlocks;
    }

    /**
     * Returns the overall number of words in all blocks.
     * 
     * @return Sum
     */
    public int getNumWords() {
        return numWords;
    }
}
