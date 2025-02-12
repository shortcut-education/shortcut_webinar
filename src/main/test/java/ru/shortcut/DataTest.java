package ru.shortcut;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.testcontainers.shaded.org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

public abstract class DataTest {

    public final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public <T> T readJson(String fileName, Class<T> aClass) throws IOException {
        var json = readFile(fileName);
        return OBJECT_MAPPER.readValue(json, aClass);
    }

    public JsonNode readTree(String fileName) throws IOException {
        var json = readFile(fileName);
        return OBJECT_MAPPER.readTree(json);
    }

    private String readFile(String fileName) throws IOException {
        var fullFileName = getResourcePath(fileName);
        ClassPathResource resource = new ClassPathResource(fullFileName);
        List<String> lines = Files.readAllLines(resource.getFile().toPath());
        return StringUtils.join(lines, "");
    }

    private String getResourcePath(String fileName) {
        var walker = StackWalker.getInstance();
        Optional<String> methodName = walker.walk(frames ->
                frames
                        .filter(it -> {
                            try {
                                return Class.forName(it.getClassName())
                                        .getMethod(it.getMethodName())
                                        .getAnnotation(Test.class) != null;
                            } catch (NoSuchMethodException | ClassNotFoundException e) {
                                return false;
                            }
                        })
                        .map(StackWalker.StackFrame::getMethodName)
                        .findFirst()
        );
        var pathToClass = Paths.get(getClass().getName().replace(".", "/"));
        var pathToTestResource = pathToClass.resolve(methodName.orElse(""));

        return pathToTestResource.resolve(fileName).toString();
    }
}
