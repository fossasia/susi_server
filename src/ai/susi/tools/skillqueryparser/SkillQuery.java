package ai.susi.tools.skillqueryparser;

import ai.susi.DAO;
import ai.susi.server.APIException;
import ai.susi.tools.IO;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Represents parsed Skill from query parameters
 */
public class SkillQuery {
    private final @Nonnull String model;
    private final @Nonnull String group;
    private final @Nonnull String language;
    private final @Nullable String skill;

    private final @Nonnull Path modelDirectory;
    private final @Nullable String privateSkillUserId; // Only set if skill is private

    public static SkillQueryParser getParser() {
        return SkillQueryParser.getInstance();
    }

    public static SkillQueryParser getParser(String skill) {
        return SkillQueryParser.getInstance(skill);
    }

    public SkillQuery(@Nonnull String model, @Nonnull String group, @Nonnull String language,
                      @Nullable String skill, @Nullable Path modelDirectory, @Nullable String userId) {
        this.model = model;
        this.group = group;
        this.language = language;
        this.skill = skill;
        this.privateSkillUserId = userId;

        if (modelDirectory == null) {
            this.modelDirectory = DAO.model_watch_dir.toPath();
        } else {
            this.modelDirectory = modelDirectory;
        }
    }

    /**
     * Create a new copy of the SkillQuery for private skills
     *
     * Clones and returns new instance since the class is immutable
     * @param userId User ID of the user owning the private skill
     * @return new instance of SkillQuery
     */
    public SkillQuery forPrivate(String userId) {
        return new SkillQuery(model, group, language, skill, DAO.private_skill_watch_dir.toPath(), userId);
    }

    public SkillQuery language(String newLanguage) {
        return new SkillQuery(model, group, newLanguage, skill, modelDirectory, privateSkillUserId);
    }

    public SkillQuery skill(String newSkill) {
        return new SkillQuery(model, group, language, newSkill, modelDirectory, privateSkillUserId);
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
        return IO.resolvePath(getGroupPath(), language);
    }

    public Path getGroupPath() {
        return IO.resolvePath(getModelPath(), group);
    }

    public Path getModelPath() {
        if (isPrivate()) {
            return IO.resolvePath(modelDirectory, privateSkillUserId);
        } else {
            return IO.resolvePath(modelDirectory, model);
        }
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

    public boolean isPrivate() {
        return privateSkillUserId != null;
    }

    @Override
    public String toString() {
        return "SkillQuery{" +
                "model='" + model + '\'' +
                ", group='" + group + '\'' +
                ", language='" + language + '\'' +
                ", skill='" + skill + '\'' +
                ", modelDirectory=" + modelDirectory +
                ", userId='" + privateSkillUserId + '\'' +
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
