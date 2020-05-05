package ai.susi.tools.skillqueryparser;

import ai.susi.DAO;
import ai.susi.DAOFileAccessTest;
import ai.susi.server.APIException;
import ai.susi.tools.ThrowingConsumer;
import ai.susi.utils.TestHelpers;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class SkillQueryTest {

    @BeforeClass
    public static void setUp() throws Exception {
        TestHelpers.initDao();
        DAO.model_watch_dir.mkdirs();
    }

    private SkillQuery getSkillQuery(Path modelDirectory) {
        return new SkillQuery("general", "Knowledge", "en", null, modelDirectory, null);
    }

    private SkillQuery getSkillQuery() {
        return getSkillQuery(null);
    }

    @Test
    public void testDefaultModelPath() {
        SkillQuery skillQuery = getSkillQuery();

        assertFalse(skillQuery.isPrivate());
        assertEquals(DAO.model_watch_dir.toPath(), skillQuery.getModelDirectory());
        assertEquals(DAO.model_watch_dir.toPath().resolve(Paths.get("general")), skillQuery.getModelPath());
        assertEquals(DAO.model_watch_dir.toPath().resolve(Paths.get("general", "Knowledge")), skillQuery.getGroupPath());
        assertEquals(DAO.model_watch_dir.toPath().resolve(Paths.get("general", "Knowledge", "en")), skillQuery.getLanguagePath());
    }

    @Test
    public void testImmutableSetters() {
        SkillQuery original = getSkillQuery();
        SkillQuery skillQuery = original
                .language("hi")
                .skill("Coffee");

        assertNotSame(original, skillQuery);
        assertEquals("hi", skillQuery.getLanguage());
        assertEquals("Coffee", skillQuery.getSkill());
    }

    @Test
    public void testPrivateModel() {
        SkillQuery skillQuery = getSkillQuery().forPrivate("gomez");

        assertTrue(skillQuery.isPrivate());
        assertEquals(DAO.private_skill_watch_dir.toPath(), skillQuery.getModelDirectory());
        assertEquals(DAO.private_skill_watch_dir.toPath().resolve(Paths.get("gomez")), skillQuery.getModelPath());
        assertEquals(DAO.private_skill_watch_dir.toPath().resolve(Paths.get("gomez", "Knowledge")), skillQuery.getGroupPath());
        assertEquals(DAO.private_skill_watch_dir.toPath().resolve(Paths.get("gomez", "Knowledge", "en")), skillQuery.getLanguagePath());
    }

    @Test(expected = NullPointerException.class)
    public void testNonNullChecksForLanguagePath() {
        getSkillQuery().language(null).getLanguagePath();
    }

    @Test(expected = NullPointerException.class)
    public void testNonNullChecksForSkill() {
        getSkillQuery().getSkillFile();
    }

    @Test(expected = NullPointerException.class)
    public void testNonNullChecksForSkillRequire() throws APIException {
        getSkillQuery().requireOrThrow();
    }

    private void captureRethrow(ThrowingConsumer<Path, APIException> consumer) throws Exception {
        final Exception[] captured = {null};
        DAOFileAccessTest.testInModelPath(path -> {
            try {
                consumer.accept(path);
            } catch (APIException e) {
                captured[0] = e;
            }
        });

        if (captured[0] != null)
            throw captured[0];
    }

    @Test(expected = APIException.class)
    public void testThrowOnAbsentFile() throws Exception {
        captureRethrow(path -> getSkillQuery(path).skill("absent").requireOrThrow());
    }

    private SkillQuery testReturnNamedFile(Path path) {
        String skill = "Wikipedia";
        SkillQuery skillQuery = getSkillQuery(path).skill(skill);

        assertEquals(skillQuery.getLanguagePath().resolve(skill + ".txt"), skillQuery.getSkillFile().toPath());

        return skillQuery;
    }

    @Test
    public void testReturnsAbsentNamedFile() throws IOException {
        DAOFileAccessTest.testInModelPath(this::testReturnNamedFile);
    }

    @Test(expected = APIException.class)
    public void testThrowsOnReturnsAbsentNamedFile() throws Exception {
        captureRethrow(path -> testReturnNamedFile(path).requireOrThrow());
    }

    @Test
    public void testReturnsExistingNamedFile() throws IOException {
        DAOFileAccessTest.testInModelPath(path -> {
            SkillQuery skillQuery = testReturnNamedFile(path);

            skillQuery.getLanguagePath().toFile().mkdirs();
            File skillFile = skillQuery.getLanguagePath().resolve("WiKiPeDiA.txt").toFile();
            skillFile.createNewFile();

            assertEquals(skillFile, skillQuery.requireSkillFileOrThrow());
        });
    }

    @Test
    public void testReturnsParsedFile() throws IOException {
        DAOFileAccessTest.testInModelPath(path -> {
            SkillQuery skillQuery = testReturnNamedFile(path).skill("tepid_lemur");

            skillQuery.getLanguagePath().toFile().mkdirs();
            DAOFileAccessTest.writeSkillFile(skillQuery.getLanguagePath(), "ManJaro Unchained");
            DAOFileAccessTest.writeSkillFile(skillQuery.getLanguagePath(), "Cosplay Disco");
            Path skillPath = DAOFileAccessTest.writeSkillFile(skillQuery.getLanguagePath(), "Tepid Lemur");

            assertEquals(skillPath, skillQuery.requireSkillFileOrThrow().toPath());
        });
    }

}
