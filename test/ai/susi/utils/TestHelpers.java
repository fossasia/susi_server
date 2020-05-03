package ai.susi.utils;

import ai.susi.DAO;
import ai.susi.SusiServer;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Map;

public class TestHelpers {

    public static void initDao() throws Exception {
        Path data = FileSystems.getDefault().getPath("data");
        Map<String, String> config = SusiServer.readConfig(data);
        DAO.init(config, data, false);
    }

}
