/**
 *  TimeoutMatcher
 *  Copyright 13.03.2017 by Michael Peter Christen, @0rb1t3r
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;

import ai.susi.DAO;

/**
 * A timeout matcher is a workaround to non-terminating matcher methods.
 * Such non-terminating matcher exist as a bug inside java, as documented
 * in https://bugs.openjdk.java.net/browse/JDK-8158615
 * Since the bugfix is only available in Java 9, we need a permanent patch
 * arount that. All matcher calls must use this method to ensure termination
 * of matchers.
 */
public class TimeoutMatcher {

    private final static ExecutorService EXECUTOR = new ThreadPoolExecutor(
            0, 1000, 10L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
    private final static Map<Long, Thread> computing = new ConcurrentHashMap<>();
    
    private final Matcher matcher;
    
    public TimeoutMatcher(Matcher matcher) {
        this.matcher = matcher;
    }

    public boolean matches() {
        Future<Boolean> future = EXECUTOR.submit(new Callable<Boolean>(){
            @Override
            public Boolean call() throws Exception {
                Thread t = Thread.currentThread();
                t.setName("TimeoutMatcher: '" + TimeoutMatcher.this.matcher.pattern() + "'");
                computing.put(t.getId(), t);
                boolean matches = false;
                try {
                    matches = TimeoutMatcher.this.matcher.matches();
                } catch (Throwable e) {} finally {
                    computing.remove(t.getId());
                }
                return matches;
            }
        });
        try {
            return future.get(1, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            future.cancel(true);
            return false;
        }
    }
    
    public boolean find() {
        Future<Boolean> future = EXECUTOR.submit(new Callable<Boolean>(){
            @Override
            public Boolean call() throws Exception {
                Thread t = Thread.currentThread();
                t.setName("TimeoutMatcher: '" + TimeoutMatcher.this.matcher.pattern() + "'");
                computing.put(t.getId(), t);
                boolean find = false;
                try {
                    find = TimeoutMatcher.this.matcher.find();
                } catch (Throwable e) {} finally {
                    computing.remove(t.getId());
                }
                return find;
            }
        });
        try {
            return future.get(1, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            return false;
        }
    }
    
    public static void terminateAll() {
        List<Long> deleteIds = new ArrayList<>();
        computing.values().forEach(job -> {
            if (job.isAlive()) try {
                job.interrupt();
                DAO.log("interrupting TimeoutMatcher: '" + job.getName() + "'");
            } catch (Throwable e) {}
            else deleteIds.add(job.getId());
        });
        deleteIds.forEach(id -> computing.remove(id));
    }
}
