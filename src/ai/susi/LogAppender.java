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
import java.util.List;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;

public class LogAppender extends AppenderSkeleton {

    private int maxlines;
    private List<String> lines;

    public LogAppender(Layout layout, int maxlines) {
        this.layout = layout;
        this.maxlines = maxlines;
        this.lines = new ArrayList<>();
        String line = layout.getHeader();
        this.lines.add(line);
    }

    @Override
    public void append(LoggingEvent event) {
        if (event == null) return;
        String line = this.layout.format(event);
        this.lines.add(line);
        if (event.getThrowableInformation() != null) {
            for (String t: event.getThrowableStrRep()) this.lines.add(t + "\n");
        }
        while (this.lines.size() > this.maxlines) {
            this.lines.remove(0);
        }
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
    
    public List<String> getLines() {
        return this.lines;
    }
}
