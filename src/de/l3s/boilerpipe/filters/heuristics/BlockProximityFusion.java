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
 * Fuses adjacent blocks if their distance (in blocks) does not exceed a certain limit.
 * This probably makes sense only in cases where an upstream filter already has removed some blocks.
 * 
 * @author Christian Kohlschütter
 */
public final class BlockProximityFusion implements BoilerpipeFilter {

    private final int maxBlocksDistance;

    public static final BlockProximityFusion MAX_DISTANCE_1 = new BlockProximityFusion(
            1, false, false);
    public static final BlockProximityFusion MAX_DISTANCE_1_SAME_TAGLEVEL = new BlockProximityFusion(
            1, false, true);
    public static final BlockProximityFusion MAX_DISTANCE_1_CONTENT_ONLY = new BlockProximityFusion(
            1, true, false);
    public static final BlockProximityFusion MAX_DISTANCE_1_CONTENT_ONLY_SAME_TAGLEVEL = new BlockProximityFusion(
            1, true, true);

    private final boolean contentOnly;

	private final boolean sameTagLevelOnly;

    /**
     * Creates a new {@link BlockProximityFusion} instance.
     *
     * @param maxBlocksDistance The maximum distance in blocks.
     * @param contentOnly 
     */
    public BlockProximityFusion(final int maxBlocksDistance,
            final boolean contentOnly, final boolean sameTagLevelOnly) {
        this.maxBlocksDistance = maxBlocksDistance;
        this.contentOnly = contentOnly;
		this.sameTagLevelOnly = sameTagLevelOnly;
    }

    public boolean process(TextDocument doc)
            throws BoilerpipeProcessingException {
        List<TextBlock> textBlocks = doc.getTextBlocks();
        if (textBlocks.size() < 2) {
            return false;
        }

        boolean changes = false;
        TextBlock prevBlock;

        int offset;
        if (contentOnly) {
            prevBlock = null;
            offset = 0;
            for (TextBlock tb : textBlocks) {
                offset++;
                if (tb.isContent()) {
                    prevBlock = tb;
                    break;
                }
            }
            if (prevBlock == null) {
                return false;
            }
        } else {
            prevBlock = textBlocks.get(0);
            offset = 1;
        }

        for (Iterator<TextBlock> it = textBlocks.listIterator(offset); it
                .hasNext();) {
            TextBlock block = it.next();
            if (!block.isContent()) {
                prevBlock = block;
                continue;
            }
            int diffBlocks = block.getOffsetBlocksStart()
                    - prevBlock.getOffsetBlocksEnd() - 1;
            if (diffBlocks <= maxBlocksDistance) {
                boolean ok = true;
                if (contentOnly) {
                    if (!prevBlock.isContent()
                            || !block.isContent()) {
                        ok = false;
                    }
                }
                if(ok && sameTagLevelOnly && prevBlock.getTagLevel() != block.getTagLevel()) {
                	ok = false;
                }
                if (ok) {
                    prevBlock.mergeNext(block);
                    it.remove();
                    changes = true;
                } else {
                    prevBlock = block;
                }
            } else {
                prevBlock = block;
            }
        }

        return changes;
    }

}
