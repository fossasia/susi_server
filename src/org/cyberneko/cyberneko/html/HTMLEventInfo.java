/* 
 * Copyright 2002-2009 Andy Clark, Marc Guillemot
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cyberneko.html;

/**
 * This interface is used to pass augmentated information to the
 * application through the XNI pipeline.
 *
 * @author Andy Clark
 *
 * @version $Id: HTMLEventInfo.java,v 1.4 2005/02/14 03:56:54 andyc Exp $
 */
public interface HTMLEventInfo {

    //
    // HTMLEventInfo methods
    //

    // location information

    /** Returns the line number of the beginning of this event.*/
    public int getBeginLineNumber();

    /** Returns the column number of the beginning of this event.*/
    public int getBeginColumnNumber();

    /** Returns the character offset of the beginning of this event.*/
    public int getBeginCharacterOffset();

    /** Returns the line number of the end of this event.*/
    public int getEndLineNumber();

    /** Returns the column number of the end of this event.*/
    public int getEndColumnNumber();

    /** Returns the character offset of the end of this event.*/
    public int getEndCharacterOffset();

    // other information

    /** Returns true if this corresponding event was synthesized. */
    public boolean isSynthesized();

    /**
     * Synthesized infoset item.
     *
     * @author Andy Clark
     */
    public static class SynthesizedItem
        implements HTMLEventInfo {

        //
        // HTMLEventInfo methods
        //

        // location information

        /** Returns the line number of the beginning of this event.*/
        public int getBeginLineNumber() {
            return -1;
        } // getBeginLineNumber():int

        /** Returns the column number of the beginning of this event.*/
        public int getBeginColumnNumber() { 
            return -1;
        } // getBeginColumnNumber():int

        /** Returns the character offset of the beginning of this event.*/
        public int getBeginCharacterOffset() { 
            return -1;
        } // getBeginCharacterOffset():int

        /** Returns the line number of the end of this event.*/
        public int getEndLineNumber() {
            return -1;
        } // getEndLineNumber():int

        /** Returns the column number of the end of this event.*/
        public int getEndColumnNumber() {
            return -1;
        } // getEndColumnNumber():int

        /** Returns the character offset of the end of this event.*/
        public int getEndCharacterOffset() { 
            return -1;
        } // getEndCharacterOffset():int

        // other information

        /** Returns true if this corresponding event was synthesized. */
        public boolean isSynthesized() {
            return true;
        } // isSynthesized():boolean

        //
        // Object methods
        //

        /** Returns a string representation of this object. */
        public String toString() {
            return "synthesized";
        } // toString():String

    } // class SynthesizedItem

} // interface HTMLEventInfo
