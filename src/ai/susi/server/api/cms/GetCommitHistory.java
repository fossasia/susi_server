package ai.susi.server.api.cms;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by chetankaushik on 12/06/17.
 * This Servlet gives the complete commit history of the susi skill repository
 * Can be tested at: -
 * 127.0.0.1:4000/cms/getCommitHistory.json
 */
public class GetCommitHistory extends AbstractAPIHandler implements APIHandler {
    private static final long serialVersionUID = -5686523277755750923L;


    @Override
    public String getAPIPath() {
        return "/cms/getCommitHistory.json";
    }

    @Override
    public UserRole getMinimalUserRole() {
        return UserRole.ANONYMOUS;
    }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) throws APIException {
        JSONObject commit;
        JSONObject json = new JSONObject(true);
        json.put("accepted", false);
        JSONArray commitsArray;
        commitsArray = new JSONArray();
        
        //Add to git
        try (Git git = DAO.getGit()) {
            Iterable<RevCommit> logs;
            logs = git.log().call();
            int i = 0;
            for (RevCommit rev : logs) {
                commit = new JSONObject();
                commit.put("commitRev", rev);
                commit.put("commitName", rev.getName());
                commit.put("commitID", rev.getId().getName());
                commit.put("commit_message", rev.getShortMessage());

                commitsArray.put(i, commit);
                i++;
            }
            json.put("accepted", true);
            json.put("commitsArray", commitsArray);

        } catch (IOException | GitAPIException e) {
            e.printStackTrace();
        }

        return new ServiceResponse(json);
    }

}
