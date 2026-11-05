package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@RestController
@RequestMapping("/api")
public class ApiController {

    @Value("${app.allowed-root:./allowed_root}")
    private String allowedRoot;

    @PostMapping("/delete")
    public ResponseEntity<Map<String, Object>> delete(@RequestBody Map<String, List<String>> body) {
        List<String> paths = body.getOrDefault("paths", List.of());
        Path root = Paths.get(allowedRoot).toAbsolutePath().normalize();
        List<String> deleted = new ArrayList<>();
        List<Map<String, String>> errors = new ArrayList<>();

        for (String raw : paths) {
            try {
                if (raw == null || raw.isBlank()) {
                    errors.add(err(raw, "empty path"));
                    continue;
                }
                Path p = root.resolve(raw).normalize();
                if (!p.startsWith(root)) {
                    errors.add(err(raw, "outside allowed root"));
                    continue;
                }
                if (!Files.exists(p, LinkOption.NOFOLLOW_LINKS)) {
                    errors.add(err(raw, "does not exist"));
                    continue;
                }
                if (Files.isSymbolicLink(p)) {
                    errors.add(err(raw, "symlink refused"));
                    continue;
                }
                if (!Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS)) {
                    errors.add(err(raw, "not a regular file"));
                    continue;
                }
                Files.delete(p);
                deleted.add(raw);
            } catch (IOException | InvalidPathException e) {
                errors.add(err(raw, e.getClass().getSimpleName() + ": " + e.getMessage()));
            }
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("deleted", deleted);
        resp.put("errors", errors);
        return ResponseEntity.ok(resp);
    }

    private Map<String, String> err(String path, String reason) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("path", path == null ? "" : path);
        m.put("reason", reason);
        return m;
    }
}
