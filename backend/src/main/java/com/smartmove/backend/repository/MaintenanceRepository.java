
package com.smartmove.backend.repository;

import com.smartmove.backend.entity.Maintenance;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MaintenanceRepository
        extends JpaRepository<Maintenance, Long> {

    // Get complete maintenance history for a vehicle.
    List<Maintenance> findByVehicleIdOrderByScheduledDateDesc(
            Long vehicleId
    );

    // Admin: filter maintenance records by status.
    List<Maintenance> findByStatusIgnoreCaseOrderByScheduledDateDesc(
            String status
    );

    // Admin: filter maintenance by priority.
    List<Maintenance> findByPriorityIgnoreCaseOrderByScheduledDateAsc(
            String priority
    );

    // Filter maintenance records by type.
    List<Maintenance> findByMaintenanceTypeIgnoreCaseOrderByScheduledDateDesc(
            String maintenanceType
    );

    // Get maintenance records scheduled within a period.
    List<Maintenance> findByScheduledDateBetweenOrderByScheduledDateAsc(
            LocalDate startDate,
            LocalDate endDate
    );

    // Get maintenance records created by a user.
    // Useful for driver-reported vehicle issues.
    List<Maintenance> findByReportedByIdOrderByCreatedAtDesc(
            Long userId
    );

    // Count maintenance records for a vehicle.
    long countByVehicleId(Long vehicleId);

    // Count maintenance records by status.
    long countByStatusIgnoreCase(String status);

    // Find upcoming maintenance.
    @Query("""
        SELECT m
        FROM Maintenance m
        JOIN FETCH m.vehicle
        WHERE m.scheduledDate BETWEEN :today AND :endDate
          AND m.status = 'SCHEDULED'
        ORDER BY m.scheduledDate ASC
        """)
    List<Maintenance> findUpcomingMaintenance(
            @Param("today") LocalDate today,
            @Param("endDate") LocalDate endDate
    );

    // Find overdue maintenance.
    @Query("""
        SELECT m
        FROM Maintenance m
        JOIN FETCH m.vehicle
        WHERE m.scheduledDate < :today
          AND m.status IN ('SCHEDULED', 'IN_PROGRESS')
        ORDER BY m.scheduledDate ASC
        """)
    List<Maintenance> findOverdueMaintenance(
            @Param("today") LocalDate today
    );

    // Find maintenance currently in progress.
    @Query("""
        SELECT m
        FROM Maintenance m
        JOIN FETCH m.vehicle
        WHERE m.status = 'IN_PROGRESS'
        ORDER BY m.scheduledDate ASC
        """)
    List<Maintenance> findMaintenanceInProgress();

    // Check if a vehicle has an active maintenance record.
    @Query("""
        SELECT COUNT(m)
        FROM Maintenance m
        WHERE m.vehicle.id = :vehicleId
          AND m.status = 'IN_PROGRESS'
        """)
    long countActiveMaintenanceForVehicle(
            @Param("vehicleId") Long vehicleId
    );

    // Check maintenance scheduled during a proposed trip.
    // This supports trip scheduling conflict checks.
    @Query("""
        SELECT COUNT(m)
        FROM Maintenance m
        WHERE m.vehicle.id = :vehicleId
          AND m.status IN ('SCHEDULED', 'IN_PROGRESS')
          AND m.scheduledDate BETWEEN :startDate AND :endDate
        """)
    long countMaintenanceConflicts(
            @Param("vehicleId") Long vehicleId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // Retrieve maintenance together with vehicle details.
    @Query("""
        SELECT m
        FROM Maintenance m
        JOIN FETCH m.vehicle
        WHERE m.id = :maintenanceId
        """)
    Optional<Maintenance> findDetailedById(
            @Param("maintenanceId") Long maintenanceId
    );

    // Lock a maintenance record before updating it.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT m
        FROM Maintenance m
        WHERE m.id = :maintenanceId
        """)
    Optional<Maintenance> findByIdForUpdate(
            @Param("maintenanceId") Long maintenanceId
    );

    // Calculate total completed maintenance expenses.
    @Query("""
        SELECT COALESCE(SUM(m.actualCost), 0)
        FROM Maintenance m
        WHERE m.status = 'COMPLETED'
        """)
    BigDecimal calculateTotalMaintenanceCost();

    // Calculate maintenance expenses during a period.
    @Query("""
        SELECT COALESCE(SUM(m.actualCost), 0)
        FROM Maintenance m
        WHERE m.status = 'COMPLETED'
          AND m.completedDate >= :startDate
          AND m.completedDate <= :endDate
        """)
    BigDecimal calculateMaintenanceCostBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // Calculate total maintenance expenses for one vehicle.
    @Query("""
        SELECT COALESCE(SUM(m.actualCost), 0)
        FROM Maintenance m
        WHERE m.vehicle.id = :vehicleId
          AND m.status = 'COMPLETED'
        """)
    BigDecimal calculateMaintenanceCostByVehicle(
            @Param("vehicleId") Long vehicleId
    );

    // Maintenance expenses grouped by vehicle.
    // Result: [vehicle ID, registration number, total cost]
    @Query("""
        SELECT m.vehicle.id,
               m.vehicle.registrationNumber,
               SUM(m.actualCost)
        FROM Maintenance m
        WHERE m.status = 'COMPLETED'
          AND m.actualCost IS NOT NULL
        GROUP BY m.vehicle.id,
                 m.vehicle.registrationNumber
        ORDER BY SUM(m.actualCost) DESC
        """)
    List<Object[]> calculateMaintenanceCostPerVehicle();

    // Maintenance expenses grouped by maintenance type.
    // Result: [maintenance type, total cost]
    @Query("""
        SELECT m.maintenanceType,
               SUM(m.actualCost)
        FROM Maintenance m
        WHERE m.status = 'COMPLETED'
          AND m.actualCost IS NOT NULL
        GROUP BY m.maintenanceType
        ORDER BY SUM(m.actualCost) DESC
        """)
    List<Object[]> calculateMaintenanceCostByType();

    // Completed maintenance history for coursework reports.
    @Query("""
        SELECT m
        FROM Maintenance m
        JOIN FETCH m.vehicle
        WHERE m.status = 'COMPLETED'
        ORDER BY m.completedDate DESC
        """)
    List<Maintenance> findCompletedMaintenanceHistory();
}
