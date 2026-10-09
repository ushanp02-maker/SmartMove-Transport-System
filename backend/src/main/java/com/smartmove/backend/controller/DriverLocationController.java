
package com.smartmove.backend.controller;

import com.smartmove.backend.service.CurrentUserService;
import com.smartmove.backend.service.DriverLocationService;

import com.smartmove.backend.service.DriverLocationService.SubmitLocationRequest;
import com.smartmove.backend.service.DriverLocationService.LocationProfile;
import com.smartmove.backend.service.DriverLocationService.LiveTripLocation;
import com.smartmove.backend.service.DriverLocationService.TrackingStatistics;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tracking")
@Validated
public class DriverLocationController {

    private final DriverLocationService locationService;
    private final CurrentUserService currentUserService;

    public DriverLocationController(
            DriverLocationService locationService,
            CurrentUserService currentUserService
    ) {
        this.locationService = locationService;
        this.currentUserService = currentUserService;
    }

    // ==========================================
    // DRIVER: SUBMIT GPS UPDATE
    // ==========================================

    @PostMapping("/location")
    public ResponseEntity<LocationProfile> submitLocation(
            @Valid @RequestBody LocationUpdatePayload request
    ) {
        currentUserService.requireDriver();

        LocationProfile saved = locationService.submitLocation(
                new SubmitLocationRequest(
                        request.tripId(),
                        request.latitude(),
                        request.longitude(),
                        request.accuracyMeters(),
                        request.speedKmh(),
                        request.headingDegrees(),
                        request.altitudeMeters(),
                        request.moving(),
                        request.recordedAt()
                )
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(saved);
    }

    // ==========================================
    // PASSENGER / DRIVER / ADMIN:
    // LATEST LOCATION FOR AN AUTHORIZED TRIP
    // ==========================================

    @GetMapping("/trips/{tripId}/live")
    public ResponseEntity<LiveTripLocation> getLiveTripLocation(
            @PathVariable @Positive Long tripId
    ) {
        return ResponseEntity.ok(
                locationService.getLiveTripLocation(tripId)
        );
    }

    // ==========================================
    // DRIVER: MY LATEST LOCATION
    // ==========================================

    @GetMapping("/me/latest")
    public ResponseEntity<LocationProfile> getMyLatestLocation() {

        currentUserService.requireDriver();

        return ResponseEntity.ok(
                locationService.getMyLatestLocation()
        );
    }

    // ==========================================
    // DRIVER: MY TRIP LOCATION HISTORY
    // ==========================================

    @GetMapping("/me/trips/{tripId}/history")
    public ResponseEntity<List<LocationProfile>> getMyTripHistory(
            @PathVariable @Positive Long tripId
    ) {
        currentUserService.requireDriver();

        return ResponseEntity.ok(
                locationService.getMyTripHistory(tripId)
        );
    }

    // ==========================================
    // ADMIN: TRIP LOCATION HISTORY
    // ==========================================

    @GetMapping("/admin/trips/{tripId}/history")
    public ResponseEntity<List<LocationProfile>> getTripHistory(
            @PathVariable @Positive Long tripId
    ) {
        currentUserService.requireAdmin();

        return ResponseEntity.ok(
                locationService.getTripHistory(tripId)
        );
    }

    // ==========================================
    // ADMIN: PAGINATED TRIP HISTORY
    // ==========================================

    @GetMapping("/admin/trips/{tripId}/history/page")
    public ResponseEntity<Page<LocationProfile>> getTripHistoryPage(
            @PathVariable @Positive Long tripId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        currentUserService.requireAdmin();

        return ResponseEntity.ok(
                locationService.getTripHistoryPage(
                        tripId,
                        createPageable(page, size)
                )
        );
    }

    // ==========================================
    // ADMIN: DRIVER TRACKING HISTORY
    // ==========================================

    @GetMapping("/admin/drivers/{driverId}/history")
    public ResponseEntity<Page<LocationProfile>> getDriverHistory(
            @PathVariable @Positive Long driverId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        currentUserService.requireAdmin();

        return ResponseEntity.ok(
                locationService.getDriverHistory(
                        driverId,
                        createPageable(page, size)
                )
        );
    }

    // ==========================================
    // ADMIN: VEHICLE TRACKING HISTORY
    // ==========================================

    @GetMapping("/admin/vehicles/{vehicleId}/history")
    public ResponseEntity<Page<LocationProfile>> getVehicleHistory(
            @PathVariable @Positive Long vehicleId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        currentUserService.requireAdmin();

        return ResponseEntity.ok(
                locationService.getVehicleHistory(
                        vehicleId,
                        createPageable(page, size)
                )
        );
    }

    // ==========================================
    // ADMIN: TRIP TRACKING STATISTICS
    // ==========================================

    @GetMapping("/admin/trips/{tripId}/stats")
    public ResponseEntity<TrackingStatistics> getTrackingStatistics(
            @PathVariable @Positive Long tripId
    ) {
        currentUserService.requireAdmin();

        return ResponseEntity.ok(
                locationService.getTrackingStatistics(tripId)
        );
    }

    // ==========================================
    // SUPER ADMIN: DELETE TRACKING HISTORY
    // ==========================================

    @DeleteMapping("/admin/trips/{tripId}/history")
    public ResponseEntity<Map<String, Object>> deleteTripHistory(
            @PathVariable @Positive Long tripId
    ) {
        currentUserService.requireSuperAdmin();

        long deletedCount =
                locationService.deleteTripHistory(tripId);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Trip tracking history deleted",
                        "tripId",
                        tripId,
                        "deletedRecords",
                        deletedCount
                )
        );
    }

    // ==========================================
    // REQUEST VALIDATION
    // ==========================================

    public record LocationUpdatePayload(

            @NotNull(message = "Trip ID is required")
            @Positive
            Long tripId,

            @NotNull(message = "Latitude is required")
            @DecimalMin("-90.0")
            @DecimalMax("90.0")
            Double latitude,

            @NotNull(message = "Longitude is required")
            @DecimalMin("-180.0")
            @DecimalMax("180.0")
            Double longitude,

            @DecimalMin("0.0")
            @DecimalMax("1000.0")
            Double accuracyMeters,

            @DecimalMin("0.0")
            @DecimalMax("200.0")
            Double speedKmh,

            @DecimalMin("0.0")
            @DecimalMax("359.999999")
            Double headingDegrees,

            @DecimalMin("-500.0")
            @DecimalMax("10000.0")
            Double altitudeMeters,

            Boolean moving,

            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME
            )
            LocalDateTime recordedAt

    ) {}

    // ==========================================
    // PAGINATION HELPER
    // ==========================================

    private Pageable createPageable(
            int page,
            int size
    ) {
        if (page < 0 || size < 1 || size > 200) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page must be nonnegative and size "
                            + "must be between 1 and 200"
            );
        }

        return PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Direction.DESC,
                        "recordedAt"
                )
        );
    }
}
