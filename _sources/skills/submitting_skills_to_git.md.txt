# Submitting Skills to git

If you created your own Susi conversation Skills, you may want to submit these to the Susi git repository at
https://github.com/fossasia/susi_server

To do so, you must prepare your skills as a file and submit this as a pull request.
Adding a file to the general Susi skill set is not difficult, but requires some more preparation than just testing
the skills with a Susi Dream.

## Getting Started

It's easy, DON'T PANIC. You don't need to be a software developer to enhance Susi.

### What you have to do

You must run a local `susi_server` to test your self-made conversation rules

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

You may have done that already using the susi_skill_development_tutorial.md
If you have not done that, read that document first.

### Write a file to your own Susi installation

In your own `susi_server` directory, open the subdirectory `conf/susi` (or cd into `susi_server/conf/susi`): here you
find a collection of `.txt` (Susi EzD) files and `.json` (Susi Mind) files.
* Create our own EzD file, i.e. name it `000_en_tutorial.txt` (you may want to give it your individual name, when you submit it to your repository the name should make it visible what to expect from the new skills you submitted.
* Edit the file and add your skills. Maybe you just need to do a copy-paste from your Susi Dream test.
* Check that the skills are working by calling the chat api (see above)
* If you change anything, it is maybe necessary to restart Susi to see the effect

### Send us a pull request

If you think that your Skills are working, please send us a pull request so we can integrate it to our set of Susi Skills for everyone!

Thank you!