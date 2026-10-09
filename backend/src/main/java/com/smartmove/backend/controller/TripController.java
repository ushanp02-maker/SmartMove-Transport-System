
package com.smartmove.backend.controller;

import com.smartmove.backend.service.TripService;
import com.smartmove.backend.service.TripService.CreateTripRequest;
import com.smartmove.backend.service.TripService.UpdateTripRequest;
import com.smartmove.backend.service.TripService.TripProfile;
import com.smartmove.backend.service.TripService.TripStatistics;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trips")
@Validated
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    // ==========================================
    // ADMIN: ALL TRIPS
    // ==========================================

    @GetMapping
    public ResponseEntity<List<TripProfile>> getAllTrips() {
        return ResponseEntity.ok(
                tripService.getAllTrips()
        );
    }

    // ==========================================
    // ADMIN: PAGINATED TRIPS
    // ==========================================

    @GetMapping("/page")
    public ResponseEntity<Page<TripProfile>> getTripsPage(
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
                        "departureTime"
                )
        );

        return ResponseEntity.ok(
                tripService.getTrips(pageable)
        );
    }

    // ==========================================
    // PASSENGER: UPCOMING TRIPS
    // ==========================================

    @GetMapping("/upcoming")
    public ResponseEntity<List<TripProfile>> getUpcomingTrips() {
        return ResponseEntity.ok(
                tripService.getUpcomingTrips()
        );
    }

    // ==========================================
    // PASSENGER: UPCOMING TRIPS BY ROUTE
    // ==========================================

    @GetMapping("/route/{routeId}/upcoming")
    public ResponseEntity<List<TripProfile>> getUpcomingTripsByRoute(
            @PathVariable @Positive Long routeId
    ) {
        return ResponseEntity.ok(
                tripService.getUpcomingTripsByRoute(routeId)
        );
    }

    // ==========================================
    // ADMIN: FILTER TRIPS BY STATUS
    // ==========================================

    @GetMapping("/status/{status}")
    public ResponseEntity<List<TripProfile>> getTripsByStatus(
            @PathVariable String status
    ) {
        return ResponseEntity.ok(
                tripService.getTripsByStatus(status)
        );
    }

    // ==========================================
    // ADMIN: TRIPS WITHIN DATE RANGE
    // ==========================================

    @GetMapping("/between")
    public ResponseEntity<List<TripProfile>> getTripsBetween(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime start,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime end
    ) {
        return ResponseEntity.ok(
                tripService.getTripsBetween(start, end)
        );
    }

    // ==========================================
    // DRIVER: ASSIGNED TRIPS
    // ==========================================

    @GetMapping("/driver/{driverId}")
    public ResponseEntity<List<TripProfile>> getDriverTrips(
            @PathVariable @Positive Long driverId
    ) {
        return ResponseEntity.ok(
                tripService.getDriverTrips(driverId)
        );
    }

    // ==========================================
    // ADMIN: VEHICLE TRIP HISTORY
    // ==========================================

    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<List<TripProfile>> getVehicleTrips(
            @PathVariable @Positive Long vehicleId
    ) {
        return ResponseEntity.ok(
                tripService.getVehicleTrips(vehicleId)
        );
    }

    // ==========================================
    // ADMIN: TRIP STATISTICS
    // ==========================================

    @GetMapping("/stats")
    public ResponseEntity<TripStatistics> getTripStatistics() {
        return ResponseEntity.ok(
                tripService.getTripStatistics()
        );
    }

    // ==========================================
    // ADMIN: TRIP COUNT
    // ==========================================

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> countTrips() {
        return ResponseEntity.ok(
                Map.of(
                        "totalTrips",
                        tripService.getTripStatistics().totalTrips()
                )
        );
    }

    // ==========================================
    // TRIP PROFILE BY ID
    // ==========================================

    @GetMapping("/{tripId}")
    public ResponseEntity<TripProfile> getTrip(
            @PathVariable @Positive Long tripId
    ) {
        return ResponseEntity.ok(
                tripService.getTripProfile(tripId)
        );
    }

    // ==========================================
    // ADMIN: CREATE TRIP
    // ==========================================

    @PostMapping
    public ResponseEntity<TripProfile> createTrip(
            @Valid @RequestBody CreateTripPayload request
    ) {
        TripProfile trip = tripService.createTrip(
                new CreateTripRequest(
                        request.routeId(),
                        request.vehicleId(),
                        request.driverId(),
                        request.departureTime(),
                        request.arrivalTime(),
                        request.fare()
                )
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(trip);
    }

    // ==========================================
    // ADMIN: UPDATE SCHEDULED TRIP
    // ==========================================

    @PutMapping("/{tripId}")
    public ResponseEntity<TripProfile> updateTrip(
            @PathVariable @Positive Long tripId,
            @Valid @RequestBody UpdateTripPayload request
    ) {
        return ResponseEntity.ok(
                tripService.updateTrip(
                        tripId,
                        new UpdateTripRequest(
                                request.routeId(),
                                request.vehicleId(),
                                request.driverId(),
                                request.departureTime(),
                                request.arrivalTime(),
                                request.fare()
                        )
                )
        );
    }

    // ==========================================
    // ADMIN / AUTHORIZED DRIVER:
    // UPDATE TRIP STATUS
    // ==========================================

    @PatchMapping("/{tripId}/status")
    public ResponseEntity<TripProfile> updateTripStatus(
            @PathVariable @Positive Long tripId,
            @Valid @RequestBody TripStatusPayload request
    ) {
        return ResponseEntity.ok(
                tripService.updateTripStatus(
                        tripId,
                        request.status()
                )
        );
    }

    // ==========================================
    // REQUEST DTOs
    // ==========================================

    public record CreateTripPayload(

            @NotNull(message = "Route ID is required")
            @Positive
            Long routeId,

            @NotNull(message = "Vehicle ID is required")
            @Positive
            Long vehicleId,

            @NotNull(message = "Driver ID is required")
            @Positive
            Long driverId,

            @NotNull(message = "Departure time is required")
            @Future(message = "Departure must be in the future")
            LocalDateTime departureTime,

            @NotNull(message = "Arrival time is required")
            LocalDateTime arrivalTime,

            @DecimalMin("0.00")
            BigDecimal fare

    ) {
    }

    public record UpdateTripPayload(

            @Positive
            Long routeId,

            @Positive
            Long vehicleId,

            @Positive
            Long driverId,

            LocalDateTime departureTime,

            LocalDateTime arrivalTime,

            @DecimalMin("0.00")
            BigDecimal fare

    ) {
    }

    public record TripStatusPayload(

            @NotBlank(message = "Trip status is required")
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
