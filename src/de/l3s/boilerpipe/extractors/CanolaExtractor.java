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
package de.l3s.boilerpipe.extractors;

import java.util.List;
import java.util.ListIterator;

import de.l3s.boilerpipe.BoilerpipeFilter;
import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.document.TextBlock;
import de.l3s.boilerpipe.document.TextDocument;
import de.l3s.boilerpipe.estimators.SimpleEstimator;

/**
 * A full-text extractor trained on <a href="http://krdwrd.org/">krdwrd</a> <a
 * href
 * ="https://krdwrd.org/trac/attachment/wiki/Corpora/Canola/CANOLA.pdf">Canola
 * </a>. Works well with {@link SimpleEstimator}, too.
 * 
 * @author Christian Kohlschütter
 */
public class CanolaExtractor extends ExtractorBase {
	public static final CanolaExtractor INSTANCE = new CanolaExtractor();

	/**
	 * Returns the singleton instance for {@link CanolaExtractor}.
	 */
	public static CanolaExtractor getInstance() {
		return INSTANCE;
	}

	public boolean process(TextDocument doc)
			throws BoilerpipeProcessingException {

		return CLASSIFIER.process(doc);
	}

	/**
	 * The actual classifier, exposed.
	 */
	public static final BoilerpipeFilter CLASSIFIER = new BoilerpipeFilter() {

		public boolean process(TextDocument doc)
				throws BoilerpipeProcessingException {
			List<TextBlock> textBlocks = doc.getTextBlocks();
			boolean hasChanges = false;

			ListIterator<TextBlock> it = textBlocks.listIterator();
			if (!it.hasNext()) {
				return false;
			}
			TextBlock prevBlock = TextBlock.EMPTY_START;
			TextBlock currentBlock = it.next();
			TextBlock nextBlock = it.hasNext() ? it.next()
					: TextBlock.EMPTY_START;

			hasChanges = classify(prevBlock, currentBlock, nextBlock)
					| hasChanges;

			if (nextBlock != TextBlock.EMPTY_START) {
				while (it.hasNext()) {
					prevBlock = currentBlock;
					currentBlock = nextBlock;
					nextBlock = it.next();
					hasChanges = classify(prevBlock, currentBlock, nextBlock)
							| hasChanges;
				}
				prevBlock = currentBlock;
				currentBlock = nextBlock;
				nextBlock = TextBlock.EMPTY_START;
				hasChanges = classify(prevBlock, currentBlock, nextBlock)
						| hasChanges;
			}

			return hasChanges;
		}

		protected boolean classify(final TextBlock prev, final TextBlock curr,
				final TextBlock next) {
			final boolean isContent = (curr.getLinkDensity() > 0 && next
					.getNumWords() > 11)
					|| (curr.getNumWords() > 19 || (next.getNumWords() > 6
							&& next.getLinkDensity() == 0
							&& prev.getLinkDensity() == 0 && (curr
							.getNumWords() > 6 || prev.getNumWords() > 7 || next
							.getNumWords() > 19)));

			return curr.setIsContent(isContent);
		}
	};
}
