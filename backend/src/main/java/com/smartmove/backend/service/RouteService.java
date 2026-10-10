
package com.smartmove.backend.service;

import com.smartmove.backend.entity.Route;
import com.smartmove.backend.entity.RouteStop;
import com.smartmove.backend.repository.RouteRepository;
import com.smartmove.backend.repository.RouteStopRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class RouteService {

    private static final Set<String> VALID_STATUSES =
            Set.of("ACTIVE", "INACTIVE", "SUSPENDED");

    private static final Set<String> VALID_SERVICE_TYPES =
            Set.of(
                    "COMMUTER",
                    "EXPRESS",
                    "STAFF",
                    "PRIVATE",
                    "SPECIAL"
            );

    private final RouteRepository routeRepository;
    private final RouteStopRepository stopRepository;

    public RouteService(
            RouteRepository routeRepository,
            RouteStopRepository stopRepository
    ) {
        this.routeRepository = routeRepository;
        this.stopRepository = stopRepository;
    }

    // ==========================================
    // DATA TRANSFER OBJECTS
    // ==========================================

    public record RouteProfile(
            Long id,
            String name,
            String origin,
            String destination,
            BigDecimal distanceKm,
            Integer durationMinutes,
            BigDecimal baseFare,
            String serviceType,
            String status
    ) {
    }

    public record RouteStopProfile(
            Long id,
            Long routeId,
            String stopName,
            Integer stopOrder,
            BigDecimal latitude,
            BigDecimal longitude,
            BigDecimal distanceFromStartKm,
            Integer minutesFromStart
    ) {
    }

    public record RouteDetails(
            RouteProfile route,
            List<RouteStopProfile> stops
    ) {
    }

    public record CreateRouteRequest(
            String name,
            String origin,
            String destination,
            BigDecimal distanceKm,
            Integer durationMinutes,
            BigDecimal baseFare,
            String serviceType
    ) {
    }

    public record UpdateRouteRequest(
            String name,
            String origin,
            String destination,
            BigDecimal distanceKm,
            Integer durationMinutes,
            BigDecimal baseFare,
            String serviceType
    ) {
    }

    public record CreateStopRequest(
            String stopName,
            Integer stopOrder,
            BigDecimal latitude,
            BigDecimal longitude,
            BigDecimal distanceFromStartKm,
            Integer minutesFromStart
    ) {
    }

    public record UpdateStopRequest(
            String stopName,
            Integer stopOrder,
            BigDecimal latitude,
            BigDecimal longitude,
            BigDecimal distanceFromStartKm,
            Integer minutesFromStart
    ) {
    }

    public record RouteSegment(
            Long routeId,
            Long boardingStopId,
            Long destinationStopId,
            String boardingStop,
            String destinationStop,
            BigDecimal distanceKm,
            Integer durationMinutes,
            BigDecimal estimatedFare
    ) {
    }

    // ==========================================
    // ROUTE LOOKUP
    // ==========================================

    @Transactional(readOnly = true)
    public Route getRouteById(Long routeId) {
        if (routeId == null) {
            throw badRequest("Route ID is required");
        }

        return routeRepository.findById(routeId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Route not found"
                        )
                );
    }

    @Transactional(readOnly = true)
    public RouteProfile getRouteProfile(Long routeId) {
        return toProfile(getRouteById(routeId));
    }

    @Transactional(readOnly = true)
    public RouteDetails getRouteDetails(Long routeId) {
        Route route = getRouteById(routeId);

        List<RouteStopProfile> stops =
                stopRepository
                        .findByRouteIdOrderByStopOrderAsc(routeId)
                        .stream()
                        .map(this::toStopProfile)
                        .toList();

        return new RouteDetails(
                toProfile(route),
                stops
        );
    }

    // ==========================================
    // ADMIN: CREATE ROUTE
    // ==========================================

    @Transactional
    public RouteProfile createRoute(
            CreateRouteRequest request
    ) {
        if (request == null) {
            throw badRequest("Route details are required");
        }

        validateDistance(request.distanceKm());
        validateDuration(request.durationMinutes());
        validateFare(request.baseFare());

        Route route = new Route();

        route.setName(requireText(request.name(), "Route name"));
        route.setOrigin(
                requireText(request.origin(), "Origin")
        );
        route.setDestination(
                requireText(request.destination(), "Destination")
        );
        route.setDistanceKm(request.distanceKm());
        route.setDurationMinutes(request.durationMinutes());
        route.setBaseFare(request.baseFare());
        route.setServiceType(
                normalizeServiceType(request.serviceType())
        );

        // Route becomes active only after stops
        // and operational details are configured.
        route.setStatus("INACTIVE");

        Route saved = routeRepository.save(route);
        addEndpointStops(saved);
        return toProfile(saved);
    }

    // ==========================================
    // ADMIN: UPDATE ROUTE
    // ==========================================

    @Transactional
    public RouteProfile updateRoute(
            Long routeId,
            UpdateRouteRequest request
    ) {
        if (request == null) {
            throw badRequest("Update details are required");
        }

        Route route = getRouteById(routeId);

        if (request.name() != null) {
            route.setName(
                    requireText(request.name(), "Route name")
            );
        }

        if (request.origin() != null) {
            route.setOrigin(
                    requireText(request.origin(), "Origin")
            );
        }

        if (request.destination() != null) {
            route.setDestination(
                    requireText(
                            request.destination(), "Destination"
                    )
            );
        }

        if (request.distanceKm() != null) {
            validateDistance(request.distanceKm());
            route.setDistanceKm(request.distanceKm());
        }

        if (request.durationMinutes() != null) {
            validateDuration(request.durationMinutes());
            route.setDurationMinutes(request.durationMinutes());
        }

        if (request.baseFare() != null) {
            validateFare(request.baseFare());
            route.setBaseFare(request.baseFare());
        }

        if (request.serviceType() != null) {
            route.setServiceType(
                    normalizeServiceType(
                            request.serviceType()
                    )
            );
        }

        return toProfile(routeRepository.save(route));
    }

    // ==========================================
    // ADMIN: ROUTE STATUS
    // ==========================================

    @Transactional
    public RouteProfile updateRouteStatus(
            Long routeId,
            String newStatus
    ) {
        Route route = getRouteById(routeId);

        String status = requireText(
                newStatus, "Route status"
        ).toUpperCase(Locale.ROOT);

        if (!VALID_STATUSES.contains(status)) {
            throw badRequest("Invalid route status");
        }

        if ("ACTIVE".equals(status)) {
            List<RouteStop> stops =
                    stopRepository
                            .findByRouteIdOrderByStopOrderAsc(
                                    routeId
                            );

            if (stops.isEmpty()) {
                addEndpointStops(route);
                stops = stopRepository.findByRouteIdOrderByStopOrderAsc(routeId);
            }
            if (stops.size() < 2) {
                throw badRequest(
                        "At least two stops are required to activate a route"
                );
            }

            validateStopSequence(stops);

            if (route.getDistanceKm() == null
                    || route.getDurationMinutes() == null) {
                throw badRequest(
                        "Route distance and duration are required"
                );
            }
        }

        route.setStatus(status);

        return toProfile(routeRepository.save(route));
    }

    /**
     * Register the origin and destination as actual ordered stops.
     * Coordinates are left unset: place names alone are not precise GPS positions.
     */
    private void addEndpointStops(Route route) {
        RouteStop origin = new RouteStop();
        origin.setRoute(route);
        origin.setStopName(route.getOrigin());
        origin.setStopOrder(0);
        origin.setDistanceFromStartKm(BigDecimal.ZERO);
        origin.setMinutesFromStart(0);
        stopRepository.save(origin);

        RouteStop destination = new RouteStop();
        destination.setRoute(route);
        destination.setStopName(route.getDestination());
        destination.setStopOrder(1);
        destination.setDistanceFromStartKm(route.getDistanceKm());
        destination.setMinutesFromStart(route.getDurationMinutes());
        stopRepository.save(destination);
    }

    // ==========================================
    // ADMIN: ADD ROUTE STOP
    // ==========================================

    @Transactional
    public RouteStopProfile addRouteStop(
            Long routeId,
            CreateStopRequest request
    ) {
        Route route = getRouteById(routeId);

        if (request == null) {
            throw badRequest("Stop details are required");
        }

        validateStopFields(
                request.stopOrder(),
                request.latitude(),
                request.longitude(),
                request.distanceFromStartKm(),
                request.minutesFromStart()
        );

        if (stopRepository.existsByRouteIdAndStopOrder(
                routeId, request.stopOrder()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Stop order already exists on this route"
            );
        }

        RouteStop stop = new RouteStop();

        stop.setRoute(route);
        stop.setStopName(
                requireText(request.stopName(), "Stop name")
        );
        stop.setStopOrder(request.stopOrder());
        stop.setLatitude(request.latitude());
        stop.setLongitude(request.longitude());
        stop.setDistanceFromStartKm(
                request.distanceFromStartKm()
        );
        stop.setMinutesFromStart(
                request.minutesFromStart()
        );

        return toStopProfile(stopRepository.save(stop));
    }

    // ==========================================
    // ADMIN: UPDATE ROUTE STOP
    // ==========================================

    @Transactional
    public RouteStopProfile updateRouteStop(
            Long routeId,
            Long stopId,
            UpdateStopRequest request
    ) {
        RouteStop stop = getStopOnRoute(routeId, stopId);

        if (request == null) {
            throw badRequest("Stop details are required");
        }

        if (request.stopName() != null) {
            stop.setStopName(
                    requireText(
                            request.stopName(), "Stop name"
                    )
            );
        }

        if (request.stopOrder() != null) {
            if (request.stopOrder() < 0) {
                throw badRequest(
                        "Stop order cannot be negative"
                );
            }

            if (!request.stopOrder().equals(
                    stop.getStopOrder()
            ) && stopRepository
                    .existsByRouteIdAndStopOrder(
                            routeId,
                            request.stopOrder()
                    )) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Stop order already exists"
                );
            }

            stop.setStopOrder(request.stopOrder());
        }

        if (request.latitude() != null) {
            validateLatitude(request.latitude());
            stop.setLatitude(request.latitude());
        }

        if (request.longitude() != null) {
            validateLongitude(request.longitude());
            stop.setLongitude(request.longitude());
        }

        if (request.distanceFromStartKm() != null) {
            validateNonnegative(
                    request.distanceFromStartKm(),
                    "Distance from start"
            );
            stop.setDistanceFromStartKm(
                    request.distanceFromStartKm()
            );
        }

        if (request.minutesFromStart() != null) {
            if (request.minutesFromStart() < 0) {
                throw badRequest(
                        "Minutes from start cannot be negative"
                );
            }

            stop.setMinutesFromStart(
                    request.minutesFromStart()
            );
        }

        return toStopProfile(stopRepository.save(stop));
    }

    // ==========================================
    // ADMIN: DELETE ROUTE STOP
    // ==========================================

    @Transactional
    public void deleteRouteStop(
            Long routeId,
            Long stopId
    ) {
        RouteStop stop = getStopOnRoute(routeId, stopId);

        // Historical bookings may reference this stop.
        // Database FK constraints protect referenced rows.
        // Controllers should prefer deactivation or
        // controlled route revision for live routes.
        if ("ACTIVE".equalsIgnoreCase(
                stop.getRoute().getStatus()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Deactivate the route before removing stops"
            );
        }

        stopRepository.delete(stop);
    }

    // ==========================================
    // ROUTE LISTS
    // ==========================================

    @Transactional(readOnly = true)
    public List<RouteProfile> getAllRoutes() {
        return routeRepository.findAll()
                .stream()
                .map(this::toProfile)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<RouteProfile> getRoutes(
            Pageable pageable
    ) {
        return routeRepository.findAll(pageable)
                .map(this::toProfile);
    }

    @Transactional(readOnly = true)
    public List<RouteProfile> getActiveRoutes() {
        return routeRepository
                .findByStatusIgnoreCase("ACTIVE")
                .stream()
                .map(this::toProfile)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RouteProfile> searchRoutes(
            String keyword
    ) {
        if (keyword == null || keyword.isBlank()) {
            return getAllRoutes();
        }

        String search = keyword.trim()
                .toLowerCase(Locale.ROOT);

        return routeRepository.findAll()
                .stream()
                .filter(route ->
                        containsIgnoreCase(
                                route.getName(), search
                        )
                                || containsIgnoreCase(
                                route.getOrigin(), search
                        )
                                || containsIgnoreCase(
                                route.getDestination(), search
                        )
                                || containsIgnoreCase(
                                route.getServiceType(), search
                        )
                )
                .map(this::toProfile)
                .toList();
    }

    // ==========================================
    // PASSENGER: SEARCH BETWEEN STOPS
    // ==========================================

    @Transactional(readOnly = true)
    public List<RouteDetails> findRoutesBetweenStops(
            String boardingStop,
            String destinationStop
    ) {
        String boarding = requireText(
                boardingStop, "Boarding stop"
        );

        String destination = requireText(
                destinationStop, "Destination stop"
        );

        List<Long> routeIds =
                stopRepository.findRouteIdsBetweenStops(
                        boarding, destination
                );

        return routeIds.stream()
                .filter(id ->
                        "ACTIVE".equalsIgnoreCase(
                                getRouteById(id).getStatus()
                        )
                )
                .map(this::getRouteDetails)
                .toList();
    }

    // ==========================================
    // PASSENGER: ROUTE SEGMENT & FARE
    // ==========================================

    @Transactional(readOnly = true)
    public RouteSegment calculateRouteSegment(
            Long routeId,
            Long boardingStopId,
            Long destinationStopId
    ) {
        Route route = getRouteById(routeId);

        RouteStop boarding = getStopOnRoute(
                routeId, boardingStopId
        );

        RouteStop destination = getStopOnRoute(
                routeId, destinationStopId
        );

        if (boarding.getStopOrder()
                >= destination.getStopOrder()) {
            throw badRequest(
                    "Destination must come after boarding stop"
            );
        }

        if (boarding.getDistanceFromStartKm() == null
                || destination.getDistanceFromStartKm() == null) {
            throw badRequest(
                    "Stop distance information is incomplete"
            );
        }

        BigDecimal segmentDistance =
                destination.getDistanceFromStartKm()
                        .subtract(
                                boarding.getDistanceFromStartKm()
                        );

        if (segmentDistance.signum() <= 0) {
            throw badRequest(
                    "Invalid route segment distance"
            );
        }

        Integer segmentDuration = null;

        if (boarding.getMinutesFromStart() != null
                && destination.getMinutesFromStart() != null) {
            segmentDuration =
                    destination.getMinutesFromStart()
                            - boarding.getMinutesFromStart();

            if (segmentDuration < 0) {
                throw badRequest(
                        "Invalid stop timing information"
                );
            }
        }

        if (route.getDistanceKm() == null
                || route.getDistanceKm().signum() <= 0) {
            throw badRequest(
                    "Route distance must be configured"
            );
        }

        BigDecimal estimatedFare =
                route.getBaseFare()
                        .multiply(segmentDistance)
                        .divide(
                                route.getDistanceKm(),
                                2,
                                RoundingMode.HALF_UP
                        );

        return new RouteSegment(
                routeId,
                boardingStopId,
                destinationStopId,
                boarding.getStopName(),
                destination.getStopName(),
                segmentDistance,
                segmentDuration,
                estimatedFare
        );
    }

    // ==========================================
    // ADMIN: ROUTE STATISTICS
    // ==========================================

    @Transactional(readOnly = true)
    public long countRoutes() {
        return routeRepository.count();
    }

    @Transactional(readOnly = true)
    public long countActiveRoutes() {
        return routeRepository
                .findByStatusIgnoreCase("ACTIVE")
                .size();
    }

    // ==========================================
    // INTERNAL HELPERS
    // ==========================================

    private RouteStop getStopOnRoute(
            Long routeId,
            Long stopId
    ) {
        if (routeId == null || stopId == null) {
            throw badRequest(
                    "Route ID and stop ID are required"
            );
        }

        RouteStop stop = stopRepository.findById(stopId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Route stop not found"
                        )
                );

        if (!routeId.equals(stop.getRoute().getId())) {
            throw badRequest(
                    "Stop does not belong to this route"
            );
        }

        return stop;
    }

    private void validateStopSequence(
            List<RouteStop> stops
    ) {
        BigDecimal previousDistance = null;
        Integer previousMinutes = null;

        for (RouteStop stop : stops) {
            if (stop.getDistanceFromStartKm() == null
                    || stop.getMinutesFromStart() == null) {
                throw badRequest(
                        "All stops need distance and timing information"
                );
            }

            if (previousDistance != null
                    && stop.getDistanceFromStartKm()
                    .compareTo(previousDistance) <= 0) {
                throw badRequest(
                        "Stop distances must increase"
                );
            }

            if (previousMinutes != null
                    && stop.getMinutesFromStart()
                    <= previousMinutes) {
                throw badRequest(
                        "Stop timings must increase"
                );
            }

            previousDistance =
                    stop.getDistanceFromStartKm();

            previousMinutes =
                    stop.getMinutesFromStart();
        }
    }

    private RouteProfile toProfile(Route route) {
        return new RouteProfile(
                route.getId(),
                route.getName(),
                route.getOrigin(),
                route.getDestination(),
                route.getDistanceKm(),
                route.getDurationMinutes(),
                route.getBaseFare(),
                route.getServiceType(),
                route.getStatus()
        );
    }

    private RouteStopProfile toStopProfile(
            RouteStop stop
    ) {
        return new RouteStopProfile(
                stop.getId(),
                stop.getRoute().getId(),
                stop.getStopName(),
                stop.getStopOrder(),
                stop.getLatitude(),
                stop.getLongitude(),
                stop.getDistanceFromStartKm(),
                stop.getMinutesFromStart()
        );
    }

    private void validateStopFields(
            Integer order,
            BigDecimal latitude,
            BigDecimal longitude,
            BigDecimal distance,
            Integer minutes
    ) {
        if (order == null || order < 0) {
            throw badRequest(
                    "Stop order must be nonnegative"
            );
        }

        if (latitude != null) {
            validateLatitude(latitude);
        }

        if (longitude != null) {
            validateLongitude(longitude);
        }

        if (distance != null) {
            validateNonnegative(
                    distance, "Distance from start"
            );
        }

        if (minutes != null && minutes < 0) {
            throw badRequest(
                    "Minutes from start cannot be negative"
            );
        }
    }

    private void validateLatitude(BigDecimal latitude) {
        if (latitude.compareTo(
                BigDecimal.valueOf(-90)
        ) < 0 || latitude.compareTo(
                BigDecimal.valueOf(90)
        ) > 0) {
            throw badRequest("Invalid latitude");
        }
    }

    private void validateLongitude(BigDecimal longitude) {
        if (longitude.compareTo(
                BigDecimal.valueOf(-180)
        ) < 0 || longitude.compareTo(
                BigDecimal.valueOf(180)
        ) > 0) {
            throw badRequest("Invalid longitude");
        }
    }

    private void validateDistance(BigDecimal distance) {
        if (distance != null
                && distance.signum() <= 0) {
            throw badRequest(
                    "Route distance must be positive"
            );
        }
    }

    private void validateDuration(Integer duration) {
        if (duration != null && duration <= 0) {
            throw badRequest(
                    "Route duration must be positive"
            );
        }
    }

    private void validateFare(BigDecimal fare) {
        if (fare == null || fare.signum() < 0) {
            throw badRequest(
                    "Base fare must be nonnegative"
            );
        }
    }

    private void validateNonnegative(
            BigDecimal value,
            String field
    ) {
        if (value.signum() < 0) {
            throw badRequest(
                    field + " cannot be negative"
            );
        }
    }

    private String normalizeServiceType(
            String value
    ) {
        String type = value == null
                || value.isBlank()
                ? "COMMUTER"
                : value.trim().toUpperCase(Locale.ROOT);

        if (!VALID_SERVICE_TYPES.contains(type)) {
            throw badRequest("Invalid service type");
        }

        return type;
    }

    private String requireText(
            String value,
            String field
    ) {
        if (value == null || value.isBlank()) {
            throw badRequest(field + " is required");
        }

        return value.trim();
    }

    private boolean containsIgnoreCase(
            String value,
            String keyword
    ) {
        return value != null
                && value.toLowerCase(Locale.ROOT)
                .contains(keyword);
    }

    private ResponseStatusException badRequest(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }
}
