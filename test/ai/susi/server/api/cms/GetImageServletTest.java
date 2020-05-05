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
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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

    private StubServletOutputStream testImageAccess(String... params) throws IOException {
        HttpServletRequest request = TestHelpers.getMockRequestWithParams(params);
        HttpServletResponse response = mock(HttpServletResponse.class);

        StubServletOutputStream stream = new StubServletOutputStream();
        when(response.getOutputStream()).thenReturn(stream);

        new GetImageServlet().doGet(request, response);

        return stream;
    }

    private void assertNotContains(String content, String data) {
        assertFalse("Servlet should not access data external to the base directory. Data: " + content, content.contains(data));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAccessExtraDataForImageParam() throws IOException {
        StubServletOutputStream stream = testImageAccess("image", "../settings/private.settings.json");
        assertNotContains(stream.getContent(), "private_key");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAccessExtraDataForSliderImageParam() throws IOException {
        StubServletOutputStream stream = testImageAccess("sliderImage", "../settings/private.settings.json");
        assertNotContains(stream.getContent(), "private_key");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAccessExtraDataForAvatarImageParam() throws IOException {
        StubServletOutputStream stream = testImageAccess(
                "image", "../../settings/private.settings.json",
                "avatar", "true"
        );
        assertNotContains(stream.getContent(), "private_key");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAccessExtraDataForModelImage() throws IOException {
        testImageAccess(
                "image", "../../settings/private.settings.json",
                "model", "..",
                "language", "..",
                "group", ".."
        );
    }

    @Test
    public void shouldAccessDefaultImageForAbsentModelImage() throws IOException {
        StubServletOutputStream stream = testImageAccess(
                "image", "images/test.jpg",
                "model", "general",
                "language", "en",
                "group", "Knowledge"
        );
        assertEquals(stream.getContent(), IO.readFile(GetImageServlet.getDefaultImage()).toString());
    }

    @Test
    public void shouldAccessDefaultImageForAbsentAvatar() throws IOException {
        StubServletOutputStream stream = testImageAccess(
                "image", "avatar/haha.jpg",
                "avatar", "true"
        );
        assertEquals(stream.getContent(), IO.readFile(GetImageServlet.getDefaultImage()).toString());
    }

    @Test
    public void shouldAccessDefaultImage() throws IOException {
        StubServletOutputStream stream = testImageAccess();
        assertEquals(stream.getContent(), IO.readFile(GetImageServlet.getDefaultImage()).toString());
    }
}
