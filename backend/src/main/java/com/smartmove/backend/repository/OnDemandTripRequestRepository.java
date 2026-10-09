
package com.smartmove.backend.repository;

import com.smartmove.backend.entity.OnDemandTripRequest;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface OnDemandTripRequestRepository
        extends JpaRepository<OnDemandTripRequest, Long> {

    // ==========================================
    // PASSENGER REQUEST HISTORY
    // ==========================================

    List<OnDemandTripRequest>
    findByPassengerIdOrderByCreatedAtDesc(
            Long passengerId
    );

    Page<OnDemandTripRequest>
    findByPassengerIdOrderByCreatedAtDesc(
            Long passengerId,
            Pageable pageable
    );

    // ==========================================
    // PASSENGER REQUESTS BY STATUS
    // ==========================================

    List<OnDemandTripRequest>
    findByPassengerIdAndStatusIgnoreCaseOrderByCreatedAtDesc(
            Long passengerId,
            String status
    );

    Page<OnDemandTripRequest>
    findByPassengerIdAndStatusIgnoreCaseOrderByCreatedAtDesc(
            Long passengerId,
            String status,
            Pageable pageable
    );

    // ==========================================
    // ALL REQUESTS BY STATUS
    // ==========================================

    List<OnDemandTripRequest>
    findByStatusIgnoreCaseOrderByCreatedAtDesc(
            String status
    );

    Page<OnDemandTripRequest>
    findByStatusIgnoreCaseOrderByCreatedAtDesc(
            String status,
            Pageable pageable
    );

    // ==========================================
    // PENDING REQUESTS
    // OLDEST REQUESTS FIRST
    // ==========================================

    List<OnDemandTripRequest>
    findByStatusIgnoreCaseOrderByCreatedAtAsc(
            String status
    );

    // ==========================================
    // MULTIPLE REQUEST STATUSES
    // ==========================================

    List<OnDemandTripRequest>
    findByStatusInOrderByCreatedAtDesc(
            Collection<String> statuses
    );

    // ==========================================
    // REQUESTS BY SERVICE TYPE
    // ==========================================

    List<OnDemandTripRequest>
    findByServiceTypeIgnoreCaseOrderByCreatedAtDesc(
            String serviceType
    );

    Page<OnDemandTripRequest>
    findByServiceTypeIgnoreCaseOrderByCreatedAtDesc(
            String serviceType,
            Pageable pageable
    );

    // ==========================================
    // REQUESTS BY PICKUP ADDRESS
    // ==========================================

    List<OnDemandTripRequest>
    findByPickupAddressContainingIgnoreCaseOrderByCreatedAtDesc(
            String pickupAddress
    );

    // ==========================================
    // REQUESTS BY DESTINATION ADDRESS
    // ==========================================

    List<OnDemandTripRequest>
    findByDestinationAddressContainingIgnoreCaseOrderByCreatedAtDesc(
            String destinationAddress
    );

    // ==========================================
    // REQUESTS BY PICKUP TIME RANGE
    // ==========================================

    List<OnDemandTripRequest>
    findByRequestedPickupTimeBetweenOrderByRequestedPickupTimeAsc(
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    Page<OnDemandTripRequest>
    findByRequestedPickupTimeBetween(
            LocalDateTime startTime,
            LocalDateTime endTime,
            Pageable pageable
    );

    // ==========================================
    // UPCOMING APPROVED REQUESTS
    // ==========================================

    @Query("""
        SELECT r
        FROM OnDemandTripRequest r
        WHERE r.status = 'APPROVED'
          AND r.requestedPickupTime >= :currentTime
        ORDER BY r.requestedPickupTime ASC
        """)
    List<OnDemandTripRequest> findUpcomingApprovedRequests(
            @Param("currentTime")
            LocalDateTime currentTime
    );

    // ==========================================
    // APPROVED REQUESTS AWAITING ASSIGNMENT
    // ==========================================

    @Query("""
        SELECT r
        FROM OnDemandTripRequest r
        WHERE r.status = 'APPROVED'
          AND r.assignedTrip IS NULL
        ORDER BY r.requestedPickupTime ASC
        """)
    List<OnDemandTripRequest>
    findApprovedUnassignedRequests();

    // ==========================================
    // REQUESTS ASSIGNED TO A TRIP
    // ==========================================

    List<OnDemandTripRequest>
    findByAssignedTripIdOrderByCreatedAtDesc(
            Long tripId
    );

    boolean existsByAssignedTripId(
            Long tripId
    );

    // ==========================================
    // REQUESTS REVIEWED BY ADMIN
    // ==========================================

    List<OnDemandTripRequest>
    findByReviewedByIdOrderByReviewedAtDesc(
            Long accountId
    );

    // ==========================================
    // REQUESTS CREATED WITHIN A PERIOD
    // ==========================================

    List<OnDemandTripRequest>
    findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    // ==========================================
    // REQUESTS BY PASSENGER AND PICKUP PERIOD
    // ==========================================

    List<OnDemandTripRequest>
    findByPassengerIdAndRequestedPickupTimeBetweenOrderByRequestedPickupTimeAsc(
            Long passengerId,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    // ==========================================
    // COUNT REQUESTS BY STATUS
    // ==========================================

    long countByStatusIgnoreCase(
            String status
    );

    // ==========================================
    // COUNT REQUESTS BY PASSENGER
    // ==========================================

    long countByPassengerId(
            Long passengerId
    );

    long countByPassengerIdAndStatusIgnoreCase(
            Long passengerId,
            String status
    );

    // ==========================================
    // COUNT REQUESTS BY SERVICE TYPE
    // ==========================================

    long countByServiceTypeIgnoreCase(
            String serviceType
    );

    // ==========================================
    // COUNT UNASSIGNED APPROVED REQUESTS
    // ==========================================

    @Query("""
        SELECT COUNT(r)
        FROM OnDemandTripRequest r
        WHERE r.status = 'APPROVED'
          AND r.assignedTrip IS NULL
        """)
    long countApprovedUnassignedRequests();

    // ==========================================
    // TOTAL PASSENGERS REQUESTED
    // ==========================================

    @Query("""
        SELECT COALESCE(SUM(r.passengerCount), 0)
        FROM OnDemandTripRequest r
        WHERE r.status IN :statuses
        """)
    Long sumPassengerCountByStatuses(
            @Param("statuses")
            Collection<String> statuses
    );

    // ==========================================
    // TOTAL APPROVED FARES
    // ==========================================

    @Query("""
        SELECT COALESCE(SUM(r.approvedFare), 0)
        FROM OnDemandTripRequest r
        WHERE r.status IN :statuses
          AND r.approvedFare IS NOT NULL
        """)
    BigDecimal sumApprovedFaresByStatuses(
            @Param("statuses")
            Collection<String> statuses
    );

    // ==========================================
    // TOTAL ESTIMATED FARES
    // ==========================================

    @Query("""
        SELECT COALESCE(SUM(r.estimatedFare), 0)
        FROM OnDemandTripRequest r
        WHERE r.status IN :statuses
          AND r.estimatedFare IS NOT NULL
        """)
    BigDecimal sumEstimatedFaresByStatuses(
            @Param("statuses")
            Collection<String> statuses
    );

    // ==========================================
    // LOCK REQUEST FOR APPROVAL / ASSIGNMENT
    // ==========================================

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT r
        FROM OnDemandTripRequest r
        WHERE r.id = :requestId
        """)
    Optional<OnDemandTripRequest> findByIdForUpdate(
            @Param("requestId") Long requestId
    );
}
