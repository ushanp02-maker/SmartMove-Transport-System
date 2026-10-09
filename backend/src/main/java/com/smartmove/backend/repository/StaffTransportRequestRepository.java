
package com.smartmove.backend.repository;

import com.smartmove.backend.entity.StaffTransportRequest;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface StaffTransportRequestRepository
        extends JpaRepository<StaffTransportRequest, Long> {

    // ==========================================
    // PASSENGER REQUEST HISTORY
    // ==========================================

    List<StaffTransportRequest>
    findByPassengerIdOrderByCreatedAtDesc(
            Long passengerId
    );

    Page<StaffTransportRequest>
    findByPassengerIdOrderByCreatedAtDesc(
            Long passengerId,
            Pageable pageable
    );

    // ==========================================
    // PASSENGER REQUESTS BY STATUS
    // ==========================================

    List<StaffTransportRequest>
    findByPassengerIdAndStatusIgnoreCaseOrderByCreatedAtDesc(
            Long passengerId,
            String status
    );

    // ==========================================
    // ALL REQUESTS BY STATUS
    // ==========================================

    List<StaffTransportRequest>
    findByStatusIgnoreCaseOrderByCreatedAtDesc(
            String status
    );

    Page<StaffTransportRequest>
    findByStatusIgnoreCaseOrderByCreatedAtDesc(
            String status,
            Pageable pageable
    );

    // ==========================================
    // PENDING REQUESTS
    // ==========================================

    List<StaffTransportRequest>
    findByStatusIgnoreCaseOrderByCreatedAtAsc(
            String status
    );

    // ==========================================
    // REQUESTS MATCHING MULTIPLE STATUSES
    // ==========================================

    List<StaffTransportRequest>
    findByStatusInOrderByCreatedAtDesc(
            Collection<String> statuses
    );

    // ==========================================
    // REQUESTS BY ORGANIZATION
    // ==========================================

    List<StaffTransportRequest>
    findByOrganizationNameContainingIgnoreCaseOrderByCreatedAtDesc(
            String organizationName
    );

    Page<StaffTransportRequest>
    findByOrganizationNameContainingIgnoreCase(
            String organizationName,
            Pageable pageable
    );

    // ==========================================
    // REQUESTS BY JOURNEY TYPE
    // ==========================================

    List<StaffTransportRequest>
    findByJourneyTypeIgnoreCaseOrderByCreatedAtDesc(
            String journeyType
    );

    // ==========================================
    // REQUESTS BY FREQUENCY
    // ==========================================

    List<StaffTransportRequest>
    findByFrequencyIgnoreCaseOrderByCreatedAtDesc(
            String frequency
    );

    // ==========================================
    // REQUESTS BY REQUESTED DATE
    // ==========================================

    List<StaffTransportRequest>
    findByRequestedDateOrderByPickupTimeAsc(
            LocalDate requestedDate
    );

    List<StaffTransportRequest>
    findByRequestedDateBetweenOrderByRequestedDateAscPickupTimeAsc(
            LocalDate startDate,
            LocalDate endDate
    );

    Page<StaffTransportRequest>
    findByRequestedDateBetween(
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    );

    // ==========================================
    // UPCOMING APPROVED REQUESTS
    // ==========================================

    @Query("""
        SELECT r
        FROM StaffTransportRequest r
        WHERE r.status = 'APPROVED'
          AND r.requestedDate >= :today
        ORDER BY r.requestedDate ASC,
                 r.pickupTime ASC
        """)
    List<StaffTransportRequest>
    findUpcomingApprovedRequests(
            @Param("today") LocalDate today
    );

    // ==========================================
    // APPROVED REQUESTS WITHOUT ASSIGNED TRIPS
    // ==========================================

    @Query("""
        SELECT r
        FROM StaffTransportRequest r
        WHERE r.status = 'APPROVED'
          AND r.assignedTrip IS NULL
        ORDER BY r.requestedDate ASC,
                 r.pickupTime ASC
        """)
    List<StaffTransportRequest>
    findApprovedUnassignedRequests();

    // ==========================================
    // ASSIGNED TRIP REQUESTS
    // ==========================================

    List<StaffTransportRequest>
    findByAssignedTripIdOrderByCreatedAtDesc(
            Long tripId
    );

    boolean existsByAssignedTripId(
            Long tripId
    );

    // ==========================================
    // ADMIN REVIEW HISTORY
    // ==========================================

    List<StaffTransportRequest>
    findByReviewedByIdOrderByReviewedAtDesc(
            Long accountId
    );

    // ==========================================
    // REQUESTS CREATED DURING A PERIOD
    // ==========================================

    List<StaffTransportRequest>
    findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
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
    // COUNT REQUESTS BY FREQUENCY
    // ==========================================

    long countByFrequencyIgnoreCase(
            String frequency
    );

    // ==========================================
    // COUNT REQUESTS BY JOURNEY TYPE
    // ==========================================

    long countByJourneyTypeIgnoreCase(
            String journeyType
    );

    // ==========================================
    // COUNT APPROVED REQUESTS WITHOUT A TRIP
    // ==========================================

    @Query("""
        SELECT COUNT(r)
        FROM StaffTransportRequest r
        WHERE r.status = 'APPROVED'
          AND r.assignedTrip IS NULL
        """)
    long countApprovedUnassignedRequests();

    // ==========================================
    // TOTAL PASSENGERS REQUESTED
    // ==========================================

    @Query("""
        SELECT COALESCE(SUM(r.passengerCount), 0)
        FROM StaffTransportRequest r
        WHERE r.status IN :statuses
        """)
    Long sumPassengerCountByStatuses(
            @Param("statuses")
            Collection<String> statuses
    );

    // ==========================================
    // PESSIMISTIC LOCK FOR APPROVAL / ASSIGNMENT
    // ==========================================

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT r
        FROM StaffTransportRequest r
        WHERE r.id = :requestId
        """)
    Optional<StaffTransportRequest> findByIdForUpdate(
            @Param("requestId") Long requestId
    );
}
