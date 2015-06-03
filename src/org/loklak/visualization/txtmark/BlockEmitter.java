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

import java.util.List;

/**
 * Block emitter interface. An example for a code block emitter is given below:
 *
 * <pre>
 * <code>public void emitBlock(StringBuilder out, List&lt;String&gt; lines, String meta)
 * {
 *     out.append("&lt;pre&gt;&lt;code&gt;");
 *     for(final String s : lines)
 *     {
 *         for(int i = 0; i &lt; s.length(); i++)
 *         {
 *             final char c = s.charAt(i);
 *             switch(c)
 *             {
 *             case '&amp;':
 *                 out.append("&amp;amp;");
 *                 break;
 *             case '&lt;':
 *                 out.append("&amp;lt;");
 *                 break;
 *             case '&gt;':
 *                 out.append("&amp;gt;");
 *                 break;
 *             default:
 *                 out.append(c);
 *                 break;
 *             }
 *         }
 *         out.append('\n');
 *     }
 *     out.append("&lt;/code&gt;&lt;/pre&gt;\n");
 * }
 * </code>
 * </pre>
 *
 *
 * @author René Jeschke &lt;rene_jeschke@yahoo.de&gt;
 * @since 0.7
 */
public interface BlockEmitter
{
    /**
     * This method is responsible for outputting a markdown block and for any
     * needed pre-processing like escaping HTML special characters.
     *
     * @param out
     *            The StringBuilder to append to
     * @param lines
     *            List of lines
     * @param meta
     *            Meta information as a single String (if any) or empty String
     */
    public void emitBlock(StringBuilder out, List<String> lines, String meta);
}
