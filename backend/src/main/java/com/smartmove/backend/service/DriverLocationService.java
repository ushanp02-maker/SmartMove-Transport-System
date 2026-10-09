
package com.smartmove.backend.service;

import com.smartmove.backend.document.DriverLocation;
import com.smartmove.backend.entity.Booking;
import com.smartmove.backend.entity.Trip;
import com.smartmove.backend.entity.UserRole;

import com.smartmove.backend.repository.BookingRepository;
import com.smartmove.backend.repository.DriverLocationRepository;
import com.smartmove.backend.repository.TripRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class DriverLocationService {

    private static final Set<String> TRACKABLE_STATUSES =
            Set.of("IN_PROGRESS", "ONGOING");

    private static final Set<String> VISIBLE_BOOKING_STATUSES =
            Set.of("CONFIRMED");

    private static final double EARTH_RADIUS_KM = 6371.0088;

    private static final double MAX_ACCURACY_METERS = 1000.0;

    private static final int MAX_FUTURE_SECONDS = 60;

    private static final int MAX_LOCATION_AGE_MINUTES = 10;

    private final DriverLocationRepository locationRepository;
    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;
    private final CurrentUserService currentUserService;

    public DriverLocationService(
            DriverLocationRepository locationRepository,
            TripRepository tripRepository,
            BookingRepository bookingRepository,
            CurrentUserService currentUserService
    ) {
        this.locationRepository = locationRepository;
        this.tripRepository = tripRepository;
        this.bookingRepository = bookingRepository;
        this.currentUserService = currentUserService;
    }

    // ==========================================
    // REQUEST AND RESPONSE DTOs
    // ==========================================

    public record SubmitLocationRequest(
            Long tripId,
            Double latitude,
            Double longitude,
            Double accuracyMeters,
            Double speedKmh,
            Double headingDegrees,
            Double altitudeMeters,
            Boolean moving,
            LocalDateTime recordedAt
    ) {}

    public record LocationProfile(
            String id,
            Long tripId,
            Long driverId,
            Long vehicleId,
            Double latitude,
            Double longitude,
            Double accuracyMeters,
            Double speedKmh,
            Double headingDegrees,
            Double altitudeMeters,
            String tripStatus,
            Boolean moving,
            String source,
            LocalDateTime recordedAt,
            LocalDateTime receivedAt,
            Double distanceFromPreviousKm
    ) {}

    public record LiveTripLocation(
            Long tripId,
            String tripStatus,
            boolean trackingAvailable,
            boolean locationFresh,
            LocationProfile location
    ) {}

    public record TrackingStatistics(
            Long tripId,
            long totalUpdates,
            LocalDateTime firstRecordedAt,
            LocalDateTime lastRecordedAt,
            Double totalRecordedDistanceKm
    ) {}

    // ==========================================
    // DRIVER: SUBMIT GPS LOCATION
    // ==========================================

    public LocationProfile submitLocation(
            SubmitLocationRequest request
    ) {
        currentUserService.requireDriver();

        if (request == null || request.tripId() == null) {
            throw badRequest("Trip ID is required");
        }

        Long driverId =
                currentUserService.getCurrentDriverId();

        Trip trip = findTrip(request.tripId());

        if (trip.getDriver() == null
                || !driverId.equals(trip.getDriver().getId())) {
            throw forbidden(
                    "You are not assigned to this trip"
            );
        }

        if (!isTrackable(trip.getStatus())) {
            throw conflict(
                    "GPS updates are allowed only while "
                            + "the trip is in progress"
            );
        }

        validateCoordinates(
                request.latitude(),
                request.longitude()
        );

        validateAccuracy(request.accuracyMeters());
        validateSpeed(request.speedKmh());
        validateHeading(request.headingDegrees());
        validateAltitude(request.altitudeMeters());

        LocalDateTime now = LocalDateTime.now();

        LocalDateTime recordedAt =
                request.recordedAt() == null
                        ? now
                        : request.recordedAt();

        validateTimestamp(recordedAt, now);

        DriverLocation previous = locationRepository
                .findFirstByTripIdOrderByRecordedAtDesc(
                        trip.getId()
                )
                .orElse(null);

        if (previous != null
                && previous.getRecordedAt() != null
                && !recordedAt.isAfter(
                previous.getRecordedAt()
        )) {
            throw conflict(
                    "GPS update must be newer than "
                            + "the previous accepted update"
            );
        }

        Double distanceFromPrevious = null;

        if (previous != null
                && previous.getLatitude() != null
                && previous.getLongitude() != null) {

            distanceFromPrevious = calculateDistanceKm(
                    previous.getLatitude(),
                    previous.getLongitude(),
                    request.latitude(),
                    request.longitude()
            );
        }

        DriverLocation location = new DriverLocation();

        location.setTripId(trip.getId());
        location.setDriverId(driverId);
        location.setVehicleId(
                trip.getVehicle().getId()
        );

        location.setLatitude(request.latitude());
        location.setLongitude(request.longitude());
        location.setAccuracyMeters(
                request.accuracyMeters()
        );
        location.setSpeedKmh(request.speedKmh());
        location.setHeadingDegrees(
                request.headingDegrees()
        );
        location.setAltitudeMeters(
                request.altitudeMeters()
        );

        location.setTripStatus(trip.getStatus());
        location.setMoving(request.moving());
        location.setSource("DRIVER_PHONE");

        location.setRecordedAt(recordedAt);
        location.setReceivedAt(now);

        location.setDistanceFromPreviousKm(
                distanceFromPrevious
        );

        return toProfile(
                locationRepository.save(location)
        );
    }

    // ==========================================
    // PASSENGER: LATEST TRIP LOCATION
    // ==========================================

    @Transactional(readOnly = true)
    public LiveTripLocation getLiveTripLocation(
            Long tripId
    ) {
        Trip trip = findTrip(tripId);

        requireTripViewerAccess(trip);

        if (!isTrackable(trip.getStatus())) {
            return new LiveTripLocation(
                    trip.getId(),
                    trip.getStatus(),
                    false,
                    false,
                    null
            );
        }

        DriverLocation location = locationRepository
                .findFirstByTripIdOrderByRecordedAtDesc(
                        tripId
                )
                .orElse(null);

        if (location == null) {
            return new LiveTripLocation(
                    trip.getId(),
                    trip.getStatus(),
                    false,
                    false,
                    null
            );
        }

        boolean fresh = isLocationFresh(location);

        return new LiveTripLocation(
                trip.getId(),
                trip.getStatus(),
                true,
                fresh,
                toProfile(location)
        );
    }

    // ==========================================
    // DRIVER: OWN LATEST LOCATION
    // ==========================================

    @Transactional(readOnly = true)
    public LocationProfile getMyLatestLocation() {

        Long driverId =
                currentUserService.getCurrentDriverId();

        DriverLocation location = locationRepository
                .findFirstByDriverIdOrderByRecordedAtDesc(
                        driverId
                )
                .orElseThrow(() ->
                        notFound(
                                "No location updates found"
                        )
                );

        return toProfile(location);
    }

    // ==========================================
    // ADMIN: TRIP LOCATION HISTORY
    // ==========================================

    @Transactional(readOnly = true)
    public List<LocationProfile> getTripHistory(
            Long tripId
    ) {
        currentUserService.requireAdmin();

        findTrip(tripId);

        return locationRepository
                .findByTripIdOrderByRecordedAtAsc(tripId)
                .stream()
                .map(this::toProfile)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<LocationProfile> getTripHistoryPage(
            Long tripId,
            Pageable pageable
    ) {
        currentUserService.requireAdmin();

        findTrip(tripId);

        return locationRepository
                .findByTripIdOrderByRecordedAtDesc(
                        tripId,
                        pageable
                )
                .map(this::toProfile);
    }

    // ==========================================
    // DRIVER: OWN TRIP HISTORY
    // ==========================================

    @Transactional(readOnly = true)
    public List<LocationProfile> getMyTripHistory(
            Long tripId
    ) {
        Long driverId =
                currentUserService.getCurrentDriverId();

        Trip trip = findTrip(tripId);

        if (trip.getDriver() == null
                || !driverId.equals(trip.getDriver().getId())) {
            throw forbidden(
                    "You are not assigned to this trip"
            );
        }

        return locationRepository
                .findByTripIdOrderByRecordedAtAsc(tripId)
                .stream()
                .map(this::toProfile)
                .toList();
    }

    // ==========================================
    // ADMIN: DRIVER HISTORY
    // ==========================================

    @Transactional(readOnly = true)
    public Page<LocationProfile> getDriverHistory(
            Long driverId,
            Pageable pageable
    ) {
        currentUserService.requireAdmin();

        return locationRepository
                .findByDriverIdOrderByRecordedAtDesc(
                        driverId,
                        pageable
                )
                .map(this::toProfile);
    }

    // ==========================================
    // ADMIN: VEHICLE HISTORY
    // ==========================================

    @Transactional(readOnly = true)
    public Page<LocationProfile> getVehicleHistory(
            Long vehicleId,
            Pageable pageable
    ) {
        currentUserService.requireAdmin();

        return locationRepository
                .findByVehicleIdOrderByRecordedAtDesc(
                        vehicleId,
                        pageable
                )
                .map(this::toProfile);
    }

    // ==========================================
    // ADMIN: TRACKING STATISTICS
    // ==========================================

    @Transactional(readOnly = true)
    public TrackingStatistics getTrackingStatistics(
            Long tripId
    ) {
        currentUserService.requireAdmin();

        findTrip(tripId);

        List<DriverLocation> locations =
                locationRepository
                        .findByTripIdOrderByRecordedAtAsc(
                                tripId
                        );

        if (locations.isEmpty()) {
            return new TrackingStatistics(
                    tripId,
                    0,
                    null,
                    null,
                    0.0
            );
        }

        double totalDistance = locations.stream()
                .map(DriverLocation::getDistanceFromPreviousKm)
                .filter(distance -> distance != null
                        && Double.isFinite(distance)
                        && distance >= 0)
                .mapToDouble(Double::doubleValue)
                .sum();

        return new TrackingStatistics(
                tripId,
                locations.size(),
                locations.get(0).getRecordedAt(),
                locations.get(locations.size() - 1)
                        .getRecordedAt(),
                totalDistance
        );
    }

    // ==========================================
    // ADMIN: REMOVE TRIP TRACKING HISTORY
    // ==========================================

    public long deleteTripHistory(Long tripId) {

        currentUserService.requireSuperAdmin();

        Trip trip = findTrip(tripId);

        if (isTrackable(trip.getStatus())) {
            throw conflict(
                    "Tracking history cannot be deleted "
                            + "while a trip is active"
            );
        }

        return locationRepository.deleteByTripId(tripId);
    }

    // ==========================================
    // PASSENGER ACCESS CONTROL
    // ==========================================

    private void requireTripViewerAccess(Trip trip) {

        if (currentUserService.hasAnyRole(
                UserRole.ADMIN,
                UserRole.SUPER_ADMIN
        )) {
            return;
        }

        if (currentUserService.hasRole(UserRole.DRIVER)) {

            Long driverId =
                    currentUserService.getCurrentDriverId();

            if (trip.getDriver() != null
                    && driverId.equals(
                    trip.getDriver().getId()
            )) {
                return;
            }

            throw forbidden(
                    "You are not assigned to this trip"
            );
        }

        currentUserService.requirePassenger();

        Long passengerId =
                currentUserService.getCurrentPassengerId();

        List<Booking> bookings = bookingRepository
                .findByPassengerIdAndStatusIgnoreCaseOrderByBookedAtDesc(
                        passengerId,
                        "CONFIRMED"
                );

        boolean hasConfirmedBooking = bookings.stream()
                .anyMatch(booking ->
                        booking.getTrip() != null
                                && trip.getId().equals(
                                booking.getTrip().getId()
                        )
                                && VISIBLE_BOOKING_STATUSES.contains(
                                normalize(
                                        booking.getStatus()
                                )
                        )
                );

        if (!hasConfirmedBooking) {
            throw forbidden(
                    "A confirmed booking is required "
                            + "to view this trip's live location"
            );
        }
    }

    // ==========================================
    // GPS VALIDATION
    // ==========================================

    private void validateCoordinates(
            Double latitude,
            Double longitude
    ) {
        if (latitude == null
                || longitude == null
                || !Double.isFinite(latitude)
                || !Double.isFinite(longitude)
                || latitude < -90
                || latitude > 90
                || longitude < -180
                || longitude > 180) {

            throw badRequest(
                    "Invalid GPS coordinates"
            );
        }
    }

    private void validateAccuracy(Double accuracy) {

        if (accuracy == null) {
            return;
        }

        if (!Double.isFinite(accuracy)
                || accuracy < 0
                || accuracy > MAX_ACCURACY_METERS) {
            throw badRequest(
                    "GPS accuracy must be between "
                            + "0 and 1000 metres"
            );
        }
    }

    private void validateSpeed(Double speed) {

        if (speed == null) {
            return;
        }

        if (!Double.isFinite(speed)
                || speed < 0
                || speed > 200) {
            throw badRequest(
                    "GPS speed must be between "
                            + "0 and 200 km/h"
            );
        }
    }

    private void validateHeading(Double heading) {

        if (heading == null) {
            return;
        }

        if (!Double.isFinite(heading)
                || heading < 0
                || heading >= 360) {
            throw badRequest(
                    "GPS heading must be between "
                            + "0 and less than 360 degrees"
            );
        }
    }

    private void validateAltitude(Double altitude) {

        if (altitude == null) {
            return;
        }

        if (!Double.isFinite(altitude)
                || altitude < -500
                || altitude > 10000) {
            throw badRequest(
                    "Invalid GPS altitude"
            );
        }
    }

    private void validateTimestamp(
            LocalDateTime recordedAt,
            LocalDateTime now
    ) {
        if (recordedAt.isAfter(
                now.plusSeconds(MAX_FUTURE_SECONDS)
        )) {
            throw badRequest(
                    "GPS timestamp cannot be "
                            + "more than 60 seconds in the future"
            );
        }

        if (recordedAt.isBefore(
                now.minusMinutes(
                        MAX_LOCATION_AGE_MINUTES
                )
        )) {
            throw badRequest(
                    "GPS update is too old"
            );
        }
    }

    // ==========================================
    // LOCATION FRESHNESS
    // ==========================================

    private boolean isLocationFresh(
            DriverLocation location
    ) {
        if (location.getReceivedAt() == null) {
            return false;
        }

        long seconds = Duration.between(
                location.getReceivedAt(),
                LocalDateTime.now()
        ).getSeconds();

        return seconds >= 0
                && seconds <= 120;
    }

    // ==========================================
    // DISTANCE CALCULATION (HAVERSINE)
    // ==========================================

    public double calculateDistanceKm(
            double latitude1,
            double longitude1,
            double latitude2,
            double longitude2
    ) {
        double latDifference = Math.toRadians(
                latitude2 - latitude1
        );

        double lonDifference = Math.toRadians(
                longitude2 - longitude1
        );

        double a = Math.pow(
                Math.sin(latDifference / 2),
                2
        ) + Math.cos(Math.toRadians(latitude1))
                * Math.cos(Math.toRadians(latitude2))
                * Math.pow(
                Math.sin(lonDifference / 2),
                2
        );

        double c = 2 * Math.atan2(
                Math.sqrt(Math.min(1.0, a)),
                Math.sqrt(Math.max(0.0, 1.0 - a))
        );

        return EARTH_RADIUS_KM * c;
    }

    // ==========================================
    // ENTITY AND RESPONSE HELPERS
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

    private boolean isTrackable(String status) {
        return TRACKABLE_STATUSES.contains(
                normalize(status)
        );
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.trim().toUpperCase(Locale.ROOT);
    }

    private LocationProfile toProfile(
            DriverLocation location
    ) {
        return new LocationProfile(
                location.getId(),
                location.getTripId(),
                location.getDriverId(),
                location.getVehicleId(),
                location.getLatitude(),
                location.getLongitude(),
                location.getAccuracyMeters(),
                location.getSpeedKmh(),
                location.getHeadingDegrees(),
                location.getAltitudeMeters(),
                location.getTripStatus(),
                location.getMoving(),
                location.getSource(),
                location.getRecordedAt(),
                location.getReceivedAt(),
                location.getDistanceFromPreviousKm()
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
