package ai.susi.geo;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class NominatimServiceTest{
    
    public NominatimService obj = new NominatimService();

    @Test
    public void integerGeoPointsTest(){
         String location = "Chennai";
         ArrayList<IntegerGeoPoint> arr = new ArrayList<IntegerGeoPoint>();
         try{
             arr = obj.integerGeoPoints(location, 10);
             assertEquals("[13.080172051247274,80.28383308103486]", arr.get(0).toString());
         }catch(Exception e){
             assertFalse(true);
         }
    }

    @Test
    public void LocationNotFoundExceptionTest(){
        NominatimService.LocationNotFoundException obj = new NominatimService.LocationNotFoundException("Singpr");
        assertEquals("No location named 'Singpr' was found", obj.getMessage());
        assertNull(obj.getCause());
    }
}