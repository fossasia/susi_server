package ai.susi;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import ai.susi.DAO;
import ai.susi.server.APIException;
import ai.susi.tools.IO;

public class SkillTransactions {
    

    public static Boolean pullStatus = true;
    public static String conflictsPlaceholder = "%CONFLICTS%";
    
    /**
     * initialize the DAO
     * @param configMap
     * @param dataPath the path to the data directory
     */
    public static void init(long pull_delay) throws Exception{


        DAO.log("Starting Skill Data pull thread");
        Thread pullThread = new Thread() {
            @Override
            public void run() {
                while (pullStatus) {
                    try {
                        Thread.sleep(pull_delay);
                    } catch (InterruptedException e) {
                        break;
                    }
                    try {
                        pull(getGit());
                    } catch (Exception e) {
                        pullStatus = false;
                        DAO.severe("SKILL PULL THREAD", e);
                    }
                }
            }
        };
        pullThread.start();

    }
    

    public static Repository getRepository() throws IOException {
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

    public static Repository getPrivateRepository() throws IOException {
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

    public static Git getGit() throws IOException {
        Git git = new Git(getRepository());
        return git;
    }

    public static Git getPrivateGit() throws IOException {
        Git git = new Git(getPrivateRepository());
        return git;
    }

    public static void pull(Git git) throws IOException {
        try {
            PullResult pullResult = git.pull().call();
            MergeResult mergeResult = pullResult.getMergeResult();

            if (mergeResult!=null && mergeResult.getConflicts()!=null) {
                pullStatus =false;
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
    public static Thread addAndPushCommit(String commit_message, String userEmail, boolean concurrently) {
      Runnable process = new Runnable() {
        @Override
        public void run() {
          try (Git git = getGit()) {
            long t0 = System.currentTimeMillis();
            git.add().setUpdate(true).addFilepattern(".").call();
            long t1 = System.currentTimeMillis();
            git.add().addFilepattern(".").call(); // takes long
            long t2 = System.currentTimeMillis();

            // commit the changes
            pushCommit(git, commit_message, userEmail); // takes long
            long t3 = System.currentTimeMillis();
            DAO.log("jgit statistics: add-1: " + (t1 - t0) + "ms, add-2: " + (t2 - t1) + "ms, push: " + (t3 - t2) + "ms");
          } catch (IOException | GitAPIException e) {
            e.printStackTrace();

          }
        }
      };
      if (concurrently) {
        Thread t = new Thread(process);
        t.start();
        return t;
      } else {
        process.run();
        return null;
      }
    }

    public static Thread addAndPushCommitPrivate(String commit_message, String userEmail, boolean concurrently) {
      Runnable process = new Runnable() {
        @Override
        public void run() {
          try (Git git = getPrivateGit()) {
            long t0 = System.currentTimeMillis();
            git.add().setUpdate(true).addFilepattern(".").call();
            long t1 = System.currentTimeMillis();
            git.add().addFilepattern(".").call(); // takes long
            long t2 = System.currentTimeMillis();

            // commit the changes
            pushCommit(git, commit_message, userEmail); // takes long
            long t3 = System.currentTimeMillis();
            DAO.log("jgit statistics: add-1: " + (t1 - t0) + "ms, add-2: " + (t2 - t1) + "ms, push: " + (t3 - t2) + "ms");
          } catch (IOException | GitAPIException e) {
            e.printStackTrace();

          }
        }
      };
      if (concurrently) {
        Thread t = new Thread(process);
        t.start();
        return t;
      } else {
        process.run();
        return null;
      }
    }

    /**
     * commit user changes to the skill data repository
     * @param git
     * @param commit_message
     * @param userEmail
     * @throws IOException
     */
    public static void pushCommit(Git git, String commit_message, String userEmail) throws IOException {

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
