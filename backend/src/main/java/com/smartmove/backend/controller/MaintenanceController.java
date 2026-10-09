
package com.smartmove.backend.controller;

import com.smartmove.backend.service.MaintenanceService;
import com.smartmove.backend.service.MaintenanceService.MaintenanceProfile;
import com.smartmove.backend.service.MaintenanceService.CreateMaintenanceRequest;
import com.smartmove.backend.service.MaintenanceService.UpdateMaintenanceRequest;
import com.smartmove.backend.service.MaintenanceService.CompleteMaintenanceRequest;
import com.smartmove.backend.service.MaintenanceService.MaintenanceStatistics;
import com.smartmove.backend.service.MaintenanceService.MaintenanceCostByVehicle;
import com.smartmove.backend.service.MaintenanceService.MaintenanceCostByType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/maintenance")
@Validated
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    public MaintenanceController(
            MaintenanceService maintenanceService
    ) {
        this.maintenanceService = maintenanceService;
    }

    // ==========================================
    // ADMIN: ALL MAINTENANCE RECORDS
    // ==========================================

    @GetMapping
    public ResponseEntity<List<MaintenanceProfile>>
    getAllMaintenance() {
        return ResponseEntity.ok(
                maintenanceService.getAllMaintenance()
        );
    }

    // ==========================================
    // ADMIN: PAGINATED MAINTENANCE
    // ==========================================

    @GetMapping("/page")
    public ResponseEntity<Page<MaintenanceProfile>>
    getMaintenancePage(
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
                Sort.by(
                        Sort.Direction.DESC,
                        "scheduledDate"
                )
        );

        return ResponseEntity.ok(
                maintenanceService.getMaintenancePage(
                        pageable
                )
        );
    }

    // ==========================================
    // ADMIN: MAINTENANCE STATISTICS
    // ==========================================

    @GetMapping("/stats")
    public ResponseEntity<MaintenanceStatistics>
    getMaintenanceStatistics() {
        return ResponseEntity.ok(
                maintenanceService.getMaintenanceStatistics()
        );
    }

    // ==========================================
    // ADMIN: MAINTENANCE COUNT
    // ==========================================

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>>
    countMaintenance() {
        return ResponseEntity.ok(
                Map.of(
                        "totalMaintenance",
                        maintenanceService
                                .getMaintenanceStatistics()
                                .totalRecords()
                )
        );
    }

    // ==========================================
    // ADMIN: FILTER BY STATUS
    // ==========================================

    @GetMapping("/status/{status}")
    public ResponseEntity<List<MaintenanceProfile>>
    getMaintenanceByStatus(
            @PathVariable @NotBlank String status
    ) {
        return ResponseEntity.ok(
                maintenanceService.getMaintenanceByStatus(
                        status
                )
        );
    }

    // ==========================================
    // ADMIN: FILTER BY PRIORITY
    // ==========================================

    @GetMapping("/priority/{priority}")
    public ResponseEntity<List<MaintenanceProfile>>
    getMaintenanceByPriority(
            @PathVariable @NotBlank String priority
    ) {
        return ResponseEntity.ok(
                maintenanceService.getMaintenanceByPriority(
                        priority
                )
        );
    }

    // ==========================================
    // ADMIN: FILTER BY MAINTENANCE TYPE
    // ==========================================

    @GetMapping("/type/{type}")
    public ResponseEntity<List<MaintenanceProfile>>
    getMaintenanceByType(
            @PathVariable @NotBlank String type
    ) {
        return ResponseEntity.ok(
                maintenanceService.getMaintenanceByType(
                        type
                )
        );
    }

    // ==========================================
    // ADMIN: UPCOMING MAINTENANCE
    // ==========================================

    @GetMapping("/upcoming")
    public ResponseEntity<List<MaintenanceProfile>>
    getUpcomingMaintenance(
            @RequestParam(defaultValue = "30")
            @Min(0)
            int daysAhead
    ) {
        return ResponseEntity.ok(
                maintenanceService.getUpcomingMaintenance(
                        daysAhead
                )
        );
    }

    // ==========================================
    // ADMIN: OVERDUE MAINTENANCE
    // ==========================================

    @GetMapping("/overdue")
    public ResponseEntity<List<MaintenanceProfile>>
    getOverdueMaintenance() {
        return ResponseEntity.ok(
                maintenanceService.getOverdueMaintenance()
        );
    }

    // ==========================================
    // ADMIN: MAINTENANCE IN PROGRESS
    // ==========================================

    @GetMapping("/in-progress")
    public ResponseEntity<List<MaintenanceProfile>>
    getMaintenanceInProgress() {
        return ResponseEntity.ok(
                maintenanceService.getMaintenanceInProgress()
        );
    }

    // ==========================================
    // ADMIN: MAINTENANCE BY DATE RANGE
    // ==========================================

    @GetMapping("/between")
    public ResponseEntity<List<MaintenanceProfile>>
    getMaintenanceBetween(
            @RequestParam
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate start,

            @RequestParam
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate end
    ) {
        return ResponseEntity.ok(
                maintenanceService.getMaintenanceBetween(
                        start,
                        end
                )
        );
    }

    // ==========================================
    // ADMIN: VEHICLE MAINTENANCE HISTORY
    // ==========================================

    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<List<MaintenanceProfile>>
    getVehicleMaintenance(
            @PathVariable @Positive Long vehicleId
    ) {
        return ResponseEntity.ok(
                maintenanceService.getVehicleMaintenance(
                        vehicleId
                )
        );
    }

    // ==========================================
    // ADMIN: REPORTED MAINTENANCE
    // ==========================================

    @GetMapping("/reported-by/{userId}")
    public ResponseEntity<List<MaintenanceProfile>>
    getReportedMaintenance(
            @PathVariable @Positive Long userId
    ) {
        return ResponseEntity.ok(
                maintenanceService.getReportedMaintenance(
                        userId
                )
        );
    }

    // ==========================================
    // ADMIN: COMPLETED MAINTENANCE HISTORY
    // ==========================================

    @GetMapping("/completed-history")
    public ResponseEntity<List<MaintenanceProfile>>
    getCompletedHistory() {
        return ResponseEntity.ok(
                maintenanceService.getCompletedHistory()
        );
    }

    // ==========================================
    // ADMIN: TOTAL MAINTENANCE COST
    // ==========================================

    @GetMapping("/reports/total-cost")
    public ResponseEntity<Map<String, Object>>
    getTotalMaintenanceCost() {
        return ResponseEntity.ok(
                Map.of(
                        "currency", "LKR",
                        "totalCost",
                        maintenanceService.getTotalMaintenanceCost()
                )
        );
    }

    // ==========================================
    // ADMIN: MAINTENANCE COST BY DATE
    // ==========================================

    @GetMapping("/reports/cost-between")
    public ResponseEntity<Map<String, Object>>
    getMaintenanceCostBetween(
            @RequestParam
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate start,

            @RequestParam
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate end
    ) {
        BigDecimal cost =
                maintenanceService.getMaintenanceCostBetween(
                        start,
                        end
                );

        return ResponseEntity.ok(
                Map.of(
                        "start", start.toString(),
                        "end", end.toString(),
                        "currency", "LKR",
                        "totalCost", cost
                )
        );
    }

    // ==========================================
    // ADMIN: COST OF A SPECIFIC VEHICLE
    // ==========================================

    @GetMapping("/reports/vehicle/{vehicleId}")
    public ResponseEntity<Map<String, Object>>
    getMaintenanceCostByVehicle(
            @PathVariable @Positive Long vehicleId
    ) {
        return ResponseEntity.ok(
                Map.of(
                        "vehicleId", vehicleId,
                        "currency", "LKR",
                        "totalCost",
                        maintenanceService
                                .getMaintenanceCostByVehicle(
                                        vehicleId
                                )
                )
        );
    }

    // ==========================================
    // ADMIN: COST GROUPED BY VEHICLE
    // ==========================================

    @GetMapping("/reports/by-vehicle")
    public ResponseEntity<List<MaintenanceCostByVehicle>>
    getCostsByVehicle() {
        return ResponseEntity.ok(
                maintenanceService.getCostsByVehicle()
        );
    }

    // ==========================================
    // ADMIN: COST GROUPED BY TYPE
    // ==========================================

    @GetMapping("/reports/by-type")
    public ResponseEntity<List<MaintenanceCostByType>>
    getCostsByType() {
        return ResponseEntity.ok(
                maintenanceService.getCostsByType()
        );
    }

    // ==========================================
    // MAINTENANCE DETAILS BY ID
    // ==========================================

    @GetMapping("/{maintenanceId}")
    public ResponseEntity<MaintenanceProfile>
    getMaintenance(
            @PathVariable @Positive Long maintenanceId
    ) {
        return ResponseEntity.ok(
                maintenanceService.getMaintenanceProfile(
                        maintenanceId
                )
        );
    }

    // ==========================================
    // ADMIN: CREATE MAINTENANCE
    // ==========================================

    @PostMapping
    public ResponseEntity<MaintenanceProfile>
    createMaintenance(
            @Valid @RequestBody CreateMaintenancePayload request
    ) {
        MaintenanceProfile maintenance =
                maintenanceService.createMaintenance(
                        new CreateMaintenanceRequest(
                                request.vehicleId(),
                                request.maintenanceType(),
                                request.description(),
                                request.priority(),
                                request.scheduledDate(),
                                request.serviceProvider(),
                                request.estimatedCost(),
                                request.odometerReading(),
                                request.notes(),
                                request.reportedByUserId()
                        )
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(maintenance);
    }

    // ==========================================
    // ADMIN: UPDATE SCHEDULED MAINTENANCE
    // ==========================================

    @PutMapping("/{maintenanceId}")
    public ResponseEntity<MaintenanceProfile>
    updateMaintenance(
            @PathVariable @Positive Long maintenanceId,
            @Valid @RequestBody UpdateMaintenancePayload request
    ) {
        return ResponseEntity.ok(
                maintenanceService.updateMaintenance(
                        maintenanceId,
                        new UpdateMaintenanceRequest(
                                request.maintenanceType(),
                                request.description(),
                                request.priority(),
                                request.scheduledDate(),
                                request.serviceProvider(),
                                request.estimatedCost(),
                                request.odometerReading(),
                                request.notes()
                        )
                )
        );
    }

    // ==========================================
    // ADMIN: START MAINTENANCE
    // ==========================================

    @PatchMapping("/{maintenanceId}/start")
    public ResponseEntity<MaintenanceProfile>
    startMaintenance(
            @PathVariable @Positive Long maintenanceId
    ) {
        return ResponseEntity.ok(
                maintenanceService.startMaintenance(
                        maintenanceId
                )
        );
    }

    // ==========================================
    // ADMIN: COMPLETE MAINTENANCE
    // ==========================================

    @PatchMapping("/{maintenanceId}/complete")
    public ResponseEntity<MaintenanceProfile>
    completeMaintenance(
            @PathVariable @Positive Long maintenanceId,
            @Valid @RequestBody CompleteMaintenancePayload request
    ) {
        return ResponseEntity.ok(
                maintenanceService.completeMaintenance(
                        maintenanceId,
                        new CompleteMaintenanceRequest(
                                request.actualCost(),
                                request.odometerReading(),
                                request.nextServiceDate(),
                                request.notes()
                        )
                )
        );
    }

    // ==========================================
    // ADMIN: CANCEL SCHEDULED MAINTENANCE
    // ==========================================

    @PatchMapping("/{maintenanceId}/cancel")
    public ResponseEntity<MaintenanceProfile>
    cancelMaintenance(
            @PathVariable @Positive Long maintenanceId
    ) {
        return ResponseEntity.ok(
                maintenanceService.cancelMaintenance(
                        maintenanceId
                )
        );
    }

    // ==========================================
    // REQUEST DTOs
    // ==========================================

    public record CreateMaintenancePayload(

            @NotNull(message = "Vehicle ID is required")
            @Positive
            Long vehicleId,

            @NotBlank(message = "Maintenance type is required")
            String maintenanceType,

            @NotBlank(message = "Description is required")
            @Size(max = 1000)
            String description,

            String priority,

            @NotNull(message = "Scheduled date is required")
            LocalDate scheduledDate,

            @Size(max = 150)
            String serviceProvider,

            @DecimalMin("0.00")
            BigDecimal estimatedCost,

            @Min(0)
            Integer odometerReading,

            @Size(max = 1000)
            String notes,

            @Positive
            Long reportedByUserId

    ) {
    }

    public record UpdateMaintenancePayload(

            String maintenanceType,

            @Size(max = 1000)
            String description,

            String priority,

            LocalDate scheduledDate,

            @Size(max = 150)
            String serviceProvider,

            @DecimalMin("0.00")
            BigDecimal estimatedCost,

            @Min(0)
            Integer odometerReading,

            @Size(max = 1000)
            String notes

    ) {
    }

    public record CompleteMaintenancePayload(

            @NotNull(message = "Actual cost is required")
            @DecimalMin("0.00")
            BigDecimal actualCost,

            @Min(0)
            Integer odometerReading,

            LocalDate nextServiceDate,

            @Size(max = 1000)
            String notes

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
