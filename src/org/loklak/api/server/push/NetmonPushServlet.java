/**
 * FreifunkNodePushServlet
 * Copyright 16.07.2015 by Dang Hai An, @zyzo
 * <p/>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <p/>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program in the file lgpl21.txt
 * If not, see <http://www.gnu.org/licenses/>.
 */
package org.loklak.api.server.push;

import org.loklak.api.client.ClientConnection;
import org.loklak.api.server.RemoteAccess;
import org.loklak.data.DAO;
import org.loklak.geo.LocationSource;
import org.loklak.harvester.JsonFieldConverter;
import org.loklak.harvester.SourceType;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NetmonPushServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        RemoteAccess.Post post = RemoteAccess.evaluate(request);
        String remoteHash = Integer.toHexString(Math.abs(post.getClientHost().hashCode()));

        // manage DoS
        if (post.isDoS_blackout()) {
            response.sendError(503, "your request frequency is too high");
            return;
        }

        String url = post.get("url", "");
        if (url == null || url.length() == 0) {
            response.sendError(400, "your request does not contain an url to your data object");
            return;
        }

        List<Map<String, Object>> nodesList = new ArrayList<>();
        byte[] xmlText;
        try {
            xmlText = ClientConnection.download(url);
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(new String(xmlText))));
            NodeList routerList = document.getElementsByTagName("router");
            for (int i = 0; i < routerList.getLength(); i++) {
                Map<String, Object> node = convertDOMNodeToMap(routerList.item(i));
                if (node != null)
                    nodesList.add(node);
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(400, "error reading json file from url");
            return;
        }

        JsonFieldConverter converter = new JsonFieldConverter();
        List<Map<String, Object>> nodes = converter.convert(nodesList, JsonFieldConverter.JsonConversionSchemaEnum.NETMON_NODE);

        for (Map<String, Object> node : nodes) {
            if (node.get("text") == null) {
                node.put("text", "");
            }
            node.put("source_type", SourceType.NETMON.name());
            if (node.get("user") == null) {
                node.put("user", new HashMap<String, Object>());
            }

            List<Object> location_point = new ArrayList<>();
            location_point.add(node.get("longitude"));
            location_point.add(node.get("latitude"));
            node.put("location_point", location_point);
            node.put("location_mark", location_point);
            node.put("location_source", LocationSource.USER.name());
            try {
                node.put("id_str", PushServletHelper.computeMessageId(node, node.get("router_id"), SourceType.NETMON));
            } catch (Exception e) {
                DAO.log("Problem computing id" + e.getMessage());
                continue;
            }
            try {
                Map<String, Object> user = (Map<String, Object>) node.get("user");
                user.put("screen_name", computeUserId(user.get("update_date"), user.get("id"), SourceType.NETMON));
            } catch (Exception e) {
                DAO.log("Problem computing user id : " + e.getMessage());
            }
        }

        PushReport pushReport = PushServletHelper.saveMessages(nodes);

        String res = PushServletHelper.printResponse(post.get("callback", ""), pushReport);
        response.getOutputStream().println(res);
        DAO.log(request.getServletPath()
                + " -> records = " + pushReport.getRecordCount()
                + ", new = " + pushReport.getNewCount() + ", known = " + pushReport.getKnownCount() + ", from host hash " + remoteHash);

    }

    private static Map<String, Object> convertDOMNodeToMap(Node node) {
        Node directChild = node.getFirstChild();
        if (directChild == null) {
            return null;
        }
        Map<String, Object> result = new HashMap<>();
        while (directChild != null) {
            if (directChild.getChildNodes().getLength() == 1
            && directChild.getChildNodes().item(0).getNodeType() == Node.TEXT_NODE) {
                result.put(directChild.getNodeName(), directChild.getChildNodes().item(0).getTextContent());
            } else {
                result.put(directChild.getNodeName(), convertDOMNodeToMap(directChild));
            }
            directChild = directChild.getNextSibling();
        }
        return result;
    }

    private static String computeUserId(Object mtime, Object initialId, SourceType sourceType) throws Exception {
        if (mtime == null) {
            throw new Exception("mtime field is missing");
        }
        boolean hasId = initialId != null && !initialId.equals("");
        return sourceType.name() + "_" + (hasId ? initialId + "_" : "") + mtime;
    }
}
