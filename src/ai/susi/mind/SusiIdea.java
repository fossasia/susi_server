/**
 *  SusiIdea
 *  Copyright 13.07.2016 by Michael Peter Christen, @0rb1t3r
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

import java.util.LinkedHashSet;
import java.util.regex.PatternSyntaxException;

import ai.susi.DAO;
import ai.susi.mind.SusiPattern.SusiMatcher;
import ai.susi.server.ClientIdentity;

/**
 * An idea is the application of a intent on a specific input. This matches with the idea of ideas where
 * an idea is the 'sudden' solution to a problem with the hint how to apply the idea's core concept
 * on the given input details. That is what this class does: it combines a intent with the pattern
 * that matched from the input with the intent.
 */
public class SusiIdea {

    private SusiIntent intent;
    private LinkedHashSet<SusiMatcher> matchers;

    /**
     * create an idea based on a intent
     * @param intent the intent that matched
     * @throws PatternSyntaxException
     */
    public SusiIdea(SusiIntent intent) throws PatternSyntaxException {
        this.intent = intent;
        this.matchers = null;
    }

    public SusiIntent getIntent() {
        return this.intent;
    }

    /**
     * Set a matcher to an idea. Having a matcher makes the idea 'valid', it means
     * that the idea can be instantiated with a query.
     * @param matchers the idea
     * @return
     */
    public SusiIdea setMatchers(LinkedHashSet<SusiMatcher> matchers) {
        this.matchers = matchers;
        return this;
    }

    /**
     * Generate a proof that the idea is correct!
     * Several intents can be candidates for answer computation. Each of such an intent is expressed as
     * an SusiIdea object. They are combined with a recall (data objects from past answer computations)
     * and tested by construction of an answer as the result of a causality chain that is described in the
     * idea. If the chain can be constructed by finding instances of variables, then this is a kind of
     * proof that the answer is correct. That answer is returned in the SusiArgument object.
     * @param recall the data objects from past computations
     * @param identity the identity of the user
     * @param userLanguage the language of the user
     * @param minds the hierarchy of mind layers that may be used for reflection within the argument
     * @return the result of the application of the intent, a thought argument containing the thoughts which terminated into a final mindstate or NULL if the consideration should be rejected
     */
    public SusiArgument consideration(
            SusiThought recall,
            ClientIdentity identity,
            SusiLanguage userLanguage,
            SusiMind... minds) {

        // that argument is filled with an idea which consist of the query where we extract the identified data entities
        if (this.matchers != null) alternatives: for (SusiMatcher matcher: this.matchers) {

            // initialize keynote (basic data for unification) for flow
            SusiThought keynote = new SusiThought(matcher);

            // we deduced thoughts from the inferences in the intents. The keynote also carries these actions
            this.intent.getActionsClone().forEach(action -> keynote.addAction(action));

            DAO.log("Susi has an idea: on " + keynote.toString() + " apply " + this.intent.toJSON());
            // we start with the recall from previous interactions as new flow
            final SusiArgument flow = new SusiArgument(identity, userLanguage, minds) // empty flow
                    .think(recall)   // the past
                    .think(keynote); // the idea, including actions (also: the "now")

            // lets apply the intents that belong to this specific consideration
            for (SusiInference inference: this.intent.getInferences()) {
                SusiThought implication = inference.applyProcedures(flow);
                DAO.log("Susi is thinking about: " + implication.toString());
                // make sure that we are not stuck:
                // in case that we are stuck (== no progress was made) we consider the next alternative matcher
                if (implication.isFailed() || flow.mindstate().equals(implication)) continue alternatives; // TODO: do this only if specific marker is in intent

                // think
                flow.think(implication); // the future
            }

            // add skill source
            flow.addSkill(this.intent.getSkillID(), this.intent.getUtterances().iterator().next().getLine());

            return flow;
        }
        // fail, no alternative was successful
        return null;
    }

    @Override
    public int hashCode() {
        // we compare ideas only by the intent
        return this.intent.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        // we compare ideas only by the intent
        if (!(o instanceof SusiIdea)) return false;
        SusiIdea si = (SusiIdea) o;
        return this.intent.equals(si.intent);
    }

    public String toString() {
        return this.intent.toString();
    }
}
