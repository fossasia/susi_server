package ai.susi.server.api.cms;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by chetankaushik on 10/08/17.
 * Returns the file at a specific commitID. similar to Git Show.
 * http://127.0.0.1:4000/cms/getFileAtCommitID.json?model=general&group=knowledge&language=en&skill=bitcoin&commitID=214791f55c19f24d7744364495541b685539a4ee
 */
public class GetFileAtCommitID extends AbstractAPIHandler implements APIHandler {
    private static final long serialVersionUID = -5686523277755750923L;


    @Override
    public String getAPIPath() {
        return "/cms/getFileAtCommitID.json";
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
        JSONObject json = new JSONObject(true);
        json.put("accepted", false);
        String model_name = call.get("model", "general");
        File model = new File(DAO.model_watch_dir, model_name);
        String group_name = call.get("group", "Knowledge");
        File group = new File(model, group_name);
        String language_name = call.get("language", "en");
        File language = new File(group, language_name);
        String skill_name = call.get("skill", "wikipedia");
        File skill = new File(language, skill_name + ".txt");

        String commitID  = call.get("commitID", null);

        if(commitID==null){
            JSONObject error = new JSONObject();
            error.put("message","Commit ID is Null");
            error.put("accepted", false);
            return new ServiceResponse(error);

        }
        String path = skill.getPath().replace(DAO.model_watch_dir.toString(), "models");

        //Add to git
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repository = null;
        try {
            repository = builder.setGitDir((DAO.susi_skill_repo))
                    .readEnvironment() // scan environment GIT_* variables
                    .findGitDir() // scan up the file system tree
                    .build();

            ObjectId CommitIdObject = repository.resolve(commitID);

            // a RevWalk allows to walk over commits based on some filtering that is defined
            try (RevWalk revWalk = new RevWalk(repository)) {
                RevCommit commit = revWalk.parseCommit(CommitIdObject);
                // and using commit's tree find the path
                RevTree tree = commit.getTree();
                System.out.println("Having tree: " + tree);

                // now try to find a specific file
                try (TreeWalk treeWalk = new TreeWalk(repository)) {
                    treeWalk.addTree(tree);
                    treeWalk.setRecursive(true);
                    treeWalk.setFilter(PathFilter.create(path));
                    if (!treeWalk.next()) {
                        throw new IllegalStateException("Did not find expected file");
                    }

                    ObjectId objectId = treeWalk.getObjectId(0);
                    ObjectLoader loader = repository.open(objectId);

                    // and then one can the loader to read the file
                    OutputStream output = new OutputStream(){
                        private StringBuilder string = new StringBuilder();
                        @Override
                        public void write(int b) throws IOException {
                            this.string.append((char) b );
                        }

                        //Netbeans IDE automatically overrides this toString()
                        public String toString(){
                            return this.string.toString();
                        }
                    };
                    loader.copyTo(output);
                    json.put("file",output);
                }

                revWalk.dispose();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new ServiceResponse(json);
    }

}
