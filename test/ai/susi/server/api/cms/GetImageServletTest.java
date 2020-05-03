package ai.susi.server.api.cms;

import ai.susi.DAO;
import ai.susi.tools.IO;
import ai.susi.utils.StubServletOutputStream;
import ai.susi.utils.TestHelpers;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class GetImageServletTest {

    @BeforeClass
    public static void setUp() throws Exception {
        TestHelpers.initDao();
        Stream.of("image_uploads", "avatar_uploads", "slider_uploads")
            .map(folder -> new File(DAO.data_dir, folder))
            .filter(file -> !file.exists())
            .forEach(File::mkdirs);
    }

    private StubServletOutputStream testImageAccess(Map<String, String> params) throws IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(request.getServletPath()).thenReturn("test");
        when(request.getRemoteHost()).thenReturn("test");
        params.forEach((key, value) -> when(request.getParameter(key)).thenReturn(value));

        StubServletOutputStream stream = new StubServletOutputStream();
        when(response.getOutputStream()).thenReturn(stream);

        new GetImageServlet().doGet(request, response);

        return stream;
    }

    private void assertNotContains(String content, String data) {
        assertFalse("Servlet should not access data external to the base directory. Data: " + content, content.contains(data));
    }

    private Map<String, String> getParams(String... args) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < args.length - 1; i+=2) {
            map.put(args[i], args[i+1]);
        }
        return map;
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAccessExtraDataForImageParam() throws IOException {
        StubServletOutputStream stream = testImageAccess(getParams("image", "../settings/private.settings.json"));
        assertNotContains(stream.getContent(), "private_key");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAccessExtraDataForSliderImageParam() throws IOException {
        StubServletOutputStream stream = testImageAccess(getParams("sliderImage", "../settings/private.settings.json"));
        assertNotContains(stream.getContent(), "private_key");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAccessExtraDataForAvatarImageParam() throws IOException {
        StubServletOutputStream stream = testImageAccess(getParams(
                "image", "../../settings/private.settings.json",
                "avatar", "true"
        ));
        assertNotContains(stream.getContent(), "private_key");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAccessExtraDataForModelImage() throws IOException {
        testImageAccess(getParams(
                "image", "../../settings/private.settings.json",
                "model", "..",
                "language", "..",
                "group", ".."
        ));
    }

    @Test
    public void shouldAccessDefaultImageForAbsentModelImage() throws IOException {
        StubServletOutputStream stream = testImageAccess(getParams(
                "image", "images/test.jpg",
                "model", "general",
                "language", "en",
                "group", "Knowledge"
        ));
        assertEquals(stream.getContent(), IO.readFile(GetImageServlet.getDefaultImage()).toString());
    }

    @Test
    public void shouldAccessDefaultImageForAbsentAvatar() throws IOException {
        StubServletOutputStream stream = testImageAccess(getParams(
                "image", "avatar/haha.jpg",
                "avatar", "true"
        ));
        assertEquals(stream.getContent(), IO.readFile(GetImageServlet.getDefaultImage()).toString());
    }

    @Test
    public void shouldAccessDefaultImage() throws IOException {
        StubServletOutputStream stream = testImageAccess(getParams());
        assertEquals(stream.getContent(), IO.readFile(GetImageServlet.getDefaultImage()).toString());
    }
}
