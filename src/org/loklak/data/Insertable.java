package org.loklak.data;

/**
 * Created by hadoop on 16-8-3.
 */



import java.util.*;

/**
 * This is the interface that allow you to store data in elasticseach index
 * This is a lower level implemention of DAOWrapper. DAOWrapper may be a better solution if you don't care how we store data in elasticsearch index
 *
 * However, this one is apparently more flexible
 */
public interface Insertable {
    String getText();
    String getUsername();
    String getUserID();
    String getDomain();
    Collection<Map.Entry<String, String>> getExtraField();
}
