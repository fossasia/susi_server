/**
 *  SusiProcedures
 *  Copyright 14.07.2016 by Michael Peter Christen, @0rb1t3r
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *  
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program in the file lgpl21.txt
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package ai.susi.mind;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ai.susi.tools.TimeoutMatcher;

public class SusiProcedures extends LinkedHashMap<Pattern, BiFunction<SusiArgument, Matcher, SusiThought>> implements Map<Pattern, BiFunction<SusiArgument, Matcher, SusiThought>> {

    private static final long serialVersionUID = 4531596762427825563L;

    public SusiProcedures() {
        super();
    }

    /**
     * Deduction is the application of an intent on perception and a world model.
     * In this method the mappings from the intent set is applied to the perception q and previous
     * deduction steps as given with the flow. Every mapping that has a matcher 
     * with the perception causes the application of the stored lambda function on the perception
     * producing a thought. If the thought generation is not successful (which means that the lambda
     * fails or produces a null output) then the next mappings from the intent set is tried.
     * In case that no inspiration is possible, an empty thought is produced, containing nothing.
     * @param q the perception
     * @return a thought from the application of the intent set
     */
    public SusiThought deduce(SusiArgument flow, String q) {
        if (q == null) return new SusiThought();
        q = q.trim();
        for (Map.Entry<Pattern, BiFunction<SusiArgument, Matcher, SusiThought>> pe: this.entrySet()) {
            Pattern p = pe.getKey();
            Matcher m = p.matcher(q);
            if (new TimeoutMatcher(m).matches()) try {
                SusiThought json = pe.getValue().apply(flow, m);
                if (json != null) {
                    json.setProcess(p.pattern());
                    return json;
                }
            } catch (Throwable e) {
                // applying an intent may produce various failure, including
                // - IOExceptions if the intent needs external resources
                // - NullPointerException if the intent needs a flow which can be null in case of the attempt of an inspiration
                // we silently ignore these exceptions as they are normal and acceptable during thinking
            }
        }

        // no success: produce an empty thought
        return new SusiThought();
    }

    /**
     * Inspiration is the application of an intent on a minimum perception. In this method the mappings
     * from the intent set is applied only to the perception q.
     * @param q the perception
     * @return a thought from the application of the intent set
     */
    public SusiThought inspire(String q) {
        return deduce(null, q);
    }
}
