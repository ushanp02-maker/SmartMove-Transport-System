
package com.smartmove.backend.service;

import com.smartmove.backend.entity.Trip;
import com.smartmove.backend.entity.UserRole;
import com.smartmove.backend.repository.TripRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class TripStatusService {

    private static final String SCHEDULED = "SCHEDULED";
    private static final String IN_PROGRESS = "IN_PROGRESS";
    private static final String PAUSED = "PAUSED";
    private static final String COMPLETED = "COMPLETED";
    private static final String CANCELLED = "CANCELLED";

    private static final Set<String> ACTIVE_STATUSES =
            Set.of(IN_PROGRESS, PAUSED);

    private final TripRepository tripRepository;
    private final CurrentUserService currentUserService;

    public TripStatusService(
            TripRepository tripRepository,
            CurrentUserService currentUserService
    ) {
        this.tripRepository = tripRepository;
        this.currentUserService = currentUserService;
    }

    // ==========================================
    // RESPONSE DTOs
    // ==========================================

    public record TripStatusProfile(
            Long tripId,
            Long routeId,
            Long driverId,
            Long vehicleId,
            String status,
            LocalDateTime scheduledDeparture,
            LocalDateTime scheduledArrival,
            boolean active,
            boolean completed,
            boolean cancelled
    ) {}

    public record TripStatusStatistics(
            long scheduledTrips,
            long inProgressTrips,
            long pausedTrips,
            long completedTrips,
            long cancelledTrips
    ) {}

    // ==========================================
    // DRIVER: START TRIP
    // ==========================================

    @Transactional
    public TripStatusProfile startTrip(Long tripId) {

        Trip trip = getAssignedTrip(tripId);

        requireCurrentStatus(
                trip,
                SCHEDULED
        );

        if (trip.getDepartureTime() == null) {
            throw conflict(
                    "Trip departure time is missing"
            );
        }

        LocalDateTime now = LocalDateTime.now();

        // Prevent starting a trip excessively early.
        if (now.isBefore(
                trip.getDepartureTime().minusMinutes(30)
        )) {
            throw conflict(
                    "Trip cannot be started more than "
                            + "30 minutes before scheduled departure"
            );
        }

        trip.setStatus(IN_PROGRESS);

        return toProfile(tripRepository.save(trip));
    }

    // ==========================================
    // DRIVER: PAUSE TRIP
    // ==========================================

    @Transactional
    public TripStatusProfile pauseTrip(Long tripId) {

        Trip trip = getAssignedTrip(tripId);

        requireCurrentStatus(
                trip,
                IN_PROGRESS
        );

        trip.setStatus(PAUSED);

        return toProfile(tripRepository.save(trip));
    }

    // ==========================================
    // DRIVER: RESUME TRIP
    // ==========================================

    @Transactional
    public TripStatusProfile resumeTrip(Long tripId) {

        Trip trip = getAssignedTrip(tripId);

        requireCurrentStatus(
                trip,
                PAUSED
        );

        trip.setStatus(IN_PROGRESS);

        return toProfile(tripRepository.save(trip));
    }

    // ==========================================
    // DRIVER: COMPLETE TRIP
    // ==========================================

    @Transactional
    public TripStatusProfile completeTrip(Long tripId) {

        Trip trip = getAssignedTrip(tripId);

        requireCurrentStatus(
                trip,
                IN_PROGRESS
        );

        trip.setStatus(COMPLETED);

        return toProfile(tripRepository.save(trip));
    }

    // ==========================================
    // ADMIN: CANCEL TRIP
    // ==========================================

    @Transactional
    public TripStatusProfile cancelTrip(Long tripId) {

        currentUserService.requireAdmin();

        Trip trip = findTrip(tripId);

        String status = normalize(trip.getStatus());

        if (COMPLETED.equals(status)
                || CANCELLED.equals(status)) {
            throw conflict(
                    "Completed or cancelled trips "
                            + "cannot be cancelled again"
            );
        }

        trip.setStatus(CANCELLED);

        return toProfile(tripRepository.save(trip));
    }

    // ==========================================
    // ADMIN: FORCE COMPLETE TRIP
    // ==========================================

    @Transactional
    public TripStatusProfile forceCompleteTrip(
            Long tripId
    ) {
        currentUserService.requireAdmin();

        Trip trip = findTrip(tripId);

        String status = normalize(trip.getStatus());

        if (!ACTIVE_STATUSES.contains(status)) {
            throw conflict(
                    "Only active trips can be completed"
            );
        }

        trip.setStatus(COMPLETED);

        return toProfile(tripRepository.save(trip));
    }

    // ==========================================
    // GET TRIP STATUS
    // ==========================================

    @Transactional(readOnly = true)
    public TripStatusProfile getTripStatus(
            Long tripId
    ) {
        currentUserService.requireRole(
                UserRole.PASSENGER,
                UserRole.DRIVER,
                UserRole.ADMIN,
                UserRole.SUPER_ADMIN
        );

        return toProfile(findTrip(tripId));
    }

    // ==========================================
    // DRIVER: MY ASSIGNED TRIP STATUSES
    // ==========================================

    @Transactional(readOnly = true)
    public List<TripStatusProfile> getMyTrips() {

        Long driverId =
                currentUserService.getCurrentDriverId();

        return tripRepository.findAll()
                .stream()
                .filter(trip ->
                        trip.getDriver() != null
                                && driverId.equals(
                                trip.getDriver().getId()
                        )
                )
                .map(this::toProfile)
                .toList();
    }

    // ==========================================
    // ADMIN: ACTIVE TRIPS
    // ==========================================

    @Transactional(readOnly = true)
    public List<TripStatusProfile> getActiveTrips() {

        currentUserService.requireAdmin();

        return tripRepository.findAll()
                .stream()
                .filter(trip ->
                        ACTIVE_STATUSES.contains(
                                normalize(trip.getStatus())
                        )
                )
                .map(this::toProfile)
                .toList();
    }

    // ==========================================
    // ADMIN: STATUS STATISTICS
    // ==========================================

    @Transactional(readOnly = true)
    public TripStatusStatistics getStatistics() {

        currentUserService.requireAdmin();

        List<Trip> trips = tripRepository.findAll();

        return new TripStatusStatistics(
                countStatus(trips, SCHEDULED),
                countStatus(trips, IN_PROGRESS),
                countStatus(trips, PAUSED),
                countStatus(trips, COMPLETED),
                countStatus(trips, CANCELLED)
        );
    }

    // ==========================================
    // DRIVER ASSIGNMENT VALIDATION
    // ==========================================

    private Trip getAssignedTrip(Long tripId) {

        Long currentDriverId =
                currentUserService.getCurrentDriverId();

        Trip trip = findTrip(tripId);

        if (trip.getDriver() == null
                || !currentDriverId.equals(
                trip.getDriver().getId()
        )) {
            throw forbidden(
                    "You are not assigned to this trip"
            );
        }

        return trip;
    }

    // ==========================================
    // ENTITY LOOKUP
    // ==========================================

    private Trip findTrip(Long tripId) {

        if (tripId == null || tripId <= 0) {
            throw badRequest(
                    "Valid trip ID is required"
            );
        }

        return tripRepository.findById(tripId)
                .orElseThrow(() ->
                        notFound("Trip not found")
                );
    }

    // ==========================================
    // STATUS TRANSITION VALIDATION
    // ==========================================

    private void requireCurrentStatus(
            Trip trip,
            String requiredStatus
    ) {
        if (!requiredStatus.equals(
                normalize(trip.getStatus())
        )) {
            throw conflict(
                    "Trip must have status "
                            + requiredStatus
                            + " for this operation"
            );
        }
    }

    // ==========================================
    // STATUS HELPERS
    // ==========================================

    private long countStatus(
            List<Trip> trips,
            String status
    ) {
        return trips.stream()
                .filter(trip ->
                        status.equals(
                                normalize(trip.getStatus())
                        )
                )
                .count();
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.trim().toUpperCase(Locale.ROOT);
    }

    // ==========================================
    // RESPONSE MAPPING
    // ==========================================

    private TripStatusProfile toProfile(Trip trip) {

        String status = normalize(trip.getStatus());

        return new TripStatusProfile(
                trip.getId(),
                trip.getRoute().getId(),
                trip.getDriver().getId(),
                trip.getVehicle().getId(),
                status,
                trip.getDepartureTime(),
                trip.getArrivalTime(),
                ACTIVE_STATUSES.contains(status),
                COMPLETED.equals(status),
                CANCELLED.equals(status)
        );
    }

    // ==========================================
    // ERROR HELPERS
    // ==========================================

    private ResponseStatusException badRequest(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }

    private ResponseStatusException forbidden(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                message
        );
    }

    private ResponseStatusException conflict(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                message
        );
    }

    private ResponseStatusException notFound(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                message
        );
    }
}
