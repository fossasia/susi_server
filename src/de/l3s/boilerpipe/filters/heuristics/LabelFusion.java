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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.l3s.boilerpipe.BoilerpipeFilter;
import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.document.TextBlock;
import de.l3s.boilerpipe.document.TextDocument;
import de.l3s.boilerpipe.labels.DefaultLabels;

/**
 * Fuses adjacent blocks if their labels are equal.
 * 
 * @author Christian Kohlschütter
 */
public final class LabelFusion implements BoilerpipeFilter {

    public static final LabelFusion INSTANCE = new LabelFusion();

    /**
     * Creates a new {@link LabelFusion} instance.
     */
    private LabelFusion() {
    }

    public boolean process(TextDocument doc)
            throws BoilerpipeProcessingException {
        List<TextBlock> textBlocks = doc.getTextBlocks();
        if (textBlocks.size() < 2) {
            return false;
        }

        boolean changes = false;
        TextBlock prevBlock = textBlocks.get(0);
        int offset = 1;

        for (Iterator<TextBlock> it = textBlocks.listIterator(offset); it
                .hasNext();) {
            TextBlock block = it.next();

            if(equalLabels(prevBlock.getLabels(), block.getLabels())) {
                prevBlock.mergeNext(block);
                it.remove();
                changes = true;
            } else {
                prevBlock = block;
            }
        }

        return changes;
    }

	private boolean equalLabels(Set<String> labels, Set<String> labels2) {
		if(labels == null || labels2 == null) {
			return false;
		}
		return markupLabelsOnly(labels).equals(markupLabelsOnly(labels2));
	}
	
	private Set<String> markupLabelsOnly(final Set<String> set1) {
		Set<String> set = new HashSet<String>(set1);
		for(Iterator<String> it = set.iterator(); it.hasNext(); ) {
			final String label = it.next();
			if(!label.startsWith(DefaultLabels.MARKUP_PREFIX)) {
				it.remove();
			}
		}
		return set;
	}

}
