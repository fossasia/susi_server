package org.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class JSONExceptionTest {
    public final String message1 = "The specified index does not exist in array. Please correct the error.";
    public final Throwable cause1 = new Throwable("ArrayIndexOutOfBoundsException");
    public final Throwable cause2 = new Throwable("IllegalArgumentException");

    JSONException Object;

    @Test
    public void JSONExceptionTestMethod1(){
        Object = new JSONException(message1);
        assertEquals("The specified index does not exist in array. Please correct the error.", Object.getMessage());
        assertNull(Object.getCause());
    }

    @Test
    public void JSONExceptionTestMethod2(){
        Object = new JSONException(message1, cause1);
        assertEquals("The specified index does not exist in array. Please correct the error.", Object.getMessage());
        assertEquals("java.lang.Throwable: ArrayIndexOutOfBoundsException",Object.getCause().toString());
    }

    @Test
    public void JSONExceptionTestMethod3(){
        Object = new JSONException(cause2);
        assertEquals("IllegalArgumentException", Object.getMessage());
        assertEquals("java.lang.Throwable: IllegalArgumentException",Object.getCause().toString());
    }
}
