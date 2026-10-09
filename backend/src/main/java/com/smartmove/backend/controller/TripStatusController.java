
package com.smartmove.backend.controller;

import com.smartmove.backend.service.CurrentUserService;
import com.smartmove.backend.service.TripStatusService;

import com.smartmove.backend.service.TripStatusService.TripStatusProfile;
import com.smartmove.backend.service.TripStatusService.TripStatusStatistics;

import jakarta.validation.constraints.Positive;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trip-status")
@Validated
public class TripStatusController {

    private final TripStatusService tripStatusService;
    private final CurrentUserService currentUserService;

    public TripStatusController(
            TripStatusService tripStatusService,
            CurrentUserService currentUserService
    ) {
        this.tripStatusService = tripStatusService;
        this.currentUserService = currentUserService;
    }

    // ==========================================
    // DRIVER: START ASSIGNED TRIP
    // ==========================================

    @PatchMapping("/driver/trips/{tripId}/start")
    public ResponseEntity<TripStatusProfile> startTrip(
            @PathVariable @Positive Long tripId
    ) {
        currentUserService.requireDriver();

        return ResponseEntity.ok(
                tripStatusService.startTrip(tripId)
        );
    }

    // ==========================================
    // DRIVER: PAUSE ASSIGNED TRIP
    // ==========================================

    @PatchMapping("/driver/trips/{tripId}/pause")
    public ResponseEntity<TripStatusProfile> pauseTrip(
            @PathVariable @Positive Long tripId
    ) {
        currentUserService.requireDriver();

        return ResponseEntity.ok(
                tripStatusService.pauseTrip(tripId)
        );
    }

    // ==========================================
    // DRIVER: RESUME ASSIGNED TRIP
    // ==========================================

    @PatchMapping("/driver/trips/{tripId}/resume")
    public ResponseEntity<TripStatusProfile> resumeTrip(
            @PathVariable @Positive Long tripId
    ) {
        currentUserService.requireDriver();

        return ResponseEntity.ok(
                tripStatusService.resumeTrip(tripId)
        );
    }

    // ==========================================
    // DRIVER: COMPLETE ASSIGNED TRIP
    // ==========================================

    @PatchMapping("/driver/trips/{tripId}/complete")
    public ResponseEntity<TripStatusProfile> completeTrip(
            @PathVariable @Positive Long tripId
    ) {
        currentUserService.requireDriver();

        return ResponseEntity.ok(
                tripStatusService.completeTrip(tripId)
        );
    }

    // ==========================================
    // DRIVER: MY ASSIGNED TRIPS
    // ==========================================

    @GetMapping("/driver/my-trips")
    public ResponseEntity<List<TripStatusProfile>> getMyTrips() {

        currentUserService.requireDriver();

        return ResponseEntity.ok(
                tripStatusService.getMyTrips()
        );
    }

    // ==========================================
    // AUTHENTICATED USER: TRIP STATUS
    // ==========================================

    @GetMapping("/trips/{tripId}")
    public ResponseEntity<TripStatusProfile> getTripStatus(
            @PathVariable @Positive Long tripId
    ) {
        return ResponseEntity.ok(
                tripStatusService.getTripStatus(tripId)
        );
    }

    // ==========================================
    // ADMIN: ALL ACTIVE TRIPS
    // ==========================================

    @GetMapping("/admin/active")
    public ResponseEntity<List<TripStatusProfile>> getActiveTrips() {

        currentUserService.requireAdmin();

        return ResponseEntity.ok(
                tripStatusService.getActiveTrips()
        );
    }

    // ==========================================
    // ADMIN: TRIP STATUS STATISTICS
    // ==========================================

    @GetMapping("/admin/stats")
    public ResponseEntity<TripStatusStatistics> getStatistics() {

        currentUserService.requireAdmin();

        return ResponseEntity.ok(
                tripStatusService.getStatistics()
        );
    }

    // ==========================================
    // ADMIN: CANCEL TRIP
    // ==========================================

    @PatchMapping("/admin/trips/{tripId}/cancel")
    public ResponseEntity<TripStatusProfile> cancelTrip(
            @PathVariable @Positive Long tripId
    ) {
        currentUserService.requireAdmin();

        return ResponseEntity.ok(
                tripStatusService.cancelTrip(tripId)
        );
    }

    // ==========================================
    // ADMIN: FORCE COMPLETE TRIP
    // ==========================================

    @PatchMapping("/admin/trips/{tripId}/complete")
    public ResponseEntity<TripStatusProfile> forceCompleteTrip(
            @PathVariable @Positive Long tripId
    ) {
        currentUserService.requireAdmin();

        return ResponseEntity.ok(
                tripStatusService.forceCompleteTrip(tripId)
        );
    }
}
