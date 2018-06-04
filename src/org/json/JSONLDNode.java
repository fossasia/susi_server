/**
 *  JSONLDNode
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
 * https://json-ld.org/spec/latest/json-ld/#node-objects
 * 
 * A node object represents zero or more properties of a node in the graph serialized by the JSON-LD
 *  document. A JSON object is a node object if it exists outside of a JSON-LD context and:
 *  - it is not the top-most JSON object in the JSON-LD document consisting of no other members
 *    than @graph and @context,
 *  - it does not contain the @value, @list, or @set keywords, and
 *  - it is not a graph object.
 *  
 *  The properties of a node in a graph may be spread among different node objects within a document.
 *  When that happens, the keys of the different node objects need to be merged to create the
 *  properties of the resulting node.
 *  
 *  A node object MUST be a JSON object. All keys which are not IRIs, compact IRIs, terms valid in
 *  the active context, or one of the following keywords (or alias of such a keyword) MUST be ignored
 *  when processed:
 *  
 *  @context,
 *  @id,
 *  @graph,
 *  @nest,
 *  @type,
 *  @reverse, or
 *  @index
 *
 *
 */
public class JSONLDNode extends JSONObject {

    public static final String BASE = "@base";
    public static final String BLANK_NODE_PREFIX = "_:";
    public static final String CONTAINER = "@container";
    public static final String CONTEXT = "@context";
    public static final String DEFAULT = "@default";
    public static final String EMBED = "@embed";
    public static final String EMBED_CHILDREN = "@embedChildren";
    public static final String EXPLICIT = "@explicit";
    public static final String GRAPH = "@graph";
    public static final String ID = "@id";
    public static final String INDEX = "@index";
    public static final String LANGUAGE = "@language";
    public static final String LIST = "@list";
    public static final String NONE = "@none";
    public static final String NULL = "@null";
    public static final String OMIT_DEFAULT = "@omitDefault";
    public static final String PRESERVE = "@preserve";
    public static final String REQUIRE_ALL = "@requireAll";
    public static final String REVERSE = "@reverse";
    public static final String SET = "@set";
    public static final String TYPE = "@type";
    public static final String VALUE = "@value";
    public static final String VOCAB = "@vocab";
	
    
    
    
}
