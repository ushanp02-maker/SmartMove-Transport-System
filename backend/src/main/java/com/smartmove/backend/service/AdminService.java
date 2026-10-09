
package com.smartmove.backend.service;

import com.smartmove.backend.entity.Driver;
import com.smartmove.backend.entity.Passenger;
import com.smartmove.backend.entity.UserAccount;
import com.smartmove.backend.entity.UserRole;

import com.smartmove.backend.repository.DriverRepository;
import com.smartmove.backend.repository.PassengerRepository;
import com.smartmove.backend.repository.UserAccountRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AdminService {

    private static final Set<String> ACCOUNT_STATUSES =
            Set.of("ACTIVE", "SUSPENDED", "DISABLED");

    private final UserAccountRepository accountRepository;
    private final DriverRepository driverRepository;
    private final PassengerRepository passengerRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    public AdminService(
            UserAccountRepository accountRepository,
            DriverRepository driverRepository,
            PassengerRepository passengerRepository,
            PasswordEncoder passwordEncoder,
            AuthService authService
    ) {
        this.accountRepository = accountRepository;
        this.driverRepository = driverRepository;
        this.passengerRepository = passengerRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
    }

    // ==========================================
    // RESPONSE AND REQUEST RECORDS
    // ==========================================

    public record AccountProfile(
            Long id,
            String username,
            String email,
            String role,
            String accountStatus,
            Long passengerId,
            Long driverId,
            LocalDateTime createdAt
    ) {}

    public record CreateAdminRequest(
            String username,
            String email,
            String password,
            String role
    ) {}

    public record CreateDriverAccountRequest(
            Long driverId,
            String username,
            String password
    ) {}

    public record ChangeRoleRequest(
            String newRole
    ) {}

    public record ChangeAccountStatusRequest(
            String newStatus
    ) {}

    public record ResetPasswordRequest(
            String newPassword
    ) {}

    public record AccountStatistics(
            long totalAccounts,
            long activeAccounts,
            long suspendedAccounts,
            long disabledAccounts,
            long passengerAccounts,
            long driverAccounts,
            long adminAccounts,
            long superAdminAccounts
    ) {}

    // ==========================================
    // CREATE ADMINISTRATIVE ACCOUNT
    // ==========================================

    @Transactional
    public AccountProfile createAdmin(
            Long actingAccountId,
            CreateAdminRequest request
    ) {
        requireSuperAdmin(actingAccountId);

        if (request == null) {
            throw badRequest("Admin account details are required");
        }

        String role = normalizeRole(request.role());

        if (!role.equals(UserRole.ADMIN.name())
                && !role.equals(UserRole.SUPER_ADMIN.name())) {
            throw badRequest(
                    "Administrative accounts must use ADMIN or SUPER_ADMIN"
            );
        }

        String username = validateUsername(request.username());
        String email = validateEmail(request.email());

        validatePassword(request.password());
        ensureUniqueCredentials(username, email);

        UserAccount account = new UserAccount();
        account.setUsername(username);
        account.setEmail(email);
        account.setPasswordHash(
                passwordEncoder.encode(request.password())
        );
        account.setRole(role);
        account.setAccountStatus("ACTIVE");
        account.setPassengerId(null);
        account.setDriverId(null);
        account.setCreatedAt(LocalDateTime.now());

        return toProfile(accountRepository.save(account));
    }

    // ==========================================
    // CREATE DRIVER LOGIN ACCOUNT
    // ==========================================

    @Transactional
    public AccountProfile createDriverAccount(
            Long actingAccountId,
            CreateDriverAccountRequest request
    ) {
        requireAdmin(actingAccountId);

        if (request == null || request.driverId() == null) {
            throw badRequest("Driver ID is required");
        }

        Driver driver = driverRepository
                .findById(request.driverId())
                .orElseThrow(() ->
                        notFound("Driver not found")
                );

        if (accountRepository
                .findByDriverId(driver.getId())
                .isPresent()) {
            throw conflict(
                    "This driver already has a login account"
            );
        }

        String username = validateUsername(request.username());
        String email = validateEmail(driver.getEmail());

        validatePassword(request.password());
        ensureUniqueCredentials(username, email);

        UserAccount account = new UserAccount();
        account.setUsername(username);
        account.setEmail(email);
        account.setPasswordHash(
                passwordEncoder.encode(request.password())
        );
        account.setRole(UserRole.DRIVER.name());
        account.setAccountStatus("ACTIVE");
        account.setDriverId(driver.getId());
        account.setPassengerId(null);
        account.setCreatedAt(LocalDateTime.now());

        return toProfile(accountRepository.save(account));
    }

    // ==========================================
    // ACCOUNT LOOKUP
    // ==========================================

    @Transactional(readOnly = true)
    public AccountProfile getAccount(
            Long actingAccountId,
            Long targetAccountId
    ) {
        requireAdmin(actingAccountId);

        UserAccount account = findAccount(targetAccountId);

        ensureAccountVisibleToActor(
                actingAccountId,
                account
        );

        return toProfile(account);
    }

    @Transactional(readOnly = true)
    public Page<AccountProfile> getAllAccounts(
            Long actingAccountId,
            Pageable pageable
    ) {
        UserAccount actor = requireAdmin(actingAccountId);

        return accountRepository.findAll(pageable)
                .map(account -> {
                    if (!isSuperAdmin(actor)
                            && isSuperAdmin(account)) {
                        return null;
                    }
                    return toProfile(account);
                })
                .map(profile -> profile);
    }

    @Transactional(readOnly = true)
    public List<AccountProfile> getAccountsByRole(
            Long actingAccountId,
            String role
    ) {
        UserAccount actor = requireAdmin(actingAccountId);

        String normalizedRole = normalizeRole(role);

        if (normalizedRole.equals(UserRole.SUPER_ADMIN.name())
                && !isSuperAdmin(actor)) {
            throw forbidden(
                    "Only Super Admins can view Super Admin accounts"
            );
        }

        return accountRepository.findByRole(normalizedRole)
                .stream()
                .map(this::toProfile)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AccountProfile> getAccountsByStatus(
            Long actingAccountId,
            String status
    ) {
        UserAccount actor = requireAdmin(actingAccountId);

        return accountRepository
                .findByAccountStatus(normalizeStatus(status))
                .stream()
                .filter(account ->
                        isSuperAdmin(actor)
                                || !isSuperAdmin(account)
                )
                .map(this::toProfile)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AccountProfile> searchAccounts(
            Long actingAccountId,
            String keyword
    ) {
        UserAccount actor = requireAdmin(actingAccountId);

        String search = keyword == null
                ? ""
                : keyword.trim().toLowerCase(Locale.ROOT);

        return accountRepository.findAll()
                .stream()
                .filter(account ->
                        isSuperAdmin(actor)
                                || !isSuperAdmin(account)
                )
                .filter(account ->
                        account.getUsername()
                                .toLowerCase(Locale.ROOT)
                                .contains(search)
                                || account.getEmail()
                                .toLowerCase(Locale.ROOT)
                                .contains(search)
                )
                .map(this::toProfile)
                .toList();
    }

    // ==========================================
    // CHANGE ACCOUNT ROLE
    // ==========================================

    @Transactional
    public AccountProfile changeAccountRole(
            Long actingAccountId,
            Long targetAccountId,
            ChangeRoleRequest request
    ) {
        requireSuperAdmin(actingAccountId);

        if (request == null) {
            throw badRequest("Role details are required");
        }

        UserAccount target = findAccount(targetAccountId);

        if (actingAccountId.equals(targetAccountId)) {
            throw conflict(
                    "You cannot change your own administrative role"
            );
        }

        String newRole = normalizeRole(request.newRole());
        String oldRole = target.getRole();

        if (newRole.equals(oldRole)) {
            return toProfile(target);
        }

        // Linked passenger/driver accounts must not be
        // converted into administrative accounts.
        if (target.getPassengerId() != null
                || target.getDriverId() != null) {
            throw conflict(
                    "Linked passenger or driver accounts cannot "
                            + "be assigned another role"
            );
        }

        // Administrative accounts cannot be converted into
        // passenger or driver accounts without a linked profile.
        if (!newRole.equals(UserRole.ADMIN.name())
                && !newRole.equals(UserRole.SUPER_ADMIN.name())) {
            throw conflict(
                    "Use dedicated registration to create "
                            + "passenger or driver accounts"
            );
        }

        ensureNotLastActiveSuperAdmin(target, newRole, null);

        target.setRole(newRole);

        return toProfile(accountRepository.save(target));
    }

    // ==========================================
    // CHANGE ACCOUNT STATUS
    // ==========================================

    @Transactional
    public AccountProfile changeAccountStatus(
            Long actingAccountId,
            Long targetAccountId,
            ChangeAccountStatusRequest request
    ) {
        UserAccount actor = requireAdmin(actingAccountId);

        if (request == null) {
            throw badRequest("Account status is required");
        }

        UserAccount target = findAccount(targetAccountId);

        if (isSuperAdmin(target) && !isSuperAdmin(actor)) {
            throw forbidden(
                    "Only Super Admins can manage Super Admin accounts"
            );
        }

        if (actingAccountId.equals(targetAccountId)) {
            throw conflict(
                    "You cannot change your own account status"
            );
        }

        String newStatus = normalizeStatus(request.newStatus());

        ensureNotLastActiveSuperAdmin(
                target,
                null,
                newStatus
        );

        target.setAccountStatus(newStatus);

        return toProfile(accountRepository.save(target));
    }

    // ==========================================
    // ADMINISTRATIVE PASSWORD RESET
    // ==========================================

    @Transactional
    public AccountProfile resetAccountPassword(
            Long actingAccountId,
            Long targetAccountId,
            ResetPasswordRequest request
    ) {
        UserAccount actor = requireAdmin(actingAccountId);
        UserAccount target = findAccount(targetAccountId);

        if (request == null) {
            throw badRequest("New password is required");
        }

        if (isSuperAdmin(target) && !isSuperAdmin(actor)) {
            throw forbidden(
                    "Only Super Admins can reset Super Admin passwords"
            );
        }

        if (actingAccountId.equals(targetAccountId)) {
            throw conflict(
                    "Use the change-password feature for your own account"
            );
        }

        validatePassword(request.newPassword());

        if (passwordEncoder.matches(
                request.newPassword(),
                target.getPasswordHash()
        )) {
            throw badRequest(
                    "New password must differ from the existing password"
            );
        }

        target.setPasswordHash(
                passwordEncoder.encode(request.newPassword())
        );

        return toProfile(accountRepository.save(target));
    }

    // ==========================================
    // ACCOUNT STATISTICS
    // ==========================================

    @Transactional(readOnly = true)
    public AccountStatistics getAccountStatistics(
            Long actingAccountId
    ) {
        requireSuperAdmin(actingAccountId);

        return new AccountStatistics(
                accountRepository.count(),
                accountRepository
                        .findByAccountStatus("ACTIVE").size(),
                accountRepository
                        .findByAccountStatus("SUSPENDED").size(),
                accountRepository
                        .findByAccountStatus("DISABLED").size(),
                accountRepository
                        .findByRole(UserRole.PASSENGER.name()).size(),
                accountRepository
                        .findByRole(UserRole.DRIVER.name()).size(),
                accountRepository
                        .findByRole(UserRole.ADMIN.name()).size(),
                accountRepository
                        .findByRole(UserRole.SUPER_ADMIN.name()).size()
        );
    }

    // ==========================================
    // PERMISSION HELPERS
    // ==========================================

    private UserAccount requireAdmin(Long accountId) {
        UserAccount actor = findAccount(accountId);

        authService.requireRole(
                actor,
                UserRole.ADMIN,
                UserRole.SUPER_ADMIN
        );

        return actor;
    }

    private UserAccount requireSuperAdmin(Long accountId) {
        UserAccount actor = findAccount(accountId);

        authService.requireRole(
                actor,
                UserRole.SUPER_ADMIN
        );

        return actor;
    }

    private boolean isSuperAdmin(UserAccount account) {
        return account != null
                && UserRole.SUPER_ADMIN.name()
                .equalsIgnoreCase(account.getRole());
    }

    private void ensureAccountVisibleToActor(
            Long actorId,
            UserAccount target
    ) {
        UserAccount actor = findAccount(actorId);

        if (isSuperAdmin(target) && !isSuperAdmin(actor)) {
            throw forbidden(
                    "Only Super Admins can view Super Admin accounts"
            );
        }
    }

    private void ensureNotLastActiveSuperAdmin(
            UserAccount target,
            String proposedRole,
            String proposedStatus
    ) {
        if (!isSuperAdmin(target)
                || !"ACTIVE".equalsIgnoreCase(
                target.getAccountStatus()
        )) {
            return;
        }

        boolean removingRole = proposedRole != null
                && !UserRole.SUPER_ADMIN.name()
                .equals(proposedRole);

        boolean deactivating = proposedStatus != null
                && !"ACTIVE".equals(proposedStatus);

        if (!removingRole && !deactivating) {
            return;
        }

        long activeSuperAdmins = accountRepository
                .findByRoleAndAccountStatus(
                        UserRole.SUPER_ADMIN.name(),
                        "ACTIVE"
                )
                .size();

        if (activeSuperAdmins <= 1) {
            throw conflict(
                    "The last active Super Admin cannot "
                            + "be demoted or deactivated"
            );
        }
    }

    // ==========================================
    // VALIDATION HELPERS
    // ==========================================

    private UserAccount findAccount(Long accountId) {
        if (accountId == null || accountId <= 0) {
            throw badRequest("Valid account ID is required");
        }

        return accountRepository.findById(accountId)
                .orElseThrow(() ->
                        notFound("User account not found")
                );
    }

    private String validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw badRequest("Username is required");
        }

        String value = username.trim();

        if (value.length() < 3 || value.length() > 100) {
            throw badRequest(
                    "Username must contain 3 to 100 characters"
            );
        }

        if (!value.matches("^[A-Za-z0-9._-]+$")) {
            throw badRequest(
                    "Username can contain letters, numbers, "
                            + "dots, underscores and hyphens"
            );
        }

        return value;
    }

    private String validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw badRequest("Email is required");
        }

        String value = email.trim()
                .toLowerCase(Locale.ROOT);

        if (value.length() > 180
                || !value.matches(
                "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
        )) {
            throw badRequest("Invalid email address");
        }

        return value;
    }

    private void validatePassword(String password) {
        if (password == null
                || password.length() < 8
                || password.length() > 128) {
            throw badRequest(
                    "Password must contain 8 to 128 characters"
            );
        }
    }

    private void ensureUniqueCredentials(
            String username,
            String email
    ) {
        if (accountRepository
                .existsByUsernameIgnoreCase(username)) {
            throw conflict("Username already exists");
        }

        if (accountRepository
                .existsByEmailIgnoreCase(email)) {
            throw conflict("Email already exists");
        }
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            throw badRequest("Role is required");
        }

        String value = role.trim()
                .toUpperCase(Locale.ROOT);

        try {
            UserRole.valueOf(value);
            return value;
        } catch (IllegalArgumentException exception) {
            throw badRequest("Invalid account role");
        }
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            throw badRequest("Account status is required");
        }

        String value = status.trim()
                .toUpperCase(Locale.ROOT);

        if (!ACCOUNT_STATUSES.contains(value)) {
            throw badRequest("Invalid account status");
        }

        return value;
    }

    // ==========================================
    // SAFE RESPONSE MAPPING
    // ==========================================

    private AccountProfile toProfile(UserAccount account) {
        return new AccountProfile(
                account.getId(),
                account.getUsername(),
                account.getEmail(),
                account.getRole(),
                account.getAccountStatus(),
                account.getPassengerId(),
                account.getDriverId(),
                account.getCreatedAt()
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

    private ResponseStatusException forbidden(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.FORBIDDEN,
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
