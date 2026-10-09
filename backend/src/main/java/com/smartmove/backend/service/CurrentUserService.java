
package com.smartmove.backend.service;

import com.smartmove.backend.entity.UserAccount;
import com.smartmove.backend.entity.UserRole;
import com.smartmove.backend.repository.UserAccountRepository;

import org.springframework.http.HttpStatus;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;

@Service
@Transactional(readOnly = true)
public class CurrentUserService {

    private final UserAccountRepository accountRepository;

    public CurrentUserService(
            UserAccountRepository accountRepository
    ) {
        this.accountRepository = accountRepository;
    }

    // ==========================================
    // CURRENT AUTHENTICATED ACCOUNT
    // ==========================================

    public UserAccount getCurrentAccount() {

        Authentication authentication =
                SecurityContextHolder.getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw unauthorized();
        }

        String username = authentication.getName();

        if (username == null
                || username.isBlank()
                || "anonymousUser".equalsIgnoreCase(username)) {
            throw unauthorized();
        }

        UserAccount account = accountRepository
                .findByUsernameIgnoreCase(username)
                .orElseThrow(this::unauthorized);

        if (!"ACTIVE".equalsIgnoreCase(
                account.getAccountStatus()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This account is not active"
            );
        }

        return account;
    }

    // ==========================================
    // CURRENT ACCOUNT PROFILE
    // ==========================================

    public CurrentAccountProfile getCurrentAccountProfile() {

        UserAccount account = getCurrentAccount();

        return new CurrentAccountProfile(
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

    public record CurrentAccountProfile(
            Long accountId,
            String username,
            String email,
            String role,
            String accountStatus,
            Long passengerId,
            Long driverId,
            java.time.LocalDateTime createdAt
    ) {}

    // ==========================================
    // CURRENT ACCOUNT ID
    // ==========================================

    public Long getCurrentAccountId() {
        return getCurrentAccount().getId();
    }

    // ==========================================
    // ROLE CHECKS
    // ==========================================

    public boolean hasRole(UserRole role) {

        if (role == null) {
            return false;
        }

        UserAccount account = getCurrentAccount();

        return role.name().equalsIgnoreCase(
                account.getRole()
        );
    }

    public boolean hasAnyRole(UserRole... roles) {

        if (roles == null || roles.length == 0) {
            return false;
        }

        UserAccount account = getCurrentAccount();

        return Arrays.stream(roles)
                .filter(role -> role != null)
                .anyMatch(role ->
                        role.name().equalsIgnoreCase(
                                account.getRole()
                        )
                );
    }

    public void requireRole(UserRole... allowedRoles) {

        if (!hasAnyRole(allowedRoles)) {
            throw forbidden("Insufficient permissions");
        }
    }

    public void requireAdmin() {

        requireRole(
                UserRole.ADMIN,
                UserRole.SUPER_ADMIN
        );
    }

    public void requireSuperAdmin() {

        requireRole(UserRole.SUPER_ADMIN);
    }

    public void requireDriver() {

        requireRole(UserRole.DRIVER);
    }

    public void requirePassenger() {

        requireRole(UserRole.PASSENGER);
    }

    // ==========================================
    // PASSENGER OWNERSHIP
    // ==========================================

    public Long getCurrentPassengerId() {

        UserAccount account = getCurrentAccount();

        requireRole(UserRole.PASSENGER);

        if (account.getPassengerId() == null) {
            throw forbidden(
                    "No passenger profile is linked to this account"
            );
        }

        return account.getPassengerId();
    }

    public void requirePassengerOwnership(
            Long passengerId
    ) {
        UserAccount account = getCurrentAccount();

        // Admins may manage passenger records.
        if (isAdministrative(account)) {
            return;
        }

        if (!UserRole.PASSENGER.name()
                .equalsIgnoreCase(account.getRole())) {
            throw forbidden(
                    "Only passengers may access this resource"
            );
        }

        if (passengerId == null
                || !passengerId.equals(
                account.getPassengerId()
        )) {
            throw forbidden(
                    "You cannot access another passenger's data"
            );
        }
    }

    // ==========================================
    // DRIVER OWNERSHIP
    // ==========================================

    public Long getCurrentDriverId() {

        UserAccount account = getCurrentAccount();

        requireRole(UserRole.DRIVER);

        if (account.getDriverId() == null) {
            throw forbidden(
                    "No driver profile is linked to this account"
            );
        }

        return account.getDriverId();
    }

    public void requireDriverOwnership(
            Long driverId
    ) {
        UserAccount account = getCurrentAccount();

        // Admins may manage driver records.
        if (isAdministrative(account)) {
            return;
        }

        if (!UserRole.DRIVER.name()
                .equalsIgnoreCase(account.getRole())) {
            throw forbidden(
                    "Only drivers may access this resource"
            );
        }

        if (driverId == null
                || !driverId.equals(
                account.getDriverId()
        )) {
            throw forbidden(
                    "You cannot access another driver's data"
            );
        }
    }

    // ==========================================
    // ACCOUNT OWNERSHIP
    // ==========================================

    public void requireAccountOwnership(
            Long accountId
    ) {
        UserAccount account = getCurrentAccount();

        if (isAdministrative(account)) {
            return;
        }

        if (accountId == null
                || !accountId.equals(account.getId())) {
            throw forbidden(
                    "You cannot access another user's account"
            );
        }
    }

    // ==========================================
    // BOOKING OWNERSHIP
    // ==========================================

    public void requireBookingOwnership(
            Long bookingPassengerId
    ) {
        requirePassengerOwnership(bookingPassengerId);
    }

    // ==========================================
    // ADMINISTRATIVE ROLE HELPER
    // ==========================================

    private boolean isAdministrative(
            UserAccount account
    ) {
        return UserRole.ADMIN.name()
                .equalsIgnoreCase(account.getRole())
                || UserRole.SUPER_ADMIN.name()
                .equalsIgnoreCase(account.getRole());
    }

    // ==========================================
    // ERROR HELPERS
    // ==========================================

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Authentication is required"
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
}
