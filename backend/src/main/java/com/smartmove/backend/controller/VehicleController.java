
package com.smartmove.backend.controller;

import com.smartmove.backend.service.VehicleService;
import com.smartmove.backend.service.VehicleService.CreateVehicleRequest;
import com.smartmove.backend.service.VehicleService.UpdateVehicleRequest;
import com.smartmove.backend.service.VehicleService.VehicleCompliance;
import com.smartmove.backend.service.VehicleService.VehicleProfile;
import com.smartmove.backend.service.VehicleService.VehicleStatistics;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vehicles")
@Validated
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    // ==========================================
    // ADMIN: ALL VEHICLES
    // ==========================================

    @GetMapping
    public ResponseEntity<List<VehicleProfile>> getAllVehicles() {
        return ResponseEntity.ok(
                vehicleService.getAllVehicles()
        );
    }

    // ==========================================
    // ADMIN: PAGINATED VEHICLES
    // ==========================================

    @GetMapping("/page")
    public ResponseEntity<Page<VehicleProfile>> getVehiclesPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        if (page < 0 || size < 1 || size > 100) {
            throw badRequest(
                    "Page must be nonnegative and size must be between 1 and 100"
            );
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "id")
        );

        return ResponseEntity.ok(
                vehicleService.getVehicles(pageable)
        );
    }

    // ==========================================
    // ADMIN: SEARCH VEHICLES
    // ==========================================

    @GetMapping("/search")
    public ResponseEntity<List<VehicleProfile>> searchVehicles(
            @RequestParam(required = false) String keyword
    ) {
        return ResponseEntity.ok(
                vehicleService.searchVehicles(keyword)
        );
    }

    // ==========================================
    // AVAILABLE VEHICLES
    // ==========================================

    @GetMapping("/available")
    public ResponseEntity<List<VehicleProfile>> getAvailableVehicles(
            @RequestParam(defaultValue = "1")
            @Min(1)
            @Max(200)
            int minimumCapacity
    ) {
        return ResponseEntity.ok(
                vehicleService.getAvailableVehicles(
                        minimumCapacity
                )
        );
    }

    // ==========================================
    // FLEET STATISTICS
    // ==========================================

    @GetMapping("/stats")
    public ResponseEntity<VehicleStatistics> getVehicleStatistics() {
        return ResponseEntity.ok(
                vehicleService.getVehicleStatistics()
        );
    }

    // ==========================================
    // MAINTENANCE MONITORING
    // ==========================================

    @GetMapping("/maintenance/due")
    public ResponseEntity<List<VehicleProfile>> getVehiclesDueForService(
            @RequestParam(defaultValue = "30")
            @Min(0)
            @Max(365)
            int daysAhead
    ) {
        return ResponseEntity.ok(
                vehicleService.getVehiclesDueForService(
                        daysAhead
                )
        );
    }

    // ==========================================
    // VEHICLE COUNT
    // ==========================================

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> countVehicles() {
        return ResponseEntity.ok(
                Map.of(
                        "totalVehicles",
                        vehicleService.getVehicleStatistics()
                                .totalVehicles()
                )
        );
    }

    // ==========================================
    // VEHICLE PROFILE BY ID
    // ==========================================

    @GetMapping("/{vehicleId}")
    public ResponseEntity<VehicleProfile> getVehicle(
            @PathVariable Long vehicleId
    ) {
        return ResponseEntity.ok(
                vehicleService.getVehicleProfile(vehicleId)
        );
    }

    // ==========================================
    // VEHICLE DOCUMENT COMPLIANCE
    // ==========================================

    @GetMapping("/{vehicleId}/compliance")
    public ResponseEntity<VehicleCompliance> getVehicleCompliance(
            @PathVariable Long vehicleId
    ) {
        return ResponseEntity.ok(
                vehicleService.checkVehicleCompliance(vehicleId)
        );
    }

    // ==========================================
    // ADMIN: REGISTER VEHICLE
    // ==========================================

    @PostMapping
    public ResponseEntity<VehicleProfile> createVehicle(
            @Valid @RequestBody CreateVehiclePayload request
    ) {
        VehicleProfile vehicle = vehicleService.createVehicle(
                new CreateVehicleRequest(
                        request.registrationNumber(),
                        request.name(),
                        request.vehicleType(),
                        request.seatingCapacity(),
                        request.currentMileage(),
                        request.nextServiceDate(),
                        request.manufactureYear()
                )
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(vehicle);
    }

    // ==========================================
    // ADMIN: UPDATE VEHICLE
    // ==========================================

    @PutMapping("/{vehicleId}")
    public ResponseEntity<VehicleProfile> updateVehicle(
            @PathVariable Long vehicleId,
            @Valid @RequestBody UpdateVehiclePayload request
    ) {
        return ResponseEntity.ok(
                vehicleService.updateVehicle(
                        vehicleId,
                        new UpdateVehicleRequest(
                                request.registrationNumber(),
                                request.name(),
                                request.vehicleType(),
                                request.seatingCapacity(),
                                request.currentMileage(),
                                request.nextServiceDate(),
                                request.manufactureYear()
                        )
                )
        );
    }

    // ==========================================
    // ADMIN: UPDATE VEHICLE STATUS
    // ==========================================

    @PatchMapping("/{vehicleId}/status")
    public ResponseEntity<VehicleProfile> updateVehicleStatus(
            @PathVariable Long vehicleId,
            @Valid @RequestBody VehicleStatusPayload request
    ) {
        return ResponseEntity.ok(
                vehicleService.updateVehicleStatus(
                        vehicleId,
                        request.status()
                )
        );
    }

    // ==========================================
    // REQUEST DTOs
    // ==========================================

    public record CreateVehiclePayload(

            @NotBlank(message = "Registration number is required")
            @Size(max = 30)
            String registrationNumber,

            @NotBlank(message = "Vehicle name is required")
            @Size(max = 120)
            String name,

            @NotBlank(message = "Vehicle type is required")
            @Size(max = 50)
            String vehicleType,

            @NotNull(message = "Seating capacity is required")
            @Min(1)
            @Max(200)
            Integer seatingCapacity,

            @PositiveOrZero
            Double currentMileage,

            LocalDate nextServiceDate,

            @Min(1900)
            Integer manufactureYear

    ) {
    }

    public record UpdateVehiclePayload(

            @Size(max = 30)
            String registrationNumber,

            @Size(max = 120)
            String name,

            @Size(max = 50)
            String vehicleType,

            @Min(1)
            @Max(200)
            Integer seatingCapacity,

            @PositiveOrZero
            Double currentMileage,

            LocalDate nextServiceDate,

            @Min(1900)
            Integer manufactureYear

    ) {
    }

    public record VehicleStatusPayload(

            @NotBlank(message = "Vehicle status is required")
            String status

    ) {
    }

    // ==========================================
    // ERROR HELPER
    // ==========================================

    private ResponseStatusException badRequest(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }
}
