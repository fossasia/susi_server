/**
 *  SkillTransactions
 *  Copyright 29.10.2018 by Michael Peter Christen, @0rb1t3r
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import ai.susi.DAO;
import ai.susi.server.APIException;
import ai.susi.tools.IO;

public class SkillTransactions {
    
    private static Boolean pullStatus = true;
    public  static String conflictsPlaceholder = "%CONFLICTS%";
    private static ConcurrentLinkedDeque<CommitAction> pushQueue = new ConcurrentLinkedDeque<>();
    private static Thread pullThread;
    
    /**
     * initialize the DAO
     * @param configMap
     * @param dataPath the path to the data directory
     */
    public static void init(long pull_delay) throws Exception{

        DAO.log("Starting Skill Data pull thread");
        pullThread = new Thread() {
            @Override
            public void run() {
                while (pullStatus) {
                    try {
                        Thread.sleep(pull_delay);
                    } catch (InterruptedException e) {
                        break;
                    }
                    try {
                        pull(getPublicGit());
                    } catch (Exception e) {
                        DAO.severe("SKILL PULL THREAD", e);
                    }
                    while (pushQueue.size() > 0) {
                        CommitAction ca = pushQueue.removeFirst();
                        ca.process();
                    }
                }
            }
        };
        pullThread.start();
    }
    
    public static void close() {
        pullStatus = false;
        pullThread.interrupt();
    }
    
    public static Repository getPublicRepository() throws IOException {
      Repository repo;
      File repoFile = DAO.susi_skill_repo;
      if (repoFile.exists()) {
      // Open an existing repository
      repo = new FileRepositoryBuilder()
      .setGitDir(DAO.susi_skill_repo)
      .readEnvironment() // scan environment GIT_* variables
      .findGitDir() // scan up the file system tree
      .build();
      } else {
        // Create a new repository
        repo = new FileRepositoryBuilder()
        .setGitDir(DAO.susi_skill_repo)
        .build();
        repo.create();
      }
      return repo;
    }

    private static Repository getPrivateRepository() throws IOException {
        Repository repo;
        File repoFile = DAO.susi_private_skill_repo;
        if (repoFile.exists()) {
        // Open an existing repository
        repo = new FileRepositoryBuilder()
        .setGitDir(DAO.susi_private_skill_repo)
        .readEnvironment() // scan environment GIT_* variables
        .findGitDir() // scan up the file system tree
        .build();
        } else {
        // Create a new repository
        repo = new FileRepositoryBuilder()
        .setGitDir(DAO.susi_private_skill_repo)
        .build();
        repo.create();
        }
        return repo;
    }

    private static Git getPublicGit() throws IOException {
        Git git = new Git(getPublicRepository());
        return git;
    }

    private static Git getPrivateGit() throws IOException {
        Git git = new Git(getPrivateRepository());
        return git;
    }
    
    public static Iterable<RevCommit> getGitLog(boolean privateRepository, String path) throws IOException, GitAPIException {
        Git git = privateRepository ? getPrivateGit() : getPublicGit();
        LogCommand lc = git.log();
        if (path != null) lc.addPath(path);
        return lc.call();
    }

    public static void pull(Git git) throws IOException {
        try {
            PullResult pullResult = git.pull().call();
            MergeResult mergeResult = pullResult.getMergeResult();

            if (mergeResult!=null && mergeResult.getConflicts()!=null) {
                // we have conflicts send email to admin
                try {
                    EmailHandler.sendEmail(DAO.getConfig("skill_repo.admin_email",""), "SUSI Skill Data Conflicts", getConflictsMailContent(mergeResult));
                } catch (Throwable e) {
                    e.printStackTrace();
                }

            } else {
                PushCommand push = git.push();
                push.setCredentialsProvider(new UsernamePasswordCredentialsProvider(DAO.getConfig("github.username", ""), DAO.getConfig("github.password","")));
                push.call();
            }

        } catch (GitAPIException e) {
            throw new IOException (e.getMessage());
        }
    }
    
    private static class CommitAction {
        boolean privateRepository;
        String commitMessage;
        String userEmail;
        public CommitAction(boolean privateRepository, String commitMessage, String userEmail) {
            this.privateRepository = privateRepository;
            this.commitMessage = commitMessage;
            this.userEmail = userEmail;
        }
        
        public void process() {
            try {
                Git git = privateRepository ? getPrivateGit() : getPublicGit();
                long t0 = System.currentTimeMillis();
                git.add().setUpdate(true).addFilepattern(".").call();
                long t1 = System.currentTimeMillis();
                git.add().addFilepattern(".").call(); // takes long
                long t2 = System.currentTimeMillis();
        
                // commit the changes
                pushCommit(git, commitMessage, userEmail); // takes long
                long t3 = System.currentTimeMillis();
                DAO.log("jgit statistics: add-1: " + (t1 - t0) + "ms, add-2: " + (t2 - t1) + "ms, push: " + (t3 - t2) + "ms");
            } catch (GitAPIException | IOException e) {
                DAO.severe("git push failed: " + commitMessage, e);
            }
        }
    }
    
    public static void addAndPushCommit(boolean privateRepository, String commit_message, String userEmail) {
        pushQueue.addLast(new CommitAction(privateRepository, commit_message, userEmail));
    }
    
    /**
     * commit user changes to the skill data repository
     * @param git
     * @param commit_message
     * @param userEmail
     * @throws IOException
     */
    private static void pushCommit(Git git, String commit_message, String userEmail) throws IOException {

        // fix bad email setting
        if (userEmail==null || userEmail.isEmpty()) {
            assert false; // this should not happen
            userEmail = "anonymous@";
        }

        try {
            git.commit()
                    .setAllowEmpty(false)
                    .setAll(true)
                    .setAuthor(new PersonIdent(userEmail,userEmail))
                    .setMessage(commit_message)
                    .call();
            String remote = "origin";
            String branch = "refs/heads/master";
            String trackingBranch = "refs/remotes/" + remote + "/master";
            RefSpec spec = new RefSpec(branch + ":" + branch);

            // TODO: pull & merge

            PushCommand push = git.push();
            push.setForce(true);
            push.setCredentialsProvider(new UsernamePasswordCredentialsProvider(DAO.getConfig("github.username", ""), DAO.getConfig("github.password","")));
            push.call();
        } catch (GitAPIException e) {
            throw new IOException (e.getMessage());
        }
    }

    private static String getConflictsMailContent(MergeResult mergeResult) throws APIException {
        // get template file
        String result="";
        String conflictLines= "";
        try {
            result = IO.readFileCached(Paths.get(DAO.conf_dir + "/templates/conflicts-mail.txt"));
            for (String path :mergeResult.getConflicts().keySet()) {
                int[][] c = mergeResult.getConflicts().get(path);
                conflictLines+= "Conflicts in file " + path + "\n";
                for (int i = 0; i < c.length; ++i) {
                    conflictLines+= "  Conflict #" + i+1 +"\n";
                    for (int j = 0; j < (c[i].length) - 1; ++j) {
                        if (c[i][j] >= 0)
                            conflictLines+= "    Chunk for "
                                    + mergeResult.getMergedCommits()[j] + " starts on line #"
                                    + c[i][j] +"\n";
                    }
                }
            }
            result = result.replace(conflictsPlaceholder, conflictLines);
        } catch (IOException e) {
            throw new APIException(500, "No conflicts email template");
        }
        return result;
    }
}
