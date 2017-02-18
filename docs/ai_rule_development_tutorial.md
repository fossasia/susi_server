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

### Tutorial Level 2: Query Alternatives for One Dialog Rule

### Tutorial Level 3: Patterns in Queries

### Tutorial Level 4: Using Query-Patterns in Answers

### Tutorial Level 5: Multiple Patterns in Queries and Answers

### Tutorial Level 6: Using Variables from Answer Terms

### Tutorial Level 7: Setting Status Variables

### Tutorial Level 8: Conditions for Answers

### Tutorial Level 9: Rules used as Functions/Templates
