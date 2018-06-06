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

package ai.susi.json;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

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
public class JsonLDNode extends JSONObject {

    public static final String CONTEXT = "@context";
    public static final String TYPE = "@type";
    public static final String ID = "@id";
    
    public JsonLDNode(String type, String context) {
        super(true);
        this.put(CONTEXT, context);
        this.put(TYPE, type);
    }
    

    public JsonLDNode(String type) {
        super(true);
        this.put(TYPE, type);
    }
    
    public JsonLDNode setSubject(String subject) {
        this.put(ID, subject);
        return this;
    }
    
    public String getSubject() {
        return this.optString(ID);
    }
    
    public String getContext() {
        return this.optString(CONTEXT);
    }
    
    public String getType() {
        return this.optString(TYPE);
    }
    
    public String getVocabulary() {
        return getContext() + "/" + getType();
    }
    
    public JsonLDNode setPredicate(String key, Object value) {
        assert value instanceof String || value instanceof JsonLDNode;
        this.put(key, value);
        return this;
    }

    public String getPredicateName(String key) {
        return this.getVocabulary() + "#" + key;
    }
    
    public Object getPredicateValue(String key) {
        return this.get(key);
    }

    public List<String> getPredicates() {
        ArrayList<String> predicates = new ArrayList<>();
        this.keySet().forEach(key -> {if (key.charAt(0) != '@') predicates.add(key);});
        return predicates;
    }
    
    public String toRDFTriple() {
        return toRDFTriple(this.getSubject(), this.getVocabulary());
    }
    private String toRDFTriple(String subject, String vocabulary) {
        StringBuilder sb = new StringBuilder();
        for (String predicate: getPredicates()) {
            Object value = this.getPredicateValue(predicate);
            if (value instanceof String) sb.append("<" + subject + "> <" + vocabulary + "#" + predicate + "> <" + ((String) value) + ">\n");
            if (value instanceof JsonLDNode) sb.append(((JsonLDNode) value).toRDFTriple(subject, vocabulary));
        }
        return sb.toString();
    }
    
    /*
{
  "@context": "http://schema.org",
  "@type": "Event",
  "location": {
    "@type": "Place",
    "address": {
      "@type": "PostalAddress",
      "addressLocality": "Denver",
      "addressRegion": "CO",
      "postalCode": "80209",
      "streetAddress": "7 S. Broadway"
    },
    "name": "The Hi-Dive"
  },
  "name": "Typhoon with Radiation City",
  "offers": {
    "@type": "Offer",
    "price": "13.00",
    "priceCurrency": "USD",
    "url": "http://www.ticketfly.com/purchase/309433"
  },
  "startDate": "2013-09-14T21:30"
}
     */
    
    public static void main(String[] args) {
        JsonLDNode event = new JsonLDNode("Event", "http://schema.org")
                .setSubject("http://an.event.home.page.ninja/tomorrow.html")
                .setPredicate("name", "Typhoon with Radiation City")
                .setPredicate("startDate", "2013-09-14T21:30")
                .setPredicate("location",
                        new JsonLDNode("Place")
                            .setPredicate("name", "The Hi-Dive")
                            .setPredicate("address",
                                    new JsonLDNode("PostalAddress")
                                        .setPredicate("addressLocality", "Denver")
                                        .setPredicate("addressRegion", "CO")
                                        .setPredicate("postalCode", "80209")
                                        .setPredicate("streetAddress", "7 S. Broadway")
                            )
                )
                .setPredicate("offers", 
                        new JsonLDNode("Offer")
                            .setPredicate("price", "13.00")
                            .setPredicate("priceCurrency", "USD")
                            .setPredicate("postalCode", "80209")
                            .setPredicate("url", "http://www.ticketfly.com/purchase/309433")
                );
        System.out.println(event.toString(2));
        System.out.println(event.toRDFTriple());
    }
}
