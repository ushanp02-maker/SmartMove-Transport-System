
package com.smartmove.backend.controller;

import com.smartmove.backend.service.RouteService;
import com.smartmove.backend.service.RouteService.CreateRouteRequest;
import com.smartmove.backend.service.RouteService.UpdateRouteRequest;
import com.smartmove.backend.service.RouteService.CreateStopRequest;
import com.smartmove.backend.service.RouteService.UpdateStopRequest;
import com.smartmove.backend.service.RouteService.RouteProfile;
import com.smartmove.backend.service.RouteService.RouteStopProfile;
import com.smartmove.backend.service.RouteService.RouteDetails;
import com.smartmove.backend.service.RouteService.RouteSegment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/routes")
@Validated
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    // ==========================================
    // ADMIN: ALL ROUTES
    // ==========================================

    @GetMapping
    public ResponseEntity<List<RouteProfile>> getAllRoutes() {
        return ResponseEntity.ok(
                routeService.getAllRoutes()
        );
    }

    // ==========================================
    // ADMIN: PAGINATED ROUTES
    // ==========================================

    @GetMapping("/page")
    public ResponseEntity<Page<RouteProfile>> getRoutesPage(
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
                routeService.getRoutes(pageable)
        );
    }

    // ==========================================
    // PASSENGER: ACTIVE ROUTES
    // ==========================================

    @GetMapping("/active")
    public ResponseEntity<List<RouteProfile>> getActiveRoutes() {
        return ResponseEntity.ok(
                routeService.getActiveRoutes()
        );
    }

    // ==========================================
    // SEARCH ROUTES
    // ==========================================

    @GetMapping("/search")
    public ResponseEntity<List<RouteProfile>> searchRoutes(
            @RequestParam(required = false) String keyword
    ) {
        return ResponseEntity.ok(
                routeService.searchRoutes(keyword)
        );
    }

    // ==========================================
    // PASSENGER: FIND ROUTES BETWEEN STOPS
    // ==========================================

    @GetMapping("/between-stops")
    public ResponseEntity<List<RouteDetails>> findRoutesBetweenStops(
            @RequestParam @NotBlank String boardingStop,
            @RequestParam @NotBlank String destinationStop
    ) {
        return ResponseEntity.ok(
                routeService.findRoutesBetweenStops(
                        boardingStop,
                        destinationStop
                )
        );
    }

    // ==========================================
    // ROUTE STATISTICS
    // ==========================================

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getRouteStatistics() {
        return ResponseEntity.ok(
                Map.of(
                        "totalRoutes",
                        routeService.countRoutes(),
                        "activeRoutes",
                        routeService.countActiveRoutes()
                )
        );
    }

    // ==========================================
    // ROUTE PROFILE BY ID
    // ==========================================

    @GetMapping("/{routeId}")
    public ResponseEntity<RouteProfile> getRoute(
            @PathVariable Long routeId
    ) {
        return ResponseEntity.ok(
                routeService.getRouteProfile(routeId)
        );
    }

    // ==========================================
    // ROUTE WITH ORDERED STOPS
    // ==========================================

    @GetMapping("/{routeId}/details")
    public ResponseEntity<RouteDetails> getRouteDetails(
            @PathVariable Long routeId
    ) {
        return ResponseEntity.ok(
                routeService.getRouteDetails(routeId)
        );
    }

    // ==========================================
    // PASSENGER: SEGMENT DISTANCE AND FARE
    // ==========================================

    @GetMapping("/{routeId}/segment")
    public ResponseEntity<RouteSegment> calculateRouteSegment(
            @PathVariable Long routeId,
            @RequestParam Long boardingStopId,
            @RequestParam Long destinationStopId
    ) {
        return ResponseEntity.ok(
                routeService.calculateRouteSegment(
                        routeId,
                        boardingStopId,
                        destinationStopId
                )
        );
    }

    // ==========================================
    // ADMIN: CREATE ROUTE
    // ==========================================

    @PostMapping
    public ResponseEntity<RouteProfile> createRoute(
            @Valid @RequestBody CreateRoutePayload request
    ) {
        RouteProfile route = routeService.createRoute(
                new CreateRouteRequest(
                        request.name(),
                        request.origin(),
                        request.destination(),
                        request.distanceKm(),
                        request.durationMinutes(),
                        request.baseFare(),
                        request.serviceType()
                )
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(route);
    }

    // ==========================================
    // ADMIN: UPDATE ROUTE
    // ==========================================

    @PutMapping("/{routeId}")
    public ResponseEntity<RouteProfile> updateRoute(
            @PathVariable Long routeId,
            @Valid @RequestBody UpdateRoutePayload request
    ) {
        return ResponseEntity.ok(
                routeService.updateRoute(
                        routeId,
                        new UpdateRouteRequest(
                                request.name(),
                                request.origin(),
                                request.destination(),
                                request.distanceKm(),
                                request.durationMinutes(),
                                request.baseFare(),
                                request.serviceType()
                        )
                )
        );
    }

    // ==========================================
    // ADMIN: ACTIVATE / DEACTIVATE ROUTE
    // ==========================================

    @PatchMapping("/{routeId}/status")
    public ResponseEntity<RouteProfile> updateRouteStatus(
            @PathVariable Long routeId,
            @Valid @RequestBody RouteStatusPayload request
    ) {
        return ResponseEntity.ok(
                routeService.updateRouteStatus(
                        routeId,
                        request.status()
                )
        );
    }

    // ==========================================
    // ADMIN: ADD ROUTE STOP
    // ==========================================

    @PostMapping("/{routeId}/stops")
    public ResponseEntity<RouteStopProfile> addRouteStop(
            @PathVariable Long routeId,
            @Valid @RequestBody CreateStopPayload request
    ) {
        RouteStopProfile stop = routeService.addRouteStop(
                routeId,
                new CreateStopRequest(
                        request.stopName(),
                        request.stopOrder(),
                        request.latitude(),
                        request.longitude(),
                        request.distanceFromStartKm(),
                        request.minutesFromStart()
                )
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(stop);
    }

    // ==========================================
    // ADMIN: UPDATE ROUTE STOP
    // ==========================================

    @PutMapping("/{routeId}/stops/{stopId}")
    public ResponseEntity<RouteStopProfile> updateRouteStop(
            @PathVariable Long routeId,
            @PathVariable Long stopId,
            @Valid @RequestBody UpdateStopPayload request
    ) {
        return ResponseEntity.ok(
                routeService.updateRouteStop(
                        routeId,
                        stopId,
                        new UpdateStopRequest(
                                request.stopName(),
                                request.stopOrder(),
                                request.latitude(),
                                request.longitude(),
                                request.distanceFromStartKm(),
                                request.minutesFromStart()
                        )
                )
        );
    }

    // ==========================================
    // ADMIN: DELETE ROUTE STOP
    // ==========================================

    @DeleteMapping("/{routeId}/stops/{stopId}")
    public ResponseEntity<Void> deleteRouteStop(
            @PathVariable Long routeId,
            @PathVariable Long stopId
    ) {
        routeService.deleteRouteStop(routeId, stopId);

        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // REQUEST DTOs
    // ==========================================

    public record CreateRoutePayload(

            @NotBlank(message = "Route name is required")
            @Size(max = 150)
            String name,

            @NotBlank(message = "Origin is required")
            @Size(max = 120)
            String origin,

            @NotBlank(message = "Destination is required")
            @Size(max = 120)
            String destination,

            @Positive
            BigDecimal distanceKm,

            @Positive
            Integer durationMinutes,

            @NotNull(message = "Base fare is required")
            @PositiveOrZero
            BigDecimal baseFare,

            String serviceType

    ) {
    }

    public record UpdateRoutePayload(

            @Size(max = 150)
            String name,

            @Size(max = 120)
            String origin,

            @Size(max = 120)
            String destination,

            @Positive
            BigDecimal distanceKm,

            @Positive
            Integer durationMinutes,

            @PositiveOrZero
            BigDecimal baseFare,

            String serviceType

    ) {
    }

    public record RouteStatusPayload(

            @NotBlank(message = "Route status is required")
            String status

    ) {
    }

    public record CreateStopPayload(

            @NotBlank(message = "Stop name is required")
            @Size(max = 120)
            String stopName,

            @NotNull(message = "Stop order is required")
            @Min(0)
            Integer stopOrder,

            @DecimalMin("-90.0")
            @DecimalMax("90.0")
            BigDecimal latitude,

            @DecimalMin("-180.0")
            @DecimalMax("180.0")
            BigDecimal longitude,

            @PositiveOrZero
            BigDecimal distanceFromStartKm,

            @Min(0)
            Integer minutesFromStart

    ) {
    }

    public record UpdateStopPayload(

            @Size(max = 120)
            String stopName,

            @Min(0)
            Integer stopOrder,

            @DecimalMin("-90.0")
            @DecimalMax("90.0")
            BigDecimal latitude,

            @DecimalMin("-180.0")
            @DecimalMax("180.0")
            BigDecimal longitude,

            @PositiveOrZero
            BigDecimal distanceFromStartKm,

            @Min(0)
            Integer minutesFromStart

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
