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
package de.l3s.boilerpipe.conditions;

import de.l3s.boilerpipe.document.TextBlock;
import de.l3s.boilerpipe.labels.ConditionalLabelAction;

/**
 * Evaluates whether a given {@link TextBlock} meets a certain condition.
 * Useful in combination with {@link ConditionalLabelAction}.
 * 
 * @author Christian Kohlschuetter
 */
public interface TextBlockCondition {
    /**
     * Returns <code>true</code> iff the given {@link TextBlock} tb meets the defined condition.
     * 
     * @param tb
     * @return <code><true</code> iff the condition is met.
     */
    boolean meetsCondition(final TextBlock tb);
}
