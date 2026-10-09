
package com.smartmove.backend.service;

import com.smartmove.backend.entity.Passenger;
import com.smartmove.backend.entity.UserAccount;
import com.smartmove.backend.entity.UserRole;
import com.smartmove.backend.repository.PassengerRepository;
import com.smartmove.backend.repository.UserAccountRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class AuthService {

    private final UserAccountRepository accountRepository;
    private final PassengerRepository passengerRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserAccountRepository accountRepository,
            PassengerRepository passengerRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.accountRepository = accountRepository;
        this.passengerRepository = passengerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ==========================================
    // PASSENGER REGISTRATION
    // ==========================================

    @Transactional
    public UserAccount registerPassenger(
            String username,
            String email,
            String password,
            String fullName,
            String phone,
            String city
    ) {
        String cleanUsername = normalizeRequired(
                username, "Username"
        );

        String cleanEmail = normalizeEmail(email);

        String cleanName = normalizeRequired(
                fullName, "Full name"
        );

        validatePassword(password);

        if (accountRepository.existsByUsernameIgnoreCase(
                cleanUsername
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Username is already registered"
            );
        }

        if (accountRepository.existsByEmailIgnoreCase(
                cleanEmail
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email is already registered"
            );
        }

        Passenger passenger = new Passenger();
        passenger.setName(cleanName);
        passenger.setEmail(cleanEmail);
        passenger.setPhone(phone);
        passenger.setCity(city);

        Passenger savedPassenger =
                passengerRepository.save(passenger);

        UserAccount account = new UserAccount();

        account.setUsername(cleanUsername);
        account.setEmail(cleanEmail);
        account.setPasswordHash(
                passwordEncoder.encode(password)
        );

        account.setRole(UserRole.PASSENGER.name());
        account.setAccountStatus("ACTIVE");
        account.setPassengerId(savedPassenger.getId());
        account.setCreatedAt(LocalDateTime.now());

        return accountRepository.save(account);
    }

    // ==========================================
    // LOGIN VALIDATION
    // ==========================================

    @Transactional(readOnly = true)
    public UserAccount authenticate(
            String usernameOrEmail,
            String password
    ) {
        if (usernameOrEmail == null ||
                usernameOrEmail.isBlank() ||
                password == null) {
            throw invalidCredentials();
        }

        String identifier = usernameOrEmail.trim();

        UserAccount account = accountRepository
                .findByUsernameIgnoreCase(identifier)
                .or(() -> accountRepository
                        .findByEmailIgnoreCase(identifier))
                .orElseThrow(this::invalidCredentials);

        if (!passwordEncoder.matches(
                password,
                account.getPasswordHash()
        )) {
            throw invalidCredentials();
        }

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
    // ACCOUNT LOOKUP
    // ==========================================

    @Transactional(readOnly = true)
    public UserAccount getAccount(Long accountId) {
        if (accountId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Account ID is required"
            );
        }

        return accountRepository.findById(accountId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Account not found"
                        )
                );
    }

    // ==========================================
    // ROLE VALIDATION
    // ==========================================

    public boolean hasRole(
            UserAccount account,
            UserRole role
    ) {
        return account != null
                && role != null
                && role.name().equalsIgnoreCase(
                account.getRole()
        );
    }

    public boolean hasAnyRole(
            UserAccount account,
            UserRole... allowedRoles
    ) {
        if (account == null || allowedRoles == null) {
            return false;
        }

        for (UserRole role : allowedRoles) {
            if (hasRole(account, role)) {
                return true;
            }
        }

        return false;
    }

    public void requireRole(
            UserAccount account,
            UserRole... allowedRoles
    ) {
        if (account == null ||
                !"ACTIVE".equalsIgnoreCase(
                        account.getAccountStatus()
                )) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Access denied"
            );
        }

        if (!hasAnyRole(account, allowedRoles)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Insufficient permissions"
            );
        }
    }

    // ==========================================
    // PASSWORD MANAGEMENT
    // ==========================================

    @Transactional
    public void changePassword(
            Long accountId,
            String currentPassword,
            String newPassword
    ) {
        UserAccount account = getAccount(accountId);

        if (!passwordEncoder.matches(
                currentPassword,
                account.getPasswordHash()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Current password is incorrect"
            );
        }

        validatePassword(newPassword);

        if (passwordEncoder.matches(
                newPassword,
                account.getPasswordHash()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "New password must be different"
            );
        }

        account.setPasswordHash(
                passwordEncoder.encode(newPassword)
        );

        accountRepository.save(account);
    }

    // ==========================================
    // ACCOUNT STATUS
    // ==========================================

    @Transactional
    public UserAccount updateAccountStatus(
            Long accountId,
            String newStatus
    ) {
        UserAccount account = getAccount(accountId);

        String status = normalizeRequired(
                newStatus, "Account status"
        ).toUpperCase(Locale.ROOT);

        if (!status.equals("ACTIVE") &&
                !status.equals("SUSPENDED") &&
                !status.equals("DISABLED")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid account status"
            );
        }

        account.setAccountStatus(status);

        return accountRepository.save(account);
    }

    // ==========================================
    // VALIDATION HELPERS
    // ==========================================

    private String normalizeRequired(
            String value,
            String fieldName
    ) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    fieldName + " is required"
            );
        }

        return value.trim();
    }

    private String normalizeEmail(String email) {
        String normalized = normalizeRequired(
                email, "Email"
        ).toLowerCase(Locale.ROOT);

        if (!normalized.matches(
                "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
        )) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid email address"
            );
        }

        return normalized;
    }

    private void validatePassword(String password) {
        if (password == null ||
                password.length() < 8 ||
                password.length() > 128) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Password must contain 8 to 128 characters"
            );
        }
    }

    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid username or password"
        );
    }
}
