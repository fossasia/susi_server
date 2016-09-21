This path contains Susi cognition files. Susi is an AI system which is able to
- understand sentences using pattern matching
- access external databases thought json APIs to retrieve informatio
- do backtracking to select working data sources
- do a conversation using matching on previous conversation logs

The Susi rule system is designed to be easy to understand and it is open for contributions.
We consider this as a "wikification of AI rules". Please try to understand the rule system
and send us a pull request to add your own rule set.

The main principle for Susi rules is to be "Eliza on steroids". here is a simple example:

```
{
	“keys“ :[“angry“],
	“phrases“:[ {“type“:“pattern“, “expression“:“*I feel *“} ],
	“actions“:[ {“type“:“answer“, “select“:“random“, “phrases“:[
	“Why do you feel $2$?“
	]}]
}
```

This produces answers for sentences like "I feel mad" because there is a category assignment for the word "mad":
```
	"categories":{
		"angry":["mad", "furious", "enraged", "excited", "wrathful", "indignant", "exasperated", "aroused", "inflamed"]
  }

```
The rule therefore fires on a sentences which contain any of the category assignments of "angry"

This is a simple example. We can also use rules which contain a 'process'. Every process may be either a console process or a flow process.
Here is another example which uses both:

```
{
	"rules":[
		{
			"score"  :1,
			"phrases":[	{"type":"regex", "expression":"what is (?:(?:a )*)(.*)"},
					  	{"type":"regex", "expression":"describe (?:(?:a )*)(.*)"},
					  	{"type":"regex", "expression":"please describe (?:(?:a )*)(.*)"},
					  	{"type":"regex", "expression":"explain (?:(?:a )*)(.*)"},
					  	{"type":"regex", "expression":"please explain (?:(?:a )*)(.*)"},
					  	{"type":"regex", "expression":"search urbandictionary for (?:(?:a )*)(.*)"},
					  	{"type":"pattern", "expression":"what does * mean"}
					  ],
			"process":[	{"type":"console", "expression":"SELECT description FROM wikidata WHERE query='$1$';"},
			            {"type":"flow", "expression":"REJECT '$description$' MATCHING 'Wikipedia disambiguation page' PATTERN"} ],
			"actions":[	{"type":"answer", "select":"random", "phrases":[
							"$1$ is $description$."
						]}]
    }
  ]
}
```
This rule matches on a variety of queries and used a query term to retrieve data from wikidata. It only accepts that data if
it does not contain the sentence "Wikipedia disambiguation page" which appears many times on wikidata but is not a good
result for any query.

