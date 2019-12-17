package api.external;

import org.junit.Test;

import api.external.deliveroo.Coordinate;

import static org.junit.Assert.assertEquals;

/**
* Test file for Coordinate.java
*/
public class CoordinateTest {

    String latitudeToTest = "28.7041";
    String longitudeToTest = "77.1025";
    String expectedResult = "Lat: 28.7041, Long: 77.1025";

    //Method to test conversion of lat and long to required string
	@Test
	public void toStringTest() {
		Coordinate coordinate = new Coordinate(latitudeToTest, longitudeToTest);
		assertEquals(expectedResult, coordinate.toString());
	}

    //Method to test getLatitude() function
    @Test
	public void getLatitudeTest() {
		Coordinate coordinate = new Coordinate(latitudeToTest, longitudeToTest);
		assertEquals(latitudeToTest, coordinate.getLatitude());
	}

    //Method to test setLatitude() function
    @Test
	public void setLatitudeTest() {
		Coordinate coordinate = new Coordinate(latitudeToTest, longitudeToTest);
        coordinate.setLatitude("19.0760");
		assertEquals("19.0760", coordinate.getLatitude());
	}

    //Method to test getLongitude() function
    @Test
	public void getLongitudeTest() {
		Coordinate coordinate = new Coordinate(latitudeToTest, longitudeToTest);
		assertEquals(longitudeToTest, coordinate.getLongitude());
	}

    //Method to test setLongitude() function
    @Test
	public void setLongitudeTest() {
		Coordinate coordinate = new Coordinate(latitudeToTest, longitudeToTest);
        coordinate.setLongitude("72.8777");
		assertEquals("72.8777", coordinate.getLongitude());
	}
}
