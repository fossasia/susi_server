package ai.susi.utils;

import ai.susi.DAO;
import ai.susi.SusiServer;

import javax.servlet.http.HttpServletRequest;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestHelpers {

    public static void initDao() throws Exception {
        Path data = FileSystems.getDefault().getPath("data");
        Map<String, String> config = SusiServer.readConfig(data);
        DAO.init(config, data, false);
    }


    public static HttpServletRequest getMockRequestWithParams(Map<String, String> params) {
        HttpServletRequest request = mock(HttpServletRequest.class);

        // Required for last visit query in RemoteAccess.java
        when(request.getServletPath()).thenReturn("test");
        when(request.getRemoteHost()).thenReturn("test");

        params.forEach((key, value) -> when(request.getParameter(key)).thenReturn(value));

        return request;
    }

    public static HttpServletRequest getMockRequestWithParams(String... args) {
        return getMockRequestWithParams(getParams(args));
    }

    private static Map<String, String> getParams(String... args) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < args.length - 1; i+=2) {
            map.put(args[i], args[i+1]);
        }
        return map;
    }

}
