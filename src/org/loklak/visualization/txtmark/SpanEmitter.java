/*
 * Copyright (C) 2011-2015 René Jeschke <rene_jeschke@yahoo.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// this package was migrated from com.github.rjeschke.txtmark
package org.loklak.visualization.txtmark;

/**
 * An interface for emitting span elements. Currently only used for special
 * links.
 *
 * @author René Jeschke (rene_jeschke@yahoo.de)
 */
public interface SpanEmitter
{
    /**
     * Emits a span element.
     *
     * @param out
     *            The StringBuilder to append to.
     * @param content
     *            The span's content.
     */
    public void emitSpan(StringBuilder out, String content);
}
