/**
 * boilerpipe
 *
 * Copyright (c) 2012 Christian Kohlschütter
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
package de.l3s.boilerpipe.filters.debug;

import java.io.PrintWriter;

import de.l3s.boilerpipe.BoilerpipeFilter;
import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.document.TextDocument;

/**
 * Prints debug information about the current state of the TextDocument. (=
 * calls {@link TextDocument#debugString()}.
 * 
 * @author Christian Kohlschütter
 */
public final class PrintDebugFilter implements BoilerpipeFilter {
	/**
	 * Returns the default instance for {@link PrintDebugFilter},
	 * which dumps debug information to <code>System.out</code>
	 */
	public static final PrintDebugFilter INSTANCE = new PrintDebugFilter(
			new PrintWriter(System.out, true));
	private final PrintWriter out;

	/**
	 * Returns the default instance for {@link PrintDebugFilter},
	 * which dumps debug information to <code>System.out</code>
	 */
	public static PrintDebugFilter getInstance() {
		return INSTANCE;
	}

	/**
	 * Creates a new instance of {@link PrintDebugFilter}.
	 * 
	 * Only use this method if you are not going to dump 
	 * the debug information to <code>System.out</code> --
	 * for this case, use {@link #getInstance()} instead. 
	 * 
	 * @param out The target {@link PrintWriter}. Will not be closed
	 */
	public PrintDebugFilter(final PrintWriter out) {
		this.out = out;

	}

	@Override
	public boolean process(TextDocument doc)
			throws BoilerpipeProcessingException {
		out.println(doc.debugString());

		return false;
	}
}
