/**
 * boilerpipe
 *
 * Copyright (c) 2011 Christian Kohlschütter
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

import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import de.l3s.boilerpipe.BoilerpipeFilter;
import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.document.TextBlock;
import de.l3s.boilerpipe.document.TextDocument;

/**
 * Adds the labels of the preceding block to the current block, optionally adding a prefix.
 * 
 * @author Christian Kohlschütter
 */
public final class AddPrecedingLabelsFilter implements BoilerpipeFilter {

    public static final AddPrecedingLabelsFilter INSTANCE = new AddPrecedingLabelsFilter("");
    public static final AddPrecedingLabelsFilter INSTANCE_PRE = new AddPrecedingLabelsFilter("^");

	private final String labelPrefix;

    /**
     * Creates a new {@link AddPrecedingLabelsFilter} instance.
     *
     * @param maxBlocksDistance The maximum distance in blocks.
     * @param contentOnly 
     */
    public AddPrecedingLabelsFilter(final String labelPrefix) {
        this.labelPrefix = labelPrefix;
    }

    public boolean process(TextDocument doc)
            throws BoilerpipeProcessingException {
        List<TextBlock> textBlocks = doc.getTextBlocks();
        if (textBlocks.size() < 2) {
            return false;
        }

        boolean changes = false;
        int remaining = textBlocks.size();

        TextBlock blockBelow = null;
        TextBlock block;
        for (ListIterator<TextBlock> it = textBlocks.listIterator(textBlocks.size()); it
                .hasPrevious();) {
        	if(--remaining <= 0) {
        		break;
        	}
        	if(blockBelow == null) {
        		blockBelow = it.previous();
        		continue;
        	}
            block = it.previous();
            
            Set<String> labels = block.getLabels();
            if(labels != null && !labels.isEmpty()) {
            	for(String l : labels) {
            		blockBelow.addLabel(labelPrefix+l);
            	}
	            changes = true;
            }
            blockBelow = block;
        }

        return changes;
    }
}
