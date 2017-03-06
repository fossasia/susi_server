# AI Rule Development Tutorial

Do you want your own conversation rules for YaCy? It's surprisingly easy to add more dialog rules to Susi.

## Getting Started

It's easy, DON'T PANIC. You don't need to be a software developer to enhance Susi.

### What you have to do
- Currently we don't have an online rule development environment (which we want to make),
therefore you must run a local `susi_server` to test your self-made conversation rules
- You just have to write and edit simple text rules to create your own AI.

### Requirements

You need to have a version 1.8 of Java installed. You also need git, ant and you must be able to use a text editor :)

### Installation

* Clone your own copy of `susi_server`: run `git clone https://github.com/fossasia/susi_server.git`
* run `cd susi_server`
* run `ant`
* run `bin/start.sh`
* open http://localhost:4000 in your browser

### Access the Susi API

* in http://localhost:4000 you can enter a phrase into the query box. Write there 'hello' and push the 'get json data' button
* you can see now, that your request calls the API URL `http://localhost:4000/susi/chat.json?timezoneOffset=-60&q=hello`
* the result is given in JSON, like:
```
{
  "query": "hello",
  "count": 1,
  "client_id": "aG9zdF8xNzguMjAzLjIzMi41MA==",
  "query_date": "2017-02-17T16:06:24.794Z",
  "answers": [{
    "data": [{
      "0": "hello",
      "intent_original": "hello",
      "1": "",
      "intent_canonical": "hello",
      "intent_categorized": "hello",
      "timezoneOffset": "-60"
    }],
    "metadata": {"count": 1},
    "actions": [{
      "type": "answer",
      "expression": "Hello!"
    }]
  }],
  "answer_date": "2017-02-17T16:06:24.828Z",
  "answer_time": 34,
  "session": {"identity": {
    "type": "host",
    "name": "178.203.232.50",
    "anonymous": true
  }}
}
```
* please check the 'actions' object in `.answers.actions`: you get an action type 'answer' and and 'expression': "Hello!".
This is Susi's response on your query.
* Whenever you want to test a new Susi Rule, repeat these steps and look into the "actions" object to see Susi's answer.

## Write Easy Dialog ("EzD") conversation rules

Susi has currently two AI description languages:
* "Susi Mind Files" which are complex JSON descriptions. You can consider this as
Susi's 'low-level' expression language.
* "EzD" Easy Dialog conversation rules which are simple text files.

What you will learn here is the EzD language. It's actually much simpler than you would imagine. Let's give it a try:

### Tutorial Level 0: Fixed Query-Answer Phrase Collections

In your own `susi_server` directory, open the subdirectory `conf/susi` (or cd into `susi_server/conf/susi`): here you
find a collection of `.txt` (Susi EzD) files and `.json` (Susi Mind) files.
* Let's create our own EzD file, name it `000_en_tutorial.txt`.
* Edit `000_en_tutorial.txt` and inser the following content:
```
# susi EzD tutorial playground
::prior
roses are red
susi is a hack
```
This defines one simple rule: to answer on "roses are red" the phrase "susi is a hack". The other lines mean:
* all lines starting with `#` are comment lines and are ignored.
* all lines starting with a '::' are section declaration modifiers. Here we define that all following rules are 'prior' rules, that mean they rank higher than other rules which depend on phrase patterns. We will learn the details later
* all other text lines define dialog rules. Rules are separate with empty lines. Comment and section declaration modifiers count also like empty lines as separator.

Now you can test the new rule:
* send the following query to Susi (yes your local own susi_server, must be running, look above how that works): "roses are red"
* Susi will answer with "susi is a hack".
The EzD format is just a text file where two lines which are not separated with an empty line represent a conversation pattern.
You can actually add a third line to your file:
```
# susi EzD tutorial playground
::prior
roses are red
susi is a hack
skynet is back
```
With that file, Susi would respond on "roses are red" the answer "susi is a hack" and on the query "susi is a hack" it would respond "skynet is back". Try it!

### Tutorial Level 1: Random Answers
Conversation rules without a deterministic behavior will create less predictable results.
That can easily be defined with EzD rules. Lets consider you want a rule where different answers
on "What is your favorite dish?" are "Potatoes", "Vegetables" or "Fish". Thats easy: add an empty line
to the end of your test file and then:
```
What is your favorite dish
Potatoes|Vegetables|Fish
```

### Tutorial Level 2: Query Alternatives for One Dialog Rule
Maybe you want that Susi responds to several different queries with the same answer. This can be
done very easy with Alternatives in the query line:
```
Bonjour|Buenos días|Ciao
Hello
```

### Tutorial Level 3: Patterns in Queries
In some cases the query lines may be so similar, that you want to use a pattern to declare all possible queries for an answer instead of using fixed alternatives. Query patters can be declared using the "*" wildcard character. A "*" matches any sentence token, but not a substring. That means, a "*" replaces a word or a list of words.

```
May I * you?
Yes you may.
```

### Tutorial Level 4: Using Query-Patterns in Answers
It would be nice if we could use the content of the text which matched with the query patterns.
Every pattern that matched has a pattern number: the first pattern has the number 1 and if 
we want to use that pattern in the result, we can denote that with the term `$1$`:
```
May I get a *?
Yes you may get a $1$!
```
It is of course possible to combine Query-Patterns with alternatives
in the query-part and the response part.

### Tutorial Level 5: Multiple Patterns in Queries and Answers
You can have of course multiple wildcards in the query pattern.
There may be different reasons for that: either it is actually intended
that both wildcards are used in the response or one of the wildcard
is just there because you want to ignore everything where it matches.

The following example shows a case where both wildcards are used:
```
For * I can buy a *
Yeah, I believe $1$ is a god price for a $2$
```

Another case is, where you just want to ignore a whole part of the query:
```
* buy a *
Sure, you should buy a $2$!
```

Text patterns like `$1$`are called 'variable pattern'. There is also a special
variable pattern: `$0$` denotes the whole sentence, identical to what the user asked susi.


### Tutorial Level 6: Using Variables from Answer Terms
Susi has a memory! It is possible to store information inside Susi's user session
and use that information later on. For example, you can tell Susi your most favorite
beer brand:

```
I * like * beer
You then should have one $2$>_beerbrand!
```

Now Susi knows your most favourite beer brand, it is stored inside the variable `_beerbrand`. Note that the variable starts with a leading '_'. That has a special meaning: variables without a leading '_' are only valid while Susi is thinking about
a phrase, not after the response has computed. Susi will not remember a variable, if
that variable was set in a past converation and has not a leading '_'.

You can now use that variable in another rule:
```
* beer * best?
I bet you like $_beerbrand$ beer!
```
Note that the `*` wildcards in the query are not used at all. They are just there
to make it easy that this rule matches. It will i.e. match on "What beer brand is the best?"

Variables are only visible within the same user session. Therefore we need
authentified users and that is a main reason that you have to log on to use Susi.


### Tutorial Level 7: Setting Status Variables

### Tutorial Level 8: Conditions for Answers

### Tutorial Level 9: Rules used as Functions/Templates
