package ai.susi.server.api.cms;

import ai.susi.DAO;
import ai.susi.SkillTransactions;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import ai.susi.tools.skillqueryparser.SkillQuery;
import ai.susi.tools.skillqueryparser.SkillQueryParser;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
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
 * http://127.0.0.1:4000/cms/getFileAtCommitID.json?model=general&group=Knowledge&language=en&skill=bitcoin&commitID=214791f55c19f24d7744364495541b685539a4ee
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

        File skill = SkillQuery.getParser("wikipedia")
                .parse(call)
                .getSkillFile();

        String commitID  = call.get("commitID", null);

        if(commitID==null){
            JSONObject error = new JSONObject();
            error.put("message","Commit ID is Null");
            error.put("accepted", false);
            return new ServiceResponse(error);

        }
        String path = skill.getPath().replace(DAO.model_watch_dir.toString(), "models");

        //Add to git
        try {
            Repository repository = SkillTransactions.getPublicRepository();
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
                    json.put("commitRev", commit);
                    json.put("commitName", commit.getName());
                    json.put("commitID", commit.getId().getName());
                    json.put("commit_message", commit.getShortMessage());
                    json.put("author",commit.getAuthorIdent().getName());
                    json.put("commitDate",commit.getAuthorIdent().getWhen());
                    json.put("author_mail",commit.getAuthorIdent().getEmailAddress());
                }

                revWalk.dispose();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new ServiceResponse(json);
    }

}
