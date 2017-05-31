package api.external.deliveroo;

import java.math.BigDecimal;

import org.json.JSONObject;

public class MenuItem {
	private int id;
	private String name;
	private String description;
	private Boolean omitFromReceipts;
	private BigDecimal price;
	// Maybe for AB testing?
	private BigDecimal altModPrice;
	private int sortOrder;
	private Boolean available;
	private Boolean popular;
	private int categoryId;
	
	public MenuItem(JSONObject jsonObject) {
		this.id = jsonObject.getInt("id");
		this.name = jsonObject.getString("name");
		this.description = jsonObject.optString("description", "");
		this.omitFromReceipts = jsonObject.getBoolean("omit_from_receipts");
		this.price = jsonObject.getBigDecimal("price");
		this.altModPrice = jsonObject.optBigDecimal("alt_mod_price", null);
		this.sortOrder = jsonObject.getInt("sort_order");
		this.available = jsonObject.getBoolean("available");
		this.popular = jsonObject.getBoolean("popular");
		this.categoryId = jsonObject.getInt("category_id");
	}

	/**
	 * @Override
	 */
	public String toString() {
		return name + " (" + price + "): " + description;
	}
	
	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public Boolean getOmitFromReceipts() {
		return omitFromReceipts;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public BigDecimal getAltModPrice() {
		return altModPrice;
	}

	public int getSortOrder() {
		return sortOrder;
	}

	public Boolean getAvailable() {
		return available;
	}

	public Boolean getPopular() {
		return popular;
	}

	public int getCategoryId() {
		return categoryId;
	}
}
