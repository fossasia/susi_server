package ai.susi.tools.skillqueryparser;

import ai.susi.DAO;
import ai.susi.server.APIException;
import ai.susi.tools.IO;

import javax.annotation.Nullable;
import java.io.File;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Represents parsed Skill from query parameters
 */
public class SkillQuery {
    private final String model;
    private final String group;
    private final String language;
    private final String skill;
    private final Path modelDirectory;

    public static SkillQueryParser getParser() {
        return SkillQueryParser.getInstance();
    }

    public static SkillQueryParser getParser(String skill) {
        return SkillQueryParser.getInstance(skill);
    }

    public SkillQuery(String model, String group, String language, @Nullable String skill, @Nullable Path modelDirectory) {
        this.model = model;
        this.group = group;
        this.language = language;
        this.skill = skill;

        if (modelDirectory == null) {
            this.modelDirectory = DAO.model_watch_dir.toPath();
        } else {
            this.modelDirectory = modelDirectory;
        }
    }

    public SkillQuery(String model, String group, String language, String skill) {
        this(model, group, language, skill, null);
    }

    public SkillQuery requireOrThrow() throws APIException {
        requireSkillFileOrThrow();
        return this;
    }

    public File requireSkillFileOrThrow() throws APIException {
        File skillFile = getSkillFile();
        if (skillFile != null && skillFile.exists()) {
            return skillFile;
        }

        throw new APIException(404, "Skill does not exist.");
    }

    public File getSkillFile() {
        return getSkillFile(false);
    }

    public File getSkillFile(boolean nullIfNotFound) {
        return DAO.getSkillFileInLanguage(getLanguagePath().toFile(), skill, nullIfNotFound);
    }

    public Path getLanguagePath() {
        assertRequiredArguments();
        return IO.resolvePath(modelDirectory, model, group, language);
    }

    public Path getModelPath() {
        return IO.resolvePath(modelDirectory, model);
    }

    private void assertRequiredArguments() {
        Objects.requireNonNull(model);
        Objects.requireNonNull(group);
        Objects.requireNonNull(language);
    }

    public String getModel() {
        return model;
    }

    public String getGroup() {
        return group;
    }

    public String getLanguage() {
        return language;
    }

    public String getSkill() {
        return skill;
    }

    public Path getModelDirectory() {
        return modelDirectory;
    }

    @Override
    public String toString() {
        return "SkillQuery{" +
                "model='" + model + '\'' +
                ", group='" + group + '\'' +
                ", language='" + language + '\'' +
                ", skill='" + skill + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SkillQuery)) return false;
        SkillQuery that = (SkillQuery) o;
        return Objects.equals(model, that.model) &&
                Objects.equals(group, that.group) &&
                Objects.equals(language, that.language) &&
                Objects.equals(skill, that.skill);
    }

    @Override
    public int hashCode() {
        return Objects.hash(model, group, language, skill);
    }
}
