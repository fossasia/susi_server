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

import java.util.Iterator;
import java.util.List;

import de.l3s.boilerpipe.BoilerpipeFilter;
import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.document.TextBlock;
import de.l3s.boilerpipe.document.TextDocument;
import de.l3s.boilerpipe.labels.DefaultLabels;

/**
 * Removes {@link TextBlock}s which have explicitly been marked as
 * "not content".
 * 
 * @author Christian Kohlschütter
 */
public final class BoilerplateBlockFilter implements BoilerpipeFilter {
	public static final BoilerplateBlockFilter INSTANCE = new BoilerplateBlockFilter(
			null);
	public static final BoilerplateBlockFilter INSTANCE_KEEP_TITLE = new BoilerplateBlockFilter(
			DefaultLabels.TITLE);
	private final String labelToKeep;

	/**
	 * Returns the singleton instance for BoilerplateBlockFilter.
	 */
	public static BoilerplateBlockFilter getInstance() {
		return INSTANCE;
	}

	public BoilerplateBlockFilter(final String labelToKeep) {
		this.labelToKeep = labelToKeep;
	}

	public boolean process(TextDocument doc)
			throws BoilerpipeProcessingException {
		List<TextBlock> textBlocks = doc.getTextBlocks();
		boolean hasChanges = false;

		for (Iterator<TextBlock> it = textBlocks.iterator(); it.hasNext();) {
			TextBlock tb = it.next();
			if (!tb.isContent()
					&& (labelToKeep == null || !tb
							.hasLabel(DefaultLabels.TITLE))) {
				it.remove();
				hasChanges = true;
			}
		}

		return hasChanges;
	}

}
