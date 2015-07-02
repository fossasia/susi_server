/**
 *  ThreaddumpServlet
 *  Copyright 03.07.2015 by Michael Peter Christen, @0rb1t3r
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

package org.loklak.api.server;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.loklak.tools.CharacterCoding;
import org.loklak.tools.UTF8;

public class ThreaddumpServlet extends HttpServlet {

    private static final long serialVersionUID = -7095346222464124198L;

    private static final String multiDumpFilter = ".*((java.net.DatagramSocket.receive)|(java.lang.Thread.getAllStackTraces)|(java.net.SocketInputStream.read)|(java.net.ServerSocket.accept)|(java.net.Socket.connect)).*";
    private static final Pattern multiDumpFilterPattern = Pattern.compile(multiDumpFilter);


    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        RemoteAccess.Post post = RemoteAccess.evaluate(request);

        int multi = post.isLocalhostAccess() ? post.get("multi", 0) : 0;
        final StringBuilder buffer = new StringBuilder(1000);

        // Thread dump
        final Date dt = new Date();
        Runtime runtime = Runtime.getRuntime();

        bufferappend(buffer, "************* Start Thread Dump " + dt + " *******************");
        bufferappend(buffer, "");
        bufferappend(buffer, "Assigned   Memory = " + (runtime.maxMemory()));
        bufferappend(buffer, "Used       Memory = " + (runtime.totalMemory() - runtime.freeMemory()));
        bufferappend(buffer, "Available  Memory = " + (runtime.maxMemory() - runtime.totalMemory() + runtime.freeMemory()));
        bufferappend(buffer, "");
        bufferappend(buffer, "");

        if (multi > 0) {
            final ArrayList<Map<Thread,StackTraceElement[]>> traces = new ArrayList<Map<Thread,StackTraceElement[]>>();
            for (int i = 0; i < multi; i++) {
                try {
                    traces.add(ThreadDump.getAllStackTraces());
                } catch (final OutOfMemoryError e) {
                    break;
                }
            }
            appendStackTraceStats(buffer, traces);
        } else {
            // generate a single thread dump
            final Map<Thread,StackTraceElement[]> stackTraces = ThreadDump.getAllStackTraces();
            new ThreadDump(stackTraces, Thread.State.BLOCKED).appendStackTraces(buffer, Thread.State.BLOCKED);
            new ThreadDump(stackTraces, Thread.State.RUNNABLE).appendStackTraces(buffer, Thread.State.RUNNABLE);
            new ThreadDump(stackTraces, Thread.State.TIMED_WAITING).appendStackTraces(buffer, Thread.State.TIMED_WAITING);
            new ThreadDump(stackTraces, Thread.State.WAITING).appendStackTraces(buffer, Thread.State.WAITING);
            new ThreadDump(stackTraces, Thread.State.NEW).appendStackTraces(buffer, Thread.State.NEW);
            new ThreadDump(stackTraces, Thread.State.TERMINATED).appendStackTraces(buffer, Thread.State.TERMINATED);
        }

        bufferappend(buffer, "************* End Thread Dump " + dt + " *******************");


        bufferappend(buffer, "");
        ThreadMXBean threadbean = ManagementFactory.getThreadMXBean();
        bufferappend(buffer, "Thread list from ThreadMXBean, " + threadbean.getThreadCount() + " threads:");
        ThreadInfo[] threadinfo = threadbean.dumpAllThreads(true, true);
        for (ThreadInfo ti: threadinfo) {
            bufferappend(buffer, ti.getThreadName());
        }

        post.setResponse(response, "text/plain");
        response.getOutputStream().write(UTF8.getBytes(buffer.toString()));
    }

    public static class StackTrace {
        public String text;
        public StackTrace(final String text) {
            this.text = text;
        }
        @Override
        public boolean equals(final Object a) {
            return (a != null && a instanceof StackTrace && this.text.equals(((StackTrace) a).text));
        }
        public boolean equals(final StackTrace a) {
            return (a != null && this.text.equals(a.text));
        }
        @Override
        public int hashCode() {
            return this.text.hashCode();
        }
        @Override
        public String toString() {
            return this.text;
        }
    }
    
    public static void appendStackTraceStats(
            final StringBuilder buffer,
            final List<Map<Thread, StackTraceElement[]>> stackTraces) {

        // collect single dumps
        final Map<String, Integer> dumps = new HashMap<String, Integer>();
        ThreadDump x;
        for (final Map<Thread, StackTraceElement[]> trace: stackTraces) {
            x = new ThreadDump(trace, Thread.State.RUNNABLE);
            for (final Map.Entry<StackTrace, List<String>> e: x.entrySet()) {
                if (multiDumpFilterPattern.matcher(e.getKey().text).matches()) continue;
                Integer c = dumps.get(e.getKey().text);
                if (c == null) dumps.put(e.getKey().text, Integer.valueOf(1));
                else {
                    c = Integer.valueOf(c.intValue() + 1);
                    dumps.put(e.getKey().text, c);
                }
            }
        }

        // write dumps
        while (!dumps.isEmpty()) {
            final Map.Entry<String, Integer> e = removeMax(dumps);
            bufferappend(buffer, "Occurrences: " + e.getValue());
            bufferappend(buffer, e.getKey());
            bufferappend(buffer, "");
        }
        bufferappend(buffer, "");
    }

    private static Map.Entry<String, Integer> removeMax(final Map<String, Integer> result) {
        Map.Entry<String, Integer> max = null;
        for (final Map.Entry<String, Integer> e: result.entrySet()) {
            if (max == null || e.getValue().intValue() > max.getValue().intValue()) {
                max = e;
            }
        }
        result.remove(max.getKey());
        return max;
    }

    public static void bufferappend(final StringBuilder buffer, final String a) {
        buffer.append(a);
        buffer.append("\n");
    }
    
    public static class ThreadDump extends HashMap<StackTrace, List<String>> implements Map<StackTrace, List<String>> {

        private static final long serialVersionUID = -5587850671040354397L;

        public static Map<Thread, StackTraceElement[]> getAllStackTraces() {
            return Thread.getAllStackTraces();
        }

        public ThreadDump(
                final Map<Thread, StackTraceElement[]> stackTraces,
                final Thread.State stateIn) {
            super();

            Thread thread;
            // collect single dumps
            for (final Map.Entry<Thread, StackTraceElement[]> entry: stackTraces.entrySet()) {
                thread = entry.getKey();
                final StackTraceElement[] stackTraceElements = entry.getValue();
                StackTraceElement ste;
                String tracename = "";
                if ((stateIn == null || stateIn.equals(thread.getState())) && stackTraceElements.length > 0) {
                    final StringBuilder sb = new StringBuilder(3000);
                    final String threadtitle = tracename + "Thread= " + thread.getName() + " " + (thread.isDaemon()?"daemon":"") + " id=" + thread.getId() + " " + thread.getState().toString();
                    String className;
                    boolean cutcore = true;
                    for (int i = 0; i < stackTraceElements.length; i++) {
                        ste = stackTraceElements[i];
                        className = ste.getClassName();
                        if (cutcore && (className.startsWith("java.") || className.startsWith("sun."))) {
                            sb.setLength(0);
                            bufferappend(sb, tracename + "at " + CharacterCoding.unicode2html(ste.toString(), true));
                        } else {
                            cutcore = false;
                            bufferappend(sb, tracename + "at " + CharacterCoding.unicode2html(ste.toString(), true));
                        }
                    }
                    final String threaddump = sb.toString();
                    List<String> threads = get(threaddump);
                    if (threads == null) threads = new ArrayList<String>();
                    threads.add(threadtitle);
                    put(new StackTrace(threaddump), threads);
                }
            }
        }

        public void appendStackTraces(
                final StringBuilder buffer,
                final Thread.State stateIn) {
            bufferappend(buffer, "THREADS WITH STATES: " + stateIn.toString());
            bufferappend(buffer, "");

            // write dumps
            for (final Map.Entry<StackTrace, List<String>> entry: entrySet()) {
                final List<String> threads = entry.getValue();
                for (final String t: threads) bufferappend(buffer, t);
                bufferappend(buffer, entry.getKey().text);
                bufferappend(buffer, "");
            }
            bufferappend(buffer, "");
        }

    }
}
