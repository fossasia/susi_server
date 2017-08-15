# Retrieving User Chat History

Whenever a user logs in he must be able to view his chat history in the chat client.
A user might also be using multiple chat clients, so the history across all platforms must be in sync.

A [memory servlet](https://github.com/fossasia/susi_server/blob/development/src/ai/susi/server/api/susi/UserService.java) is used to retrieve the user history.
>api.susi.ai/susi/memory.json?access_token=ACCESS_TOKEN

When the client makes a call to the server to the above endpoint  with the ```ACCESS_TOKEN``` of the logged in user,  the server returns a list of cognitions which contain susi responses to the queries in the history.

The response from the memory servlet is of the form:
```
{
	"cognitions" : [],
	"session" : {},
}
```
A sample susi response is of the form :
```
{
	"query" : 
	"answers" : [ {
		"data" : [],
		"actions" : []
	}],
}
```
So each cognition has ```query``` as well as  ```answer ``` and thus we get a conversation message pair in the chat history.

The cognitions contain a list of SUSI responses of the above form using which chat history is rendered.

All the user messages are stored in a log file. The memory servlet digs out the history of the required user from the log file. The log uses the identity of the user and accesses only that information which has been stored for the user. If the user is not logged on, no information is available.
The conversation log is NOT stored for a particular IP. It’s stored for an Identity within the AAA system.
That identity can be represented with an email address, or there can be others.

Thus the synchronisation of history across all chat platforms is maintained.

