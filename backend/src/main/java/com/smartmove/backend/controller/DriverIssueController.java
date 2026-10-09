package com.smartmove.backend.controller;

import com.smartmove.backend.service.CurrentUserService;
import com.smartmove.backend.service.MaintenanceService;
import com.smartmove.backend.service.TripStatusService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/driver-issues")
public class DriverIssueController {
    private final CurrentUserService users;
    private final TripStatusService trips;
    private final MaintenanceService maintenance;

    public DriverIssueController(CurrentUserService users, TripStatusService trips, MaintenanceService maintenance) {
        this.users = users;
        this.trips = trips;
        this.maintenance = maintenance;
    }

    public record IssueRequest(@NotNull @Positive Long tripId,
                               @NotBlank @Size(max=1000) String description,
                               String priority) {}

    @PostMapping
    public ResponseEntity<MaintenanceService.MaintenanceProfile> report(@Valid @RequestBody IssueRequest request) {
        users.requireDriver();
        var assigned = trips.getMyTrips().stream()
            .filter(t -> t.tripId().equals(request.tripId()))
            .findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Trip is not assigned to this driver"));
        if (assigned.vehicleId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assigned trip has no vehicle");
        }
        var created = maintenance.createMaintenance(new MaintenanceService.CreateMaintenanceRequest(
            assigned.vehicleId(), "REPAIR", request.description(),
            request.priority() == null ? "HIGH" : request.priority(),
            LocalDate.now(), null, null, null,
            "Driver issue for trip #" + assigned.tripId(), users.getCurrentAccountId()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
