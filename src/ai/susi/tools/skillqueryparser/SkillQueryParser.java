package ai.susi.tools.skillqueryparser;

import ai.susi.server.Query;

import javax.servlet.http.HttpServletRequest;
import java.nio.file.Path;

public class SkillQueryParser {
    public static final String MODEL_KEY = "model";
    public static final String GROUP_KEY = "group";
    public static final String LANGUAGE_KEY = "language";
    public static final String SKILL_KEY = "skill";

    public static final String DEFAULT_MODEL = "general";
    public static final String DEFAULT_GROUP = "Knowledge";
    public static final String DEFAULT_LANGUAGE = "en";

    private final String modelKey;
    private final String groupKey;
    private final String languageKey;
    private final String skillKey;

    private final String defaultModel;
    private final String defaultGroup;
    private final String defaultLanguage;
    private final String defaultSkill;
    private final Path modelPath;

    private SkillQueryParser(
            String modelKey,
            String groupKey,
            String languageKey,
            String skillKey,
            String defaultModel,
            String defaultGroup,
            String defaultLanguage,
            String defaultSkill,
            Path modelPath) {
        this.modelKey = modelKey;
        this.groupKey = groupKey;
        this.languageKey = languageKey;
        this.skillKey = skillKey;
        this.defaultModel = defaultModel;
        this.defaultGroup = defaultGroup;
        this.defaultLanguage = defaultLanguage;
        this.defaultSkill = defaultSkill;
        this.modelPath = modelPath;
    }

    public static SkillQueryParser getInstance() {
        return Builder.getInstance().build();
    }

    public static SkillQueryParser getInstance(String skill) {
        return Builder.getInstance().skill(skill).build();
    }

    public static class Builder {
        private String modelKey = MODEL_KEY;
        private String groupKey = GROUP_KEY;
        private String languageKey = LANGUAGE_KEY;
        private String skillKey = SKILL_KEY;

        private String defaultModel;
        private String defaultGroup;
        private String defaultLanguage;
        private String defaultSkill;
        private Path modelPath;

        public Builder modelKey(String key) {
            modelKey = key;
            return this;
        }

        public Builder groupKey(String key) {
            groupKey = key;
            return this;
        }

        public Builder languageKey(String key) {
            languageKey = key;
            return this;
        }

        public Builder skillKey(String key) {
            skillKey = key;
            return this;
        }

        public Builder model(String model) {
            defaultModel = model;
            return this;
        }

        public Builder group(String group) {
            defaultGroup = group;
            return this;
        }

        public Builder language(String language) {
            defaultLanguage = language;
            return this;
        }

        public Builder skill(String skill) {
            defaultSkill = skill;
            return this;
        }

        public Builder modelPath(Path path) {
            modelPath = path;
            return this;
        }

        public SkillQueryParser build() {
            return new SkillQueryParser(modelKey, groupKey, languageKey, skillKey,
                    defaultModel, defaultGroup, defaultLanguage, defaultSkill, modelPath);
        }

        public static Builder getInstance() {
            return new Builder()
                    .model(DEFAULT_MODEL)
                    .group(DEFAULT_GROUP)
                    .language(DEFAULT_LANGUAGE);
        }
    }

    public SkillQuery parse(Query call) {
        String model = call.get(modelKey, defaultModel);
        String group = call.get(groupKey, defaultGroup);
        String language = call.get(languageKey, defaultLanguage);
        String skill = call.get(skillKey, defaultSkill);

        return new SkillQuery(model, group, language, skill, modelPath);
    }

    private String getOrDefault(HttpServletRequest req, String key, String defaultValue) {
        String value = req.getParameter(key);
        if (value != null) {
            return value;
        } else {
            return defaultValue;
        }
    }

    public SkillQuery parse(HttpServletRequest req) {
        String model = getOrDefault(req, modelKey, defaultModel);
        String group = getOrDefault(req, groupKey, defaultGroup);
        String language = getOrDefault(req, languageKey, defaultLanguage);
        String skill = getOrDefault(req, skillKey, defaultSkill);

        return new SkillQuery(model, group, language, skill, modelPath);
    }

}
