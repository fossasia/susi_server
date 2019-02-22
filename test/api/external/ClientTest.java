package api.external;

import org.junit.Test;

import api.external.deliveroo.Client;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.apache.http.impl.client.CloseableHttpClient;

/**
 *  Test for Client.java 
**/

public class ClientTest {

  @Test
  public void testGetHtttpClient() {
    Client obj = new Client();
    CloseableHttpClient client = obj.getHttpClient();
    String testString = "org.apache.http.impl.client.InternalHttpClient";
    assertNotNull(client);
    assertEquals("class", String.valueOf(client.getClass().toString().substring(0, 5)));
    assertEquals(testString, client.getClass().toString().substring(6));
    assertTrue(client.toString().contains(testString));
  }
}
