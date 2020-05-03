package ai.susi.tools.skillqueryparser;

import ai.susi.server.Query;

import java.nio.file.Path;

public class SkillQueryParser {
    public static final String DEFAULT_MODEL = "general";
    public static final String DEFAULT_GROUP = "Knowledge";
    public static final String DEFAULT_LANGUAGE = "en";

    private final String defaultModel;
    private final String defaultGroup;
    private final String defaultLanguage;
    private final String defaultSkill;
    private final Path modelPath;

    private SkillQueryParser(String defaultModel, String defaultGroup, String defaultLanguage, String defaultSkill, Path modelPath) {
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
        private String defaultModel;
        private String defaultGroup;
        private String defaultLanguage;
        private String defaultSkill;
        private Path modelPath;

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
            return new SkillQueryParser(defaultModel, defaultGroup, defaultLanguage, defaultSkill, modelPath);
        }

        public static Builder getInstance() {
            return new Builder()
                    .model(DEFAULT_MODEL)
                    .group(DEFAULT_GROUP)
                    .language(DEFAULT_LANGUAGE);
        }
    }

    public SkillQuery parse(Query call) {
        String model = call.get("model", defaultModel);
        String group = call.get("group", defaultGroup);
        String language = call.get("language", defaultLanguage);
        String skill = call.get("skill", defaultSkill);

        return new SkillQuery(model, group, language, skill, modelPath);
    }

}
