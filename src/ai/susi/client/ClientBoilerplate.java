package ai.susi.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public class ClientBoilerplate {

    public static void main(String[] args) {
        String q = args.length == 0 ? "Hello" : args[0];
        try {
            onRequest(q);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private final static String API_PATH_STUB = "/susi/chat.json?q=";
    public final static String LOCAL_SUSI = "http://127.0.0.1:4000" + API_PATH_STUB;
    public final static String HOSTED_SUSI = "http://api.susi.ai" + API_PATH_STUB;
    
    public static void onRequest(String query) throws IOException {
        InputStream stream = null;
        try {
            stream = new URL(LOCAL_SUSI + query).openStream();
        } catch (IOException e) {
            stream = new URL(HOSTED_SUSI + query).openStream();
        }
        JSONObject json = new JSONObject(new JSONTokener(new InputStreamReader(stream, StandardCharsets.UTF_8)));
        JSONArray answers = json.getJSONArray("answers");
        JSONObject answer = answers.getJSONObject(0);
        JSONArray data = answer.getJSONArray("data");
        JSONArray actions = answer.getJSONArray("actions");
        actions.forEach(action -> onAction((JSONObject) action, data));
    }
    
    public static void onAction(JSONObject action, JSONArray data) {
        String type = action.getString("type");
        
        switch (type) {
            case "answer":
                String expression = action.getString("expression");
                break;
            default:
                throw new IllegalArgumentException("Invalid action type: " + type);
        }
    }
}
