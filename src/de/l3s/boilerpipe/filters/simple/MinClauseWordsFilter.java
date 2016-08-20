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
package de.l3s.boilerpipe.filters.simple;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.l3s.boilerpipe.BoilerpipeFilter;
import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.document.TextBlock;
import de.l3s.boilerpipe.document.TextDocument;

/**
 * Keeps only blocks that have at least one segment fragment ("clause") with at
 * least <em>k</em> words (default: 5).
 * 
 * NOTE: You might consider using the {@link SplitParagraphBlocksFilter}
 * upstream.
 * 
 * @author Christian Kohlschütter
 * @see SplitParagraphBlocksFilter
 */
public final class MinClauseWordsFilter implements BoilerpipeFilter {
    public static final MinClauseWordsFilter INSTANCE = new MinClauseWordsFilter(
            5, false);
    private int minWords;
    private final boolean acceptClausesWithoutDelimiter;

    public MinClauseWordsFilter(final int minWords) {
        this(minWords, false);
    }

    public MinClauseWordsFilter(final int minWords,
            final boolean acceptClausesWithoutDelimiter) {
        this.minWords = minWords;
        this.acceptClausesWithoutDelimiter = acceptClausesWithoutDelimiter;
    }

    private final Pattern PAT_CLAUSE_DELIMITER = Pattern
            .compile("[\\p{L}\\d][\\,\\.\\:\\;\\!\\?]+([ \\n\\r]+|$)");
    private final Pattern PAT_WHITESPACE = Pattern.compile("[ \\n\\r]+");

    public boolean process(final TextDocument doc)
            throws BoilerpipeProcessingException {

        boolean changes = false;
        for (TextBlock tb : doc.getTextBlocks()) {
            if (!tb.isContent()) {
                continue;
            }
            final String text = tb.getText();

            Matcher m = PAT_CLAUSE_DELIMITER.matcher(text);
            boolean found = m.find();
            int start = 0;
            int end;
            boolean hasClause = false;
            while (found) {
                end = m.start() + 1;
                hasClause = isClause(text.subSequence(start, end));
                start = m.end();

                if (hasClause) {
                    break;
                }
                found = m.find();
            }
            end = text.length();

            // since clauses should *always end* with a delimiter, we normally
            // don't consider text without one
            if (acceptClausesWithoutDelimiter) {
                hasClause |= isClause(text.subSequence(start, end));
            }

            if (!hasClause) {
                tb.setIsContent(false);
                changes = true;
                // System.err.println("IS NOT CONTENT: " + text);
            }
        }

        return changes;

    }

    private boolean isClause(final CharSequence text) {
        Matcher m = PAT_WHITESPACE.matcher(text);
        int n = 1;
        while (m.find()) {
            n++;
            if (n >= minWords) {
                return true;
            }
        }
        return n >= minWords;
    }
}
