/**
 *  Caretaker
 *  Copyright 22.02.2015 by Michael Peter Christen, @0rb1t3r
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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AgeFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;

import ai.susi.mind.SusiCognition;
import ai.susi.mind.SusiFace;
import ai.susi.server.ClientIdentity;
import ai.susi.tools.EtherpadClient;
import ai.susi.tools.TimeoutMatcher;
import org.joda.time.DateTime;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * The caretaker class is a concurrent thread which does peer-to-peer operations
 * and data transmission asynchronously.
 */
public class Caretaker extends Thread {

    private boolean shallRun = true;
    public  final static long startupTime = System.currentTimeMillis();

    /**
     * ask the thread to shut down
     */
    public void shutdown() {
        this.shallRun = false;
        this.interrupt();
        DAO.log("catched caretaker termination signal");
    }

    public void deleteOldFiles() {
        if (DAO.deleted_skill_dir.exists() && DAO.deleted_skill_dir.isDirectory()) {
            Collection<File> filesToDelete = FileUtils.listFiles(DAO.deleted_skill_dir,
                new AgeFileFilter(DateTime.now().withTimeAtStartOfDay().minusDays(30).toDate()),
                TrueFileFilter.TRUE);    // include sub dirs
            for (File file : filesToDelete) {
                boolean success = FileUtils.deleteQuietly(file);
                if (!success) {
                    System.out.print("Deleted skill older than 30 days.");
                }
            }
        }
    }

    public void checkEtherpadChat() {
        try {
            EtherpadClient etherpad = new EtherpadClient();
            if (!etherpad.isPrivate()) return; // we check only local etherpads
            List<EtherpadClient.Message> messages = etherpad.getChatHistory("susi", 10);
            if (messages.size() == 0) return;
            EtherpadClient.Message lastQuestion = messages.get(messages.size() - 1).text.startsWith("@susi ") ? messages.get(messages.size() - 1) : null;
            /*
            System.out.println("*chat*");
            for (EtherpadClient.Message message: messages) {
                System.out.println("user:" + message.userName + ", id:" + message.userId + ", text:" + message.text);
            }
            */
            // find all lines starting with "@susi" which are not answered already
            // we devide the set of messages into two sub-sets:
            // - lines where the user asks susi a question / lines starting with "@susi " -> messages list (the list is cleaned from all other lines)
            // - lines where susi gives answers to previous questions / lines starting with "@<user>" -> user name to answered list
            Iterator<EtherpadClient.Message> i = messages.iterator();
            List<String> answeredUserNames = new ArrayList<>();
            Map<String, String> user2userid = new HashMap<>();
            while (i.hasNext()) {
                EtherpadClient.Message message = i.next();
                user2userid.put(message.userName, message.userId);
                if (!message.text.startsWith("@")) {i.remove(); continue;} // no a question/answer line
                if (message.text.startsWith("@susi ")) continue; // questions to susi: we keep those
                // remaining lines are answer lines to users. We consider they came from susi.
                // record the user of the line into answered.
                int p = message.text.indexOf(' ');
                if (p < 0) {i.remove(); continue;}
                String answeredUserName = message.text.substring(1, p);
                answeredUserNames.add(answeredUserName);
                // finally remove the line from message list because those are remainaing new questions
                i.remove();
            }
            if (messages.size() == 0) return;

            // there can be only "@susi" lines remaining. We remove now those from users in the answered list
            for (String userName: answeredUserNames) {
                String userid = user2userid.get(userName);
                if (userid == null && userName.equals("unnamed")) userid = user2userid.get("");
                if (userid == null) continue; // can be caused by a "@namex" where namex is not a user
                i = messages.iterator();
                while (i.hasNext()) {
                    EtherpadClient.Message message = i.next();
                    if (userid.equals(message.userId)) {i.remove(); break;}
                }
            }
            if (messages.size() == 0 && lastQuestion != null) messages.add(lastQuestion); // fail-over for bad counting
            if (messages.size() == 0) return;

            // the remaining lines are unanswered questions to susi
            for (EtherpadClient.Message message: messages) {
                // create a virtual http request to the susi service
                String q = message.text.substring(6).trim();
                SusiFace face = new SusiFace(q);
                String userName = message.userName;
                if (userName == null || userName.length() == 0) userName = "unnamed"; // unnamed is the name in the etherpad if no name is given
                ClientIdentity identity = new ClientIdentity(ClientIdentity.Type.nick, userName);
                face.setAuthorization(DAO.getAuthorization(identity));
                try {
                    SusiCognition cognition = face.call();
                    LinkedHashMap<String, List<String>> answers = cognition.getAnswers();
                    for (String answer: answers.keySet()) {
                        etherpad.appendChatMessage("susi", "@" + userName + " " + answer, "susi");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            // this simply means that the pad is not available. Thats ok, the pad is optional.
            //e.printStackTrace();
        }
    }

    @Override
    public void run() {

        // work loop
        while (this.shallRun) {
            try {
                deleteOldFiles();
                checkEtherpadChat();
                TimeoutMatcher.terminateAll();
            } catch (Throwable e) {
                DAO.severe("CARETAKER THREAD", e);
            }
            try {Thread.sleep(5000);} catch (InterruptedException e) {}
        }

        DAO.log("caretaker terminated");
    }

}
