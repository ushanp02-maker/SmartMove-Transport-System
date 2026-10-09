
package com.smartmove.backend.controller;

import com.smartmove.backend.service.DatabaseInitializationService;
import com.smartmove.backend.service.DatabaseInitializationService.InitializationReport;
import com.smartmove.backend.service.DatabaseInitializationService.InitializationStatus;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/database-initialization")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class DatabaseInitializationController {

    private final DatabaseInitializationService initializationService;

    public DatabaseInitializationController(
            DatabaseInitializationService initializationService
    ) {
        this.initializationService = initializationService;
    }

    // ==========================================
    // API INFORMATION
    // ==========================================

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getInfo() {

        Map<String, Object> response = new LinkedHashMap<>();

        response.put(
                "module",
                "SmartMove Database Initialization"
        );

        response.put("database", "Oracle");
        response.put("access", "SUPER_ADMIN");
        response.put("automaticInitialization", "Optional");
        response.put("manualInitialization", true);
        response.put("overwritesExistingSettings", false);
        response.put("deletesOperationalRecords", false);

        return ResponseEntity.ok(response);
    }

    // ==========================================
    // CHECK INITIALIZATION STATUS
    // ==========================================

    @GetMapping("/status")
    public ResponseEntity<InitializationStatus>
    getInitializationStatus() {

        InitializationStatus status =
                initializationService.getInitializationStatus();

        return ResponseEntity.ok(status);
    }

    // ==========================================
    // INITIALIZE MISSING DEFAULT SETTINGS
    // ==========================================

    @PostMapping("/initialize")
    public ResponseEntity<InitializationReport>
    initializeReferenceData() {

        InitializationReport report =
                initializationService.initializeReferenceData();

        return ResponseEntity.ok(report);
    }
}
