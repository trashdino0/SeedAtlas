package dev.seedatlas.core.io;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import dev.seedatlas.core.model.Project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ProjectIO {

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .addModule(new ParameterNamesModule())
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();

    private ProjectIO() {
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    public static Project read(Path path) throws IOException {
        return MAPPER.readValue(Files.readString(path), Project.class);
    }

    public static void write(Path path, Project project) throws IOException {
        Path absolute = path.toAbsolutePath();
        Path parent = absolute.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(absolute, toJson(project));
    }

    public static String toJson(Project project) throws JsonProcessingException {
        return MAPPER.writeValueAsString(project) + System.lineSeparator();
    }

    public static Project fromJson(String json) throws JsonProcessingException {
        return MAPPER.readValue(json, Project.class);
    }
}
