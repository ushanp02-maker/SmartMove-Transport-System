
package com.smartmove.backend.repository;

import com.smartmove.backend.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TripRepository
        extends JpaRepository<Trip, Long> {

    // Retrieve a trip with its route, vehicle and driver.
    @Query("""
        SELECT t FROM Trip t
        JOIN FETCH t.route
        JOIN FETCH t.vehicle
        JOIN FETCH t.driver
        WHERE t.id = :tripId
        """)
    Optional<Trip> findDetailedById(
            @Param("tripId") Long tripId
    );

    // Search upcoming trips for a particular route.
    List<Trip> findByRouteIdAndDepartureTimeGreaterThanEqualOrderByDepartureTimeAsc(
            Long routeId,
            LocalDateTime departureTime
    );

    // Find trips within a date/time range.
    List<Trip> findByDepartureTimeBetweenOrderByDepartureTimeAsc(
            LocalDateTime start,
            LocalDateTime end
    );

    // Retrieve trips assigned to a driver.
    List<Trip> findByDriverIdOrderByDepartureTimeAsc(
            Long driverId
    );

    // Retrieve trips assigned to a vehicle.
    List<Trip> findByVehicleIdOrderByDepartureTimeAsc(
            Long vehicleId
    );

    // Filter trips by operational status.
    List<Trip> findByStatusIgnoreCaseOrderByDepartureTimeAsc(
            String status
    );

    // Find upcoming scheduled trips.
    List<Trip> findByStatusIgnoreCaseAndDepartureTimeGreaterThanEqualOrderByDepartureTimeAsc(
            String status,
            LocalDateTime departureTime
    );

    // Detect overlapping driver assignments.
    // Existing start < proposed end AND existing end > proposed start.
    @Query("""
        SELECT COUNT(t) FROM Trip t
        WHERE t.driver.id = :driverId
          AND t.id <> :excludedTripId
          AND t.status NOT IN :excludedStatuses
          AND t.departureTime < :endTime
          AND t.arrivalTime > :startTime
        """)
    long countDriverScheduleConflicts(
            @Param("driverId") Long driverId,
            @Param("excludedTripId") Long excludedTripId,
            @Param("excludedStatuses") Collection<String> excludedStatuses,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // Detect overlapping vehicle assignments.
    @Query("""
        SELECT COUNT(t) FROM Trip t
        WHERE t.vehicle.id = :vehicleId
          AND t.id <> :excludedTripId
          AND t.status NOT IN :excludedStatuses
          AND t.departureTime < :endTime
          AND t.arrivalTime > :startTime
        """)
    long countVehicleScheduleConflicts(
            @Param("vehicleId") Long vehicleId,
            @Param("excludedTripId") Long excludedTripId,
            @Param("excludedStatuses") Collection<String> excludedStatuses,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // Lock an existing trip during booking transactions.
    // Used with @Transactional in the booking service.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Trip t WHERE t.id = :tripId")
    Optional<Trip> findByIdForUpdate(
            @Param("tripId") Long tripId
    );
}
