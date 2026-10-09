
package com.smartmove.backend.repository;

import com.smartmove.backend.document.DriverLocation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DriverLocationRepository
        extends MongoRepository<DriverLocation, String> {

    // ==========================================
    // LATEST LOCATION FOR A TRIP
    // ==========================================

    Optional<DriverLocation>
    findFirstByTripIdOrderByRecordedAtDesc(
            Long tripId
    );

    Optional<DriverLocation>
    findFirstByTripIdOrderByReceivedAtDesc(
            Long tripId
    );

    // ==========================================
    // LATEST LOCATION FOR A DRIVER
    // ==========================================

    Optional<DriverLocation>
    findFirstByDriverIdOrderByRecordedAtDesc(
            Long driverId
    );

    // ==========================================
    // LATEST LOCATION FOR A VEHICLE
    // ==========================================

    Optional<DriverLocation>
    findFirstByVehicleIdOrderByRecordedAtDesc(
            Long vehicleId
    );

    // ==========================================
    // COMPLETE TRIP LOCATION HISTORY
    // ==========================================

    List<DriverLocation>
    findByTripIdOrderByRecordedAtAsc(
            Long tripId
    );

    List<DriverLocation>
    findByTripIdOrderByRecordedAtDesc(
            Long tripId
    );

    Page<DriverLocation>
    findByTripIdOrderByRecordedAtDesc(
            Long tripId,
            Pageable pageable
    );

    // ==========================================
    // DRIVER LOCATION HISTORY
    // ==========================================

    List<DriverLocation>
    findByDriverIdOrderByRecordedAtDesc(
            Long driverId
    );

    Page<DriverLocation>
    findByDriverIdOrderByRecordedAtDesc(
            Long driverId,
            Pageable pageable
    );

    // ==========================================
    // VEHICLE LOCATION HISTORY
    // ==========================================

    List<DriverLocation>
    findByVehicleIdOrderByRecordedAtDesc(
            Long vehicleId
    );

    Page<DriverLocation>
    findByVehicleIdOrderByRecordedAtDesc(
            Long vehicleId,
            Pageable pageable
    );

    // ==========================================
    // TRIP LOCATIONS WITHIN A TIME RANGE
    // ==========================================

    List<DriverLocation>
    findByTripIdAndRecordedAtBetweenOrderByRecordedAtAsc(
            Long tripId,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    Page<DriverLocation>
    findByTripIdAndRecordedAtBetweenOrderByRecordedAtDesc(
            Long tripId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Pageable pageable
    );

    // ==========================================
    // DRIVER LOCATIONS WITHIN A TIME RANGE
    // ==========================================

    List<DriverLocation>
    findByDriverIdAndRecordedAtBetweenOrderByRecordedAtAsc(
            Long driverId,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    // ==========================================
    // VEHICLE LOCATIONS WITHIN A TIME RANGE
    // ==========================================

    List<DriverLocation>
    findByVehicleIdAndRecordedAtBetweenOrderByRecordedAtAsc(
            Long vehicleId,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    // ==========================================
    // LOCATIONS BY TRIP STATUS
    // ==========================================

    List<DriverLocation>
    findByTripIdAndTripStatusIgnoreCaseOrderByRecordedAtDesc(
            Long tripId,
            String tripStatus
    );

    // ==========================================
    // LOCATIONS BY TRACKING SOURCE
    // ==========================================

    List<DriverLocation>
    findByTripIdAndSourceIgnoreCaseOrderByRecordedAtDesc(
            Long tripId,
            String source
    );

    // ==========================================
    // RECENT GPS UPDATES
    // ==========================================

    List<DriverLocation>
    findByRecordedAtAfterOrderByRecordedAtDesc(
            LocalDateTime timestamp
    );

    List<DriverLocation>
    findByTripIdAndRecordedAtAfterOrderByRecordedAtDesc(
            Long tripId,
            LocalDateTime timestamp
    );

    // ==========================================
    // TRACKING STATISTICS
    // ==========================================

    long countByTripId(Long tripId);

    long countByDriverId(Long driverId);

    long countByVehicleId(Long vehicleId);

    boolean existsByTripId(Long tripId);

    // ==========================================
    // DELETE TRACKING HISTORY
    // ==========================================

    long deleteByTripId(Long tripId);

    long deleteByDriverId(Long driverId);

    long deleteByVehicleId(Long vehicleId);
}
