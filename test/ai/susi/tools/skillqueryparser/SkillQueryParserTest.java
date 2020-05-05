package ai.susi.tools.skillqueryparser;

import ai.susi.server.Query;
import ai.susi.utils.TestHelpers;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SkillQueryParserTest {

    @BeforeClass
    public static void setUp() throws Exception {
        TestHelpers.initDao();
    }

    private void parse(SkillQueryParser parser, HttpServletRequest request, Consumer<SkillQuery> consumer) {
        consumer.accept(parser.parse(request));
        consumer.accept(parser.parse(new Query(request)));
    }

    private Consumer<SkillQuery> defaultRequestConsumer =  skillQuery -> {
        assertEquals(SkillQueryParser.DEFAULT_MODEL, skillQuery.getModel());
        assertEquals(SkillQueryParser.DEFAULT_GROUP, skillQuery.getGroup());
        assertEquals(SkillQueryParser.DEFAULT_LANGUAGE, skillQuery.getLanguage());
        assertNull(skillQuery.getSkill());
    };

    @Test
    public void testDefaultParserOnEmptyRequest() {
        HttpServletRequest request = TestHelpers.getMockRequestWithParams();

        parse(SkillQueryParser.getInstance(), request, defaultRequestConsumer);
        parse(SkillQuery.getParser(), request, defaultRequestConsumer);
    }

    @Test
    public void testDefaultParserWithSkillOnEmptyRequest() {
        HttpServletRequest request = TestHelpers.getMockRequestWithParams();
        String skill = "wikipedia";
        Consumer<SkillQuery> consumer = skillQuery -> {
            assertEquals(SkillQueryParser.DEFAULT_MODEL, skillQuery.getModel());
            assertEquals(SkillQueryParser.DEFAULT_GROUP, skillQuery.getGroup());
            assertEquals(SkillQueryParser.DEFAULT_LANGUAGE, skillQuery.getLanguage());
            assertEquals(skill, skillQuery.getSkill());
        };
        parse(SkillQueryParser.getInstance(skill), request, consumer);
        parse(SkillQuery.getParser(skill), request, consumer);
    }

    private Consumer<SkillQuery> customRequestConsumer = skillQuery -> {
        assertEquals("custom", skillQuery.getModel());
        assertEquals("Jokes", skillQuery.getGroup());
        assertEquals("de", skillQuery.getLanguage());
        assertEquals("AI Relations", skillQuery.getSkill());
    };

    @Test
    public void testDefaultParserOnSpecificRequest() {
        HttpServletRequest request = TestHelpers.getMockRequestWithParams(
                "model", "custom",
                "group", "Jokes",
                "language", "de",
                "skill", "AI Relations"
        );


        parse(SkillQueryParser.getInstance(), request, customRequestConsumer);
        String skill = "Shawn Mendes";
        parse(SkillQueryParser.getInstance(skill), request, customRequestConsumer);
        parse(SkillQuery.getParser(skill), request, customRequestConsumer);
    }

    @Test
    public void testDefaultParserWithSkillOnSpecificRequest() {
        HttpServletRequest request = TestHelpers.getMockRequestWithParams(
                "model", "custom",
                "group", "Jokes",
                "language", "de"
        );

        String skill = "Shawn Mendes";
        Consumer<SkillQuery> consumer = skillQuery -> {
            assertEquals("custom", skillQuery.getModel());
            assertEquals("Jokes", skillQuery.getGroup());
            assertEquals("de", skillQuery.getLanguage());
            assertEquals(skill, skillQuery.getSkill());
        };
        parse(SkillQueryParser.getInstance(skill), request, consumer);
        parse(SkillQuery.getParser(skill), request, consumer);
    }

    @Test
    public void testParserOnSpecificRequest() {
        HttpServletRequest request = TestHelpers.getMockRequestWithParams(
                "OldModel", "custom",
                "OldGroup", "Jokes",
                "OldLanguage", "de",
                "OldSkill", "AI Relations"
        );

        SkillQueryParser.Builder builder = SkillQueryParser.Builder.getInstance()
                .modelKey("OldModel")
                .groupKey("OldGroup")
                .languageKey("OldLanguage")
                .skillKey("OldSkill");
        parse(builder.build(), request, customRequestConsumer);
        parse(builder.skill("Shawn Mendes").build(), request, customRequestConsumer);
    }

    @Test
    public void testModelPath() {
        HttpServletRequest request = TestHelpers.getMockRequestWithParams();

        Path path = Paths.get("test");

        parse(SkillQueryParser.Builder.getInstance().modelPath(path).build(), request, skillQuery -> {
            defaultRequestConsumer.accept(skillQuery);
            assertEquals(path, skillQuery.getModelDirectory());
        });
    }

    @Test
    public void testCustomBuilder() {
        HttpServletRequest request = TestHelpers.getMockRequestWithParams();

        SkillQueryParser parser = new SkillQueryParser.Builder()
                .model("custom")
                .language("hi")
                .build();

        parse(parser, request, skillQuery -> {
            assertEquals("custom", skillQuery.getModel());
            assertEquals("hi", skillQuery.getLanguage());
            assertNull(skillQuery.getGroup());
            assertNull(skillQuery.getSkill());
        });
    }

}
