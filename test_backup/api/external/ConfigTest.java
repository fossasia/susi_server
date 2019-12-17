package api.external;

import org.junit.Test;

import api.external.deliveroo.Config;

import static org.junit.Assert.assertEquals;

/**
* Test file for Config
*/
public class ConfigTest {

	//method to test getURIBuilder()
	@Test
	public void getURIBuilderTest() {
		Config config = new Config();
		String pathToTest = "/skill/add";
		String expectedResult = "https://deliveroo.co.uk/orderapp/v1/skill/add";
		assertEquals(expectedResult, config.getURIBuilder(pathToTest).toString());
	}

	//method to test HOST value
	@Test
	public void hostTest() {
		Config config = new Config();
		String HOST = "deliveroo.co.uk";
		assertEquals(HOST, config.HOST);
	}

	//method to test API_ENDPOINT value
	@Test
	public void apiEndpointTest() {
		Config config = new Config();
		String API_ENDPOINT = "/orderapp/v1";
		assertEquals(API_ENDPOINT, config.API_ENDPOINT);
	}
}
