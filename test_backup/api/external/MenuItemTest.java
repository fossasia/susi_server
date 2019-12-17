package api.external;

import org.junit.Test;
import java.math.BigDecimal;
import org.json.JSONObject;

import api.external.deliveroo.MenuItem;

import static org.junit.Assert.assertEquals;

/**
* Test file for MenuItem
*/
public class MenuItemTest {

    @Test
    public void methodToTestMenuItemMethods() {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", new Integer(111));
        jsonObject.put("name", "Test Name");
        jsonObject.put("description", "Test Description");
        jsonObject.put("omit_from_receipts", new Boolean(true));
        jsonObject.put("price", new BigDecimal("232342342343432958"));
        jsonObject.put("alt_mod_price", new BigDecimal("1423904834834434"));
        jsonObject.put("sort_order", new Integer(1));
        jsonObject.put("available", new Boolean(true));
        jsonObject.put("popular", new Boolean(false));
        jsonObject.put("category_id", new Integer(25));

        MenuItem menuItem = new MenuItem(jsonObject);
        String result = jsonObject.getString("name") + " (" +
                            jsonObject.getBigDecimal("price") + "): " +
                            jsonObject.optString("description", "");

        assertEquals(result, menuItem.toString());
        assertEquals(jsonObject.getInt("id"), menuItem.getId());
        assertEquals(jsonObject.getString("name"), menuItem.getName());
        assertEquals(jsonObject.optString("description", ""), menuItem.getDescription());
        assertEquals(jsonObject.getBoolean("omit_from_receipts"), menuItem.getOmitFromReceipts());
        assertEquals(jsonObject.getBigDecimal("price"), menuItem.getPrice());
        assertEquals(jsonObject.optBigDecimal("alt_mod_price", null), menuItem.getAltModPrice());
        assertEquals(jsonObject.getInt("sort_order"), menuItem.getSortOrder());
        assertEquals(jsonObject.getBoolean("available"), menuItem.getAvailable());
        assertEquals(jsonObject.getBoolean("popular"), menuItem.getPopular());
        assertEquals(jsonObject.getInt("category_id"), menuItem.getCategoryId());
    }
}
