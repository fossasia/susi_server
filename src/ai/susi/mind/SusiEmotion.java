/**
 *  SusiEmotion
 *  Copyright 19.10.2017 by Michael Peter Christen, @0rb1t3r
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *  
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program in the file lgpl21.txt
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package ai.susi.mind;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONObject;

/**
 * Implementation of the Plutchik wheel of emotions.
 * See https://en.wikipedia.org/wiki/Robert_Plutchik and
 * https://en.wikipedia.org/wiki/Contrasting_and_categorization_of_emotions#/media/File:Plutchik-wheel.svg
 */
public class SusiEmotion {


    public static enum Nuance {
        blank("leer"),
        annoyance("Störung"), anger("Ärger"), rage("Wut"),
        apprehension("Besorgnis"), fear("Angst"), terror("Grauen"),
        boredom("Langeweile"), disgust("Ablehnung"), loathing("Abscheu"),
        acceptance("Zustimmung"), trust("Vertrauen"), admiration("Bewunderung"),
        pensiveness("Nachdenklichkeit"), sadness("Traurigkeit"), grief("Betrübnis"),
        serenity("Gelassenheit"), joy("Freude"), ecstasy("Begeisterung"),
        distraction("Ablenkung"), surprise("Überraschung"), amazement("Erstaunen"),
        interest("Neugier"), anticipation("Erwartung"), vigilance("Wachsamkeit"),
        
        awe("Ehrfurcht"),
        aggressiveness("Streitlust"),
        submission("Unterwerfung"),
        contempt("Hass"),
        love("Liebe"),
        remorse("Reue"),
        optimism("Optimismus"),
        disapproval("Enttäuschung");
        
        Nuance(String... translation) {
            // do nothing here, translations are only here to inform the developer (so far)
        }
    }

    public interface NuanceFactory {
        public String getNuance(int strength);
        public boolean isPrimary();
        public boolean isActive();
    }
    
    public static enum Basic implements NuanceFactory {
        fear(false, Nuance.apprehension, Nuance.fear, Nuance.terror),
        anger(true, Nuance.annoyance, Nuance.anger, Nuance.rage),

        trust(false, Nuance.acceptance, Nuance.trust, Nuance.admiration),
        disgust(true, Nuance.boredom, Nuance.disgust, Nuance.loathing),

        joy(false, Nuance.serenity, Nuance.joy, Nuance.ecstasy),
        sadness(true, Nuance.pensiveness, Nuance.sadness, Nuance.grief),

        anticipation(false, Nuance.interest, Nuance.anticipation, Nuance.vigilance),
        surprise(true, Nuance.distraction, Nuance.surprise, Nuance.amazement);

        private Nuance[] nuances;
        private boolean active;
        private Basic(boolean active, Nuance... nuances) {
            if (nuances.length != 3) throw new UnsupportedOperationException("always exactly three nuances required");
            this.active = active;
            this.nuances = nuances;
        }
        public String getNuance(int strength) {
            if (strength < 0 || strength > 3) throw new UnsupportedOperationException("strength must be more or equal 0 and less than 4");
            if (strength == 0) return Nuance.blank.name();
            return this.nuances[strength].name();
        }
        public boolean isPrimary() {
            return true;
        }
        public boolean isActive() {
            return this.active;
        }
    }
    
    public static enum Derived implements NuanceFactory {
        awe(false, Basic.fear, Basic.surprise, "Ehrfurcht"),
        aggressiveness(true, Basic.anger, Basic.anticipation, "Streitlust"),
        
        submission(false, Basic.trust, Basic.fear, "Unterwerfung"),
        contempt(true, Basic.disgust, Basic.anger, "Hass"),
        
        love(false, Basic.joy, Basic.trust, "Liebe"),
        remorse(true, Basic.sadness, Basic.disgust, "Reue"),
        
        optimism(false, Basic.anticipation, Basic.joy, "Optimismus"),
        disapproval(true, Basic.surprise, Basic.sadness, "Enttäuschung");

        private Basic[] composer;
        private boolean active;
        private Derived(boolean active, Basic composer1, Basic composer2, String...translation) {
            this.composer =  new Basic[]{composer1, composer2};
        }
        public String getNuance(int strength) {
            if (strength < 0 || strength > 3) throw new UnsupportedOperationException("strength must be more or equal 0 and less than 4");
            if (strength == 1) return "weak_" + this.name();
            if (strength == 2) return this.name();
            if (strength == 3) return "strong_" + this.name();
            return Nuance.blank.name();
        }
        public boolean isPrimary() {
            return false;
        }
        public boolean isActive() {
            return this.active;
        }
    }
    
    public static enum Bipolar {
        // primary
        fear_anger(Basic.fear, Basic.anger),
        trust_disgust(Basic.trust, Basic.disgust),
        joy_sadness(Basic.joy, Basic.sadness),
        anticipation_surprise(Basic.anticipation, Basic.surprise),
        
        // derived
        awe_aggressiveness(Derived.awe, Derived.aggressiveness),
        submission_contempt(Derived.submission, Derived.contempt),
        love_remorse(Derived.love, Derived.remorse),
        optimism_disapproval(Derived.optimism, Derived.disapproval);
        
        private NuanceFactory passive, active;
        
        /**
         * A bipolar emotion consist of two opposite emotions.
         * One of the opposites is a passive emotion, the other one is an active emotion.
         * If Susi is experiencing the passive emotion, it is not required to do something.
         * If Susi has the active emotion, it shall actively take some action to get out of
         * that situation.
         * @param passive
         * @param active
         */
        private Bipolar(NuanceFactory passive, NuanceFactory active) {
            assert !passive.isActive();
            assert active.isActive();
            this.passive = passive;
            this.active = active;
        }
    }
    
    private Map<Bipolar, AtomicInteger> value;
    
    public SusiEmotion() {
        this.value = new ConcurrentHashMap<>();
        for (Bipolar b: Bipolar.values()) {
            if (b.active.isPrimary()) value.put(b, new AtomicInteger(0));
        }
    }
    
    public void inc(Basic emotion) {
        if (!emotion.isPrimary()) return;
        for (Map.Entry<Bipolar, AtomicInteger> entry: value.entrySet()) {
            if (entry.getKey().active == emotion) {
                int v = entry.getValue().get();
                v = Math.min(100, v + 25);
                entry.getValue().set(v);
                return;
            }
            if (entry.getKey().passive == emotion) {
                int v = entry.getValue().get();
                v = Math.max(-100, v - 25);
                entry.getValue().set(v);
                return;
            }
        }
    }
    
    /*
    private void calculateDerived() {
        for (Map.Entry<Bipolar, AtomicInteger> entry: value.entrySet()) {
            Bipolar bipolar = entry.getKey();
            if (bipolar.active instanceof Derived) {
                // calculate the derived value
                Derived active = (Derived) bipolar.active;
                Derived passive = (Derived) bipolar.passive;
                
            }
        }
    }
    
    private int get(Basic)
    */
    
    public int get(Bipolar emotion) {
        AtomicInteger a = this.value.get(emotion);
        return a == null ? 0 : a.get();
    }
    
    public void calmStep() {
        for (AtomicInteger a: value.values()) {
            int v = a.get();
            if (v == 0) continue;
            v = v <= -10 || v >= 10 ? v - v / 10 : 0;
            a.set(v);
        }
    }
    
    public JSONObject toJSON() {
        JSONObject json = new JSONObject(true);
        JSONObject bipolar = new JSONObject(true);
        int sum = 0;
        for (Map.Entry<Bipolar, AtomicInteger> entry: value.entrySet()) {
            int v = entry.getValue().get();
            sum += v;
            bipolar.put(entry.getKey().name(), v);
        }
        json.put("bipolar", bipolar);
        json.put("flow", - sum / 4);
        return json;
    }
 
    public static void main(String[] args) {
        SusiEmotion se = new SusiEmotion();
        for (int i = 0; i < 5; i++) {
            se.calmStep();
            se.inc(Basic.joy);
            System.out.println(se.toJSON().toString(2));
        }
        se.calmStep();
        System.out.println(se.toJSON().toString(2));
        for (int i = 0; i < 5; i++) {
            se.calmStep();
            se.inc(Basic.sadness);
            System.out.println(se.toJSON().toString(2));
        }
        for (int i = 0; i < 5; i++) {
            se.calmStep();
            System.out.println(se.toJSON().toString(2));
        }
    }
}
