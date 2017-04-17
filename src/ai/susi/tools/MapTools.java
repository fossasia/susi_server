/**
 *  MapTools
 *  Copyright 23.03.2016 by Michael Peter Christen, @0rb1t3r
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

package ai.susi.tools;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class MapTools {
    
    /**
     * sort a given map by the value
     * @param map
     * @return a map with the same keys where the key with the highest value is first
     */
    public static <K, V extends Comparable<? super V>> LinkedHashMap<K, V> sortByValue(Map<K, V> map) {
        return map
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                      ));
    }

    public static <K> Map<K, Integer> deatomize(Map<K, AtomicInteger> map) {
        final Map<K, Integer> a = new HashMap<>();
        map.forEach((k, v) -> a.put(k, v.intValue()));
        return a;
    }

    public static <K> void incCounter(Map<K, AtomicInteger> map, K key) {
        incCounter(map, key, 1);
    }
    
    public static <K> void incCounter(Map<K, AtomicInteger> map, K key, int inc) {
        AtomicInteger c = map.get(key);
        if (c == null) {
            c = new AtomicInteger(0);
            map.put(key, c);
        }
        c.addAndGet(inc);
    }
}
