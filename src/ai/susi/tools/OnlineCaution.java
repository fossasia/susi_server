/**
 *  OnlineCaution
 *  Copyright 04.01.2020 by Michael Peter Christen, @0rb1t3r
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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;


public class OnlineCaution {

    public final static long CAUTION_DEFAULT_DELAY = 3000;

    private static enum ename {CAUSE, DETAIL};

    private static AtomicLong timeout = new AtomicLong(0);
    private static ConcurrentHashMap<ename, String> event = new ConcurrentHashMap<>();

    public static void demand(String cause) {
        demand(cause, "");
    }

    public static void demand(String cause, String detail) {
        demand(cause, detail, CAUTION_DEFAULT_DELAY);
    }

    public static void demand(String cause, long time) {
        demand(cause, "", time);
    }

    public static void demand(String cause, String detail, long time) {
        timeout.set(System.currentTimeMillis() + time);
        event.put(ename.CAUSE, cause);
        event.put(ename.DETAIL, detail);
    }

    /**
     * check if a process should respect online caution
     * @return null if nothing to respect and a string if a process is ongoing, using the name of that cause
     */
    public static String respect() {
        if (timeout.get() <= System.currentTimeMillis()) return null;
        return event.get(ename.CAUSE);
    }

    /**
     * check if a process should respect online caution
     * @return true if online caution is enduring and should be respected, false otherwise
     */
    public static boolean enduring() {
        return timeout.get() > System.currentTimeMillis();
    }

    /**
     * throttle a process using this method to wait until a online caution has passed
     * @param timeout the maximum timeout for throttling
     * @return true if the throttling was applied and time of waiting has passed; false if no waiting is necessary
     */
    public static boolean throttle(long t) {
        if (!enduring()) return false;
        long wait = Math.min(timeout.get() - System.currentTimeMillis(), t);
        if (wait > 0) try {Thread.sleep(wait);} catch (InterruptedException e) {}
        return true;
    }
}
