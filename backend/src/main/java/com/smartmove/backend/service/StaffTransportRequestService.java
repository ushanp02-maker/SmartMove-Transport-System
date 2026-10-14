
package com.smartmove.backend.service;

import com.smartmove.backend.entity.Passenger;
import com.smartmove.backend.entity.StaffTransportRequest;
import com.smartmove.backend.entity.Trip;
import com.smartmove.backend.entity.UserAccount;

import com.smartmove.backend.repository.PassengerRepository;
import com.smartmove.backend.repository.StaffTransportRequestRepository;
import com.smartmove.backend.repository.TripRepository;
import com.smartmove.backend.repository.UserAccountRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class StaffTransportRequestService {

    private static final String PENDING = "PENDING";
    private static final String APPROVED = "APPROVED";
    private static final String REJECTED = "REJECTED";
    private static final String ASSIGNED = "ASSIGNED";
    private static final String CANCELLED = "CANCELLED";
    private static final String COMPLETED = "COMPLETED";

    private static final Set<String> JOURNEY_TYPES =
            Set.of("ONE_WAY", "ROUND_TRIP");

    private static final Set<String> FREQUENCIES =
            Set.of("ONE_TIME", "DAILY", "WEEKDAYS", "WEEKLY");

    private final StaffTransportRequestRepository requestRepository;
    private final PassengerRepository passengerRepository;
    private final TripRepository tripRepository;
    private final UserAccountRepository userAccountRepository;
    private final CurrentUserService currentUserService;

    public StaffTransportRequestService(
            StaffTransportRequestRepository requestRepository,
            PassengerRepository passengerRepository,
            TripRepository tripRepository,
            UserAccountRepository userAccountRepository,
            CurrentUserService currentUserService
    ) {
        this.requestRepository = requestRepository;
        this.passengerRepository = passengerRepository;
        this.tripRepository = tripRepository;
        this.userAccountRepository = userAccountRepository;
        this.currentUserService = currentUserService;
    }

    // ==========================================
    // REQUEST DTOs
    // ==========================================

    public record CreateStaffRequest(
            String organizationName,
            String contactPerson,
            String contactPhone,
            String contactEmail,
            String origin,
            String destination,
            Double originLatitude,
            Double originLongitude,
            Double destinationLatitude,
            Double destinationLongitude,
            LocalDate requestedDate,
            LocalTime pickupTime,
            LocalTime returnTime,
            Integer passengerCount,
            String journeyType,
            String frequency,
            LocalDate serviceEndDate,
            String specialRequirements
    ) {}

    public record UpdateStaffRequest(
            String organizationName,
            String contactPerson,
            String contactPhone,
            String contactEmail,
            String origin,
            String destination,
            Double originLatitude,
            Double originLongitude,
            Double destinationLatitude,
            Double destinationLongitude,
            LocalDate requestedDate,
            LocalTime pickupTime,
            LocalTime returnTime,
            Integer passengerCount,
            String journeyType,
            String frequency,
            LocalDate serviceEndDate,
            String specialRequirements
    ) {}

    public record ReviewRequest(
            String reviewNotes,
            BigDecimal approvedFare
    ) {}

    public record AssignTripRequest(
            Long tripId
    ) {}

    public record StaffRequestProfile(
            Long id,
            Long passengerId,
            String passengerName,
            String organizationName,
            String contactPerson,
            String contactPhone,
            String contactEmail,
            String origin,
            String destination,
            Double originLatitude,
            Double originLongitude,
            Double destinationLatitude,
            Double destinationLongitude,
            LocalDate requestedDate,
            LocalTime pickupTime,
            LocalTime returnTime,
            Integer passengerCount,
            String journeyType,
            String frequency,
            LocalDate serviceEndDate,
            String specialRequirements,
            String status,
            Long reviewedByAccountId,
            LocalDateTime reviewedAt,
            String reviewNotes,
            Long assignedTripId,
            LocalDateTime assignedAt,
            BigDecimal estimatedFare,
            BigDecimal approvedFare,
            String currency,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime cancelledAt
    ) {}

    public record StaffRequestStatistics(
            long totalRequests,
            long pendingRequests,
            long approvedRequests,
            long rejectedRequests,
            long assignedRequests,
            long cancelledRequests,
            long completedRequests,
            long approvedUnassignedRequests,
            long requestedPassengers
    ) {}

    // ==========================================
    // PASSENGER: CREATE REQUEST
    // ==========================================

    @Transactional
    public StaffRequestProfile createRequest(
            CreateStaffRequest request
    ) {
        Long passengerId =
                currentUserService.getCurrentPassengerId();

        if (request == null) {
            throw badRequest("Request details are required");
        }

        validateRequestDetails(
                request.organizationName(),
                request.origin(),
                request.destination(),
                request.requestedDate(),
                request.pickupTime(),
                request.returnTime(),
                request.passengerCount(),
                request.journeyType(),
                request.frequency(),
                request.serviceEndDate(),
                request.originLatitude(),
                request.originLongitude(),
                request.destinationLatitude(),
                request.destinationLongitude()
        );

        Passenger passenger = passengerRepository
                .findById(passengerId)
                .orElseThrow(() ->
                        notFound("Passenger not found")
                );

        StaffTransportRequest entity =
                new StaffTransportRequest();

        entity.setPassenger(passenger);

        applyDetails(
                entity,
                request.organizationName(),
                request.contactPerson(),
                request.contactPhone(),
                request.contactEmail(),
                request.origin(),
                request.destination(),
                request.originLatitude(),
                request.originLongitude(),
                request.destinationLatitude(),
                request.destinationLongitude(),
                request.requestedDate(),
                request.pickupTime(),
                request.returnTime(),
                request.passengerCount(),
                request.journeyType(),
                request.frequency(),
                request.serviceEndDate(),
                request.specialRequirements()
        );

        entity.setStatus(PENDING);
        entity.setCurrency("LKR");

        return toProfile(
                requestRepository.save(entity)
        );
    }

    // ==========================================
    // PASSENGER: UPDATE PENDING REQUEST
    // ==========================================

    @Transactional
    public StaffRequestProfile updateRequest(
            Long requestId,
            UpdateStaffRequest request
    ) {
        if (request == null) {
            throw badRequest("Request details are required");
        }

        StaffTransportRequest entity =
                findLockedRequest(requestId);

        requireOwner(entity);
        requireStatus(entity, PENDING);

        validateRequestDetails(
                request.organizationName(),
                request.origin(),
                request.destination(),
                request.requestedDate(),
                request.pickupTime(),
                request.returnTime(),
                request.passengerCount(),
                request.journeyType(),
                request.frequency(),
                request.serviceEndDate(),
                request.originLatitude(),
                request.originLongitude(),
                request.destinationLatitude(),
                request.destinationLongitude()
        );

        applyDetails(
                entity,
                request.organizationName(),
                request.contactPerson(),
                request.contactPhone(),
                request.contactEmail(),
                request.origin(),
                request.destination(),
                request.originLatitude(),
                request.originLongitude(),
                request.destinationLatitude(),
                request.destinationLongitude(),
                request.requestedDate(),
                request.pickupTime(),
                request.returnTime(),
                request.passengerCount(),
                request.journeyType(),
                request.frequency(),
                request.serviceEndDate(),
                request.specialRequirements()
        );

        return toProfile(
                requestRepository.save(entity)
        );
    }

    // ==========================================
    // PASSENGER: CANCEL REQUEST
    // ==========================================

    @Transactional
    public StaffRequestProfile cancelMyRequest(
            Long requestId
    ) {
        StaffTransportRequest entity =
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

        return toProfile(
                requestRepository.save(entity)
        );
    }

    // ==========================================
    // PASSENGER: MY REQUESTS
    // ==========================================

    @Transactional(readOnly = true)
    public List<StaffRequestProfile> getMyRequests() {

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
    public Page<StaffRequestProfile> getMyRequestsPage(
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

    // ==========================================
    // GET REQUEST BY ID
    // ==========================================

    @Transactional(readOnly = true)
    public StaffRequestProfile getRequestById(
            Long requestId
    ) {
        StaffTransportRequest entity =
                findRequest(requestId);

        requireOwner(entity);

        return toProfile(entity);
    }

    // ==========================================
    // ADMIN: APPROVE REQUEST
    // ==========================================

    @Transactional
    public StaffRequestProfile approveRequest(
            Long requestId,
            ReviewRequest review
    ) {
        currentUserService.requireAdmin();

        StaffTransportRequest entity =
                findLockedRequest(requestId);

        requireStatus(entity, PENDING);

        if (review != null
                && review.approvedFare() != null
                && review.approvedFare()
                .compareTo(BigDecimal.ZERO) < 0) {
            throw badRequest(
                    "Approved fare cannot be negative"
            );
        }

        UserAccount reviewer = getCurrentReviewer();

        entity.setStatus(APPROVED);
        entity.setReviewedBy(reviewer);
        entity.setReviewedAt(LocalDateTime.now());

        if (review != null) {
            entity.setReviewNotes(
                    clean(review.reviewNotes())
            );

            entity.setApprovedFare(
                    review.approvedFare()
            );
        }

        return toProfile(
                requestRepository.save(entity)
        );
    }

    // ==========================================
    // ADMIN: REJECT REQUEST
    // ==========================================

    @Transactional
    public StaffRequestProfile rejectRequest(
            Long requestId,
            ReviewRequest review
    ) {
        currentUserService.requireAdmin();

        StaffTransportRequest entity =
                findLockedRequest(requestId);

        requireStatus(entity, PENDING);

        if (review == null
                || isBlank(review.reviewNotes())) {
            throw badRequest(
                    "A rejection reason is required"
            );
        }

        entity.setStatus(REJECTED);
        entity.setReviewedBy(getCurrentReviewer());
        entity.setReviewedAt(LocalDateTime.now());
        entity.setReviewNotes(
                clean(review.reviewNotes())
        );

        return toProfile(
                requestRepository.save(entity)
        );
    }

    // ==========================================
    // ADMIN: ASSIGN APPROVED REQUEST TO TRIP
    // ==========================================

    @Transactional
    public StaffRequestProfile assignTrip(
            Long requestId,
            AssignTripRequest request
    ) {
        currentUserService.requireAdmin();

        if (request == null || request.tripId() == null) {
            throw badRequest("Trip ID is required");
        }

        StaffTransportRequest entity =
                findLockedRequest(requestId);

        requireStatus(entity, APPROVED);

        if (entity.getAssignedTrip() != null) {
            throw conflict(
                    "Request already has an assigned trip"
            );
        }

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

        if (!entity.getRequestedDate().equals(
                trip.getDepartureTime().toLocalDate()
        )) {
            throw conflict(
                    "Trip departure date does not match "
                            + "the requested transport date"
            );
        }

        if (!entity.getPickupTime().equals(
                trip.getDepartureTime().toLocalTime()
        )) {
            throw conflict(
                    "Trip departure time must match "
                            + "the requested pickup time"
            );
        }

        if (trip.getVehicle() == null
                || trip.getVehicle().getSeatingCapacity() == null
                || trip.getVehicle().getSeatingCapacity()
                < entity.getPassengerCount()) {
            throw conflict(
                    "Trip vehicle does not have "
                            + "sufficient seating capacity"
            );
        }

        if (trip.getRoute() == null) {
            throw conflict(
                    "Assigned trip must have a route"
            );
        }

        if (!normalizePlace(
                entity.getOrigin()
        ).equals(normalizePlace(
                trip.getRoute().getOrigin()
        ))) {
            throw conflict(
                    "Trip origin does not match request origin"
            );
        }

        if (!normalizePlace(
                entity.getDestination()
        ).equals(normalizePlace(
                trip.getRoute().getDestination()
        ))) {
            throw conflict(
                    "Trip destination does not match "
                            + "request destination"
            );
        }

        if (requestRepository.existsByAssignedTripId(
                trip.getId()
        )) {
            throw conflict(
                    "This trip is already assigned "
                            + "to another staff transport request"
            );
        }

        entity.setAssignedTrip(trip);
        entity.setAssignedAt(LocalDateTime.now());
        entity.setStatus(ASSIGNED);

        return toProfile(
                requestRepository.save(entity)
        );
    }

    // ==========================================
    // ADMIN: MARK REQUEST COMPLETED
    // ==========================================

    @Transactional
    public StaffRequestProfile completeRequest(
            Long requestId
    ) {
        currentUserService.requireAdmin();

        StaffTransportRequest entity =
                findLockedRequest(requestId);

        requireStatus(entity, ASSIGNED);

        Trip trip = entity.getAssignedTrip();

        if (trip == null) {
            throw conflict(
                    "No trip is assigned to this request"
            );
        }

        if (!"COMPLETED".equals(
                normalize(trip.getStatus())
        )) {
            throw conflict(
                    "The assigned trip must be completed first"
            );
        }

        entity.setStatus(COMPLETED);

        return toProfile(
                requestRepository.save(entity)
        );
    }

    // ==========================================
    // ADMIN: CANCEL REQUEST
    // ==========================================

    @Transactional
    public StaffRequestProfile cancelRequestByAdmin(
            Long requestId,
            String reason
    ) {
        currentUserService.requireAdmin();

        StaffTransportRequest entity =
                findLockedRequest(requestId);

        String status = normalize(entity.getStatus());

        if (COMPLETED.equals(status)
                || CANCELLED.equals(status)
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
                        "Cannot cancel a request with an "
                                + "active or completed trip"
                );
            }
        }

        if (isBlank(reason)) {
            throw badRequest(
                    "Cancellation reason is required"
            );
        }

        entity.setStatus(CANCELLED);
        entity.setCancelledAt(LocalDateTime.now());
        entity.setReviewNotes(clean(reason));
        entity.setReviewedBy(getCurrentReviewer());
        entity.setReviewedAt(LocalDateTime.now());

        return toProfile(
                requestRepository.save(entity)
        );
    }

    // ==========================================
    // ADMIN: LIST ALL REQUESTS
    // ==========================================

    @Transactional(readOnly = true)
    public List<StaffRequestProfile> getAllRequests() {

        currentUserService.requireAdmin();

        return requestRepository.findAll()
                .stream()
                .map(this::toProfile)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<StaffRequestProfile> getRequestsPage(
            Pageable pageable
    ) {
        currentUserService.requireAdmin();

        return requestRepository.findAll(pageable)
                .map(this::toProfile);
    }

    // ==========================================
    // ADMIN: FILTER BY STATUS
    // ==========================================

    @Transactional(readOnly = true)
    public List<StaffRequestProfile> getRequestsByStatus(
            String status
    ) {
        currentUserService.requireAdmin();

        return requestRepository
                .findByStatusIgnoreCaseOrderByCreatedAtDesc(
                        normalize(status)
                )
                .stream()
                .map(this::toProfile)
                .toList();
    }

    // ==========================================
    // ADMIN: PENDING REQUESTS
    // ==========================================

    @Transactional(readOnly = true)
    public List<StaffRequestProfile> getPendingRequests() {

        currentUserService.requireAdmin();

        return requestRepository
                .findByStatusIgnoreCaseOrderByCreatedAtAsc(
                        PENDING
                )
                .stream()
                .map(this::toProfile)
                .toList();
    }

    // ==========================================
    // ADMIN: APPROVED UNASSIGNED REQUESTS
    // ==========================================

    @Transactional(readOnly = true)
    public List<StaffRequestProfile>
    getApprovedUnassignedRequests() {

        currentUserService.requireAdmin();

        return requestRepository
                .findApprovedUnassignedRequests()
                .stream()
                .map(this::toProfile)
                .toList();
    }

    // ==========================================
    // ADMIN: SEARCH BY ORGANIZATION
    // ==========================================

    @Transactional(readOnly = true)
    public List<StaffRequestProfile> searchByOrganization(
            String organizationName
    ) {
        currentUserService.requireAdmin();

        if (isBlank(organizationName)) {
            throw badRequest(
                    "Organization search term is required"
            );
        }

        return requestRepository
                .findByOrganizationNameContainingIgnoreCaseOrderByCreatedAtDesc(
                        organizationName.trim()
                )
                .stream()
                .map(this::toProfile)
                .toList();
    }

    // ==========================================
    // ADMIN: REQUESTS BY DATE RANGE
    // ==========================================

    @Transactional(readOnly = true)
    public List<StaffRequestProfile> getRequestsByDateRange(
            LocalDate startDate,
            LocalDate endDate
    ) {
        currentUserService.requireAdmin();

        if (startDate == null
                || endDate == null
                || startDate.isAfter(endDate)) {
            throw badRequest("Invalid date range");
        }

        return requestRepository
                .findByRequestedDateBetweenOrderByRequestedDateAscPickupTimeAsc(
                        startDate,
                        endDate
                )
                .stream()
                .map(this::toProfile)
                .toList();
    }

    // ==========================================
    // ADMIN: REQUESTS BY ASSIGNED TRIP
    // ==========================================

    @Transactional(readOnly = true)
    public List<StaffRequestProfile> getRequestsByTrip(
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
    public StaffRequestStatistics getStatistics() {

        currentUserService.requireAdmin();

        return new StaffRequestStatistics(
                requestRepository.count(),
                requestRepository.countByStatusIgnoreCase(
                        PENDING
                ),
                requestRepository.countByStatusIgnoreCase(
                        APPROVED
                ),
                requestRepository.countByStatusIgnoreCase(
                        REJECTED
                ),
                requestRepository.countByStatusIgnoreCase(
                        ASSIGNED
                ),
                requestRepository.countByStatusIgnoreCase(
                        CANCELLED
                ),
                requestRepository.countByStatusIgnoreCase(
                        COMPLETED
                ),
                requestRepository.countApprovedUnassignedRequests(),
                requestRepository.sumPassengerCountByStatuses(
                        List.of(APPROVED, ASSIGNED, COMPLETED)
                )
        );
    }

    // ==========================================
    // REQUEST VALIDATION
    // ==========================================

    private void validateRequestDetails(
            String organizationName,
            String origin,
            String destination,
            LocalDate requestedDate,
            LocalTime pickupTime,
            LocalTime returnTime,
            Integer passengerCount,
            String journeyType,
            String frequency,
            LocalDate serviceEndDate,
            Double originLatitude,
            Double originLongitude,
            Double destinationLatitude,
            Double destinationLongitude
    ) {
        if (isBlank(organizationName)
                || isBlank(origin)
                || isBlank(destination)) {
            throw badRequest(
                    "Organization, origin and destination "
                            + "are required"
            );
        }

        if (normalizePlace(origin).equals(
                normalizePlace(destination)
        )) {
            throw badRequest(
                    "Origin and destination must differ"
            );
        }

        if (requestedDate == null
                || requestedDate.isBefore(LocalDate.now())) {
            throw badRequest(
                    "Requested date cannot be in the past"
            );
        }

        if (pickupTime == null) {
            throw badRequest(
                    "Pickup time is required"
            );
        }

        if (passengerCount == null
                || passengerCount < 1
                || passengerCount > 1000) {
            throw badRequest(
                    "Passenger count must be between "
                            + "1 and 1000"
            );
        }

        String normalizedJourney =
                normalizeDefault(
                        journeyType,
                        "ONE_WAY"
                );

        String normalizedFrequency =
                normalizeDefault(
                        frequency,
                        "ONE_TIME"
                );

        if (!JOURNEY_TYPES.contains(
                normalizedJourney
        )) {
            throw badRequest(
                    "Journey type must be ONE_WAY "
                            + "or ROUND_TRIP"
            );
        }

        if (!FREQUENCIES.contains(
                normalizedFrequency
        )) {
            throw badRequest(
                    "Invalid transport frequency"
            );
        }

        if ("ROUND_TRIP".equals(normalizedJourney)
                && returnTime == null) {
            throw badRequest(
                    "Return time is required "
                            + "for a round trip"
            );
        }

        if (!"ONE_TIME".equals(normalizedFrequency)) {
            if (serviceEndDate == null
                    || serviceEndDate.isBefore(
                    requestedDate
            )) {
                throw badRequest(
                        "Recurring transport requires "
                                + "a valid service end date"
                );
            }
        }

        validateCoordinatePair(
                originLatitude,
                originLongitude,
                "Origin"
        );

        validateCoordinatePair(
                destinationLatitude,
                destinationLongitude,
                "Destination"
        );
    }

    private void validateCoordinatePair(
            Double latitude,
            Double longitude,
            String label
    ) {
        if (latitude == null && longitude == null) {
            return;
        }

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
    // COPY REQUEST DETAILS TO ENTITY
    // ==========================================

    private void applyDetails(
            StaffTransportRequest entity,
            String organizationName,
            String contactPerson,
            String contactPhone,
            String contactEmail,
            String origin,
            String destination,
            Double originLatitude,
            Double originLongitude,
            Double destinationLatitude,
            Double destinationLongitude,
            LocalDate requestedDate,
            LocalTime pickupTime,
            LocalTime returnTime,
            Integer passengerCount,
            String journeyType,
            String frequency,
            LocalDate serviceEndDate,
            String specialRequirements
    ) {
        entity.setOrganizationName(
                clean(organizationName)
        );
        entity.setContactPerson(clean(contactPerson));
        entity.setContactPhone(clean(contactPhone));
        entity.setContactEmail(clean(contactEmail));

        entity.setOrigin(clean(origin));
        entity.setDestination(clean(destination));

        entity.setOriginLatitude(originLatitude);
        entity.setOriginLongitude(originLongitude);
        entity.setDestinationLatitude(
                destinationLatitude
        );
        entity.setDestinationLongitude(
                destinationLongitude
        );

        entity.setRequestedDate(requestedDate);
        entity.setPickupTime(pickupTime);
        entity.setReturnTime(returnTime);
        entity.setPassengerCount(passengerCount);

        entity.setJourneyType(
                normalizeDefault(
                        journeyType,
                        "ONE_WAY"
                )
        );

        entity.setFrequency(
                normalizeDefault(
                        frequency,
                        "ONE_TIME"
                )
        );

        entity.setServiceEndDate(serviceEndDate);
        entity.setSpecialRequirements(
                clean(specialRequirements)
        );
    }

    // ==========================================
    // ACCESS CONTROL
    // ==========================================

    private void requireOwner(
            StaffTransportRequest entity
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

    private StaffTransportRequest findRequest(
            Long requestId
    ) {
        if (requestId == null || requestId <= 0) {
            throw badRequest(
                    "Valid request ID is required"
            );
        }

        return requestRepository.findById(requestId)
                .orElseThrow(() ->
                        notFound(
                                "Staff transport request not found"
                        )
                );
    }

    private StaffTransportRequest findLockedRequest(
            Long requestId
    ) {
        if (requestId == null || requestId <= 0) {
            throw badRequest(
                    "Valid request ID is required"
            );
        }

        return requestRepository
                .findByIdForUpdate(requestId)
                .orElseThrow(() ->
                        notFound(
                                "Staff transport request not found"
                        )
                );
    }

    // ==========================================
    // STATUS VALIDATION
    // ==========================================

    private void requireStatus(
            StaffTransportRequest entity,
            String requiredStatus
    ) {
        if (!requiredStatus.equals(
                normalize(entity.getStatus())
        )) {
            throw conflict(
                    "Request must have status "
                            + requiredStatus
                            + " for this operation"
            );
        }
    }

    // ==========================================
    // RESPONSE MAPPING
    // ==========================================

    private StaffRequestProfile toProfile(
            StaffTransportRequest entity
    ) {
        return new StaffRequestProfile(
                entity.getId(),
                entity.getPassenger().getId(),
                entity.getPassenger().getName(),
                entity.getOrganizationName(),
                entity.getContactPerson(),
                entity.getContactPhone(),
                entity.getContactEmail(),
                entity.getOrigin(),
                entity.getDestination(),
                entity.getOriginLatitude(),
                entity.getOriginLongitude(),
                entity.getDestinationLatitude(),
                entity.getDestinationLongitude(),
                entity.getRequestedDate(),
                entity.getPickupTime(),
                entity.getReturnTime(),
                entity.getPassengerCount(),
                entity.getJourneyType(),
                entity.getFrequency(),
                entity.getServiceEndDate(),
                entity.getSpecialRequirements(),
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
                entity.getEstimatedFare(),
                entity.getApprovedFare(),
                entity.getCurrency(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getCancelledAt()
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

    private String normalizeDefault(
            String value,
            String defaultValue
    ) {
        return isBlank(value)
                ? defaultValue
                : normalize(value);
    }

    private String normalizePlace(String value) {
        return value == null
                ? ""
                : value.trim()
                  .replaceAll("\\s+", " ")
                  .toLowerCase(Locale.ROOT);
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
