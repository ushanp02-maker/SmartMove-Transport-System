
package com.smartmove.backend.controller;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;

import jakarta.annotation.PostConstruct;

import org.bson.Document;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemHealthController {

    private final JdbcTemplate jdbcTemplate;
    private final MongoClient mongoClient;

    @Value("${spring.application.name:smartmove-backend}")
    private String applicationName;

    @Value("${spring.data.mongodb.database:smartmove}")
    private String mongoDatabaseName;

    private Instant startedAt;

    public SystemHealthController(
            JdbcTemplate jdbcTemplate,
            MongoClient mongoClient
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.mongoClient = mongoClient;
    }

    // ==========================================
    // INITIALIZE STARTUP TIME
    // ==========================================

    @PostConstruct
    public void initialize() {
        startedAt = Instant.now();
    }

    // ==========================================
    // BASIC APPLICATION HEALTH
    // ==========================================

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put("application", applicationName);
        response.put("status", "UP");
        response.put("timestamp", Instant.now().toString());

        return ResponseEntity.ok(response);
    }

    // ==========================================
    // APPLICATION READINESS
    // ==========================================

    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> readiness() {

        boolean oracleUp = checkOracle();
        boolean mongoUp = checkMongo();

        boolean ready = oracleUp && mongoUp;

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put("application", applicationName);
        response.put("status", ready ? "UP" : "DOWN");
        response.put("oracle", oracleUp ? "UP" : "DOWN");
        response.put("mongodb", mongoUp ? "UP" : "DOWN");
        response.put("timestamp", Instant.now().toString());

        return ResponseEntity
                .status(
                        ready
                                ? HttpStatus.OK
                                : HttpStatus.SERVICE_UNAVAILABLE
                )
                .body(response);
    }

    // ==========================================
    // ADMIN: DETAILED SYSTEM STATUS
    // ==========================================

    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> status() {

        boolean oracleUp = checkOracle();
        boolean mongoUp = checkMongo();

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put("application", applicationName);
        response.put(
                "status",
                oracleUp && mongoUp ? "UP" : "DEGRADED"
        );

        response.put("timestamp", Instant.now().toString());

        response.put("uptimeSeconds", getUptimeSeconds());

        Map<String, Object> databases =
                new LinkedHashMap<>();

        databases.put(
                "oracle",
                Map.of(
                        "status",
                        oracleUp ? "UP" : "DOWN",
                        "type",
                        "Relational Database"
                )
        );

        databases.put(
                "mongodb",
                Map.of(
                        "status",
                        mongoUp ? "UP" : "DOWN",
                        "type",
                        "Document Database"
                )
        );

        response.put("databases", databases);

        Runtime runtime = Runtime.getRuntime();

        Map<String, Object> memory =
                new LinkedHashMap<>();

        memory.put(
                "totalMemoryMb",
                toMegabytes(runtime.totalMemory())
        );

        memory.put(
                "freeMemoryMb",
                toMegabytes(runtime.freeMemory())
        );

        memory.put(
                "maxMemoryMb",
                toMegabytes(runtime.maxMemory())
        );

        memory.put(
                "usedMemoryMb",
                toMegabytes(
                        runtime.totalMemory()
                                - runtime.freeMemory()
                )
        );

        response.put("memory", memory);

        response.put(
                "availableProcessors",
                runtime.availableProcessors()
        );

        return ResponseEntity.ok(response);
    }

    // ==========================================
    // ADMIN: ORACLE HEALTH
    // ==========================================

    @GetMapping("/oracle")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>>
    oracleHealth() {

        boolean connected = checkOracle();

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put("database", "Oracle");
        response.put("status", connected ? "UP" : "DOWN");
        response.put("timestamp", Instant.now().toString());

        return ResponseEntity
                .status(
                        connected
                                ? HttpStatus.OK
                                : HttpStatus.SERVICE_UNAVAILABLE
                )
                .body(response);
    }

    // ==========================================
    // ADMIN: MONGODB HEALTH
    // ==========================================

    @GetMapping("/mongodb")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>>
    mongoHealth() {

        boolean connected = checkMongo();

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put("database", "MongoDB");
        response.put("status", connected ? "UP" : "DOWN");
        response.put("timestamp", Instant.now().toString());

        return ResponseEntity
                .status(
                        connected
                                ? HttpStatus.OK
                                : HttpStatus.SERVICE_UNAVAILABLE
                )
                .body(response);
    }

    // ==========================================
    // ADMIN: APPLICATION INFORMATION
    // ==========================================

    @GetMapping("/info")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>>
    applicationInfo() {

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put("application", applicationName);
        response.put(
                "system",
                "SmartMove Transport Solutions"
        );

        response.put("backend", "Spring Boot");
        response.put("language", "Java");
        response.put("relationalDatabase", "Oracle");
        response.put("documentDatabase", "MongoDB");

        response.put("uptimeSeconds", getUptimeSeconds());

        response.put(
                "timestamp",
                Instant.now().toString()
        );

        return ResponseEntity.ok(response);
    }

    // ==========================================
    // ORACLE CONNECTIVITY CHECK
    // ==========================================

    private boolean checkOracle() {

        try {
            Integer result = jdbcTemplate.queryForObject(
                    "SELECT 1 FROM DUAL",
                    Integer.class
            );

            return result != null && result == 1;

        } catch (Exception exception) {
            return false;
        }
    }

    // ==========================================
    // MONGODB CONNECTIVITY CHECK
    // ==========================================

    private boolean checkMongo() {

        try {
            MongoDatabase database =
                    mongoClient.getDatabase(
                            mongoDatabaseName
                    );

            Document result = database.runCommand(
                    new Document("ping", 1)
            );

            return result.getDouble("ok") != null
                    && result.getDouble("ok") == 1.0;

        } catch (Exception exception) {
            return false;
        }
    }

    // ==========================================
    // UPTIME CALCULATION
    // ==========================================

    private long getUptimeSeconds() {

        if (startedAt == null) {
            return 0;
        }

        return Duration.between(
                startedAt,
                Instant.now()
        ).getSeconds();
    }

    // ==========================================
    // MEMORY CONVERSION
    // ==========================================

    private long toMegabytes(long bytes) {
        return bytes / (1024L * 1024L);
    }
}
