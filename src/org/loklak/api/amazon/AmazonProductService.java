/**
 *  AmazonProductService
 *  Copyright 05.08.2016 by Shiven Mian, @shivenmian
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

package org.loklak.api.amazon;

import java.io.StringWriter;

import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.json.JSONObject;
import org.json.XML;
import org.loklak.data.DAO;
import org.loklak.server.APIException;
import org.loklak.server.APIHandler;
import org.loklak.server.AbstractAPIHandler;
import org.loklak.server.Authorization;
import org.loklak.server.BaseUserRole;
import org.loklak.server.Query;
import org.loklak.tools.storage.JSONObjectWithDefault;
import org.w3c.dom.Document;

public class AmazonProductService extends AbstractAPIHandler implements APIHandler {

	private static final long serialVersionUID = 2279773523424505716L;

	// set your key configuration in config.properties under the Amazon API
	// Settings field
	private static final String AWS_ACCESS_KEY_ID = DAO.getConfig("aws_access_key_id", "randomxyz");
	private static final String AWS_SECRET_KEY = DAO.getConfig("aws_secret_key", "randomxyz");
	private static final String ASSOCIATE_TAG = DAO.getConfig("aws_associate_tag", "randomxyz");

	// using the USA locale
	private static final String ENDPOINT = "webservices.amazon.com";

	@Override
	public String getAPIPath() {
		return "/cms/amazonservice.json";
	}

	@Override
	public BaseUserRole getMinimalBaseUserRole() {
		return BaseUserRole.ANONYMOUS;
	}

	@Override
	public JSONObject getDefaultPermissions(BaseUserRole baseUserRole) {
		return null;
	}

	public static JSONObject fetchResults(String requestUrl, String operation) {
		JSONObject itemlookup = new JSONObject(true);
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(requestUrl);
			DOMSource domSource = new DOMSource(doc);
			StringWriter writer = new StringWriter();
			StreamResult result = new StreamResult(writer);
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.transform(domSource, result);
			JSONObject xmlresult = new JSONObject(true);
			xmlresult = XML.toJSONObject(writer.toString());
			JSONObject items = xmlresult.getJSONObject(operation).getJSONObject("Items");
			if (items.getJSONObject("Request").has("Errors")) {
				itemlookup.put("status", "error");
				itemlookup.put("reason",
						items.getJSONObject("Request").getJSONObject("Errors").getJSONObject("Error").get("Message"));
				return itemlookup;
			}
			itemlookup.put("number_of_items",
					(operation.equals("ItemLookupResponse") ? "1" : (items.getJSONArray("Item").length())));
			itemlookup.put("list_of_items", items);
		} catch (Exception e) {
			itemlookup.put("status", "error");
			itemlookup.put("reason", e);
			return itemlookup;
		}
		return itemlookup;
	}

	@Override
	public JSONObject serviceImpl(Query call, HttpServletResponse response, Authorization rights,
			JSONObjectWithDefault permissions) throws APIException {
		String ITEM_ID = call.get("id", "");
		String PRODUCT_NAME = call.get("q", "");
		String responsegroup = (call.get("response_group", "") != "" ? call.get("response_group", "") : "Large");
		if (!("".equals(ITEM_ID)) && ITEM_ID.length() != 0) {
			return itemLookup(ITEM_ID, responsegroup);
		} else if (!("".equals(PRODUCT_NAME)) && PRODUCT_NAME.length() != 0) {
			return itemSearch(PRODUCT_NAME, responsegroup);
		} else {
			return new JSONObject().put("error", "no parameters given");
		}
	}

	public JSONObject itemSearch(String query, String responsegroup) {
		JSONObject result = new JSONObject(true);
		SignedRequestsHelper helper;
		if (query.length() == 0 || "".equals(query)) {
			result.put("error", "Please specify a query to search");
			return result;
		}
		try {
			helper = SignedRequestsHelper.getInstance(ENDPOINT, AWS_ACCESS_KEY_ID, AWS_SECRET_KEY, ASSOCIATE_TAG);
		} catch (Exception e) {
			result.put("error", e.toString());
			return result;
		}
		String requestUrl = null;
		String queryString = "Service=AWSECommerceService&ResponseGroup=" + responsegroup
				+ "&Operation=ItemSearch&Keywords=" + query + "&SearchIndex=All";
		requestUrl = helper.sign(queryString);
		result = fetchResults(requestUrl, "ItemSearchResponse");
		return result;
	}

	public JSONObject itemLookup(String asin, String responsegroup) {
		SignedRequestsHelper helper;
		JSONObject result = new JSONObject(true);
		if (asin.length() == 0 || "".equals(asin)) {
			result.put("error", "Please specify an Item ID");
			return result;
		}

		try {
			helper = SignedRequestsHelper.getInstance(ENDPOINT, AWS_ACCESS_KEY_ID, AWS_SECRET_KEY, ASSOCIATE_TAG);
		} catch (Exception e) {
			result.put("error", e.toString());
			return result;
		}
		String requestUrl = null;
		String queryString = "Service=AWSECommerceService&ResponseGroup=" + responsegroup
				+ "&Operation=ItemLookup&ItemId=" + asin;
		requestUrl = helper.sign(queryString);
		result = fetchResults(requestUrl, "ItemLookupResponse");
		return result;
	}

}
