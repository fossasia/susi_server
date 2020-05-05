package ai.susi;

import ai.susi.tools.IO;
import ai.susi.tools.ThrowingConsumer;
import ai.susi.utils.TestHelpers;
import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class DAOFileAccessTest {

    @BeforeClass
    public static void setUp() throws Exception {
        TestHelpers.initDao();
        DAO.model_watch_dir.mkdirs();
    }

    public static void testInModelPath(ThrowingConsumer<Path, Exception> consumer) throws IOException {
        Path modelPath = Files.createTempDirectory(DAO.model_watch_dir.toPath(), "general");
        try {
            consumer.accept(modelPath);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            FileUtils.deleteDirectory(modelPath.toFile());
        }
    }

    @Test
    public void shouldFindSkillFileInModel() throws IOException {
        testInModelPath(modelPath -> {
            // Create some groups, languages and skills
            Stream.of("Communication", "AI", "Knowledge", "Jokes", "Music")
                    .map(modelPath::resolve)
                    .flatMap(path -> Stream.of("en", "de", "hi").map(path::resolve))
                    .peek(path -> path.toFile().mkdirs())
                    .flatMap(path -> Stream.of("test", "fun", "cool", "nice")
                            .map(skill -> skill + ".txt")
                            .map(path::resolve))
                    .forEach(path -> {
                        try {
                            path.toFile().createNewFile();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });

            String skill = "LudoKing";
            assertNull(DAO.getSkillFileInModel(modelPath.toFile(), skill));

            Path skillPath = modelPath.resolve(Paths.get("Knowledge", "en", "LuDoKiNg.txt"));
            skillPath.toFile().createNewFile();

            assertEquals(skillPath, DAO.getSkillFileInModel(modelPath.toFile(), skill).toPath());
        });
    }

    private Path getLanguagePath(Path modelPath) {
        return modelPath.resolve(Paths.get("Knowledge", "en"));
    }

    @Test
    public void shouldReturnNullForSkillFileInLanguage() throws IOException {
        testInModelPath(modelPath -> {
            Path languagePath = getLanguagePath(modelPath);
            assertNull(DAO.getSkillFileInLanguage(languagePath.toFile(), "Absent Skill", true));
        });
    }

    @Test
    public void shouldReturnNonExistentFileForSkillFileInLanguage() throws IOException {
        testInModelPath(modelPath -> {
            Path languagePath = getLanguagePath(modelPath);
            File file = DAO.getSkillFileInLanguage(languagePath.toFile(), "Absent Skill", false);
            assertNotNull(file);
            assertTrue("Skill name not present in returned file path: " + file.getPath(),
                    file.getPath().toLowerCase().contains("absent skill"));
        });
    }

    private static Path writeSkillFile(Path languagePath, String skillName, String exension) throws IOException {
        // Adding extra characters so that file is not found just by name matching
        // and file parser finder is put to test
        Path skillPath = languagePath.resolve(skillName + "extra." + exension);
        skillPath.toFile().createNewFile();
        String content = "::name " + skillName +  "\n";
        Files.write(skillPath, content.getBytes());

        return skillPath;
    }

    public static Path writeSkillFile(Path languagePath, String skillName) throws IOException {
        return writeSkillFile(languagePath, skillName, "txt");
    }

    @Test
    public void shouldReturnParsedFileForSkillFileInLanguage() throws IOException {
        testInModelPath(modelPath -> {
            Path languagePath = getLanguagePath(modelPath);
            languagePath.toFile().mkdirs();
            writeSkillFile(languagePath, "Rocket Racoon", "ezd");
            Path skillPath = writeSkillFile(languagePath, "Sam Smith");
            Path zephyrSkillPath = writeSkillFile(languagePath, "Zephyr");
            assertEquals(skillPath, DAO.getSkillFileInLanguage(languagePath.toFile(), "sam_smith", true).toPath());
            assertEquals(zephyrSkillPath, DAO.getSkillFileInLanguage(languagePath.toFile(), "zEpHyR", true).toPath());
        });
    }

    @Test(expected = IO.IllegalPathAccessException.class)
    public void shouldThrowExceptionOnDirectoryTraversal() {
        DAO.susi.getSkillMetadata("..", "..", "..", "private.settings.json");
    }

}
