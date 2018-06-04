/**
 *  JSONLDGraph
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
 * https://json-ld.org/spec/latest/json-ld/#graph-objects
 * 
 * 
 * A graph object represents a named graph, which MAY include include an explicit graph name.
 * A JSON object is a graph object if it exists outside of a JSON-LD context,
 * it is not a node object, it is not the top-most JSON object in the JSON-LD document,
 * and it consists of no members other than @graph, @index, @id and @context, or an alias
 * of one of these keywords.
 * 
 * If the graph object contains the @context key, its value MUST be null, an absolute IRI,
 * a relative IRI, a context definition, or an array composed of any of these.
 * 
 * If the graph object contains the @id key, its value is used as the identifier (graph name)
 * of a named graph, and MUST be an absolute IRI, a relative IRI, or a compact IRI
 * (including blank node identifiers). See section 3.3 Node Identifiers, section 4.4
 * Compact IRIs, and section 4.16 Identifying Blank Nodes for further discussion on @id values.
 * 
 * A graph object without an @id member is also a simple graph object and represents a
 * named graph without an explicit identifier, although in the data model it still has
 * a graph name, which is an implicitly allocated blank node identifier.
 * 
 * The value of the @graph key MUST be a node object or an array of zero or more node objects.
 *
 */
public class JSONLDGraph {

}
