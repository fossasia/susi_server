angular.module('myApp')
.factory('analyticService', 
    function($localStorage, $moment){
        // returns an object of {word:freq} pairs
        
        return {
            getNewTweets: getNewTweets,
            updateWordFreq: updateWordFreq,
            updateMentionFreq: updateMentionFreq,
            updateHashtagDateFreq: updateHashtagDateFreq
        }
        
        // returns an array of new tweets, comparing with those in local storage
        function getNewTweets(statuses){
            // initial oldest date from first element of last poll
            var prevNewest = new Date($localStorage.tweets[0].created_at);
            var idx =0;
            var tweetDate= new Date(statuses[idx].created_at);
            var currOldest = new Date(statuses[statuses.length-1].created_at);
            
            var newTweets =[];
            // if the whole array is new prepend to localStorage tweets array
            if(prevNewest < currOldest){
                $localStorage.tweets = statuses.concat($localStorage.tweets);
                newTweets = statuses;
            } else {
                // else prepend only new tweets to localStorage tweets array in desc order
                while(tweetDate > prevNewest && idx !== statuses.length){
                    newTweets.push(statuses[idx]);
                    // // console.log($localStorage.tweets);
                    idx++;
                    tweetDate= new Date(statuses[idx].created_at);
                }
            }
            return newTweets;
        }
        
        function updateWordFreq(newTweets){
            
            // sWords is an array of selected words from tweet
            var sWords = newTweets
            .map(function(tweet){ 
                // remove image tags, punctuation, links
                return tweet.text.toLowerCase()
                .replace(/<img\b[^>]*>.*?>?/g,'').replace(/[!,;.]/g,'')
                .replace(/https?:\/\/[-A-Za-z0-9+&@#/%?=~_()|!:,.;]*[-A-Za-z0-9+&@#/%=~_()|]/gi,''); 
            })
            .join(" ")
            .split(/[\s\/]+/g);
            // console.log(sWords);
            
            
            // count w/ duplicates
            var iWordsCount = sWords.length; 
            
            // object of english words to ignore
            var ignore = ['and','the','to','a','of','for','as','i','with','it','is','on','that','this','can','in','be','has','if'];
            ignore = (function(){
                var o = {}; // object prop checking > in array checking
                var iCount = ignore.length;
                for (var i=0;i<iCount;i++){
                    o[ignore[i]] = true;
                }
                return o;
            }());
            
            // use an object for the math
            var counts = {}; 
            for (var i=0; i<iWordsCount; i++) {
                var sWord = sWords[i];
                if (!ignore[sWord]) {
                    counts[sWord] = counts[sWord] || 0;
                    counts[sWord]++;
                }
            }
            // // console.log("counts: ", counts);
            
            if(Object.keys(counts).length === 0 && counts.constructor === Object){
                $localStorage.wordFreq = counts;
            } else {
                for (sWord in counts){
                    $localStorage.wordFreq[sWord] = $localStorage.wordFreq[sWord] || 0;
                    $localStorage.wordFreq[sWord] += counts[sWord];
                }
            }
            
            // use d3.map // an array of text-freq objects to return
            // var arr = []; 
            // for (sWord in counts) {
            //     arr.push({
            //         text: sWord,
            //         frequency: counts[sWord]
            //     });
            // }
            
            
            // // sort array by descending frequency 
            // return arr.sort(function(a,b){
            //     return (a.frequency > b.frequency) ? -1 : ((a.frequency < b.frequency) ? 1 : 0);
            // });

        };

        function updateMentionFreq(newTweets){
            // console.log("updating mentions");
            
                var mentionsArr = newTweets.map(function(tweet){
                    return tweet.mentions;
                }).reduce(function(prev, next){
                    return prev.concat(next);
                }, []);
                
                mentionsArr.forEach(function(mention){
                    $localStorage.mentionFreq[mention] = $localStorage.mentionFreq[mention] || 0;
                    $localStorage.mentionFreq[mention] ++;
                })
                
                // console.log($localStorage.mentionFreq);
                
                
        }
        
        function updateHashtagDateFreq(newTweets, maxHashtags){
            // console.log("updating hashtagDateFreq");

            // returns an array of obj 
            // {"hashtag": hashtag, "freq": freq}
            function getHashtagFreq(tweetArr){
                
                // res is an obj of hashtag:count pairs
                var res = {};
                
                var hashtagsArr = tweetArr.map(function(tweet){
                    return tweet.hashtags;
                }).reduce(function(prev, next){
                    return prev.concat(next);
                }, []);
                
                hashtagsArr.forEach(function(hashtag){
                    res[hashtag] = res[hashtag] || 0;
                    res[hashtag] ++;
                })
                
                // process res to return array of objects
                var arr = []; 
                for (hashtag in res) {
                    arr.push({
                        hashtag: hashtag,
                        frequency: res[hashtag]
                    });
                }
                // console.log(arr);
                arr.sort(function(a,b){ return b.frequency-a.frequency;});
                
                return arr;
            }
            
            function filterTopN(groupedByDate, N){
                
                var res = [];
                // console.log(groupedByDate);
                groupedByDate.forEach(function(obj){ 
                    console.log(obj);
                    
                    if (obj.hasOwnProperty("key") && obj["key"] === "undefined") {
                        return;
                    }
                    var topNThisDate = {};
                    // key refers to dayDiff
                    topNThisDate["day"] = obj["key"];
                    for(var i=0; i<N; i++){
                        var topN = "top"+(i+1);                        
                        topNThisDate[topN] = obj["values"][i] || 0;
                    }
                    res.push(topNThisDate);
                });
                return res;
            }
            
            var latestDateM =$moment(newTweets[0].created_at).set('hour',0).set('minute',0).set('second',0);
            
            // An array containing objects of {"dayDiff": num, "top1": str,.., "topN":str}
            var groupByDate = d3.nest()
            // compare by date only, and not time
            .key(function(d){  
                var currentM = $moment(d.created_at).set('hour',0).set('minute',0).set('second',0);
                var dayDiff = latestDateM.diff(currentM, 'days');
                // don't display day diff >= 40
                if(dayDiff<40) return dayDiff
            })
            // set latest day at idx 0 (as dayDiff = 0)
            .sortKeys(function(a,b){ return b-a;}) 
            // process tweets each dayDiff
            .rollup(function(leaves){ return getHashtagFreq(leaves); })
            .entries(newTweets);
            // console.log("grouped", groupByDate);
            
            // sort desc freq
            var filteredFreq = d3.nest()
            // flatten array to contain objs w/ dayDiff & top N hashtags 
            .rollup(function(leaves){ return filterTopN(leaves, maxHashtags); }) 
            .entries(groupByDate);
            // console.log("filtered freq", filteredFreq);
            
            $localStorage.hashtagDateFreq = filteredFreq;
                
        }
        
})