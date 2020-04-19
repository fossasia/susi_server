/**
 *  LogAppender
 *  Copyright 02.01.2018 by Michael Peter Christen, @0rb1t3r
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

package ai.susi;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;

public class LogAppender extends AppenderSkeleton {

    private int maxlines;
    private ConcurrentLinkedQueue<String> lines;

    public LogAppender(Layout layout, int maxlines) {
        this.layout = layout;
        this.maxlines = maxlines;
        this.lines = new ConcurrentLinkedQueue<>();
        String line = layout.getHeader();
        if (line != null) this.lines.add(line);
    }

    @Override
    public void append(LoggingEvent event) {
        if (event == null) return;
        String line = this.layout.format(event);
        if (line != null) this.lines.add(line);
        if (event.getThrowableInformation() != null) {
            for (String t: event.getThrowableStrRep()) if (t != null)  this.lines.add(t + "\n");
        }
        clean(this.maxlines);
    }

    @Override
    public void close() {
        lines.clear();
        lines = null;
    }

    @Override
    public boolean requiresLayout() {
        return true;
    }

    public ArrayList<String> getLines(int max) {
        Object[] a = this.lines.toArray();
        ArrayList<String> l = new ArrayList<>();
        int start = Math.max(0, a.length - max);
        for (int i = start; i < a.length; i++) l.add((String) a[i]);
        return l;
    }

    public void clean(int remaining) {
        int c = this.lines.size() - remaining;
        while (c-- > 0) this.lines.poll();
    }
}
