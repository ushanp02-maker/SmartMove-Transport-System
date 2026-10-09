
package com.smartmove.backend.service;

import com.smartmove.backend.entity.Booking;
import com.smartmove.backend.entity.Driver;
import com.smartmove.backend.entity.Route;
import com.smartmove.backend.entity.Trip;
import com.smartmove.backend.entity.Vehicle;

import com.smartmove.backend.repository.BookingRepository;
import com.smartmove.backend.repository.DriverRepository;
import com.smartmove.backend.repository.MaintenanceRepository;
import com.smartmove.backend.repository.RouteRepository;
import com.smartmove.backend.repository.TripRepository;
import com.smartmove.backend.repository.VehicleRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class TripService {

    private static final Set<String> NON_BLOCKING_STATUSES =
            Set.of("CANCELLED");

    private static final Set<String> BOOKING_STATUSES =
            Set.of("PENDING", "CONFIRMED");

    private final TripRepository tripRepository;
    private final RouteRepository routeRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final BookingRepository bookingRepository;
    private final VehicleService vehicleService;

    public TripService(
            TripRepository tripRepository,
            RouteRepository routeRepository,
            VehicleRepository vehicleRepository,
            DriverRepository driverRepository,
            MaintenanceRepository maintenanceRepository,
            BookingRepository bookingRepository,
            VehicleService vehicleService
    ) {
        this.tripRepository = tripRepository;
        this.routeRepository = routeRepository;
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.bookingRepository = bookingRepository;
        this.vehicleService = vehicleService;
    }

    // ==========================================
    // DATA TRANSFER OBJECTS
    // ==========================================

    public record TripProfile(
            Long id,
            Long routeId,
            String routeName,
            String origin,
            String destination,
            Long vehicleId,
            String vehicleRegistration,
            Integer seatingCapacity,
            Long driverId,
            String driverName,
            LocalDateTime departureTime,
            LocalDateTime arrivalTime,
            String status,
            BigDecimal fare,
            LocalDateTime createdAt
    ) {
    }

    public record CreateTripRequest(
            Long routeId,
            Long vehicleId,
            Long driverId,
            LocalDateTime departureTime,
            LocalDateTime arrivalTime,
            BigDecimal fare
    ) {
    }

    public record UpdateTripRequest(
            Long routeId,
            Long vehicleId,
            Long driverId,
            LocalDateTime departureTime,
            LocalDateTime arrivalTime,
            BigDecimal fare
    ) {
    }

    public record TripStatistics(
            long totalTrips,
            long scheduledTrips,
            long inProgressTrips,
            long completedTrips,
            long cancelledTrips,
            long upcomingTrips
    ) {
    }

    // ==========================================
    // TRIP LOOKUP
    // ==========================================

    @Transactional(readOnly = true)
    public Trip getTripById(Long tripId) {
        if (tripId == null) {
            throw badRequest("Trip ID is required");
        }

        return tripRepository.findById(tripId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Trip not found"
                        )
                );
    }

    @Transactional(readOnly = true)
    public TripProfile getTripProfile(Long tripId) {
        return toProfile(getTripById(tripId));
    }

    // ==========================================
    // ADMIN: CREATE TRIP
    // ==========================================

    @Transactional
    public TripProfile createTrip(
            CreateTripRequest request
    ) {
        if (request == null) {
            throw badRequest("Trip details are required");
        }

        validateTimes(
                request.departureTime(),
                request.arrivalTime(),
                true
        );

        Route route = findRoute(request.routeId());
        Vehicle vehicle = findVehicle(request.vehicleId());
        Driver driver = findDriver(request.driverId());

        validateRoute(route);
        validateFare(request.fare());

        validateAssignment(
                -1L,
                vehicle,
                driver,
                request.departureTime(),
                request.arrivalTime()
        );

        Trip trip = new Trip();

        trip.setRoute(route);
        trip.setVehicle(vehicle);
        trip.setDriver(driver);
        trip.setDepartureTime(request.departureTime());
        trip.setArrivalTime(request.arrivalTime());
        trip.setFare(
                request.fare() == null
                        ? route.getBaseFare()
                        : request.fare()
        );
        trip.setStatus("SCHEDULED");

        return toProfile(tripRepository.save(trip));
    }

    // ==========================================
    // ADMIN: UPDATE SCHEDULED TRIP
    // ==========================================

    @Transactional
    public TripProfile updateTrip(
            Long tripId,
            UpdateTripRequest request
    ) {
        if (request == null) {
            throw badRequest("Update details are required");
        }

        Trip trip = getTripById(tripId);

        if (!"SCHEDULED".equalsIgnoreCase(
                trip.getStatus()
        )) {
            throw conflict(
                    "Only scheduled trips can be edited"
            );
        }

        Route route = request.routeId() == null
                ? trip.getRoute()
                : findRoute(request.routeId());

        Vehicle vehicle = request.vehicleId() == null
                ? trip.getVehicle()
                : findVehicle(request.vehicleId());

        Driver driver = request.driverId() == null
                ? trip.getDriver()
                : findDriver(request.driverId());

        LocalDateTime departure =
                request.departureTime() == null
                        ? trip.getDepartureTime()
                        : request.departureTime();

        LocalDateTime arrival =
                request.arrivalTime() == null
                        ? trip.getArrivalTime()
                        : request.arrivalTime();

        validateTimes(departure, arrival, true);
        validateRoute(route);

        boolean hasBookings = hasActiveBookings(tripId);

        if (hasBookings) {
            if (!route.getId().equals(
                    trip.getRoute().getId()
            )) {
                throw conflict(
                        "Cannot change route with active bookings"
                );
            }

            if (!departure.equals(
                    trip.getDepartureTime()
            ) || !arrival.equals(
                    trip.getArrivalTime()
            )) {
                throw conflict(
                        "Cannot reschedule a trip with active bookings"
                );
            }

            if (request.fare() != null
                    && request.fare().compareTo(
                    trip.getFare()
            ) != 0) {
                throw conflict(
                        "Cannot change fare with active bookings"
                );
            }

            if (!vehicle.getId().equals(
                    trip.getVehicle().getId()
            ) && vehicle.getSeatingCapacity()
                    < trip.getVehicle().getSeatingCapacity()) {
                throw conflict(
                        "Replacement vehicle must not have lower capacity"
                );
            }
        }

        validateAssignment(
                tripId,
                vehicle,
                driver,
                departure,
                arrival
        );

        if (request.fare() != null) {
            validateFare(request.fare());
            trip.setFare(request.fare());
        }

        trip.setRoute(route);
        trip.setVehicle(vehicle);
        trip.setDriver(driver);
        trip.setDepartureTime(departure);
        trip.setArrivalTime(arrival);

        return toProfile(tripRepository.save(trip));
    }

    // ==========================================
    // TRIP STATUS MANAGEMENT
    // ==========================================

    @Transactional
    public TripProfile updateTripStatus(
            Long tripId,
            String requestedStatus
    ) {
        Trip trip = getTripById(tripId);

        String next = requireText(
                requestedStatus,
                "Trip status"
        ).toUpperCase(Locale.ROOT);

        String current = trip.getStatus()
                .toUpperCase(Locale.ROOT);

        if (current.equals(next)) {
            return toProfile(trip);
        }

        boolean allowed = switch (current) {
            case "SCHEDULED" ->
                    next.equals("IN_PROGRESS")
                            || next.equals("CANCELLED");

            case "IN_PROGRESS" ->
                    next.equals("COMPLETED");

            default -> false;
        };

        if (!allowed) {
            throw conflict(
                    "Invalid trip status transition: "
                            + current + " to " + next
            );
        }

        if ("IN_PROGRESS".equals(next)) {
            validateAssignment(
                    tripId,
                    trip.getVehicle(),
                    trip.getDriver(),
                    trip.getDepartureTime(),
                    trip.getArrivalTime()
            );
        }

        // Cancelling a booked trip requires a coordinated
        // booking/payment cancellation and refund workflow.
        if ("CANCELLED".equals(next)
                && hasActiveBookings(tripId)) {
            throw conflict(
                    "Trip has active bookings. Process cancellations "
                            + "and refunds before cancelling the trip"
            );
        }

        trip.setStatus(next);

        return toProfile(tripRepository.save(trip));
    }

    // ==========================================
    // ADMIN: ALL TRIPS
    // ==========================================

    @Transactional(readOnly = true)
    public List<TripProfile> getAllTrips() {
        return tripRepository.findAll()
                .stream()
                .map(this::toProfile)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<TripProfile> getTrips(
            Pageable pageable
    ) {
        return tripRepository.findAll(pageable)
                .map(this::toProfile);
    }

    // ==========================================
    // TRIPS BY STATUS
    // ==========================================

    @Transactional(readOnly = true)
    public List<TripProfile> getTripsByStatus(
            String status
    ) {
        return tripRepository
                .findByStatusIgnoreCaseOrderByDepartureTimeAsc(
                        requireText(status, "Status")
                )
                .stream()
                .map(this::toProfile)
                .toList();
    }

    // ==========================================
    // PASSENGER: UPCOMING TRIPS
    // ==========================================

    @Transactional(readOnly = true)
    public List<TripProfile> getUpcomingTrips() {
        return tripRepository
                .findByStatusIgnoreCaseAndDepartureTimeGreaterThanEqualOrderByDepartureTimeAsc(
                        "SCHEDULED",
                        LocalDateTime.now()
                )
                .stream()
                .filter(trip ->
                        "ACTIVE".equalsIgnoreCase(
                                trip.getRoute().getStatus()
                        )
                )
                .map(this::toProfile)
                .toList();
    }

    // ==========================================
    // PASSENGER: TRIPS BY ROUTE
    // ==========================================

    @Transactional(readOnly = true)
    public List<TripProfile> getUpcomingTripsByRoute(
            Long routeId
    ) {
        findRoute(routeId);

        return tripRepository
                .findByRouteIdAndDepartureTimeGreaterThanEqualOrderByDepartureTimeAsc(
                        routeId,
                        LocalDateTime.now()
                )
                .stream()
                .filter(trip ->
                        "SCHEDULED".equalsIgnoreCase(
                                trip.getStatus()
                        )
                )
                .map(this::toProfile)
                .toList();
    }

    // ==========================================
    // ADMIN: TRIPS WITHIN DATE RANGE
    // ==========================================

    @Transactional(readOnly = true)
    public List<TripProfile> getTripsBetween(
            LocalDateTime start,
            LocalDateTime end
    ) {
        validateTimes(start, end, false);

        return tripRepository
                .findByDepartureTimeBetweenOrderByDepartureTimeAsc(
                        start,
                        end
                )
                .stream()
                .map(this::toProfile)
                .toList();
    }

    // ==========================================
    // DRIVER: ASSIGNED TRIPS
    // ==========================================

    @Transactional(readOnly = true)
    public List<TripProfile> getDriverTrips(
            Long driverId
    ) {
        findDriver(driverId);

        return tripRepository
                .findByDriverIdOrderByDepartureTimeAsc(
                        driverId
                )
                .stream()
                .map(this::toProfile)
                .toList();
    }

    // ==========================================
    // ADMIN: VEHICLE TRIP HISTORY
    // ==========================================

    @Transactional(readOnly = true)
    public List<TripProfile> getVehicleTrips(
            Long vehicleId
    ) {
        findVehicle(vehicleId);

        return tripRepository
                .findByVehicleIdOrderByDepartureTimeAsc(
                        vehicleId
                )
                .stream()
                .map(this::toProfile)
                .toList();
    }

    // ==========================================
    // ADMIN: TRIP STATISTICS
    // ==========================================

    @Transactional(readOnly = true)
    public TripStatistics getTripStatistics() {
        List<Trip> trips = tripRepository.findAll();

        long scheduled = countStatus(
                trips, "SCHEDULED"
        );

        long inProgress = countStatus(
                trips, "IN_PROGRESS"
        );

        long completed = countStatus(
                trips, "COMPLETED"
        );

        long cancelled = countStatus(
                trips, "CANCELLED"
        );

        long upcoming = trips.stream()
                .filter(trip ->
                        "SCHEDULED".equalsIgnoreCase(
                                trip.getStatus()
                        )
                                && !trip.getDepartureTime()
                                .isBefore(LocalDateTime.now())
                )
                .count();

        return new TripStatistics(
                trips.size(),
                scheduled,
                inProgress,
                completed,
                cancelled,
                upcoming
        );
    }

    // ==========================================
    // ASSIGNMENT VALIDATION
    // ==========================================

    private void validateAssignment(
            Long excludedTripId,
            Vehicle vehicle,
            Driver driver,
            LocalDateTime departure,
            LocalDateTime arrival
    ) {
        if (!"AVAILABLE".equalsIgnoreCase(
                driver.getStatus()
        ) && !"ON_TRIP".equalsIgnoreCase(
                driver.getStatus()
        )) {
            throw conflict(
                    "Driver is not available for assignment"
            );
        }

        if (driver.getLicenseExpiry() == null
                || driver.getLicenseExpiry()
                .isBefore(arrival.toLocalDate())) {
            throw conflict(
                    "Driver licence is expired or expires before trip completion"
            );
        }

        if (!"AVAILABLE".equalsIgnoreCase(
                vehicle.getStatus()
        ) && !"ON_TRIP".equalsIgnoreCase(
                vehicle.getStatus()
        )) {
            throw conflict(
                    "Vehicle is not available for assignment"
            );
        }

        if (vehicle.getNextServiceDate() != null
                && !vehicle.getNextServiceDate()
                .isAfter(arrival.toLocalDate())) {
            throw conflict(
                    "Vehicle requires servicing before or during the trip"
            );
        }

        if (!vehicleService
                .checkVehicleCompliance(
                        vehicle.getId()
                ).compliant()) {
            throw conflict(
                    "Vehicle document or maintenance compliance failed"
            );
        }

        long driverConflicts =
                tripRepository.countDriverScheduleConflicts(
                        driver.getId(),
                        excludedTripId,
                        NON_BLOCKING_STATUSES,
                        departure,
                        arrival
                );

        if (driverConflicts > 0) {
            throw conflict(
                    "Driver already has an overlapping trip"
            );
        }

        long vehicleConflicts =
                tripRepository.countVehicleScheduleConflicts(
                        vehicle.getId(),
                        excludedTripId,
                        NON_BLOCKING_STATUSES,
                        departure,
                        arrival
                );

        if (vehicleConflicts > 0) {
            throw conflict(
                    "Vehicle already has an overlapping trip"
            );
        }

        long maintenanceConflicts =
                maintenanceRepository.countMaintenanceConflicts(
                        vehicle.getId(),
                        departure.toLocalDate(),
                        arrival.toLocalDate()
                );

        if (maintenanceConflicts > 0) {
            throw conflict(
                    "Vehicle has maintenance scheduled during the trip"
            );
        }
    }

    // ==========================================
    // DATABASE LOOKUP HELPERS
    // ==========================================

    private Route findRoute(Long routeId) {
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

    private Vehicle findVehicle(Long vehicleId) {
        if (vehicleId == null) {
            throw badRequest("Vehicle ID is required");
        }

        return vehicleRepository.findById(vehicleId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Vehicle not found"
                        )
                );
    }

    private Driver findDriver(Long driverId) {
        if (driverId == null) {
            throw badRequest("Driver ID is required");
        }

        return driverRepository.findById(driverId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Driver not found"
                        )
                );
    }

    private void validateRoute(Route route) {
        if (!"ACTIVE".equalsIgnoreCase(
                route.getStatus()
        )) {
            throw conflict(
                    "Only active routes can be scheduled"
            );
        }
    }

    private boolean hasActiveBookings(Long tripId) {
        return bookingRepository
                .findActiveBookingsForTrip(
                        tripId,
                        BOOKING_STATUSES
                )
                .size() > 0;
    }

    // ==========================================
    // GENERAL VALIDATION
    // ==========================================

    private void validateTimes(
            LocalDateTime departure,
            LocalDateTime arrival,
            boolean requireFuture
    ) {
        if (departure == null || arrival == null) {
            throw badRequest(
                    "Departure and arrival times are required"
            );
        }

        if (!arrival.isAfter(departure)) {
            throw badRequest(
                    "Arrival must be after departure"
            );
        }

        if (requireFuture
                && !departure.isAfter(
                LocalDateTime.now()
        )) {
            throw badRequest(
                    "Departure must be in the future"
            );
        }
    }

    private void validateFare(BigDecimal fare) {
        if (fare != null && fare.signum() < 0) {
            throw badRequest(
                    "Fare cannot be negative"
            );
        }
    }

    private long countStatus(
            List<Trip> trips,
            String status
    ) {
        return trips.stream()
                .filter(trip ->
                        status.equalsIgnoreCase(
                                trip.getStatus()
                        )
                )
                .count();
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

    // ==========================================
    // ENTITY TO DTO
    // ==========================================

    private TripProfile toProfile(Trip trip) {
        return new TripProfile(
                trip.getId(),
                trip.getRoute().getId(),
                trip.getRoute().getName(),
                trip.getRoute().getOrigin(),
                trip.getRoute().getDestination(),
                trip.getVehicle().getId(),
                trip.getVehicle().getRegistrationNumber(),
                trip.getVehicle().getSeatingCapacity(),
                trip.getDriver().getId(),
                trip.getDriver().getName(),
                trip.getDepartureTime(),
                trip.getArrivalTime(),
                trip.getStatus(),
                trip.getFare(),
                trip.getCreatedAt()
        );
    }

    private ResponseStatusException badRequest(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
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
}
