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

The `data` object can also be filled with a console rule, see https://github.com/fossasia/susi_server/blob/development/docs/susi_skill_development_tutorial.md#tutorial-level-11-call-an-external-api
The result of the calling of an external api fills the data object with the table, which is addressed with the console rule's `path` attribute.

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
curl http://api.susi.ai/susi/chat.json?q=hello&actions=answer,rss,table,map&timezoneOffset=-120&language=de
```


## API Action types

Every Susi answer can have one or several action objects; all of them must be rendered by a Susi client.

### The `answer` Action

This is the default action type which should be expected most:
```
{
  "query": "hello",
  "answers": [{
    "data": [],
    "metadata": {"count": 0},
    "actions": [{
      "type": "answer",
      "expression": "Hello!"
    }]
  }],
}
```
This means that a client must return the `expression` to the user in any way: it can be written, spoken or displayed in any way.
The action of type `answer` does not tell the client that a text-to-speech or other way of communication shall be used, the client
has the task to find out what is the best way to send the message to the user. The core information is, that the action is an answer
consisting of a text.

### The `table` Action

A table action is performed by sending a table of objects to the user in a table-like format.
The task to display a table can be different for different type of clients. A table action looks like. i.e.:
```
{
  "query": "stock quotes",
  "answers": [{
    "data": [
      {"currency": "EUR/USD", "rate": "1.09302", "timestamp": "1494621900069"},
      {"currency": "USD/JPY", "rate": "113.326", "timestamp": "1494621900186"},
      {"currency": "GBP/USD", "rate": "1.28835", "timestamp": "1494621900129"},
      {"currency": "EUR/GBP", "rate": "0.84831", "timestamp": "1494621900103"},
      {"currency": "USD/CHF", "rate": "1.00133", "timestamp": "1494621899461"}
    ],
    "metadata": {"count": 59},
    "actions": [{
      "type": "answer",
      "expression": "Here is a currency rate table:"
    }, {
      "type": "table",
      "count": 3,
      "columns": {
        "currency": "Valuta",
        "rate": "Quota"
      }
    }]
  }]
}
```
Please recognize that the actions object consists of two action objects; both actions must be performed. The first action
is an answer which must be displayed as explained above for `answer` actions, The second action is the `table` action.
The client then should create a table out of the data object where the column names are 'Valuta' and 'Quota'.
The `count` value gives the maximum number of lines, if that number is -1 it means 'unlimited'.
If the client would be a web interface, the rendering would look similar to this html table:
```
<table>
<tr><th>Valuta</th><th>Quota</th></tr>
<tr><td>EUR/USD</td><td>1.09302</td></tr>
<tr><td>USD/JPY</td><td>113.326</td></tr>
<tr><td>GBP/USD</td><td>1.28835</td></tr>
<tr><td>EUR/GBP</td><td>0.84831</td></tr>
<tr><td>USD/CHF</td><td>1.00133</td></tr>
</table>
```

### The `websearch` Action

This action can be performed by doing a web search on the client side:
```
{
  "query": "Oh freddled gruntbuggly",
  "answers": [{
    "data": [],
    "metadata": {"count": 0},
    "actions": [
      {"type": "answer", "expression": "I found this on the web:"},
      {
      "type": "websearch",
      "query": "Oh freddled gruntbuggly",
      "count": 3
      }
    ]
  }],
}
```
A websearch action is usually combined with an answer action type which introduces the web search result as a headline.
The query attribute for the web search can be found in the `query` object. Like in table actions, the `count` object denotes
the maximum number of results. -1 means unlimited, meaning that all the results from the web search results are used.
The API for the web search can be choosen by the client.
A typical rendering of such a search results has three lines:
* a Headline
* a Snippet or Description line (showing the content of the found document where the searched word appears)
* a link.

A rendering would look like:
```
<ul>
  <li class="title">Vogon poetry | Hitchhikers | Fandom powered by Wikia</li>
  <li class="link">http://hitchhikers.wikia.com/wiki/Vogon_poetry</li>
  <li class="description">Oh freddled gruntbuggly,: Thy micturations are to me,: As plurdled gabbleblotchits,: On a lurgid bee,: That mordiously hath blurted out,: Its earted jurtles,: Into a ...</li>
</ul>

.
.
.
... (more search hits as <ul></ul> tags)
```
The actual presentation can differ from this, i.e. using anchor tags it should be possible to click on the title or description and link to the given link content.

### The `rss` Action

RSS feeds are standardized syndication messages. The same format is used for the opensearch (see http://www.opensearch.org) xml format, which is a standardization for search results. The core of this schema is a list of three entities, which are repeated in every message of an rss feed:
* title
* description
* link

The Susi usage of such messages is, that rss feeds can be aquired by the susi_server, then translated into objects within the `data` array and then should be rendered in the same way as a web search result action (see above). To make this possible, we must denote the name of these three information entities (title, description, link) in the rss action. For example:
```
{
  "query": "Oh freddled gruntbuggly",
  "answers": [{
    "data": [{
      "url": "http://hitchhikers.wikia.com/wiki/Vogon_poetry",
      "headline": "Vogon poetry | Hitchhikers | Fandom powered by Wikia"},
      "description" : "Oh freddled gruntbuggly,: Thy micturations are to me,: As plurdled gabbleblotchits,: On a lurgid bee,: That mordiously hath blurted out,: Its earted jurtles,: Into a ..."],
    "metadata": {"count": 0},
    "actions": [
      {"type": "answer", "expression": "I found this on the web:"},
      {
      "type": "rss",
      "count": 3,
      "title": "headline",
      "description": "description",
      "link": "url"
      }
    ]
  }],
}
```
The rendering of such a result should be exactly the same as for the websearch example above. The only difference is, that the client
must not search for itself, but just shows a search result which is provided by the server. The only thing to do here is an translation
from the attribute keys within the data object into the opensearch/rss-typical keys.
