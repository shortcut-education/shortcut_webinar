package ru.shortcut.controller;

import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final JdbcTemplate jdbcTemplate;
    private final Environment environment;

    private static final Map<Long, Map<String, Object>> LOCAL_STORAGE = new HashMap<>();
    private static final AtomicLong LOCAL_ID_GENERATOR = new AtomicLong(1);

    private Boolean useDbProfileCached = null;

    public TaskController(JdbcTemplate jdbcTemplate, Environment environment) {
        this.jdbcTemplate = jdbcTemplate;
        this.environment = environment;
    }

    @GetMapping
    public Object handleGet(
            @RequestParam(name = "action", required = false, defaultValue = "list") String action,
            @RequestParam(name = "id", required = false) Long id,
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "status", required = false) String status
    ) {
        if ("list".equalsIgnoreCase(action)) {
            return listAllTasks();
        } else if ("get".equalsIgnoreCase(action)) {
            if (id == null) {
                return error("ID_IS_REQUIRED");
            }
            return getTaskById(id);
        } else if ("search".equalsIgnoreCase(action)) {
            return searchTasks(query, status);
        } else {
            return error("UNKNOWN_ACTION");
        }
    }

    @PostMapping
    public Object createTask(@RequestBody Map<String, Object> body) {
        String title = body.get("title") != null ? body.get("title").toString() : null;
        String description = body.get("description") != null ? body.get("description").toString() : null;
        String status = body.get("status") != null ? body.get("status").toString() : "NEW";
        Integer priority = null;
        try {
            if (body.get("priority") != null) {
                priority = Integer.parseInt(body.get("priority").toString());
            }
        } catch (Exception e) {
        }
        if (priority == null) {
            priority = 1;
        }

        if (title == null || title.trim().isEmpty()) {
            return error("TITLE_IS_REQUIRED");
        }

        Map<String, Object> task = new LinkedHashMap<>();
        task.put("title", title);
        task.put("description", description);
        task.put("status", status);
        task.put("priority", priority);
        task.put("createdAt", LocalDateTime.now().toString());

        if (isDbProfile()) {
            jdbcTemplate.update(
                    "INSERT INTO tasks (title, description, status, priority, created_at) VALUES (?, ?, ?, ?, ?)",
                    title, description, status, priority, LocalDateTime.now()
            );
            Long newId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM tasks", Long.class);
            task.put("id", newId);
        } else {
            long id = LOCAL_ID_GENERATOR.getAndIncrement();
            task.put("id", id);
            synchronized (LOCAL_STORAGE) {
                LOCAL_STORAGE.put(id, task);
            }
        }

        return success(task);
    }

    @PutMapping("/{id}")
    public Object updateTask(@PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        if (id == null) {
            return error("ID_IS_REQUIRED");
        }

        Map<String, Object> existing = getTaskByIdInternal(id);
        if (existing == null) {
            return error("TASK_NOT_FOUND");
        }

        String newTitle = body.get("title") != null ? body.get("title").toString() : existing.get("title").toString();
        String newDescription = body.get("description") != null ? body.get("description").toString() : (String) existing.get("description");
        String newStatus = body.get("status") != null ? body.get("status").toString() : (String) existing.get("status");
        Integer newPriority = null;
        try {
            if (body.get("priority") != null) {
                newPriority = Integer.parseInt(body.get("priority").toString());
            }
        } catch (Exception e) {
        }
        if (newPriority == null) {
            Object p = existing.get("priority");
            if (p instanceof Integer) {
                newPriority = (Integer) p;
            } else if (p != null) {
                try {
                    newPriority = Integer.parseInt(p.toString());
                } catch (Exception ex) {
                    newPriority = 1;
                }
            } else {
                newPriority = 1;
            }
        }

        if (isDbProfile()) {
            jdbcTemplate.update(
                    "UPDATE tasks SET title = ?, description = ?, status = ?, priority = ? WHERE id = ?",
                    newTitle, newDescription, newStatus, newPriority, id
            );
        } else {
            synchronized (LOCAL_STORAGE) {
                Map<String, Object> m = LOCAL_STORAGE.get(id);
                if (m != null) {
                    m.put("title", newTitle);
                    m.put("description", newDescription);
                    m.put("status", newStatus);
                    m.put("priority", newPriority);
                }
            }
        }

        Map<String, Object> updated = getTaskByIdInternal(id);
        return success(updated);
    }

    @DeleteMapping("/{id}")
    public Object deleteTask(@PathVariable("id") Long id) {
        if (id == null) {
            return error("ID_IS_REQUIRED");
        }
        if (isDbProfile()) {
            int rows = jdbcTemplate.update("DELETE FROM tasks WHERE id = ?", id);
            if (rows == 0) {
                return error("TASK_NOT_FOUND");
            }
        } else {
            synchronized (LOCAL_STORAGE) {
                if (!LOCAL_STORAGE.containsKey(id)) {
                    return error("TASK_NOT_FOUND");
                }
                LOCAL_STORAGE.remove(id);
            }
        }
        Map<String, Object> resp = new HashMap<>();
        resp.put("deletedId", id);
        return success(resp);
    }

    private Object listAllTasks() {
        if (isDbProfile()) {
            List<Map<String, Object>> list = jdbcTemplate.query("SELECT * FROM tasks", (rs, rowNum) -> mapRow(rs));
            return success(list);
        } else {
            List<Map<String, Object>> list;
            synchronized (LOCAL_STORAGE) {
                list = new ArrayList<>(LOCAL_STORAGE.values());
            }
            return success(list);
        }
    }


    private Object getTaskById(Long id) {
        Map<String, Object> task = getTaskByIdInternal(id);
        if (task == null) {
            return error("TASK_NOT_FOUND");
        }
        return success(task);
    }

    private Map<String, Object> getTaskByIdInternal(Long id) {
        if (isDbProfile()) {
            List<Map<String, Object>> list = jdbcTemplate.query(
                    "SELECT * FROM tasks WHERE id = ?",
                    ps -> ps.setLong(1, id),
                    (rs, rowNum) -> mapRow(rs)
            );
            if (list.isEmpty()) return null;
            return list.get(0);
        } else {
            synchronized (LOCAL_STORAGE) {
                return LOCAL_STORAGE.get(id);
            }
        }
    }

    private Object searchTasks(String query, String status) {
        if (query == null) query = "";
        if (status == null) status = "";

        List<Map<String, Object>> result = new ArrayList<>();

        if (isDbProfile()) {
            StringBuilder sql = new StringBuilder("SELECT * FROM tasks WHERE 1=1");
            List<Object> params = new ArrayList<>();
            if (!query.isBlank()) {
                sql.append(" AND (LOWER(title) LIKE ? OR LOWER(description) LIKE ?)");
                String q = "%" + query.toLowerCase() + "%";
                params.add(q);
                params.add(q);
            }
            if (!status.isBlank()) {
                sql.append(" AND status = ?");
                params.add(status);
            }
            result = jdbcTemplate.query(sql.toString(), params.toArray(), (rs, rowNum) -> mapRow(rs));
        } else {
            synchronized (LOCAL_STORAGE) {
                for (Map<String, Object> t : LOCAL_STORAGE.values()) {
                    boolean ok = true;
                    if (!query.isBlank()) {
                        String ttl = Objects.toString(t.get("title"), "").toLowerCase();
                        String desc = Objects.toString(t.get("description"), "").toLowerCase();
                        if (!ttl.contains(query.toLowerCase()) && !desc.contains(query.toLowerCase())) {
                            ok = false;
                        }
                    }
                    if (!status.isBlank()) {
                        String st = Objects.toString(t.get("status"), "");
                        if (!st.equalsIgnoreCase(status)) {
                            ok = false;
                        }
                    }
                    if (ok) {
                        result.add(t);
                    }
                }
            }
        }

        result.sort((a, b) -> {
            int p1 = Integer.parseInt(Objects.toString(a.get("priority"), "0"));
            int p2 = Integer.parseInt(Objects.toString(b.get("priority"), "0"));
            if (p1 != p2) {
                return Integer.compare(p2, p1);
            }
            String c1 = Objects.toString(a.get("createdAt"), "");
            String c2 = Objects.toString(b.get("createdAt"), "");
            return c1.compareTo(c2);
        });

        return success(result);
    }

    private Map<String, Object> mapRow(ResultSet rs) throws SQLException {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", rs.getLong("id"));
        map.put("title", rs.getString("title"));
        map.put("description", rs.getString("description"));
        map.put("status", rs.getString("status"));
        map.put("priority", rs.getInt("priority"));
        map.put("createdAt", rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime().toString() : null);
        return map;
    }

    private Map<String, Object> success(Object data) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", true);
        resp.put("data", data);
        resp.put("timestamp", System.currentTimeMillis());
        return resp;
    }

    private Map<String, Object> error(String code) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", false);
        resp.put("errorCode", code);
        resp.put("timestamp", System.currentTimeMillis());
        return resp;
    }


    private boolean isDbProfile() {
        if (useDbProfileCached == null) {
            String[] activeProfiles = environment.getActiveProfiles();
            boolean db = false;
            if (activeProfiles != null) {
                for (String p : activeProfiles) {
                    if ("h2".equalsIgnoreCase(p)) {
                        db = true;
                        break;
                    }
                }
            }
            useDbProfileCached = db;
        }
        return useDbProfileCached;
    }
}
