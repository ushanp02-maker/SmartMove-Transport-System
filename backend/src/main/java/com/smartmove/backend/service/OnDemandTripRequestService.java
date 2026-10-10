
package com.smartmove.backend.service;

import com.smartmove.backend.entity.OnDemandTripRequest;
import com.smartmove.backend.entity.Passenger;
import com.smartmove.backend.entity.Trip;
import com.smartmove.backend.entity.UserAccount;

import com.smartmove.backend.repository.OnDemandTripRequestRepository;
import com.smartmove.backend.repository.PassengerRepository;
import com.smartmove.backend.repository.TripRepository;
import com.smartmove.backend.repository.UserAccountRepository;
import com.smartmove.backend.repository.StaffTransportRequestRepository;
import com.smartmove.backend.repository.BookingRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class OnDemandTripRequestService {

    private static final String PENDING = "PENDING";
    private static final String APPROVED = "APPROVED";
    private static final String REJECTED = "REJECTED";
    private static final String ASSIGNED = "ASSIGNED";
    private static final String CANCELLED = "CANCELLED";
    private static final String COMPLETED = "COMPLETED";

    private static final Set<String> SERVICE_TYPES =
            Set.of("STANDARD", "STAFF", "CUSTOM");

    private static final double EARTH_RADIUS_KM = 6371.0088;

    // Geographic straight-line distance is an estimate,
    // not the actual road distance.
    private static final double MINIMUM_LOCATION_SEPARATION_KM =
            0.05;

    private final OnDemandTripRequestRepository requestRepository;
    private final PassengerRepository passengerRepository;
    private final TripRepository tripRepository;
    private final UserAccountRepository userAccountRepository;
    private final StaffTransportRequestRepository staffRequestRepository;
    private final BookingRepository bookingRepository;
    private final CurrentUserService currentUserService;

    public OnDemandTripRequestService(
            OnDemandTripRequestRepository requestRepository,
            PassengerRepository passengerRepository,
            TripRepository tripRepository,
            UserAccountRepository userAccountRepository,
            StaffTransportRequestRepository staffRequestRepository,
            BookingRepository bookingRepository,
            CurrentUserService currentUserService
    ) {
        this.requestRepository = requestRepository;
        this.passengerRepository = passengerRepository;
        this.tripRepository = tripRepository;
        this.userAccountRepository = userAccountRepository;
        this.staffRequestRepository = staffRequestRepository;
        this.bookingRepository = bookingRepository;
        this.currentUserService = currentUserService;
    }

    // ==========================================
    // REQUEST AND RESPONSE DTOs
    // ==========================================

    public record CreateOnDemandRequest(
            String pickupAddress,
            Double pickupLatitude,
            Double pickupLongitude,
            String destinationAddress,
            Double destinationLatitude,
            Double destinationLongitude,
            LocalDateTime requestedPickupTime,
            Integer passengerCount,
            String serviceType,
            String specialRequirements
    ) {}

    public record UpdateOnDemandRequest(
            String pickupAddress,
            Double pickupLatitude,
            Double pickupLongitude,
            String destinationAddress,
            Double destinationLatitude,
            Double destinationLongitude,
            LocalDateTime requestedPickupTime,
            Integer passengerCount,
            String serviceType,
            String specialRequirements
    ) {}

    public record ReviewRequest(
            String reviewNotes,
            BigDecimal approvedFare
    ) {}

    public record AssignTripRequest(
            Long tripId
    ) {}

    public record CancelRequest(
            String reason
    ) {}

    public record DistanceEstimate(
            double straightLineDistanceKm,
            String distanceType
    ) {}

    public record OnDemandRequestProfile(
            Long id,
            Long passengerId,
            String passengerName,
            String pickupAddress,
            Double pickupLatitude,
            Double pickupLongitude,
            String destinationAddress,
            Double destinationLatitude,
            Double destinationLongitude,
            LocalDateTime requestedPickupTime,
            Integer passengerCount,
            String serviceType,
            String specialRequirements,
            BigDecimal estimatedDistanceKm,
            Integer estimatedDurationMinutes,
            BigDecimal estimatedFare,
            BigDecimal approvedFare,
            String currency,
            String status,
            Long reviewedByAccountId,
            LocalDateTime reviewedAt,
            String reviewNotes,
            Long assignedTripId,
            LocalDateTime assignedAt,
            LocalDateTime cancelledAt,
            String cancellationReason,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record OnDemandStatistics(
            long totalRequests,
            long pendingRequests,
            long approvedRequests,
            long rejectedRequests,
            long assignedRequests,
            long cancelledRequests,
            long completedRequests,
            long approvedUnassignedRequests,
            long requestedPassengers,
            BigDecimal totalApprovedFares
    ) {}

    // ==========================================
    // PASSENGER: CREATE REQUEST
    // ==========================================

    @Transactional
    public OnDemandRequestProfile createRequest(
            CreateOnDemandRequest request
    ) {
        currentUserService.requirePassenger();

        if (request == null) {
            throw badRequest("Request details are required");
        }

        validateDetails(
                request.pickupAddress(),
                request.pickupLatitude(),
                request.pickupLongitude(),
                request.destinationAddress(),
                request.destinationLatitude(),
                request.destinationLongitude(),
                request.requestedPickupTime(),
                request.passengerCount(),
                request.serviceType(),
                request.specialRequirements()
        );

        Long passengerId =
                currentUserService.getCurrentPassengerId();

        Passenger passenger = passengerRepository
                .findById(passengerId)
                .orElseThrow(() ->
                        notFound("Passenger not found")
                );

        OnDemandTripRequest entity =
                new OnDemandTripRequest();

        entity.setPassenger(passenger);

        applyDetails(
                entity,
                request.pickupAddress(),
                request.pickupLatitude(),
                request.pickupLongitude(),
                request.destinationAddress(),
                request.destinationLatitude(),
                request.destinationLongitude(),
                request.requestedPickupTime(),
                request.passengerCount(),
                request.serviceType(),
                request.specialRequirements()
        );

        entity.setStatus(PENDING);
        entity.setCurrency("LKR");

        return toProfile(requestRepository.save(entity));
    }

    // ==========================================
    // PASSENGER: UPDATE PENDING REQUEST
    // ==========================================

    @Transactional
    public OnDemandRequestProfile updateRequest(
            Long requestId,
            UpdateOnDemandRequest request
    ) {
        currentUserService.requirePassenger();

        if (request == null) {
            throw badRequest("Request details are required");
        }

        OnDemandTripRequest entity =
                findLockedRequest(requestId);

        requireOwner(entity);
        requireStatus(entity, PENDING);

        validateDetails(
                request.pickupAddress(),
                request.pickupLatitude(),
                request.pickupLongitude(),
                request.destinationAddress(),
                request.destinationLatitude(),
                request.destinationLongitude(),
                request.requestedPickupTime(),
                request.passengerCount(),
                request.serviceType(),
                request.specialRequirements()
        );

        applyDetails(
                entity,
                request.pickupAddress(),
                request.pickupLatitude(),
                request.pickupLongitude(),
                request.destinationAddress(),
                request.destinationLatitude(),
                request.destinationLongitude(),
                request.requestedPickupTime(),
                request.passengerCount(),
                request.serviceType(),
                request.specialRequirements()
        );

        return toProfile(requestRepository.save(entity));
    }

    // ==========================================
    // PASSENGER: CANCEL OWN REQUEST
    // ==========================================

    @Transactional
    public OnDemandRequestProfile cancelMyRequest(
            Long requestId,
            CancelRequest request
    ) {
        currentUserService.requirePassenger();

        OnDemandTripRequest entity =
                findLockedRequest(requestId);

        requireOwner(entity);

        String status = normalize(entity.getStatus());

        if (!PENDING.equals(status)
                && !APPROVED.equals(status)) {
            throw conflict(
                    "Only pending or unassigned approved "
                            + "requests can be cancelled"
            );
        }

        if (entity.getAssignedTrip() != null) {
            throw conflict(
                    "An assigned request must be handled "
                            + "by an administrator"
            );
        }

        entity.setStatus(CANCELLED);
        entity.setCancelledAt(LocalDateTime.now());

        if (request != null) {
            entity.setCancellationReason(
                    clean(request.reason())
            );
        }

        return toProfile(requestRepository.save(entity));
    }

    // ==========================================
    // PASSENGER: REQUEST HISTORY
    // ==========================================

    @Transactional(readOnly = true)
    public List<OnDemandRequestProfile> getMyRequests() {

        Long passengerId =
                currentUserService.getCurrentPassengerId();

        return requestRepository
                .findByPassengerIdOrderByCreatedAtDesc(
                        passengerId
                )
                .stream()
                .map(this::toProfile)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<OnDemandRequestProfile> getMyRequestsPage(
            Pageable pageable
    ) {
        Long passengerId =
                currentUserService.getCurrentPassengerId();

        return requestRepository
                .findByPassengerIdOrderByCreatedAtDesc(
                        passengerId,
                        pageable
                )
                .map(this::toProfile);
    }

    @Transactional(readOnly = true)
    public List<OnDemandRequestProfile> getMyRequestsByStatus(
            String status
    ) {
        Long passengerId =
                currentUserService.getCurrentPassengerId();

        return requestRepository
                .findByPassengerIdAndStatusIgnoreCaseOrderByCreatedAtDesc(
                        passengerId,
                        validateStatus(status)
                )
                .stream()
                .map(this::toProfile)
                .toList();
    }

    // ==========================================
    // OWNER / ADMIN: GET REQUEST
    // ==========================================

    @Transactional(readOnly = true)
    public OnDemandRequestProfile getRequestById(
            Long requestId
    ) {
        OnDemandTripRequest entity =
                findRequest(requestId);

        requireOwner(entity);

        return toProfile(entity);
    }

    // ==========================================
    // ADMIN: APPROVE REQUEST
    // ==========================================

    @Transactional
    public OnDemandRequestProfile approveRequest(
            Long requestId,
            ReviewRequest review
    ) {
        currentUserService.requireAdmin();

        OnDemandTripRequest entity =
                findLockedRequest(requestId);

        requireStatus(entity, PENDING);
        ensurePickupStillFuture(entity);

        if (review != null
                && review.approvedFare() != null
                && review.approvedFare()
                .compareTo(BigDecimal.ZERO) < 0) {
            throw badRequest(
                    "Approved fare cannot be negative"
            );
        }

        entity.setStatus(APPROVED);
        entity.setReviewedBy(getCurrentReviewer());
        entity.setReviewedAt(LocalDateTime.now());

        if (review != null) {
            entity.setReviewNotes(clean(review.reviewNotes()));
            entity.setApprovedFare(review.approvedFare());
        }

        return toProfile(requestRepository.save(entity));
    }

    // ==========================================
    // ADMIN: REJECT REQUEST
    // ==========================================

    @Transactional
    public OnDemandRequestProfile rejectRequest(
            Long requestId,
            ReviewRequest review
    ) {
        currentUserService.requireAdmin();

        OnDemandTripRequest entity =
                findLockedRequest(requestId);

        requireStatus(entity, PENDING);

        if (review == null || isBlank(review.reviewNotes())) {
            throw badRequest(
                    "A rejection reason is required"
            );
        }

        entity.setStatus(REJECTED);
        entity.setReviewedBy(getCurrentReviewer());
        entity.setReviewedAt(LocalDateTime.now());
        entity.setReviewNotes(clean(review.reviewNotes()));

        return toProfile(requestRepository.save(entity));
    }

    // ==========================================
    // ADMIN: ASSIGN APPROVED REQUEST TO TRIP
    // ==========================================

    @Transactional
    public OnDemandRequestProfile assignTrip(
            Long requestId,
            AssignTripRequest request
    ) {
        currentUserService.requireAdmin();

        if (request == null
                || request.tripId() == null
                || request.tripId() <= 0) {
            throw badRequest("Valid trip ID is required");
        }

        OnDemandTripRequest entity =
                findLockedRequest(requestId);

        requireStatus(entity, APPROVED);
        ensurePickupStillFuture(entity);

        Trip trip = tripRepository
                .findById(request.tripId())
                .orElseThrow(() ->
                        notFound("Trip not found")
                );

        if (!"SCHEDULED".equals(
                normalize(trip.getStatus())
        )) {
            throw conflict(
                    "Only scheduled trips can be assigned"
            );
        }

        if (trip.getDepartureTime() == null
                || trip.getDepartureTime()
                .isBefore(LocalDateTime.now())) {
            throw conflict(
                    "The assigned trip must be in the future"
            );
        }

        // Permit a maximum 30-minute pickup-time difference.
        long differenceMinutes = Math.abs(
                Duration.between(
                        entity.getRequestedPickupTime(),
                        trip.getDepartureTime()
                ).toMinutes()
        );

        if (differenceMinutes > 30) {
            throw conflict(
                    "Trip departure time must be within "
                            + "30 minutes of requested pickup"
            );
        }

        if (trip.getRoute() == null
                || trip.getVehicle() == null) {
            throw conflict(
                    "Trip must have a route and vehicle"
            );
        }

        if (trip.getVehicle().getSeatingCapacity() == null
                || trip.getVehicle().getSeatingCapacity()
                < entity.getPassengerCount()) {
            throw conflict(
                    "Trip vehicle does not have enough seats"
            );
        }

        // Avoid assigning the same trip to another
        // on-demand or staff transport request.
        if (requestRepository.existsByAssignedTripId(
                trip.getId()
        ) || staffRequestRepository.existsByAssignedTripId(
                trip.getId()
        )) {
            throw conflict(
                    "This trip is already assigned "
                            + "to another transport request"
            );
        }

        // Avoid attaching a private on-demand request
        // to a trip that already has seat bookings.
        if (!bookingRepository
                .findByTripId(trip.getId()).isEmpty()) {
            throw conflict(
                    "Trip already has passenger bookings"
            );
        }

        // Coordinate validation:
        // The assigned route must have origin and destination
        // coordinates available for accurate map matching.
        //
        // Route currently stores text endpoints, while
        // RouteStop stores coordinates. Until automated
        // matching is implemented, administrators must
        // verify the geographic route manually.

        entity.setAssignedTrip(trip);
        entity.setAssignedAt(LocalDateTime.now());
        entity.setStatus(ASSIGNED);

        return toProfile(requestRepository.save(entity));
    }

    // ==========================================
    // ADMIN: COMPLETE REQUEST
    // ==========================================

    @Transactional
    public OnDemandRequestProfile completeRequest(
            Long requestId
    ) {
        currentUserService.requireAdmin();

        OnDemandTripRequest entity =
                findLockedRequest(requestId);

        requireStatus(entity, ASSIGNED);

        Trip trip = entity.getAssignedTrip();

        if (trip == null
                || !"COMPLETED".equals(
                normalize(trip.getStatus())
        )) {
            throw conflict(
                    "Assigned trip must be completed first"
            );
        }

        entity.setStatus(COMPLETED);

        return toProfile(requestRepository.save(entity));
    }

    // ==========================================
    // ADMIN: CANCEL REQUEST
    // ==========================================

    @Transactional
    public OnDemandRequestProfile cancelByAdmin(
            Long requestId,
            CancelRequest request
    ) {
        currentUserService.requireAdmin();

        if (request == null || isBlank(request.reason())) {
            throw badRequest(
                    "Cancellation reason is required"
            );
        }

        OnDemandTripRequest entity =
                findLockedRequest(requestId);

        String status = normalize(entity.getStatus());

        if (CANCELLED.equals(status)
                || COMPLETED.equals(status)
                || REJECTED.equals(status)) {
            throw conflict(
                    "This request cannot be cancelled"
            );
        }

        if (entity.getAssignedTrip() != null) {
            String tripStatus = normalize(
                    entity.getAssignedTrip().getStatus()
            );

            if ("IN_PROGRESS".equals(tripStatus)
                    || "PAUSED".equals(tripStatus)
                    || "COMPLETED".equals(tripStatus)) {
                throw conflict(
                        "Cannot cancel a request whose "
                                + "trip is active or completed"
                );
            }
        }

        entity.setStatus(CANCELLED);
        entity.setCancelledAt(LocalDateTime.now());
        entity.setCancellationReason(
                clean(request.reason())
        );
        entity.setReviewedBy(getCurrentReviewer());
        entity.setReviewedAt(LocalDateTime.now());

        return toProfile(requestRepository.save(entity));
    }

    // ==========================================
    // ADMIN: LIST REQUESTS
    // ==========================================

    @Transactional(readOnly = true)
    public List<OnDemandRequestProfile> getAllRequests() {

        currentUserService.requireAdmin();

        return requestRepository.findAll()
                .stream()
                .map(this::toProfile)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<OnDemandRequestProfile> getRequestsPage(
            Pageable pageable
    ) {
        currentUserService.requireAdmin();

        return requestRepository.findAll(pageable)
                .map(this::toProfile);
    }

    @Transactional(readOnly = true)
    public List<OnDemandRequestProfile> getPendingRequests() {

        currentUserService.requireAdmin();

        return requestRepository
                .findByStatusIgnoreCaseOrderByCreatedAtAsc(
                        PENDING
                )
                .stream()
                .map(this::toProfile)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OnDemandRequestProfile>
    getApprovedUnassignedRequests() {

        currentUserService.requireAdmin();

        return requestRepository
                .findApprovedUnassignedRequests()
                .stream()
                .map(this::toProfile)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OnDemandRequestProfile> getByStatus(
            String status
    ) {
        currentUserService.requireAdmin();

        return requestRepository
                .findByStatusIgnoreCaseOrderByCreatedAtDesc(
                        validateStatus(status)
                )
                .stream()
                .map(this::toProfile)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OnDemandRequestProfile> getByServiceType(
            String serviceType
    ) {
        currentUserService.requireAdmin();

        return requestRepository
                .findByServiceTypeIgnoreCaseOrderByCreatedAtDesc(
                        validateServiceType(serviceType)
                )
                .stream()
                .map(this::toProfile)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OnDemandRequestProfile> searchByPickup(
            String query
    ) {
        currentUserService.requireAdmin();

        if (isBlank(query)) {
            throw badRequest("Pickup search term is required");
        }

        return requestRepository
                .findByPickupAddressContainingIgnoreCaseOrderByCreatedAtDesc(
                        query.trim()
                )
                .stream()
                .map(this::toProfile)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OnDemandRequestProfile> searchByDestination(
            String query
    ) {
        currentUserService.requireAdmin();

        if (isBlank(query)) {
            throw badRequest(
                    "Destination search term is required"
            );
        }

        return requestRepository
                .findByDestinationAddressContainingIgnoreCaseOrderByCreatedAtDesc(
                        query.trim()
                )
                .stream()
                .map(this::toProfile)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OnDemandRequestProfile> getByPickupPeriod(
            LocalDateTime start,
            LocalDateTime end
    ) {
        currentUserService.requireAdmin();

        if (start == null
                || end == null
                || start.isAfter(end)) {
            throw badRequest("Invalid pickup date range");
        }

        return requestRepository
                .findByRequestedPickupTimeBetweenOrderByRequestedPickupTimeAsc(
                        start,
                        end
                )
                .stream()
                .map(this::toProfile)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OnDemandRequestProfile> getByTrip(
            Long tripId
    ) {
        currentUserService.requireAdmin();

        return requestRepository
                .findByAssignedTripIdOrderByCreatedAtDesc(
                        tripId
                )
                .stream()
                .map(this::toProfile)
                .toList();
    }

    // ==========================================
    // ADMIN: STATISTICS
    // ==========================================

    @Transactional(readOnly = true)
    public OnDemandStatistics getStatistics() {

        currentUserService.requireAdmin();

        List<String> approvedStatuses =
                List.of(APPROVED, ASSIGNED, COMPLETED);

        Long passengerTotal =
                requestRepository.sumPassengerCountByStatuses(
                        approvedStatuses
                );

        BigDecimal fareTotal =
                requestRepository.sumApprovedFaresByStatuses(
                        approvedStatuses
                );

        return new OnDemandStatistics(
                requestRepository.count(),
                requestRepository.countByStatusIgnoreCase(PENDING),
                requestRepository.countByStatusIgnoreCase(APPROVED),
                requestRepository.countByStatusIgnoreCase(REJECTED),
                requestRepository.countByStatusIgnoreCase(ASSIGNED),
                requestRepository.countByStatusIgnoreCase(CANCELLED),
                requestRepository.countByStatusIgnoreCase(COMPLETED),
                requestRepository.countApprovedUnassignedRequests(),
                passengerTotal == null ? 0L : passengerTotal,
                fareTotal == null
                        ? BigDecimal.ZERO
                        : fareTotal
        );
    }

    // ==========================================
    // PUBLIC DISTANCE ESTIMATION
    // ==========================================

    public DistanceEstimate estimateDistance(
            Double pickupLatitude,
            Double pickupLongitude,
            Double destinationLatitude,
            Double destinationLongitude
    ) {
        validateCoordinates(
                pickupLatitude,
                pickupLongitude,
                "Pickup"
        );

        validateCoordinates(
                destinationLatitude,
                destinationLongitude,
                "Destination"
        );

        double distance = calculateHaversineKm(
                pickupLatitude,
                pickupLongitude,
                destinationLatitude,
                destinationLongitude
        );

        return new DistanceEstimate(
                roundDistance(distance),
                "STRAIGHT_LINE"
        );
    }

    // ==========================================
    // INPUT VALIDATION
    // ==========================================

    private void validateDetails(
            String pickupAddress,
            Double pickupLatitude,
            Double pickupLongitude,
            String destinationAddress,
            Double destinationLatitude,
            Double destinationLongitude,
            LocalDateTime pickupTime,
            Integer passengerCount,
            String serviceType,
            String specialRequirements
    ) {
        if (isBlank(pickupAddress)
                || isBlank(destinationAddress)) {
            throw badRequest(
                    "Pickup and destination addresses "
                            + "are required"
            );
        }

        if (pickupAddress.trim().length() > 250
                || destinationAddress.trim().length() > 250) {
            throw badRequest(
                    "Address cannot exceed 250 characters"
            );
        }

        validateCoordinates(
                pickupLatitude,
                pickupLongitude,
                "Pickup"
        );

        validateCoordinates(
                destinationLatitude,
                destinationLongitude,
                "Destination"
        );

        double distance = calculateHaversineKm(
                pickupLatitude,
                pickupLongitude,
                destinationLatitude,
                destinationLongitude
        );

        if (distance < MINIMUM_LOCATION_SEPARATION_KM) {
            throw badRequest(
                    "Pickup and destination must be "
                            + "at least 50 metres apart"
            );
        }

        if (pickupTime == null
                || !pickupTime.isAfter(LocalDateTime.now())) {
            throw badRequest(
                    "Requested pickup time must be "
                            + "in the future"
            );
        }

        if (passengerCount == null
                || passengerCount < 1
                || passengerCount > 1000) {
            throw badRequest(
                    "Passenger count must be "
                            + "between 1 and 1000"
            );
        }

        validateServiceType(serviceType);

        if (specialRequirements != null
                && specialRequirements.length() > 2000) {
            throw badRequest(
                    "Special requirements cannot exceed "
                            + "2000 characters"
            );
        }
    }

    private void validateCoordinates(
            Double latitude,
            Double longitude,
            String label
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
                    label + " coordinates are invalid"
            );
        }
    }

    // ==========================================
    // ENTITY FIELD MAPPING
    // ==========================================

    private void applyDetails(
            OnDemandTripRequest entity,
            String pickupAddress,
            Double pickupLatitude,
            Double pickupLongitude,
            String destinationAddress,
            Double destinationLatitude,
            Double destinationLongitude,
            LocalDateTime pickupTime,
            Integer passengerCount,
            String serviceType,
            String specialRequirements
    ) {
        entity.setPickupAddress(clean(pickupAddress));
        entity.setPickupLatitude(pickupLatitude);
        entity.setPickupLongitude(pickupLongitude);

        entity.setDestinationAddress(
                clean(destinationAddress)
        );
        entity.setDestinationLatitude(
                destinationLatitude
        );
        entity.setDestinationLongitude(
                destinationLongitude
        );

        entity.setRequestedPickupTime(pickupTime);
        entity.setPassengerCount(passengerCount);
        entity.setServiceType(
                validateServiceType(serviceType)
        );
        entity.setSpecialRequirements(
                clean(specialRequirements)
        );

        double distance = calculateHaversineKm(
                pickupLatitude,
                pickupLongitude,
                destinationLatitude,
                destinationLongitude
        );

        entity.setEstimatedDistanceKm(
                BigDecimal.valueOf(distance)
                        .setScale(2, RoundingMode.HALF_UP)
        );

        // No road-routing provider is connected yet.
        // Do not invent travel time or a fare.
        entity.setEstimatedDurationMinutes(null);
        entity.setEstimatedFare(null);
    }

    // ==========================================
    // AUTHORIZATION HELPERS
    // ==========================================

    private void requireOwner(
            OnDemandTripRequest entity
    ) {
        currentUserService.requirePassengerOwnership(
                entity.getPassenger().getId()
        );
    }

    private UserAccount getCurrentReviewer() {

        Long accountId =
                currentUserService.getCurrentAccountId();

        return userAccountRepository
                .findById(accountId)
                .orElseThrow(() ->
                        notFound("Reviewer account not found")
                );
    }

    // ==========================================
    // ENTITY LOOKUP
    // ==========================================

    private OnDemandTripRequest findRequest(
            Long requestId
    ) {
        validateId(requestId);

        return requestRepository.findById(requestId)
                .orElseThrow(() ->
                        notFound(
                                "On-demand request not found"
                        )
                );
    }

    private OnDemandTripRequest findLockedRequest(
            Long requestId
    ) {
        validateId(requestId);

        return requestRepository
                .findByIdForUpdate(requestId)
                .orElseThrow(() ->
                        notFound(
                                "On-demand request not found"
                        )
                );
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw badRequest("Valid ID is required");
        }
    }

    // ==========================================
    // STATUS VALIDATION
    // ==========================================

    private void requireStatus(
            OnDemandTripRequest entity,
            String expectedStatus
    ) {
        if (!expectedStatus.equals(
                normalize(entity.getStatus())
        )) {
            throw conflict(
                    "Request must be " + expectedStatus
                            + " for this operation"
            );
        }
    }

    private String validateStatus(String status) {

        String value = normalize(status);

        if (!Set.of(
                PENDING,
                APPROVED,
                REJECTED,
                ASSIGNED,
                CANCELLED,
                COMPLETED
        ).contains(value)) {
            throw badRequest("Invalid request status");
        }

        return value;
    }

    private String validateServiceType(
            String serviceType
    ) {
        String value = isBlank(serviceType)
                ? "STANDARD"
                : normalize(serviceType);

        if (!SERVICE_TYPES.contains(value)) {
            throw badRequest(
                    "Invalid service type"
            );
        }

        return value;
    }

    private void ensurePickupStillFuture(
            OnDemandTripRequest entity
    ) {
        if (entity.getRequestedPickupTime() == null
                || !entity.getRequestedPickupTime()
                .isAfter(LocalDateTime.now())) {
            throw conflict(
                    "Requested pickup time has already passed"
            );
        }
    }

    // ==========================================
    // DISTANCE CALCULATION
    // ==========================================

    private double calculateHaversineKm(
            double lat1,
            double lon1,
            double lat2,
            double lon2
    ) {
        double latitudeDifference =
                Math.toRadians(lat2 - lat1);

        double longitudeDifference =
                Math.toRadians(lon2 - lon1);

        double a =
                Math.pow(
                        Math.sin(latitudeDifference / 2),
                        2
                )
                        + Math.cos(Math.toRadians(lat1))
                        * Math.cos(Math.toRadians(lat2))
                        * Math.pow(
                        Math.sin(longitudeDifference / 2),
                        2
                );

        a = Math.max(0.0, Math.min(1.0, a));

        return 2 * EARTH_RADIUS_KM
                * Math.atan2(
                Math.sqrt(a),
                Math.sqrt(1 - a)
        );
    }

    private double roundDistance(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    // ==========================================
    // RESPONSE MAPPING
    // ==========================================

    private OnDemandRequestProfile toProfile(
            OnDemandTripRequest entity
    ) {
        return new OnDemandRequestProfile(
                entity.getId(),
                entity.getPassenger().getId(),
                entity.getPassenger().getName(),
                entity.getPickupAddress(),
                entity.getPickupLatitude(),
                entity.getPickupLongitude(),
                entity.getDestinationAddress(),
                entity.getDestinationLatitude(),
                entity.getDestinationLongitude(),
                entity.getRequestedPickupTime(),
                entity.getPassengerCount(),
                entity.getServiceType(),
                entity.getSpecialRequirements(),
                entity.getEstimatedDistanceKm(),
                entity.getEstimatedDurationMinutes(),
                entity.getEstimatedFare(),
                entity.getApprovedFare(),
                entity.getCurrency(),
                entity.getStatus(),
                entity.getReviewedBy() == null
                        ? null
                        : entity.getReviewedBy().getId(),
                entity.getReviewedAt(),
                entity.getReviewNotes(),
                entity.getAssignedTrip() == null
                        ? null
                        : entity.getAssignedTrip().getId(),
                entity.getAssignedAt(),
                entity.getCancelledAt(),
                entity.getCancellationReason(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    // ==========================================
    // STRING HELPERS
    // ==========================================

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.trim().toUpperCase(Locale.ROOT);
    }

    // ==========================================
    // HTTP ERROR HELPERS
    // ==========================================

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

    private ResponseStatusException notFound(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                message
        );
    }
}
