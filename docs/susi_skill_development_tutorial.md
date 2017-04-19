# Susi Skill Development Tutorial

Do you want your own AI Skills for Susi? It's surprisingly easy to add more Skills to Susi.

## Getting Started

It's easy, DON'T PANIC. You don't need to be a software developer to enhance Susi.

### What you have to do

We have a Susi Skill develoment environment based on an Etherpad. Are you unaware what an Etherpad is? It is a blank web page where you can just put in your text and everyone can collaborate.

* open http://dream.asksusi.com
* name a dream (just pick a name for your tests)
* the etherpad is filled with a welcome message, just delete the content completely

ATTENTION: the Susi Dream zone is a TEMPORARY zone. We may clean up that place at any time. It is only for testing your new Susi Skills
If you want your new Susi Skills to be permanent, send us a pull request, see [submitting_skills_to_git.md](submitting_skills_to_git.md) for a tutorial.

### Preparation to start testing

To test the Susi Skills you are editing, you require the Susi Android Application (see: https://github.com/fossasia/susi_android ) or you can also test them online at http://asksusi.com/chat

Within the Susi chat dialog, enter

```
dream <testname>
```

where `<testname>` is the name of the etherpad you just entered in http://dream.asksusi.com

Now all Skills you enter in the dream zone are available instantly in your chat! Thats easy, is it?

To stop testing your new Susi Skills, write `stop dreaming`.

### Tutorial Level 0: Fixed Query-Answer Phrase Collections

In you dream test zone (the etherpad) write:
```
# susi Skill tutorial playground
roses are red
susi is a hack
```
This defines one simple Skill: to answer on "roses are red" the phrase "susi is a hack". The other lines mean:
* all lines starting with `#` are comment lines and are ignored.
* all other text lines define Skills. Skills are separated by empty lines. Comment and section declaration modifiers also count as empty lines and separate Skills.

Now you can test the new Skill:
* send the following query to Susi: "roses are red"
* Susi will answer with "susi is a hack".
The Skill file is just a text file where two lines which are not separated with an empty line represent a conversation pattern.
You can actually add a third line to your file:
```
# susi tutorial playground
::prior
roses are red
susi is a hack
skynet is back
```
With that file, Susi would respond on "roses are red" the answer "susi is a hack" and on the query "susi is a hack" it would respond "skynet is back". Try it!

### Tutorial Level 1: Random Answers
Skills without a deterministic behavior will create less predictable results.
That can easily be defined with Skills. Lets consider you want a Skill where different answers
on "What is your favorite dish?" are "Potatoes", "Vegetables" or "Fish". Thats easy: add an empty line
to the end of your test file and then:
```
What is your favorite dish
Potatoes|Vegetables|Fish
```

### Tutorial Level 2: Query Alternatives
Maybe you want that Susi responds to several different queries with the same answer. This can be
done very easy with Alternatives in the query line:
```
Bonjour|Buenos días|Ciao
Hello
```

### Tutorial Level 3: Patterns in Queries
In some cases the query lines may be so similar, that you want to use a pattern to declare all possible queries for an answer instead of using fixed alternatives. Query patters can be declared using the `*` wildcard character. A `*` matches any sentence token, but not a substring. That means, a `*` replaces a word or a list of words.

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

A status variable is exactly the same variable as in the previous tutorial level. The difference is that the value for the variable
does not come from a term that is visible in the answer. Instead, an invisible 'status' can be assigned to the variable.

```
I am so happy!
Good for you!^excited^>_mood

I am bored.
Make something!^inactive^>_mood

How do I feel?
You are $_mood$.
```

In this example, Susi remembers your mood and can tell you about it. The actual word which was used to describe the mood was never printed to the user before, because using the `^^` symbols it got quoted and became invisible.

### Tutorial Level 8: Conditions for Answers

Whenever you are using variables in answers which are not set in the same rule, you should test if these variables exist and had been set.
It is possible to add simple conditions to the answer lines:

```
How do I feel?
?$_mood$:You are $_mood$.:I don't know your mood.
```

This rule replaces the last rule from the latest Level. It makes a distinction between the case where no knowledge about the mood is there, or the mood is set.

```
Shall I *?
?$_mood$=excited:You will be happy, whatever I say!
```

### Tutorial Level 9: Rules used as Functions/Templates (Basic Self-Reflection)

Susi can call itself during an answer preparation. This can be used to create rules which are designed
to be called in such a self-call. For example:

```
function colour
red|green|blue|white|black|yellow|purple|brown
```

We would not expect that anybody asks "function colour". But we can use this to add the name of a colour in our answer:

```
What is your favourite colour?
?$_mycolour$:My favourite colour is $_mycolour$!:I like `function colour`>_mycolour!
```

Here, the colour is randomly generated with the `function colour` call, but only if Susi has not done that yet. If Susi just generated a colour in the answer, that answer will be stored in the variable `_mycolour`. But if that variable already existed, it will be used to make the answer without the `function colour`.

### Tutorial Level 10: Embed Javascript into a Skill

If you are able to compute whatever you want to inside a rule, there are billions of possibilities of what you can do with Susi Skills.
Embedding Javascript is extremely easy, for example:

```
javascript hello
!javascript:$!$ from Nashorn
print('Hello world');
eol
```

What you see here is the bang-notion which always starts with a '!', followed with the script language name that is used, then followed
with a ':' and then follows the return statement. The value of the script is represented with the $!$ variable object.
This javascript skill catches everything up that the script produces: std-out, error-out an direct term computations, i.e.


```
compute * to the power of *
!javascript:$1$^$2$ = $!$
Math.pow($1$, $2$)
eol
```

### Tutorial Level 11: Call an external API

Important parts of an AI implementation is, to be able to access big data, many different data sources and to steer
services outside of the body of the AI. To do so, it must be possible to call an external API.
Such a service is called a 'console service' in Susi:

```
tweet about *
!console:$text$
{
"url":"http://api.loklak.org/api/search.json?q=$1$",
"path":"$.statuses"
}
eol
```

This will call the loklak search API and gets back a big list of tweets from the given query in $1$. That list is somewhere inside the
answer json of the API call, and we must tell Susi where it can find that list. This hint is given in the attribute 'path' which has
the syntax of an JSONPath. Here, the statuses object contains a list of objects, which contain always the same attribut keys.
One of these attributes has the name 'text' and that attribute is selected with the $text$ pattern.
Note that the bang definition part until the eol line must be given in JSON.

### Tutorial Level 12: Custom Actions

Susi Skills may return different types of actions. Non-answer actions my get their content using console rules.
Custom actions therefore are combined console rules with custom action types

(to be implemented)

### Tutorial Level 13: Thinking with Backtracking

Backtracking is the ability of a program to revert a already made setting and take an alternative option. If we consider this behaviour at different states of a computation, then this produces a tree-like parameter graph, which is called a decision tree. Susi's data structures are made in such a way, that result tables are an element of 'thinking'. Such result tables are 'bags' for backtracking options. We will learn how to use that principle to create loops which are useful for problem-solving.

(to be implemented)

### Tutorial Level 14: Expert Systems with first-order logic

A first-order logic is expressed in terms of relations, represented as facts and rules. We already defined facts and rules using the methods as described above. Backtracking and unification is the computation model for such kind of expert systems. In this tutorial we will learn how to express a program flow using logic elements.

(to be implemented)

### Tutorial Level 15: Skill Reflection

We are able to set variable content and read them in skills. But we must also be able to read skills in the same way as we read variables. We should be able to answer on questions like 'we cannot solve this because there is no rule for that', or 'we have several rules, which one is preferred'. 

(to be implemented)

### Tutorial Level 15: Skills which create Skills

It is a core principle of intelligent systems to be able to learn and enhance themselves. We want to create skill rules which are able to create new skill rules.

(to be implemened)

### Tutorial Level 16: Inter-Susi Instance Dialog

Susi runs in user instances: every chat user of a Susi instance is an individual instance. Instances may be customized i.e. if the user calls Susi to dream with a test skill set. Therefore different Susi instances behave differently. It should be possible that two different Susi instances have different 'opinions' and that two instances start a dialog with each other to find a consensus.
We will learn here how to connect those instances to each other so they can talk.

(to be implemented)
