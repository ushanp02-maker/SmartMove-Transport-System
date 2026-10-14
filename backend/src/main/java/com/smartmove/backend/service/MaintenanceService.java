
package com.smartmove.backend.service;

import com.smartmove.backend.entity.Maintenance;
import com.smartmove.backend.entity.UserAccount;
import com.smartmove.backend.entity.Vehicle;

import com.smartmove.backend.repository.MaintenanceRepository;
import com.smartmove.backend.repository.TripRepository;
import com.smartmove.backend.repository.UserAccountRepository;
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
public class MaintenanceService {

    private static final Set<String> TYPES = Set.of(
            "ROUTINE_SERVICE",
            "REPAIR",
            "INSPECTION",
            "TYRE_REPLACEMENT",
            "EMERGENCY_REPAIR",
            "OTHER"
    );

    private static final Set<String> PRIORITIES = Set.of(
            "LOW", "NORMAL", "HIGH", "URGENT"
    );

    private final MaintenanceRepository maintenanceRepository;
    private final VehicleRepository vehicleRepository;
    private final UserAccountRepository accountRepository;
    private final TripRepository tripRepository;

    public MaintenanceService(
            MaintenanceRepository maintenanceRepository,
            VehicleRepository vehicleRepository,
            UserAccountRepository accountRepository,
            TripRepository tripRepository
    ) {
        this.maintenanceRepository = maintenanceRepository;
        this.vehicleRepository = vehicleRepository;
        this.accountRepository = accountRepository;
        this.tripRepository = tripRepository;
    }

    // ==========================================
    // DATA TRANSFER OBJECTS
    // ==========================================

    public record MaintenanceProfile(
            Long id,
            Long vehicleId,
            String vehicleRegistration,
            String vehicleName,
            String maintenanceType,
            String description,
            String status,
            String priority,
            LocalDate scheduledDate,
            LocalDate startedDate,
            LocalDate completedDate,
            String serviceProvider,
            BigDecimal estimatedCost,
            BigDecimal actualCost,
            String currency,
            Integer odometerReading,
            LocalDate nextServiceDate,
            String notes,
            Long reportedByUserId,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            boolean overdue
    ) {
    }

    public record CreateMaintenanceRequest(
            Long vehicleId,
            String maintenanceType,
            String description,
            String priority,
            LocalDate scheduledDate,
            String serviceProvider,
            BigDecimal estimatedCost,
            Integer odometerReading,
            String notes,
            Long reportedByUserId
    ) {
    }

    public record UpdateMaintenanceRequest(
            String maintenanceType,
            String description,
            String priority,
            LocalDate scheduledDate,
            String serviceProvider,
            BigDecimal estimatedCost,
            Integer odometerReading,
            String notes
    ) {
    }

    public record CompleteMaintenanceRequest(
            BigDecimal actualCost,
            Integer odometerReading,
            LocalDate nextServiceDate,
            String notes
    ) {
    }

    public record MaintenanceStatistics(
            long totalRecords,
            long scheduledRecords,
            long inProgressRecords,
            long completedRecords,
            long cancelledRecords,
            long overdueRecords,
            BigDecimal totalCompletedCost
    ) {
    }

    public record MaintenanceCostByVehicle(
            Long vehicleId,
            String registrationNumber,
            BigDecimal totalCost
    ) {
    }

    public record MaintenanceCostByType(
            String maintenanceType,
            BigDecimal totalCost
    ) {
    }

    // ==========================================
    // LOOKUP
    // ==========================================

    @Transactional(readOnly = true)
    public Maintenance getMaintenanceById(Long id) {
        return maintenanceRepository.findById(
                requiredId(id, "Maintenance ID")
        ).orElseThrow(() -> notFound(
                "Maintenance record not found"
        ));
    }

    @Transactional(readOnly = true)
    public MaintenanceProfile getMaintenanceProfile(Long id) {
        return toProfile(getMaintenanceById(id));
    }

    // ==========================================
    // CREATE MAINTENANCE
    // ==========================================

    @Transactional
    public MaintenanceProfile createMaintenance(
            CreateMaintenanceRequest request
    ) {
        if (request == null) {
            throw badRequest("Maintenance details are required");
        }

        Vehicle vehicle = findVehicle(request.vehicleId());

        if ("INACTIVE".equalsIgnoreCase(vehicle.getStatus())
                || "SUSPENDED".equalsIgnoreCase(vehicle.getStatus())) {
            throw conflict(
                    "Cannot schedule maintenance for an inactive or suspended vehicle"
            );
        }

        String type = normalize(
                request.maintenanceType(),
                TYPES,
                "Maintenance type"
        );

        String priority = request.priority() == null
                ? "NORMAL"
                : normalize(
                request.priority(),
                PRIORITIES,
                "Priority"
        );

        String description = requiredText(
                request.description(),
                "Description",
                1000
        );

        LocalDate date = request.scheduledDate();

        if (date == null || date.isBefore(LocalDate.now())) {
            throw badRequest(
                    "Scheduled date must be today or later"
            );
        }

        validateMoney(request.estimatedCost());
        validateOdometer(
                vehicle,
                request.odometerReading()
        );

        // Maintenance dates must not conflict with
        // existing non-cancelled trip assignments.
        ensureNoTripConflict(vehicle.getId(), date);

        Maintenance maintenance = new Maintenance();

        maintenance.setVehicle(vehicle);
        maintenance.setMaintenanceType(type);
        maintenance.setDescription(description);
        maintenance.setPriority(priority);
        maintenance.setScheduledDate(date);
        maintenance.setStatus("SCHEDULED");
        maintenance.setServiceProvider(
                optionalText(
                        request.serviceProvider(),
                        150
                )
        );
        maintenance.setEstimatedCost(
                request.estimatedCost()
        );
        maintenance.setOdometerReading(
                request.odometerReading()
        );
        maintenance.setNotes(
                optionalText(request.notes(), 1000)
        );

        if (request.reportedByUserId() != null) {
            UserAccount account = accountRepository
                    .findById(request.reportedByUserId())
                    .orElseThrow(() ->
                            notFound("Reporting user not found")
                    );

            maintenance.setReportedBy(account);
        }

        return toProfile(
                maintenanceRepository.save(maintenance)
        );
    }

    // ==========================================
    // UPDATE SCHEDULED MAINTENANCE
    // ==========================================

    @Transactional
    public MaintenanceProfile updateMaintenance(
            Long id,
            UpdateMaintenanceRequest request
    ) {
        if (request == null) {
            throw badRequest("Update details are required");
        }

        Maintenance maintenance = lockMaintenance(id);

        if (!"SCHEDULED".equalsIgnoreCase(
                maintenance.getStatus()
        )) {
            throw conflict(
                    "Only scheduled maintenance can be edited"
            );
        }

        if (request.maintenanceType() != null) {
            maintenance.setMaintenanceType(
                    normalize(
                            request.maintenanceType(),
                            TYPES,
                            "Maintenance type"
                    )
            );
        }

        if (request.description() != null) {
            maintenance.setDescription(
                    requiredText(
                            request.description(),
                            "Description",
                            1000
                    )
            );
        }

        if (request.priority() != null) {
            maintenance.setPriority(
                    normalize(
                            request.priority(),
                            PRIORITIES,
                            "Priority"
                    )
            );
        }

        if (request.scheduledDate() != null) {
            if (request.scheduledDate()
                    .isBefore(LocalDate.now())) {
                throw badRequest(
                        "Scheduled date cannot be in the past"
                );
            }

            ensureNoTripConflict(
                    maintenance.getVehicle().getId(),
                    request.scheduledDate()
            );

            maintenance.setScheduledDate(
                    request.scheduledDate()
            );
        }

        if (request.serviceProvider() != null) {
            maintenance.setServiceProvider(
                    optionalText(
                            request.serviceProvider(),
                            150
                    )
            );
        }

        if (request.estimatedCost() != null) {
            validateMoney(request.estimatedCost());
            maintenance.setEstimatedCost(
                    request.estimatedCost()
            );
        }

        if (request.odometerReading() != null) {
            validateOdometer(
                    maintenance.getVehicle(),
                    request.odometerReading()
            );
            maintenance.setOdometerReading(
                    request.odometerReading()
            );
        }

        if (request.notes() != null) {
            maintenance.setNotes(
                    optionalText(request.notes(), 1000)
            );
        }

        return toProfile(
                maintenanceRepository.save(maintenance)
        );
    }

    // ==========================================
    // START MAINTENANCE
    // ==========================================

    @Transactional
    public MaintenanceProfile startMaintenance(Long id) {
        Maintenance maintenance = lockMaintenance(id);

        if (!"SCHEDULED".equalsIgnoreCase(
                maintenance.getStatus()
        )) {
            throw conflict(
                    "Only scheduled maintenance can be started"
            );
        }

        Vehicle vehicle = maintenance.getVehicle();

        ensureNoTripConflict(
                vehicle.getId(),
                LocalDate.now()
        );

        if (maintenanceRepository
                .countActiveMaintenanceForVehicle(
                        vehicle.getId()
                ) > 0) {
            throw conflict(
                    "Vehicle already has maintenance in progress"
            );
        }

        maintenance.setStatus("IN_PROGRESS");
        maintenance.setStartedDate(LocalDate.now());

        vehicle.setStatus("MAINTENANCE");

        vehicleRepository.save(vehicle);

        return toProfile(
                maintenanceRepository.save(maintenance)
        );
    }

    // ==========================================
    // COMPLETE MAINTENANCE
    // ==========================================

    @Transactional
    public MaintenanceProfile completeMaintenance(
            Long id,
            CompleteMaintenanceRequest request
    ) {
        if (request == null) {
            throw badRequest("Completion details are required");
        }

        Maintenance maintenance = lockMaintenance(id);

        if (!"IN_PROGRESS".equalsIgnoreCase(
                maintenance.getStatus()
        )) {
            throw conflict(
                    "Only in-progress maintenance can be completed"
            );
        }

        if (request.actualCost() == null) {
            throw badRequest("Actual cost is required");
        }

        validateMoney(request.actualCost());

        Vehicle vehicle = maintenance.getVehicle();

        validateOdometer(
                vehicle,
                request.odometerReading()
        );

        if (request.nextServiceDate() != null
                && !request.nextServiceDate()
                .isAfter(LocalDate.now())) {
            throw badRequest(
                    "Next service date must be in the future"
            );
        }

        maintenance.setActualCost(request.actualCost());
        maintenance.setCompletedDate(LocalDate.now());
        maintenance.setStatus("COMPLETED");

        if (request.odometerReading() != null) {
            maintenance.setOdometerReading(
                    request.odometerReading()
            );

            vehicle.setCurrentMileage(
                    request.odometerReading().doubleValue()
            );
        }

        if (request.nextServiceDate() != null) {
            maintenance.setNextServiceDate(
                    request.nextServiceDate()
            );

            vehicle.setNextServiceDate(
                    request.nextServiceDate()
            );
        }

        if (request.notes() != null) {
            maintenance.setNotes(
                    optionalText(request.notes(), 1000)
            );
        }

        // Vehicle availability must be restored only
        // after the vehicle compliance checks run.
        vehicle.setStatus("UNAVAILABLE");

        vehicleRepository.save(vehicle);

        return toProfile(
                maintenanceRepository.save(maintenance)
        );
    }

    // ==========================================
    // CANCEL MAINTENANCE
    // ==========================================

    @Transactional
    public MaintenanceProfile cancelMaintenance(Long id) {
        Maintenance maintenance = lockMaintenance(id);

        if (!"SCHEDULED".equalsIgnoreCase(
                maintenance.getStatus()
        )) {
            throw conflict(
                    "Only scheduled maintenance can be cancelled"
            );
        }

        maintenance.setStatus("CANCELLED");

        return toProfile(
                maintenanceRepository.save(maintenance)
        );
    }

    // ==========================================
    // VEHICLE MAINTENANCE HISTORY
    // ==========================================

    @Transactional(readOnly = true)
    public List<MaintenanceProfile> getVehicleMaintenance(
            Long vehicleId
    ) {
        findVehicle(vehicleId);

        return maintenanceRepository
                .findByVehicleIdOrderByScheduledDateDesc(
                        vehicleId
                )
                .stream()
                .map(this::toProfile)
                .toList();
    }

    // ==========================================
    // ADMIN: ALL MAINTENANCE
    // ==========================================

    @Transactional(readOnly = true)
    public List<MaintenanceProfile> getAllMaintenance() {
        return maintenanceRepository.findAll()
                .stream()
                .map(this::toProfile)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<MaintenanceProfile> getMaintenancePage(
            Pageable pageable
    ) {
        return maintenanceRepository.findAll(pageable)
                .map(this::toProfile);
    }

    @Transactional(readOnly = true)
    public List<MaintenanceProfile> getMaintenanceByStatus(
            String status
    ) {
        return maintenanceRepository
                .findByStatusIgnoreCaseOrderByScheduledDateDesc(
                        requiredText(status, "Status", 30)
                )
                .stream()
                .map(this::toProfile)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MaintenanceProfile> getMaintenanceByPriority(
            String priority
    ) {
        return maintenanceRepository
                .findByPriorityIgnoreCaseOrderByScheduledDateAsc(
                        normalize(
                                priority,
                                PRIORITIES,
                                "Priority"
                        )
                )
                .stream()
                .map(this::toProfile)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MaintenanceProfile> getMaintenanceByType(
            String type
    ) {
        return maintenanceRepository
                .findByMaintenanceTypeIgnoreCaseOrderByScheduledDateDesc(
                        normalize(type, TYPES, "Maintenance type")
                )
                .stream()
                .map(this::toProfile)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MaintenanceProfile> getUpcomingMaintenance(
            int daysAhead
    ) {
        if (daysAhead < 0 || daysAhead > 365) {
            throw badRequest(
                    "Days ahead must be between 0 and 365"
            );
        }

        LocalDate today = LocalDate.now();

        return maintenanceRepository
                .findUpcomingMaintenance(
                        today,
                        today.plusDays(daysAhead)
                )
                .stream()
                .map(this::toProfile)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MaintenanceProfile> getOverdueMaintenance() {
        return maintenanceRepository
                .findOverdueMaintenance(LocalDate.now())
                .stream()
                .map(this::toProfile)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MaintenanceProfile> getMaintenanceInProgress() {
        return maintenanceRepository
                .findMaintenanceInProgress()
                .stream()
                .map(this::toProfile)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MaintenanceProfile> getMaintenanceBetween(
            LocalDate start,
            LocalDate end
    ) {
        if (start == null || end == null
                || start.isAfter(end)) {
            throw badRequest(
                    "Invalid maintenance date range"
            );
        }

        return maintenanceRepository
                .findByScheduledDateBetweenOrderByScheduledDateAsc(
                        start,
                        end
                )
                .stream()
                .map(this::toProfile)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MaintenanceProfile> getReportedMaintenance(
            Long userId
    ) {
        if (userId == null) {
            throw badRequest("User ID is required");
        }

        return maintenanceRepository
                .findByReportedByIdOrderByCreatedAtDesc(
                        userId
                )
                .stream()
                .map(this::toProfile)
                .toList();
    }

    // ==========================================
    // ADMIN: STATISTICS
    // ==========================================

    @Transactional(readOnly = true)
    public MaintenanceStatistics getMaintenanceStatistics() {
        return new MaintenanceStatistics(
                maintenanceRepository.count(),
                maintenanceRepository.countByStatusIgnoreCase(
                        "SCHEDULED"
                ),
                maintenanceRepository.countByStatusIgnoreCase(
                        "IN_PROGRESS"
                ),
                maintenanceRepository.countByStatusIgnoreCase(
                        "COMPLETED"
                ),
                maintenanceRepository.countByStatusIgnoreCase(
                        "CANCELLED"
                ),
                maintenanceRepository
                        .findOverdueMaintenance(LocalDate.now())
                        .size(),
                maintenanceRepository
                        .calculateTotalMaintenanceCost()
        );
    }

    // ==========================================
    // ADMIN: COST REPORTS
    // ==========================================

    @Transactional(readOnly = true)
    public BigDecimal getTotalMaintenanceCost() {
        return maintenanceRepository
                .calculateTotalMaintenanceCost();
    }

    @Transactional(readOnly = true)
    public BigDecimal getMaintenanceCostBetween(
            LocalDate start,
            LocalDate end
    ) {
        if (start == null || end == null
                || start.isAfter(end)) {
            throw badRequest("Invalid date range");
        }

        return maintenanceRepository
                .calculateMaintenanceCostBetween(
                        start,
                        end
                );
    }

    @Transactional(readOnly = true)
    public BigDecimal getMaintenanceCostByVehicle(
            Long vehicleId
    ) {
        findVehicle(vehicleId);

        return maintenanceRepository
                .calculateMaintenanceCostByVehicle(
                        vehicleId
                );
    }

    @Transactional(readOnly = true)
    public List<MaintenanceCostByVehicle> getCostsByVehicle() {
        return maintenanceRepository
                .calculateMaintenanceCostPerVehicle()
                .stream()
                .map(row -> new MaintenanceCostByVehicle(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        (BigDecimal) row[2]
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MaintenanceCostByType> getCostsByType() {
        return maintenanceRepository
                .calculateMaintenanceCostByType()
                .stream()
                .map(row -> new MaintenanceCostByType(
                        (String) row[0],
                        (BigDecimal) row[1]
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MaintenanceProfile> getCompletedHistory() {
        return maintenanceRepository
                .findCompletedMaintenanceHistory()
                .stream()
                .map(this::toProfile)
                .toList();
    }

    // ==========================================
    // VALIDATION AND LOOKUP HELPERS
    // ==========================================

    private Maintenance lockMaintenance(Long id) {
        return maintenanceRepository
                .findByIdForUpdate(
                        requiredId(id, "Maintenance ID")
                )
                .orElseThrow(() ->
                        notFound("Maintenance record not found")
                );
    }

    private Vehicle findVehicle(Long id) {
        return vehicleRepository
                .findById(requiredId(id, "Vehicle ID"))
                .orElseThrow(() ->
                        notFound("Vehicle not found")
                );
    }

    private Long requiredId(Long id, String field) {
        if (id == null || id <= 0) {
            throw badRequest(field + " must be positive");
        }
        return id;
    }

    private String requiredText(
            String value,
            String field,
            int maxLength
    ) {
        if (value == null || value.isBlank()) {
            throw badRequest(field + " is required");
        }

        String result = value.trim();

        if (result.length() > maxLength) {
            throw badRequest(field + " is too long");
        }

        return result;
    }

    private String optionalText(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String result = value.trim();

        if (result.length() > maxLength) {
            throw badRequest(
                    "Text exceeds maximum length of " + maxLength
            );
        }

        return result;
    }

    private String normalize(
            String value,
            Set<String> allowed,
            String field
    ) {
        String normalized = requiredText(
                value,
                field,
                50
        ).toUpperCase(Locale.ROOT);

        if (!allowed.contains(normalized)) {
            throw badRequest(
                    "Unsupported " + field.toLowerCase(
                            Locale.ROOT
                    )
            );
        }

        return normalized;
    }

    private void validateMoney(BigDecimal amount) {
        if (amount != null && amount.signum() < 0) {
            throw badRequest(
                    "Cost cannot be negative"
            );
        }
    }

    private void validateOdometer(
            Vehicle vehicle,
            Integer reading
    ) {
        if (reading == null) {
            return;
        }

        if (reading < 0) {
            throw badRequest(
                    "Odometer reading cannot be negative"
            );
        }

        Double current = vehicle.getCurrentMileage();

        if (current != null && reading < current) {
            throw badRequest(
                    "Odometer reading cannot be below current vehicle mileage"
            );
        }
    }

    private void ensureNoTripConflict(
            Long vehicleId,
            LocalDate date
    ) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        long conflicts = tripRepository
                .countVehicleScheduleConflicts(
                        vehicleId,
                        -1L,
                        Set.of("CANCELLED"),
                        start,
                        end
                );

        if (conflicts > 0) {
            throw conflict(
                    "Vehicle has an existing trip assignment on this date"
            );
        }
    }

    private boolean isOverdue(Maintenance maintenance) {
        return maintenance.getScheduledDate() != null
                && maintenance.getScheduledDate()
                .isBefore(LocalDate.now())
                && (
                "SCHEDULED".equalsIgnoreCase(
                        maintenance.getStatus()
                )
                        || "IN_PROGRESS".equalsIgnoreCase(
                        maintenance.getStatus()
                )
        );
    }

    private MaintenanceProfile toProfile(
            Maintenance maintenance
    ) {
        Vehicle vehicle = maintenance.getVehicle();

        Long reportedById =
                maintenance.getReportedBy() == null
                        ? null
                        : maintenance.getReportedBy().getId();

        return new MaintenanceProfile(
                maintenance.getId(),
                vehicle.getId(),
                vehicle.getRegistrationNumber(),
                vehicle.getName(),
                maintenance.getMaintenanceType(),
                maintenance.getDescription(),
                maintenance.getStatus(),
                maintenance.getPriority(),
                maintenance.getScheduledDate(),
                maintenance.getStartedDate(),
                maintenance.getCompletedDate(),
                maintenance.getServiceProvider(),
                maintenance.getEstimatedCost(),
                maintenance.getActualCost(),
                maintenance.getCurrency(),
                maintenance.getOdometerReading(),
                maintenance.getNextServiceDate(),
                maintenance.getNotes(),
                reportedById,
                maintenance.getCreatedAt(),
                maintenance.getUpdatedAt(),
                isOverdue(maintenance)
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

    private ResponseStatusException notFound(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                message
        );
    }
}
