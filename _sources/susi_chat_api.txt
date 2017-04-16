# Susi Chat API

The Susi Chat API enables Susi clients to perform different kinds of actions.
The most prominent action may be:
 * processing of an expression (which can be written like a chat or spoken with text-to-speech)
 
but there are also other action types, like:
 * showing an image
 * showing a pie chart
 * showing a web search result (either retrieved by the client itself or delivered by the server)
 * showing rss feeds
 * showing a table
 * playing a sound or a video
 * asking the user questions with default answers
 * doing special activities that a client provides (i.e. setting an alarm, starting a timer etc.)

## Getting Started

To develop client actions, you need a working susi_server. You can either use our hosted service at http://api.susi.ai
or your own Susi server at http://127.0.0.1:4000

For an easy first test, just call the following URL: http://api.susi.ai/susi/chat.json?q=hello

You will get a JSON result which hat the following structure:

```
{
  "answers": [
    {
      "data": [],
      "actions": [
        {
          "type": "answer",
          "expression": "Hi, I'm Susi"
        }
      ]
    }
  ]
}
```
There is actually more information within the JSON, but these are the most important parts:

* every chat response may have several answers, but at least there is only one at `$.answers[0]`
* every answer has a data part which is a list of JSON objects at `$answers[].data`
* every answer has also a list of actions with at least one action at `$.answers[0].actions[0]`
* every action has at least one object with name "type" at `$.answers[].actions[].type`.

The `type` object within each action is the key to the different action types.
It is important that a client implementation evaluates the action types and makes activities based on those action types.

### Result Logic

Because an API response may have several answers each with several actions, it is important to know what that means
an what a client must do if these objects are returned with multiple options.

* `answers` objects are disjunctive, that means only one of them must be considered. It is not wrong to evaluate all of them,
  but it is also valid to evaluate only one of them.
* `actions` objects are conjunctive, that means all of them must be considered. The client must loop through all of the actions
  and perform all of the action types that are inside. 

The meaning of the `answers` is similar of what a meta-search engine would return: all the answers are valid, you can pick the one
that you like -- or all of them. The meaning of the `actions` is only sound, if all of them are performed. A simple example is the
setting of an alarm in the client; one action would be used to print out (or say) a written message that an alarm is set, the other
action would technically address the setting of the clock with exact values for the timer.

### Data object sharing

The `data` object at `$answers[].data` is a zone which may information for special actions, like showing a table, a piechart, a search result etc. In such cases the action does not contain the table, piechart, search result data itself but refer to keys within the `data` object. Therefore that data can be shared across the actions, so several actions can use the same data.

### Susi Thought Transparency

When Susi skills are processed, a data object called a 'Susi Thought' is moved along a inference chain. The essence of that chain (a process called 'brain melting') is then stored inside the data object if a skill is successfull. The susi skill processing actually fills therefore the data object. Short-memory and Long-memory values, used within the skill processing are also present in the data object. The data therefore shows the 'latest thought' of Susi and gives transparency about the process that caused the given answer.

### Client Abilities

Not all clients may be able to show graphics, set a timer or play an audio file, therefore it is important that a client declares
it's abilities within the request to the chat API. The abilities are declared as the list of actions that a client
can evaluate. Together with the action abilities also location, language and time zone information is transmitted to the susi_server
with each request.

## Prototype Client Implemetation

For an easy quick-start, just copy the following code to develop your own Susi client

### Java
```
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
```

## API Request attributes

The following attributes can be transmitted to the server with each request

* actions (optional; defaults to 'answer')
* timezoneOffset (optional, should be same as returned with JavaScript getTimezoneOffset(); defaults to 0)
* longitude (optional)
* latitude (optional)
* language (optional, ISO 639-1 two-letter lowercase characters,; defaults to 'en')
* q (mandatory value, a UTF-8 encoded query string)

All requests to the `/susi/chat.json` API can also be made with a POST request. A proper GET-Request would look like:
```
```


## API Action types

### The `answer` Action
