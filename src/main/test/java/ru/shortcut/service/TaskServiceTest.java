package ru.shortcut.service;

import ru.shortcut.dto.TaskDto;
import ru.shortcut.entity.Task;
import ru.shortcut.exceptions.TaskNotFoundException;
import ru.shortcut.mapper.TaskMapper;
import ru.shortcut.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private TaskMapper taskMapper;
    @InjectMocks
    private TaskService taskService;

    private Task task1;
    private Task task2;
    private TaskDto taskDto1;
    private TaskDto taskDto2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        task1 = new Task(1L, "Jira1", "JiraCodeTask1", "OPEN");
        task2 = new Task(2L, "Jira2", "JiraCodeTask2", "NEW");
        taskDto1 = new TaskDto("Jira1", "JiraCodeTask1", "OPEN");
        taskDto2 = new TaskDto("Jira2", "JiraCodeTask2", "NEW");

    }

    @Test
    void getTaskByTitleSuccess() {
        when(taskRepository.findByTitle("Jira1")).thenReturn(Optional.of(task1));
        when(taskMapper.toDto(task1)).thenReturn(taskDto1);

        TaskDto dto = taskService.getTaskByTitle("Jira1");

        assertNotNull(dto);
        assertEquals("Jira1", dto.title());
        assertEquals("JiraCodeTask1", dto.description());
        assertEquals("OPEN", dto.status());
        verify(taskRepository, times(1)).findByTitle("Jira1");
        verify(taskMapper, times(1)).toDto(task1);
    }

    @Test
    void getTaskByTitleNoFound() {
        when(taskRepository.findByTitle("Jira001")).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class, () -> {
            taskService.getTaskByTitle("Jira001");
        });

        verify(taskRepository, times(1)).findByTitle("Jira001");
    }

    @Test
    void getTaskByStatusSuccess() {
        when(taskRepository.findByStatus("OPEN")).thenReturn(Optional.of(task1));
        when(taskMapper.toDto(task1)).thenReturn(taskDto1);

        TaskDto dto = taskService.getTaskByStatus("OPEN");

        assertNotNull(dto);
        assertEquals("Jira1", dto.title());
        assertEquals("JiraCodeTask1", dto.description());
        assertEquals("OPEN", dto.status());
        verify(taskRepository, times(1)).findByStatus("OPEN");
        verify(taskMapper, times(1)).toDto(task1);
    }

    @Test
    void getTaskByStatusNoFound() {
        when(taskRepository.findByStatus("CLOSED")).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class, () -> {
            taskService.getTaskByStatus("CLOSED");
        });

        verify(taskRepository, times(1)).findByStatus("CLOSED");
    }

    @Test
    void createTaskSuccess() {

        TaskDto inputDto = new TaskDto("Jira3", "JiraCodeTask3", "NEW");
        Task entityToSave = new Task(null, "Jira3", "JiraCodeTask3", "NEW");
        Task savedEntity = new Task(3L, "Jira3", "JiraCodeTask3", "NEW");
        TaskDto expectedDto = new TaskDto("Jira3", "JiraCodeTask3", "NEW");


        when(taskMapper.toEntity(inputDto)).thenReturn(entityToSave);
        when(taskRepository.save(entityToSave)).thenReturn(new Task(3L, "Jira3", "JiraCodeTask3", "NEW"));
        when(taskMapper.toDto(savedEntity)).thenReturn(expectedDto);

        TaskDto result = taskService.createTask(inputDto);

        assertNotNull(result);
        assertEquals(expectedDto.title(), result.title());
        assertEquals(expectedDto.description(), result.description());
        assertEquals(expectedDto.status(), result.status());

        verify(taskMapper, times(1)).toEntity(inputDto);
        verify(taskRepository, times(1)).save(entityToSave);
        verify(taskMapper, times(1)).toDto(savedEntity);
    }

    @Test
    void createTaskSaveFails() {
        TaskDto inputDto = new TaskDto("Jira3", "JiraCodeTask3", "NEW");
        Task entityToSave = new Task(null, "Jira3", "JiraCodeTask3", "NEW");

        when(taskMapper.toEntity(inputDto)).thenReturn(entityToSave);
        when(taskRepository.save(entityToSave)).thenThrow(new RuntimeException("DataBase error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            taskService.createTask(inputDto);
        });

        assertEquals("DataBase error", exception.getMessage());

        verify(taskMapper, times(1)).toEntity(inputDto);
        verify(taskRepository, times(1)).save(entityToSave);
        verify(taskMapper, never()).toDto(any());

    }

    @Test
    void updateTaskSuccess() {
        TaskDto updatedDto = new TaskDto("UpdatedJira", "UpdatedDescription", "IN_PROGRESS");
        Task updatedTask = new Task(1L, "UpdatedJira", "UpdatedDescription", "IN_PROGRESS");

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task1));
        when(taskRepository.save(any(Task.class))).thenReturn(updatedTask);
        when(taskMapper.toDto(updatedTask)).thenReturn(updatedDto);

        TaskDto result = taskService.updateTask(1L, updatedDto);

        assertNotNull(result);
        assertEquals("UpdatedJira", result.title());
        assertEquals("UpdatedDescription", result.description());
        assertEquals("IN_PROGRESS", result.status());

        verify(taskRepository, times(1)).findById(1L);
        verify(taskRepository, times(1)).save(task1);
        verify(taskMapper, times(1)).toDto(updatedTask);
    }

    @Test
    void updateTaskNotFound() {
        TaskDto updatedDto = new TaskDto("UpdatedJira", "UpdatedDescription", "IN_PROGRESS");

        when(taskRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class, () -> taskService.updateTask(1L, updatedDto));

        verify(taskRepository, times(1)).findById(1L);
        verify(taskRepository, never()).save(any(Task.class));
        verify(taskMapper, never()).toDto(any());
    }

    @Test
    void deleteTaskSuccess() {
        when(taskRepository.existsById(1L)).thenReturn(true);

        assertDoesNotThrow(() -> taskService.deleteTask(1L));

        verify(taskRepository, times(1)).existsById(1L);
        verify(taskRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteTaskNotFound() {
        when(taskRepository.existsById(1L)).thenReturn(false);

        assertThrows(TaskNotFoundException.class, () -> taskService.deleteTask(1L));

        verify(taskRepository, times(1)).existsById(1L);
        verify(taskRepository, never()).deleteById(anyLong());
    }

    @Test
    void getAllTaskSuccess() {
        when(taskRepository.findAll()).thenReturn(Arrays.asList(task1, task2));
        when(taskMapper.toDto(task1)).thenReturn(taskDto1);
        when(taskMapper.toDto(task2)).thenReturn(taskDto2);

        List<TaskDto> dtoList = taskService.getAllTask();

        assertNotNull(dtoList);
        assertEquals(2, dtoList.size());
        assertEquals("Jira1", dtoList.get(0).title());
        assertEquals("Jira2", dtoList.get(1).title());
        verify(taskRepository, times(1)).findAll();
    }

}