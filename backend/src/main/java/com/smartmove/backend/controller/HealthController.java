package com.smartmove.backend.controller;

import org.springframework.boot.info.BuildProperties;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class HealthController {

    private final BuildProperties buildProperties;

    public HealthController(BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }

    @GetMapping("/api/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(Map.of(
                        "status", "UP",
                        "application", "SmartMove Backend",
                        "version", buildProperties.getVersion(),
                        "timestamp", Instant.now().toString()
                ));
    }
}