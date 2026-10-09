
package com.smartmove.backend.service;

import com.smartmove.backend.document.VehicleDocument;
import com.smartmove.backend.entity.Vehicle;
import com.smartmove.backend.repository.MaintenanceRepository;
import com.smartmove.backend.repository.VehicleDocumentRepository;
import com.smartmove.backend.repository.VehicleRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class VehicleService {

    private static final Set<String> VALID_STATUSES = Set.of(
            "AVAILABLE",
            "UNAVAILABLE",
            "ON_TRIP",
            "MAINTENANCE",
            "INACTIVE",
            "SUSPENDED"
    );

    private static final Set<String> REQUIRED_DOCUMENTS = Set.of(
            "REGISTRATION",
            "INSURANCE",
            "REVENUE_LICENSE",
            "FITNESS_CERTIFICATE"
    );

    private final VehicleRepository vehicleRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final VehicleDocumentRepository documentRepository;

    public VehicleService(
            VehicleRepository vehicleRepository,
            MaintenanceRepository maintenanceRepository,
            VehicleDocumentRepository documentRepository
    ) {
        this.vehicleRepository = vehicleRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.documentRepository = documentRepository;
    }

    // ==========================================
    // DATA TRANSFER OBJECTS
    // ==========================================

    public record VehicleProfile(
            Long id,
            String registrationNumber,
            String name,
            String vehicleType,
            Integer seatingCapacity,
            String status,
            Double currentMileage,
            LocalDate nextServiceDate,
            Integer manufactureYear,
            LocalDate createdAt,
            boolean serviceOverdue,
            boolean maintenanceInProgress
    ) {
    }

    public record CreateVehicleRequest(
            String registrationNumber,
            String name,
            String vehicleType,
            Integer seatingCapacity,
            Double currentMileage,
            LocalDate nextServiceDate,
            Integer manufactureYear
    ) {
    }

    public record UpdateVehicleRequest(
            String registrationNumber,
            String name,
            String vehicleType,
            Integer seatingCapacity,
            Double currentMileage,
            LocalDate nextServiceDate,
            Integer manufactureYear
    ) {
    }

    public record VehicleCompliance(
            Long vehicleId,
            boolean compliant,
            List<String> missingOrInvalidDocuments,
            boolean serviceOverdue,
            boolean maintenanceInProgress
    ) {
    }

    public record VehicleStatistics(
            long totalVehicles,
            long availableVehicles,
            long unavailableVehicles,
            long onTripVehicles,
            long maintenanceVehicles,
            long inactiveVehicles,
            long overdueServices
    ) {
    }

    // ==========================================
    // VEHICLE LOOKUP
    // ==========================================

    @Transactional(readOnly = true)
    public Vehicle getVehicleById(Long vehicleId) {
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

    @Transactional(readOnly = true)
    public VehicleProfile getVehicleProfile(Long vehicleId) {
        return toProfile(getVehicleById(vehicleId));
    }

    // ==========================================
    // ADMIN: REGISTER VEHICLE
    // ==========================================

    @Transactional
    public VehicleProfile createVehicle(
            CreateVehicleRequest request
    ) {
        if (request == null) {
            throw badRequest("Vehicle details are required");
        }

        String registration = requireText(
                request.registrationNumber(),
                "Registration number"
        ).toUpperCase(Locale.ROOT);

        if (vehicleRepository
                .existsByRegistrationNumberIgnoreCase(
                        registration
                )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Vehicle registration already exists"
            );
        }

        validateCapacity(request.seatingCapacity());
        validateMileage(request.currentMileage());
        validateManufactureYear(request.manufactureYear());

        Vehicle vehicle = new Vehicle();

        vehicle.setRegistrationNumber(registration);
        vehicle.setName(requireText(request.name(), "Name"));
        vehicle.setVehicleType(
                requireText(request.vehicleType(), "Vehicle type")
        );
        vehicle.setSeatingCapacity(request.seatingCapacity());
        vehicle.setCurrentMileage(
                request.currentMileage() == null
                        ? 0.0
                        : request.currentMileage()
        );
        vehicle.setNextServiceDate(request.nextServiceDate());
        vehicle.setManufactureYear(request.manufactureYear());
        vehicle.setStatus("UNAVAILABLE");

        Vehicle saved = vehicleRepository.save(vehicle);

        return toProfile(saved);
    }

    // ==========================================
    // ADMIN: UPDATE VEHICLE
    // ==========================================

    @Transactional
    public VehicleProfile updateVehicle(
            Long vehicleId,
            UpdateVehicleRequest request
    ) {
        if (request == null) {
            throw badRequest("Update details are required");
        }

        Vehicle vehicle = getVehicleById(vehicleId);

        if (request.registrationNumber() != null) {
            String registration = requireText(
                    request.registrationNumber(),
                    "Registration number"
            ).toUpperCase(Locale.ROOT);

            if (!registration.equalsIgnoreCase(
                    vehicle.getRegistrationNumber()
            ) && vehicleRepository
                    .existsByRegistrationNumberIgnoreCase(
                            registration
                    )) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Vehicle registration already exists"
                );
            }

            vehicle.setRegistrationNumber(registration);
        }

        if (request.name() != null) {
            vehicle.setName(
                    requireText(request.name(), "Name")
            );
        }

        if (request.vehicleType() != null) {
            vehicle.setVehicleType(
                    requireText(
                            request.vehicleType(),
                            "Vehicle type"
                    )
            );
        }

        if (request.seatingCapacity() != null) {
            validateCapacity(request.seatingCapacity());
            vehicle.setSeatingCapacity(
                    request.seatingCapacity()
            );
        }

        if (request.currentMileage() != null) {
            validateMileage(request.currentMileage());

            if (vehicle.getCurrentMileage() != null
                    && request.currentMileage()
                    < vehicle.getCurrentMileage()) {
                throw badRequest(
                        "Mileage cannot be decreased"
                );
            }

            vehicle.setCurrentMileage(
                    request.currentMileage()
            );
        }

        if (request.nextServiceDate() != null) {
            vehicle.setNextServiceDate(
                    request.nextServiceDate()
            );
        }

        if (request.manufactureYear() != null) {
            validateManufactureYear(
                    request.manufactureYear()
            );
            vehicle.setManufactureYear(
                    request.manufactureYear()
            );
        }

        return toProfile(vehicleRepository.save(vehicle));
    }

    // ==========================================
    // VEHICLE STATUS MANAGEMENT
    // ==========================================

    @Transactional
    public VehicleProfile updateVehicleStatus(
            Long vehicleId,
            String newStatus
    ) {
        Vehicle vehicle = getVehicleById(vehicleId);

        String status = requireText(
                newStatus,
                "Vehicle status"
        ).toUpperCase(Locale.ROOT);

        if (!VALID_STATUSES.contains(status)) {
            throw badRequest("Invalid vehicle status");
        }

        if ("AVAILABLE".equals(status)) {
            VehicleCompliance compliance =
                    checkVehicleCompliance(vehicleId);

            if (!compliance.compliant()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Vehicle is not compliant or requires maintenance"
                );
            }
        }

        // Controllers must restrict status transitions
        // to authorized users. Trip scheduling will
        // manage ON_TRIP transitions separately.
        vehicle.setStatus(status);

        return toProfile(vehicleRepository.save(vehicle));
    }

    // ==========================================
    // ADMIN: FLEET LISTS
    // ==========================================

    @Transactional(readOnly = true)
    public List<VehicleProfile> getAllVehicles() {
        return vehicleRepository.findAll()
                .stream()
                .map(this::toProfile)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<VehicleProfile> getVehicles(
            Pageable pageable
    ) {
        return vehicleRepository.findAll(pageable)
                .map(this::toProfile);
    }

    @Transactional(readOnly = true)
    public List<VehicleProfile> getAvailableVehicles(
            int minimumCapacity
    ) {
        if (minimumCapacity < 1) {
            throw badRequest(
                    "Minimum capacity must be positive"
            );
        }

        return vehicleRepository
                .findByStatusIgnoreCaseAndSeatingCapacityGreaterThanEqual(
                        "AVAILABLE",
                        minimumCapacity
                )
                .stream()
                .filter(vehicle ->
                        checkVehicleCompliance(
                                vehicle.getId()
                        ).compliant()
                )
                .map(this::toProfile)
                .toList();
    }

    // ==========================================
    // ADMIN: VEHICLE SEARCH
    // ==========================================

    @Transactional(readOnly = true)
    public List<VehicleProfile> searchVehicles(
            String keyword
    ) {
        if (keyword == null || keyword.isBlank()) {
            return getAllVehicles();
        }

        String search = keyword.trim()
                .toLowerCase(Locale.ROOT);

        return vehicleRepository.findAll()
                .stream()
                .filter(vehicle ->
                        containsIgnoreCase(
                                vehicle.getRegistrationNumber(),
                                search
                        )
                                || containsIgnoreCase(
                                vehicle.getName(),
                                search
                        )
                                || containsIgnoreCase(
                                vehicle.getVehicleType(),
                                search
                        )
                                || containsIgnoreCase(
                                vehicle.getStatus(),
                                search
                        )
                )
                .map(this::toProfile)
                .toList();
    }

    // ==========================================
    // MAINTENANCE MONITORING
    // ==========================================

    @Transactional(readOnly = true)
    public List<VehicleProfile> getVehiclesDueForService(
            int daysAhead
    ) {
        if (daysAhead < 0 || daysAhead > 365) {
            throw badRequest(
                    "Days ahead must be between 0 and 365"
            );
        }

        LocalDate deadline =
                LocalDate.now().plusDays(daysAhead);

        return vehicleRepository
                .findByNextServiceDateLessThanEqual(
                        deadline
                )
                .stream()
                .map(this::toProfile)
                .toList();
    }

    // ==========================================
    // DOCUMENT COMPLIANCE
    // ==========================================

    @Transactional(readOnly = true)
    public VehicleCompliance checkVehicleCompliance(
            Long vehicleId
    ) {
        Vehicle vehicle = getVehicleById(vehicleId);

        LocalDate today = LocalDate.now();

        List<VehicleDocument> documents =
                documentRepository
                        .findByVehicleIdOrderByCreatedAtDesc(
                                vehicleId
                        );

        Set<String> validTypes = documents.stream()
                .filter(document ->
                        "ACTIVE".equalsIgnoreCase(
                                document.getStatus()
                        )
                                && "VERIFIED".equalsIgnoreCase(
                                document.getVerificationStatus()
                        )
                                && (
                                document.getExpiryDate() == null
                                        || !document.getExpiryDate()
                                        .isBefore(today)
                        )
                )
                .map(VehicleDocument::getDocumentType)
                .filter(type -> type != null)
                .map(type ->
                        type.toUpperCase(Locale.ROOT)
                )
                .collect(Collectors.toSet());

        List<String> missing = REQUIRED_DOCUMENTS.stream()
                .filter(type -> !validTypes.contains(type))
                .sorted()
                .toList();

        boolean serviceOverdue =
                isServiceOverdue(vehicle);

        boolean maintenanceInProgress =
                hasActiveMaintenance(vehicleId);

        boolean compliant =
                missing.isEmpty()
                        && !serviceOverdue
                        && !maintenanceInProgress;

        return new VehicleCompliance(
                vehicleId,
                compliant,
                missing,
                serviceOverdue,
                maintenanceInProgress
        );
    }

    // ==========================================
    // ADMIN: FLEET STATISTICS
    // ==========================================

    @Transactional(readOnly = true)
    public VehicleStatistics getVehicleStatistics() {
        List<Vehicle> vehicles =
                vehicleRepository.findAll();

        long available = countStatus(
                vehicles, "AVAILABLE"
        );

        long onTrip = countStatus(
                vehicles, "ON_TRIP"
        );

        long maintenance = countStatus(
                vehicles, "MAINTENANCE"
        );

        long inactive = countStatus(
                vehicles, "INACTIVE"
        );

        long overdue = vehicles.stream()
                .filter(this::isServiceOverdue)
                .count();

        return new VehicleStatistics(
                vehicles.size(),
                available,
                vehicles.size() - available,
                onTrip,
                maintenance,
                inactive,
                overdue
        );
    }

    // ==========================================
    // INTERNAL HELPERS
    // ==========================================

    private VehicleProfile toProfile(Vehicle vehicle) {
        return new VehicleProfile(
                vehicle.getId(),
                vehicle.getRegistrationNumber(),
                vehicle.getName(),
                vehicle.getVehicleType(),
                vehicle.getSeatingCapacity(),
                vehicle.getStatus(),
                vehicle.getCurrentMileage(),
                vehicle.getNextServiceDate(),
                vehicle.getManufactureYear(),
                vehicle.getCreatedAt(),
                isServiceOverdue(vehicle),
                hasActiveMaintenance(vehicle.getId())
        );
    }

    private boolean isServiceOverdue(Vehicle vehicle) {
        return vehicle.getNextServiceDate() != null
                && vehicle.getNextServiceDate()
                .isBefore(LocalDate.now());
    }

    private boolean hasActiveMaintenance(Long vehicleId) {
        return maintenanceRepository
                .countActiveMaintenanceForVehicle(
                        vehicleId
                ) > 0;
    }

    private long countStatus(
            List<Vehicle> vehicles,
            String status
    ) {
        return vehicles.stream()
                .filter(vehicle ->
                        status.equalsIgnoreCase(
                                vehicle.getStatus()
                        )
                )
                .count();
    }

    private void validateCapacity(Integer capacity) {
        if (capacity == null || capacity < 1
                || capacity > 200) {
            throw badRequest(
                    "Seating capacity must be between 1 and 200"
            );
        }
    }

    private void validateMileage(Double mileage) {
        if (mileage != null
                && (!Double.isFinite(mileage)
                || mileage < 0)) {
            throw badRequest(
                    "Mileage must be a nonnegative finite number"
            );
        }
    }

    private void validateManufactureYear(Integer year) {
        if (year != null
                && (year < 1900
                || year > LocalDate.now().getYear() + 1)) {
            throw badRequest(
                    "Invalid manufacture year"
            );
        }
    }

    private String requireText(
            String value,
            String fieldName
    ) {
        if (value == null || value.isBlank()) {
            throw badRequest(
                    fieldName + " is required"
            );
        }

        return value.trim();
    }

    private boolean containsIgnoreCase(
            String value,
            String keyword
    ) {
        return value != null
                && value.toLowerCase(Locale.ROOT)
                .contains(keyword);
    }

    private ResponseStatusException badRequest(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }
}
