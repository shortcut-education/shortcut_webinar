package ru.shortcut.mapper;

import ru.shortcut.DataTest;
import ru.shortcut.dto.TaskDto;
import ru.shortcut.entity.Task;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.io.IOException;


class TaskMapperTest extends DataTest {

    private final static TaskMapper TASK_MAPPER = Mappers.getMapper(TaskMapper.class);

    @Test
    public void shouldMapToDtoSuccessfully() throws IOException {
        var taskDto = readJson("input-task-dto.json", TaskDto.class);

        var task = TASK_MAPPER.toEntity(taskDto);
        var input = OBJECT_MAPPER.writeValueAsString(task);

        var output = readTree("output-task.json").toString();

        Assertions.assertEquals(input, output);
    }

    @Test
    public void shouldFailToMapToDtoWhenInvalidStatus() throws IOException {
        var taskDto = readJson("input-task-dto.json", TaskDto.class);

        var task = TASK_MAPPER.toEntity(taskDto);
        var input = OBJECT_MAPPER.writeValueAsString(task);

        var output = readTree("output-task.json").toString();

        Assertions.assertNotEquals(input, output);
    }

    @Test
    public void shouldMapToEntitySuccessfully() throws IOException {
        var task = readJson("input-task.json", Task.class);

        var taskDto = TASK_MAPPER.toDto(task);
        var input = OBJECT_MAPPER.writeValueAsString(taskDto);

        var output = readTree("output-task-dto.json").toString();

        Assertions.assertEquals(input, output);
    }

    @Test
    public void shouldFailToMapToEntityWhenTitleNull() throws IOException {
        var task = readJson("input-task.json", Task.class);

        var taskDto = TASK_MAPPER.toDto(task);
        var input = OBJECT_MAPPER.writeValueAsString(taskDto);

        var output = readTree("output-task-dto.json").toString();

        Assertions.assertNotEquals(input, output);
    }
}