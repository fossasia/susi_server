/**
 *  IndexEntry
 *  Copyright 22.07.2015 by Michael Peter Christen, @0rb1t3r
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *  
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; wo even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program in the file lgpl21.txt
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package org.loklak.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.loklak.objects.MessageEntry;
import org.loklak.objects.Timeline;
import org.loklak.tools.bayes.BayesClassifier;
import org.loklak.tools.bayes.Classification;

public class Classifier {
    
    private final static Category NEGATIVE_FEATURE = Category.NONE;
    
    public enum Category {
        joy,trust,fear,surprise,sadness,disgust,anger,anticipation,
        swear,sex,leet,troll,
        english, german, french, spanish,
        NONE;
    }
    
    public enum Context {
        
        emotion(new Category[]{Category.joy,Category.trust,Category.fear,Category.surprise,Category.sadness,Category.disgust,Category.anger,Category.anticipation}),
        profanity(new Category[]{Category.swear,Category.sex,Category.leet,Category.troll}),
        language(new Category[]{Category.english, Category.german, Category.french, Category.spanish});
        
        public Map<Category, Set<String>> categories;
        BayesClassifier<String, Category> bayes;
        private Context(Category... categories) {
            this.categories = new HashMap<>();
            this.bayes = new BayesClassifier<>();
            for (Category f: categories) this.categories.put(f, null);
        }
        public void init(final int capacity) {
            this.bayes.setMemoryCapacity(capacity);
            // read the categories
            for (Category f: this.categories.keySet()) {
                String keys = DAO.getConfig("classification." + this.name() + "." + f.name(), "");
                Set<String> keyset = new HashSet<>();
                for (String key: keys.toLowerCase().split(",")) keyset.add(key);
                this.categories.put(f, keyset);
            }
            // consistency check of categories: identify words appearing not in one category only
            Set<String> inconsistentWords = new HashSet<>();
            for (Map.Entry<Category, Set<String>> c0: this.categories.entrySet()) {
                for (String key: c0.getValue()) {
                    doublecheck: for (Map.Entry<Category, Set<String>> c1: this.categories.entrySet()) {
                        if (c1.getKey().equals(c0.getKey())) continue doublecheck;
                        if (c1.getValue().contains(key)) {inconsistentWords.add(key); break doublecheck;}
                    }
                }
            }
            // remove inconsistent words from all categories
            for (String key: inconsistentWords) {
                forgetWord(key);
            }
        }
        public Set<String> vocabulary() {
            Set<String> v = new HashSet<String>();
            for (Set<String> v0: this.categories.values()) v.addAll(v0);
            return v;
        }
        public void forgetWord(String key) {
            for (Map.Entry<Category, Set<String>> c2: this.categories.entrySet()) {
                c2.getValue().remove(key);
            }
        }
        public void learnPhrase(String phrase) {
            try {
                List<String> words = normalize(phrase);
                for (Map.Entry<Category, Set<String>> entry: categories.entrySet()) {
                    for (String word: words) {
                        if (word.length() == 0) continue;
                        if (entry.getValue().contains(word)) {
                            bayes.learn(entry.getKey(), words);
                        }
                    }
                }
                bayes.learn(NEGATIVE_FEATURE, words);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        public Classification<String, Category> classify(String phrase) {
            List<String> words = normalize(phrase);
            return this.bayes.classify(words);
        }
        private List<String> normalize(String phrase) {
            String cleanphrase = phrase.toLowerCase().replaceAll("\\W", " ");
            String[] rawtokens = cleanphrase.split("\\s");
            List<String> tokens = new ArrayList<>();
            for (String token: rawtokens) if (token.length() > 2) tokens.add(token);
            return tokens;
        }
    }
    
    public static synchronized void learnPhrase(String message) {
        for (Context c: Context.values()) c.learnPhrase(message);
    }
    
    public static Map<Context, Classification<String, Category>> classify(String phrase) {
        Map<Context, Classification<String, Category>> map = new HashMap<>();
        for (Context c: Context.values()) {
            Classification<String, Category> classification = c.classify(phrase);
            if (classification == null) return null;
            if (classification.getProbability() == 0.0) return null;
            map.put(c, classification);
        }
        return map;
    }
    
    public static void init(int maxsize, int initsize) {
        
        // load the context keys
        for (Context c: Context.values()) c.init(maxsize);
        /*
        // ensure consistency throughout the contexts: remove words which could confuse the bayesian filter
        for (Context c: Context.values()) {
            Set<String> voc = c.vocabulary();
            for (Context c0: Context.values()) {
                if (c0.equals(c)) continue;
                for (String key: voc) c0.forget(key);
            }
        }
         */
        
        // load a test set
        DAO.SearchLocalMessages testset = new DAO.SearchLocalMessages("", Timeline.Order.CREATED_AT, 0, initsize, 0);
        Timeline tl = testset.timeline;
        for (Context c: Context.values()) {
            //Set<String> voc = c.vocabulary();
            for (MessageEntry m: tl) {
                c.learnPhrase(m.getText(Integer.MAX_VALUE, ""));
            }
        }
        /*
        for (MessageEntry m: tl) {
            System.out.println(m.getText());
            System.out.print("  -> ");
            Map<Context, Classification<String, Category>> classification = classify(m.getText());
            for (Map.Entry<Context, Classification<String, Category>> c: classification.entrySet()) {
                System.out.print(c.getKey().name()  + " = " + c.getValue().getCategory() + "[" + c.getValue().getProbability() + "]" + "  ");
            }
            System.out.println();
        }
        */
    }
    
}
