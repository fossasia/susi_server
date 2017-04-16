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

### Client Abilities

Not all clients may be able to show graphics, set a timer or play an audio file, therefore it is important that a client declares
it's abilities within the request to the chat API. This also includes location and language information. 

## API Request attributes

## API Action types
