package ai.susi.server.api.cms;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.json.JsonTray;
import ai.susi.server.*;
import org.json.JSONObject;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * Necessary parameters : access_token, example:
 * http://localhost:4000/cms/getPrivateSkillList.json?access_token=6O7cqoMbzlClxPwg1is31Tz5pjVwo3
 * Other parameter, (not necessary) search:
 * http://localhost:4000/cms/getPrivateSkillList.json?access_token=6O7cqoMbzlClxPwg1is31Tz5pjVwo3&search=test_bot
 */

public class ConsoleSkillService extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = 4120658108778105134L;

    @Override
    public UserRole getMinimalUserRole() {
        return UserRole.ANONYMOUS;
    }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    @Override
    public String getAPIPath() {
        return "/cms/consoleSkillService.json";
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization authorization,
            final JsonObjectWithDefault permissions) throws APIException {

// {
//   "console":{
//     "wikidata-en":{
//       "example":"http://127.0.0.1:4000/susi/console.json?q=%22SELECT%20*%20FROM%20wikidata-en%20WHERE%20query=%27football%27;%22",
//       "url":"https://www.wikidata.org/w/api.php?action=wbsearchentities&format=json&language=en&search=",
//       "test":"football",
//       "parser":"json",
//       "path":"$.search",
//       "license":"Creative Commons CC0 License"
//     },
//     "urbandictionary":{
//       "example":"http://127.0.0.1:4000/susi/console.json?q=%22SELECT%20*%20FROM%20urbandictionary%20WHERE%20query=%27football%27;%22",
//       "url":"http://api.urbandictionary.com/v0/define?term=",
//       "test":"football",
//       "parser":"json",
//       "path":"$.list",
//       "license":"Copyright by urbandictionary.com, http://about.urbandictionary.com/tos"
//     },
//     "locations":{
//       "example":"http://127.0.0.1:4000/susi/console.json?q=%22SELECT%20location%20FROM%20locations%20WHERE%20query=%27rome%27;%22",
//       "url":"http://api.loklak.org/api/console.json?q=SELECT%20location%20FROM%20locations%20WHERE%20location='$query$';",
//       "test":"rome",
//       "parser":"json",
//       "path":"$.data[0]",
//       "license":"Copyright by GeoNames"
//     },
//     "location-info":{
//       "example":"http://127.0.0.1:4000/susi/console.json?q=%22SELECT%20*%20FROM%20location-info%20WHERE%20query=%27london%27;%22",
//       "url":"https://en.wikipedia.org/w/api.php?action=opensearch&limit=1&format=json&search=",
//       "test":"london",
//       "parser":"json",
//       "path":"$.[2]",
//       "license":"Copyright by Wikipedia, https://wikimediafoundation.org/wiki/Terms_of_Use/en"
//     },
//     "yacy":{
//       "example":"http://127.0.0.1:4000/susi/console.json?q=%22SELECT%20title,%20link%20FROM%20yacy%20WHERE%20query=%27java%27;%22",
//       "url":"http://yacy.searchlab.eu/solr/select?wt=yjson&q=",
//       "test":"java",
//       "parser":"json",
//       "path":"$.channels[0].items",
//       "license":""
//     },
//     "messages":{
//       "example":"http://127.0.0.1:4000/susi/console.json?q=%22SELECT%20*%20FROM%20messages%20WHERE%20query=%27loklak%27;%22",
//       "url":"http://api.loklak.org/api/console.json?q=SELECT%20*%20FROM%20messages%20WHERE%20query='$query$';",
//       "test":"loklak",
//       "parser":"json",
//       "path":"$.data"
//     },
//     "messages-group-by-screen_name":{
//       "example":"http://127.0.0.1:4000/susi/console.json?q=%22SELECT%20*%20FROM%20messages-group-by-screen_name%20WHERE%20query=%27loklak%27;%22",
//       "url":"http://api.loklak.org/api/console.json?q=SELECT%20*%20FROM%20messages%20WHERE%20query='$query$'%20GROUP%20BY%20screen_name;",
//       "test":"loklak",
//       "parser":"json",
//       "path":"$.data"
//     },
//     "messages-order-by-favourites_count":{
//       "example":"http://127.0.0.1:4000/susi/console.json?q=%22SELECT%20*%20FROM%20messages-order-by-favourites_count%20WHERE%20query=%27winter is coming /pure%27;%22",
//       "url":"http://api.loklak.org/api/console.json?q=SELECT%20*%20FROM%20messages%20WHERE%20query='$query$'%20ORDER%20BY%20FAVOURITES_COUNT;",
//       "test":"winter is coming /pure",
//       "parser":"json",
//       "path":"$.data"
//     },
//     "messageid":{
//       "example":"http://127.0.0.1:4000/susi/console.json?q=%22SELECT%20*%20FROM%20messageid%20WHERE%20query=%27787120060066213888%27;%22",
//       "url":"http://api.loklak.org/api/console.json?q=SELECT%20*%20FROM%20messages%20WHERE%20id='$query$';",
//       "test":"787120060066213888",
//       "parser":"json",
//       "path":"$.data"
//       },
//     "queries":{
//       "example":"http://127.0.0.1:4000/susi/console.json?q=%22SELECT%20*%20FROM%20queries%20WHERE%20query=%27frankfurt%27;%22",
//       "url":"http://api.loklak.org/api/console.json?q=SELECT%20*%20FROM%20queries%20WHERE%20query='$query$';",
//       "test":"frankfurt",
//       "parser":"json",
//       "path":"$.data"
//     },
//     "users":{
//       "example":"http://127.0.0.1:4000/susi/console.json?q=%22SELECT%20*%20FROM%20users%20WHERE%20query=%270rb1t3r%27;%22",
//       "url":"http://api.loklak.org/api/console.json?q=SELECT%20*%20FROM%20users%20WHERE%20screen_name='$query$';",
//       "test":"0rb1t3r",
//       "parser":"json",
//       "path":"$.data"
//     },
//     "meetup":{
//       "example":"http://127.0.0.1:4000/susi/console.json?q=%22SELECT%20*%20FROM%20meetup%20WHERE%20query=%27http://www.meetup.com/Women-Who-Code-Delhi%27;%22",
//       "url":"http://api.loklak.org/api/console.json?q=SELECT%20*%20FROM%20meetup%20WHERE%20url='$query$';",
//       "test":"http://www.meetup.com/Women-Who-Code-Delhi",
//       "parser":"json",
//       "path":"$.data"
//     },
//     "rss":{
//       "example":"http://127.0.0.1:4000/susi/console.json?q=%22SELECT%20*%20FROM%20rss%20WHERE%20query=%27http://www.independent.co.uk/news/uk/rss%27;",
//       "url":"http://api.loklak.org/api/console.json?q=SELECT%20*%20FROM%20rss%20WHERE%20url='$query$';",
//       "test":"http://www.independent.co.uk/news/uk/rss",
//       "parser":"json",
//       "path":"$.data"
//     },
//     "eventbrite":{
//       "example":"http://127.0.0.1:4000/susi/console.json?q=%22SELECT%20*%20FROM%20eventbrite%20WHERE%20query=%27https://www.eventbrite.fr/e/billets-europeade-2016-concert-de-musique-vocale-25592599153?aff=es2%27;",
//       "url":"http://api.loklak.org/api/console.json?q=SELECT%20*%20FROM%20eventbrite%20WHERE%20url='$query$';",
//       "test":"https://www.eventbrite.fr/e/billets-europeade-2016-concert-de-musique-vocale-25592599153?aff=es2",
//       "parser":"json",
//       "path":"$.data"
//     },
//     "wordpress":{
//       "example":"http://127.0.0.1:4000/susi/console.json?q=%22SELECT%20*%20FROM%20wordpress%20WHERE%20query=%27https://jigyasagrover.wordpress.com/%27;",
//       "url":"http://api.loklak.org/api/console.json?q=SELECT%20*%20FROM%20wordpress%20WHERE%20url='$query$';",
//       "test":"https://jigyasagrover.wordpress.com/",
//       "parser":"json",
//       "path":"$.data"
//     },
//     "githubProfile":{
//       "example":"http://127.0.0.1:4000/susi/console.json?q=%22SELECT%20*%20FROM%20githubProfile%20WHERE%20query=%27torvalds%27;",
//       "url":"http://api.loklak.org/api/console.json?q=SELECT%20*%20FROM%20githubProfile%20WHERE%20profile='$query$';",
//       "test":"torvalds",
//       "parser":"json",
//       "path":"$.data"
//     },
//     "locationwisetime":{
//       "example":"http://127.0.0.1:4000/susi/console.json?q=%22SELECT%20*%20FROM%20locationwisetime%20WHERE%20query=%27london%27;%22",
//       "url":"http://api.loklak.org/api/console.json?q=SELECT%20*%20FROM%20locationwisetime%20WHERE%20query='$query$';",
//       "test":"london",
//       "parser":"json",
//       "path":"$.data"
//     },
//     "instagramprofile":{
//       "example":"http://127.0.0.1:4000/susi/console.json?q=%22SELECT%20*%20FROM%20instagramprofile%20WHERE%20query=%27justinpjtrudeau%27;%22",
//       "url":"http://api.loklak.org/api/console.json?q=SELECT%20*%20FROM%20instagramprofile%20WHERE%20profile='$query$';",
//       "test":"justinpjtrudeau",
//       "parser":"json",
//       "path":"$.data[0].ProfilePage[0].user.media.nodes"
//     },
//     "wikigeodata":{
//       "example":"http://127.0.0.1:4000/susi/console.json?q=%22SELECT%20*%20FROM%20wikigeodata%20WHERE%20query=%27london%27;%22",
//       "url":"http://api.loklak.org/api/console.json?q=SELECT%20*%20FROM%20wikigeodata%20WHERE%20place='$query$';",
//       "test":"london",
//       "parser":"json",
//       "path":"$.data"
//     },
//     "quoraprofile":{
//       "example":"http://127.0.0.1:4000/susi/console.json?q=%22SELECT%20*%20FROM%20quoraprofile%20WHERE%20query=%27justinpjtrudeau%27;%22",
//       "url":"http://api.loklak.org/api/console.json?q=SELECT%20*%20FROM%20quoraprofile%20WHERE%20profile='$query$';",
//       "test":"justinpjtrudeau",
//       "parser":"json",
//       "path":"$.data"
//     },
//     "youtubesearch":{
//       "example":"http://localhost:4000/susi/console.json?q=SELECT%20*%20FROM%20youtubesearch%20WHERE%20query=%27tschunk%27;",
//       "url":"http://api.loklak.org/api/console.json?q=SELECT%20*%20FROM%20youtubesearch%20WHERE%20query=%27$query$%27;",
//       "test":"tschunk",
//       "parser":"json",
//       "path":"$.data"
//     }
//   }
// }



        JSONObject result = new JSONObject();
        List<JSONObject> consoleServices = new ArrayList<JSONObject>();
        String[] skills = {"Wikipedia Data", "Urban Dictionary","Locations","Yacy","Messages","Queries","Users","Meetup","RSS","EventBrite","Wordpress","Github Profile","Location Wise Time","Instagram Profile","Wiki Geo Data","Quora Profile","Youtube Search"};
        String[] descriptions = {"Fetch Data from Wikipedia","Fetch Data from Urban Dictionary",""};
        String[] examples = {"http://127.0.0.1:4000/susi/console.json?q=%22SELECT%20*%20FROM%20wikidata-en%20WHERE%20query=%27football%27;%22", 
        "http://127.0.0.1:4000/susi/console.json?q=%22SELECT%20*%20FROM%20urbandictionary%20WHERE%20query=%27football%27;%22"};
        int index = 0;
        for (String skillName : skills) {
          JSONObject skill = new JSONObject();
          skill.put("name", skillName);
          skill.put("description", descriptions[index]);
          skill.put("example", examples[index]);
          index = index+1;
          consoleServices.add(skill);
        }

        try {
            result.put("consoleServices", consoleServices);
            result.put("accepted", true);
            result.put("message", "Success: Fetched all Console Skill Services");
            return new ServiceResponse(result);
        } catch (Exception e) {
            throw new APIException(500, "Failed to fetch the requested list!");
        }

    }
}
