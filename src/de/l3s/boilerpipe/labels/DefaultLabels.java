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
package de.l3s.boilerpipe.labels;

import de.l3s.boilerpipe.document.TextBlock;

/**
 * Some pre-defined labels which can be used in conjunction with
 * {@link TextBlock#addLabel(String)} and {@link TextBlock#hasLabel(String)}.
 * 
 * @author Christian Kohlschütter
 */
public final class DefaultLabels {
    public static final String TITLE = "de.l3s.boilerpipe/TITLE";
    public static final String ARTICLE_METADATA = "de.l3s.boilerpipe/ARTICLE_METADATA";
    public static final String INDICATES_END_OF_TEXT = "de.l3s.boilerpipe/INDICATES_END_OF_TEXT";
    public static final String MIGHT_BE_CONTENT = "de.l3s.boilerpipe/MIGHT_BE_CONTENT";
    public static final String VERY_LIKELY_CONTENT = "de.l3s.boilerpipe/VERY_LIKELY_CONTENT";
    public static final String STRICTLY_NOT_CONTENT = "de.l3s.boilerpipe/STRICTLY_NOT_CONTENT";
    public static final String HR = "de.l3s.boilerpipe/HR";
    public static final String LI = "de.l3s.boilerpipe/LI";

    public static final String HEADING = "de.l3s.boilerpipe/HEADING";
    public static final String H1 = "de.l3s.boilerpipe/H1";
    public static final String H2 = "de.l3s.boilerpipe/H2";
    public static final String H3 = "de.l3s.boilerpipe/H3";
    
    public static final String MARKUP_PREFIX = "<";
    
    private DefaultLabels() {
    	// not to be instantiated
    }
}
