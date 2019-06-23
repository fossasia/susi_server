/**
 *  BulkAnswerService
 *  Copyright 21.02.2019 by Michael Peter Christen, @0rb1t3r
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

package ai.susi.server.api.susi;

import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.APIException;
import ai.susi.server.APIHandler;
import ai.susi.server.AbstractAPIHandler;
import ai.susi.server.Authorization;
import ai.susi.server.Query;
import ai.susi.server.ServiceResponse;
import ai.susi.server.UserRole;

import org.json.JSONArray;
import org.json.JSONObject;


import javax.servlet.http.HttpServletResponse;

/*
call this like:
curl -X POST -H "Content-Type: text/plain" --data "what is the meaning of life" http://localhost:4000/susi/answers.txt

or:
curl -X POST 'http://localhost:4000/susi/answers.txt' -H "Content-Type: text/plain" -d '
What is SCRUM?
Do you like SCRUM?
What did you do yesterday?
What will you do today?
What is blocking progress?
'

This will answer all questions in this list. A possible output could be:
>>>
What is SCRUM?
scrum is a Agile software development framework.

Do you like SCRUM?
Did you really just ask me that?

What did you do yesterday?
I can't answer that particular question.

What will you do today?
That's private.

What is blocking progress?
Let me get back to you on that.
<<<

This interface is therefore good for automated testing. Test files can be feeded into the API and can be compared with expectations.
 */
public class BulkAnswerService extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = 857847830309879111L;

    @Override
    public UserRole getMinimalUserRole() { return UserRole.ANONYMOUS; }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    public String getAPIPath() {
        return "/susi/answers.txt";
    }

    @Override
    public ServiceResponse serviceImpl(Query post, HttpServletResponse response, Authorization user, final JsonObjectWithDefault permissions) throws APIException {

        String text = post.get("BODY", "").trim();

        // for each line of this text, create an answer. Then output the text again with answers after each input line
        String[] lines = text.split(System.lineSeparator());
        StringBuffer sb = new StringBuffer(text.length() * 2);
        lineloop: for (String line: lines) {
            String s = line.trim();
            if (s.length() == 0) {
                sb.append(System.lineSeparator());
                continue lineloop;
            }
            if (s.charAt(0) == '#') {
                sb.append(s).append(System.lineSeparator());
                continue lineloop;
            }
            JSONObject json = SusiService.serviceImpl(post, response, user, permissions, s);
            if (!json.has("answers")) continue lineloop;
            JSONArray answers = json.getJSONArray("answers");
            if (answers.isEmpty()) continue lineloop;
            JSONObject answer = answers.getJSONObject(0);
            if (!answer.has("actions")) continue lineloop;
            JSONArray actions = answer.getJSONArray("actions");
            for (int i = 0; i < actions.length(); i++) {
                JSONObject action = actions.getJSONObject(i);
                if (action.has("type") && action.has("expression") && "answer".equals(action.getString("type"))) {
                    sb.append(s).append(System.lineSeparator());
                    sb.append("### ANSWER: ").append(action.getString("expression")).append(System.lineSeparator());
                    if (answer.has("skills")) {
                        JSONArray skills = answer.getJSONArray("skills");
                        for (int j = 0; j < skills.length(); j++) {
                            sb.append("### SOURCE: ").append(skills.getString(j)).append(System.lineSeparator());
                        }
                    }
                    sb.append(System.lineSeparator());
                    continue lineloop;
                }
            }
        }
        return new ServiceResponse(sb.toString());
    }

}
