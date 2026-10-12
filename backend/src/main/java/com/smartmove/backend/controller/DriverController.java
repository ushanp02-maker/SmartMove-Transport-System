package com.smartmove.backend.controller;

import com.smartmove.backend.service.DriverService;
import com.smartmove.backend.service.CurrentUserService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/drivers")
public class DriverController {

    private final DriverService drivers;
    private final CurrentUserService currentUser;

    public DriverController(
            DriverService drivers,
            CurrentUserService currentUser
    ) {
        this.drivers = drivers;
        this.currentUser = currentUser;
    }

    // ==========================================
    // DRIVER INPUT DTO
    // ==========================================

    public record DriverInput(
            String name,
            String email,
            String phone,
            String licenseNumber,
            LocalDate licenseExpiry,
            Integer experienceYears
    ) {}

    // ==========================================
    // ADMIN: CREATE DRIVER
    // ==========================================

    @PostMapping
    public ResponseEntity<DriverService.DriverProfile> create(
            @RequestBody DriverInput input
    ) {
        currentUser.requireAdmin();

        return ResponseEntity.status(HttpStatus.CREATED).body(
                drivers.createDriver(
                        new DriverService.CreateDriverRequest(
                                input.name(),
                                input.email(),
                                input.phone(),
                                input.licenseNumber(),
                                input.licenseExpiry(),
                                input.experienceYears()
                        )
                )
        );
    }

    // ==========================================
    // DRIVER: MY PROFILE
    // ==========================================

    @GetMapping("/me")
    public DriverService.DriverProfile myProfile() {
        currentUser.requireDriver();

        return drivers.getDriverProfile(
                currentUser.getCurrentDriverId()
        );
    }

    // ==========================================
    // ADMIN: UPDATE DRIVER
    // ==========================================

    @PutMapping("/{id}")
    public DriverService.DriverProfile update(
            @PathVariable Long id,
            @RequestBody DriverInput input
    ) {
        currentUser.requireAdmin();

        return drivers.updateDriver(
                id,
                new DriverService.UpdateDriverRequest(
                        input.name(),
                        input.phone(),
                        input.licenseNumber(),
                        input.licenseExpiry(),
                        input.experienceYears()
                )
        );
    }

    // ==========================================
    // ADMIN: UPDATE DRIVER STATUS
    // ==========================================

    @PatchMapping("/{id}/status")
    public DriverService.DriverProfile changeStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> input
    ) {
        currentUser.requireAdmin();

        // Validate request body
        if (input == null || input.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Driver status request body is required"
            );
        }

        String newStatus = input.get("newStatus");

        if (newStatus == null || newStatus.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Driver status is required"
            );
        }

        return drivers.updateDriverStatus(id, newStatus);
    }

    // ==========================================
    // ADMIN: LIST ALL DRIVERS
    // ==========================================

    @GetMapping
    public List<DriverService.DriverProfile> list() {
        currentUser.requireAdmin();

        return drivers.getAllDrivers();
    }

    // ==========================================
    // ADMIN: SEARCH DRIVERS
    // ==========================================

    @GetMapping("/search")
    public List<DriverService.DriverProfile> searchDrivers(
            @RequestParam(required = false) String keyword
    ) {
        currentUser.requireAdmin();

        return drivers.searchDrivers(keyword);
    }
}