
package com.smartmove.backend.service;

import com.smartmove.backend.entity.Driver;
import com.smartmove.backend.entity.UserAccount;
import com.smartmove.backend.repository.DriverRepository;
import com.smartmove.backend.repository.UserAccountRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class DriverService {

    private final DriverRepository driverRepository;
    private final UserAccountRepository accountRepository;

    public DriverService(
            DriverRepository driverRepository,
            UserAccountRepository accountRepository
    ) {
        this.driverRepository = driverRepository;
        this.accountRepository = accountRepository;
    }

    // ==========================================
    // DRIVER DTOs
    // ==========================================

    public record DriverProfile(
            Long id,
            String name,
            String email,
            String phone,
            String licenseNumber,
            LocalDate licenseExpiry,
            Integer experienceYears,
            String status,
            LocalDate joinedDate,
            boolean licenseExpired,
            boolean licenseExpiringSoon
    ) {
    }

    public record CreateDriverRequest(
            String name,
            String email,
            String phone,
            String licenseNumber,
            LocalDate licenseExpiry,
            Integer experienceYears
    ) {
    }

    public record UpdateDriverRequest(
            String name,
            String phone,
            String licenseNumber,
            LocalDate licenseExpiry,
            Integer experienceYears
    ) {
    }

    public record DriverStatistics(
            long totalDrivers,
            long availableDrivers,
            long unavailableDrivers,
            long suspendedDrivers,
            long expiredLicenses,
            long expiringLicenses
    ) {
    }

    // ==========================================
    // DRIVER LOOKUP
    // ==========================================

    @Transactional(readOnly = true)
    public Driver getDriverById(Long driverId) {
        if (driverId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Driver ID is required"
            );
        }

        return driverRepository.findById(driverId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Driver not found"
                        )
                );
    }

    @Transactional(readOnly = true)
    public DriverProfile getDriverProfile(Long driverId) {
        return toProfile(getDriverById(driverId));
    }

    @Transactional(readOnly = true)
    public DriverProfile getProfileByAccountId(
            Long accountId
    ) {
        UserAccount account = getAccount(accountId);

        if (account.getDriverId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No driver profile linked to this account"
            );
        }

        return getDriverProfile(account.getDriverId());
    }

    // ==========================================
    // ADMIN: CREATE DRIVER
    // ==========================================

    @Transactional
    public DriverProfile createDriver(
            CreateDriverRequest request
    ) {
        if (request == null) {
            throw badRequest("Driver details are required");
        }

        String name = requireText(
                request.name(), "Driver name"
        );

        String email = normalizeEmail(request.email());

        String licenseNumber = requireText(
                request.licenseNumber(), "License number"
        );

        validateLicenseExpiry(request.licenseExpiry());
        validateExperience(request.experienceYears());

        if (driverRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Driver email already exists"
            );
        }

        if (driverRepository.existsByLicenseNumberIgnoreCase(
                licenseNumber
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "License number already exists"
            );
        }

        Driver driver = new Driver();

        driver.setName(name);
        driver.setEmail(email);
        driver.setPhone(normalizeOptional(request.phone()));
        driver.setLicenseNumber(licenseNumber);
        driver.setLicenseExpiry(request.licenseExpiry());
        driver.setExperienceYears(request.experienceYears());
        driver.setStatus("AVAILABLE");
        driver.setJoinedDate(LocalDate.now());

        return toProfile(driverRepository.save(driver));
    }

    // ==========================================
    // UPDATE DRIVER
    // ==========================================

    @Transactional
    public DriverProfile updateDriver(
            Long driverId,
            UpdateDriverRequest request
    ) {
        if (request == null) {
            throw badRequest("Update details are required");
        }

        Driver driver = getDriverById(driverId);

        if (request.name() != null) {
            driver.setName(
                    requireText(request.name(), "Driver name")
            );
        }

        if (request.phone() != null) {
            driver.setPhone(
                    normalizeOptional(request.phone())
            );
        }

        if (request.licenseNumber() != null) {
            String licenseNumber = requireText(
                    request.licenseNumber(),
                    "License number"
            );

            if (!licenseNumber.equalsIgnoreCase(
                    driver.getLicenseNumber()
            ) && driverRepository
                    .existsByLicenseNumberIgnoreCase(
                            licenseNumber
                    )) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "License number already exists"
                );
            }

            driver.setLicenseNumber(licenseNumber);
        }

        if (request.licenseExpiry() != null) {
            validateLicenseExpiry(request.licenseExpiry());
            driver.setLicenseExpiry(request.licenseExpiry());
        }

        if (request.experienceYears() != null) {
            validateExperience(request.experienceYears());
            driver.setExperienceYears(
                    request.experienceYears()
            );
        }

        return toProfile(driverRepository.save(driver));
    }

    // ==========================================
    // DRIVER AVAILABILITY
    // ==========================================

    @Transactional
    public DriverProfile updateDriverStatus(
            Long driverId,
            String newStatus
    ) {
        Driver driver = getDriverById(driverId);

        String status = requireText(
                newStatus, "Driver status"
        ).toUpperCase(Locale.ROOT);

        if (!List.of(
                "AVAILABLE",
                "UNAVAILABLE",
                "ON_TRIP",
                "ON_LEAVE",
                "SUSPENDED",
                "INACTIVE"
        ).contains(status)) {
            throw badRequest("Invalid driver status");
        }

        // Suspended drivers cannot change their own
        // status through an ordinary availability flow.
        // The controller must additionally enforce
        // administrator authorization.
        if ("SUSPENDED".equalsIgnoreCase(
                driver.getStatus()
        ) && !"SUSPENDED".equals(status)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Suspended driver requires administrative review"
            );
        }

        if ("AVAILABLE".equals(status)
                && isLicenseExpired(driver)) {
            throw badRequest(
                    "Driver cannot be available with an expired license"
            );
        }

        driver.setStatus(status);

        return toProfile(driverRepository.save(driver));
    }

    // ==========================================
    // ADMIN: DRIVER LISTS
    // ==========================================

    @Transactional(readOnly = true)
    public List<DriverProfile> getAllDrivers() {
        return driverRepository.findAll()
                .stream()
                .map(this::toProfile)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<DriverProfile> getDrivers(
            Pageable pageable
    ) {
        return driverRepository.findAll(pageable)
                .map(this::toProfile);
    }

    @Transactional(readOnly = true)
    public List<DriverProfile> getAvailableDrivers() {
        return driverRepository.findAll()
                .stream()
                .filter(driver ->
                        "AVAILABLE".equalsIgnoreCase(
                                driver.getStatus()
                        )
                                && !isLicenseExpired(driver)
                )
                .map(this::toProfile)
                .toList();
    }

    // ==========================================
    // ADMIN: DRIVER SEARCH
    // ==========================================

    @Transactional(readOnly = true)
    public List<DriverProfile> searchDrivers(
            String keyword
    ) {
        if (keyword == null || keyword.isBlank()) {
            return getAllDrivers();
        }

        String search = keyword.trim()
                .toLowerCase(Locale.ROOT);

        return driverRepository.findAll()
                .stream()
                .filter(driver ->
                        containsIgnoreCase(
                                driver.getName(), search
                        )
                                || containsIgnoreCase(
                                driver.getEmail(), search
                        )
                                || containsIgnoreCase(
                                driver.getPhone(), search
                        )
                                || containsIgnoreCase(
                                driver.getLicenseNumber(), search
                        )
                )
                .map(this::toProfile)
                .toList();
    }

    // ==========================================
    // LICENSE EXPIRY MANAGEMENT
    // ==========================================

    @Transactional(readOnly = true)
    public List<DriverProfile> getExpiredLicenseDrivers() {
        return driverRepository.findAll()
                .stream()
                .filter(this::isLicenseExpired)
                .map(this::toProfile)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DriverProfile> getExpiringLicenseDrivers(
            int daysAhead
    ) {
        if (daysAhead < 0 || daysAhead > 365) {
            throw badRequest(
                    "Days ahead must be between 0 and 365"
            );
        }

        return driverRepository.findAll()
                .stream()
                .filter(driver ->
                        isLicenseExpiringSoon(
                                driver, daysAhead
                        )
                )
                .map(this::toProfile)
                .toList();
    }

    // ==========================================
    // DRIVER STATISTICS
    // ==========================================

    @Transactional(readOnly = true)
    public DriverStatistics getDriverStatistics() {
        List<Driver> drivers = driverRepository.findAll();

        long available = drivers.stream()
                .filter(driver ->
                        "AVAILABLE".equalsIgnoreCase(
                                driver.getStatus()
                        ) && !isLicenseExpired(driver)
                )
                .count();

        long suspended = drivers.stream()
                .filter(driver ->
                        "SUSPENDED".equalsIgnoreCase(
                                driver.getStatus()
                        )
                )
                .count();

        long expired = drivers.stream()
                .filter(this::isLicenseExpired)
                .count();

        long expiring = drivers.stream()
                .filter(driver ->
                        isLicenseExpiringSoon(driver, 30)
                )
                .count();

        return new DriverStatistics(
                drivers.size(),
                available,
                drivers.size() - available,
                suspended,
                expired,
                expiring
        );
    }

    // ==========================================
    // INTERNAL HELPERS
    // ==========================================

    private UserAccount getAccount(Long accountId) {
        if (accountId == null) {
            throw badRequest("Account ID is required");
        }

        return accountRepository.findById(accountId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Account not found"
                        )
                );
    }

    private DriverProfile toProfile(Driver driver) {
        return new DriverProfile(
                driver.getId(),
                driver.getName(),
                driver.getEmail(),
                driver.getPhone(),
                driver.getLicenseNumber(),
                driver.getLicenseExpiry(),
                driver.getExperienceYears(),
                driver.getStatus(),
                driver.getJoinedDate(),
                isLicenseExpired(driver),
                isLicenseExpiringSoon(driver, 30)
        );
    }

    private boolean isLicenseExpired(Driver driver) {
        return driver.getLicenseExpiry() == null
                || driver.getLicenseExpiry()
                .isBefore(LocalDate.now());
    }

    private boolean isLicenseExpiringSoon(
            Driver driver,
            int daysAhead
    ) {
        LocalDate expiry = driver.getLicenseExpiry();

        if (expiry == null) {
            return false;
        }

        LocalDate today = LocalDate.now();

        return !expiry.isBefore(today)
                && !expiry.isAfter(
                today.plusDays(daysAhead)
        );
    }

    private void validateLicenseExpiry(LocalDate expiry) {
        if (expiry == null) {
            throw badRequest("License expiry date is required");
        }
    }

    private void validateExperience(Integer years) {
        if (years != null && (years < 0 || years > 80)) {
            throw badRequest(
                    "Experience years must be between 0 and 80"
            );
        }
    }

    private String normalizeEmail(String email) {
        String value = requireText(
                email, "Email"
        ).toLowerCase(Locale.ROOT);

        if (!value.matches(
                "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
        )) {
            throw badRequest("Invalid email address");
        }

        return value;
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

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
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
