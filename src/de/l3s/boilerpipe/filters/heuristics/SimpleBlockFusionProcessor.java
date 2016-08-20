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

import java.util.Iterator;
import java.util.List;

import de.l3s.boilerpipe.BoilerpipeFilter;
import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.document.TextBlock;
import de.l3s.boilerpipe.document.TextDocument;

/**
 * Merges two subsequent blocks if their text densities are equal.
 * 
 * @author Christian Kohlschütter
 */
public class SimpleBlockFusionProcessor implements BoilerpipeFilter {
    public static final SimpleBlockFusionProcessor INSTANCE = new SimpleBlockFusionProcessor();

    /**
     * Returns the singleton instance for BlockFusionProcessor.
     */
    public static SimpleBlockFusionProcessor getInstance() {
        return INSTANCE;
    }

    public boolean process(TextDocument doc)
            throws BoilerpipeProcessingException {
        List<TextBlock> textBlocks = doc.getTextBlocks();
        boolean changes = false;

        if (textBlocks.size() < 2) {
            return false;
        }

        TextBlock b1 = textBlocks.get(0);
        for (Iterator<TextBlock> it = textBlocks.listIterator(1); it.hasNext();) {
            TextBlock b2 = it.next();

            final boolean similar = (b1.getTextDensity() == b2.getTextDensity());
            
            if(similar) {
                b1.mergeNext(b2);
                it.remove();
                changes = true;
            } else {
                b1 = b2;
            }
        }

        return changes;
    }

}
