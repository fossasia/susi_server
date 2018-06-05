/**
 *  JSONLDValue
 *  Copyright 04.06.2018 by Michael Peter Christen, @0rb1t3r
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

package org.json;

/**
 * https://json-ld.org/spec/latest/json-ld/#value-objects
 * 
 * A value object is used to explicitly associate a type or a language with a value to
 * create a typed value or a language-tagged string.
 * 
 * A value object MUST be a JSON object containing the @value key. It MAY also
 * contain an @type, an @language, an @index, or an @context key but MUST NOT
 * contain both an @type and an @language key at the same time. A value object MUST NOT
 * contain any other keys that expand to an absolute IRI or keyword.
 * 
 * The value associated with the @value key MUST be either a string, a number, true, false or null.
 * 
 * The value associated with the @type key MUST be a term, a compact IRI, an absolute IRI,
 * a string which can be turned into an absolute IRI using the vocabulary mapping, or null.
 * 
 * The value associated with the @language key MUST have the lexical form described in [BCP47], or be null.
 * 
 * The value associated with the @index key MUST be a string.
 * 
 */
public class JSONLDValue {

}
