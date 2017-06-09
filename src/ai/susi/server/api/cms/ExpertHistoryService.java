package ai.susi.server.api.cms;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

public class ExpertHistoryService extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = 18344224L;
    int count = 0;
    JSONObject commit ;
    JSONArray commitsArray;
    @Override
    public BaseUserRole getMinimalBaseUserRole() {
        return BaseUserRole.ANONYMOUS;
    }

    @Override
    public JSONObject getDefaultPermissions(BaseUserRole baseUserRole) {
        return null;
    }

    @Override
    public String getAPIPath() {
        return "/cms/getHistory.json";
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        super.doGet(request, response);
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
        response.setHeader("Pragma", "no-cache"); // HTTP 1.0.
        response.setDateHeader("Expires", 0); // Proxies.
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) {

        String model_name = call.get("model", "general");
        File model = new File(DAO.model_watch_dir, model_name);
        String group_name = call.get("group", "knowledge");
        File group = new File(model, group_name);
        String language_name = call.get("language", "en");
        File language = new File(group, language_name);
        String expert_name = call.get("expert", "wikipedia");
        File expert = new File(language, expert_name + ".txt");
        commitsArray = new JSONArray();
        String path =  expert.getPath().replace(DAO.model_watch_dir.toString(), "models");
        //Add to git
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repository = null;
        try {
            repository = builder.setGitDir((DAO.susi_skill_repo))
                    .readEnvironment() // scan environment GIT_* variables
                    .findGitDir() // scan up the file system tree
                    .build();

            try (Git git = new Git(repository)) {

                Iterable<RevCommit> logs;

                logs = git.log()
                        .addPath(path)
                        .call();
                int i=0;
                for (RevCommit rev : logs) {
                    System.out.println("Commit: " + rev /* + ", name: " + rev.getName() + ", id: " + rev.getId().getName() */);
                    commit = new JSONObject();
                    commit.put("commitRev",rev);
                    commit.put("commitName",rev.getName());
                    commit.put("commitID",rev.getId().getName());
                    commit.put("commit_message",rev.getShortMessage());

                    commitsArray.put(i,commit);
                    i++;
                }

            } catch (GitAPIException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return  new ServiceResponse(commitsArray);
    }
}
