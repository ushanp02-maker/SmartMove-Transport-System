
package com.smartmove.backend.config;

import com.smartmove.backend.entity.UserAccount;
import com.smartmove.backend.entity.UserRole;
import com.smartmove.backend.repository.UserAccountRepository;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;

import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Locale;

@Component
public class SuperAdminBootstrap implements ApplicationRunner {

    private final UserAccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    public SuperAdminBootstrap(
            UserAccountRepository accountRepository,
            PasswordEncoder passwordEncoder,
            Environment environment
    ) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
    }

    // ==========================================
    // APPLICATION STARTUP
    // ==========================================

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        String enabled = environment.getProperty(
                "SMARTMOVE_BOOTSTRAP_ENABLED",
                "false"
        );

        // Bootstrap is disabled unless explicitly enabled.
        if (!Boolean.parseBoolean(enabled)) {
            return;
        }

        // Never automatically create a Super Admin if
        // at least one already exists.
        if (!accountRepository.findByRole(
                UserRole.SUPER_ADMIN.name()
        ).isEmpty()) {
            System.out.println(
                    "SmartMove: Super Admin already exists. "
                            + "Bootstrap skipped."
            );
            return;
        }

        String username = environment.getProperty(
                "SMARTMOVE_SUPERADMIN_USERNAME"
        );

        String email = environment.getProperty(
                "SMARTMOVE_SUPERADMIN_EMAIL"
        );

        String password = environment.getProperty(
                "SMARTMOVE_SUPERADMIN_PASSWORD"
        );

        // All three credentials are mandatory.
        if (username == null || username.isBlank()
                || email == null || email.isBlank()
                || password == null || password.isBlank()) {

            throw new IllegalStateException(
                    "Super Admin bootstrap is enabled, "
                            + "but username, email or password is missing."
            );
        }

        username = username.trim();
        email = email.trim().toLowerCase(Locale.ROOT);

        // ==========================================
        // VALIDATE USERNAME
        // ==========================================

        if (username.length() < 3
                || username.length() > 100
                || !username.matches(
                "^[A-Za-z0-9._-]+$"
        )) {
            throw new IllegalStateException(
                    "Invalid bootstrap username. "
                            + "Use 3-100 letters, numbers, dots, "
                            + "underscores or hyphens."
            );
        }

        // ==========================================
        // VALIDATE EMAIL
        // ==========================================

        if (email.length() > 180
                || !email.matches(
                "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
        )) {
            throw new IllegalStateException(
                    "Invalid bootstrap email address."
            );
        }

        // ==========================================
        // VALIDATE PASSWORD
        // ==========================================

        if (password.length() < 12
                || password.length() > 128) {
            throw new IllegalStateException(
                    "Bootstrap password must contain "
                            + "12-128 characters."
            );
        }

        // ==========================================
        // CHECK UNIQUE CREDENTIALS
        // ==========================================

        if (accountRepository
                .existsByUsernameIgnoreCase(username)) {
            throw new IllegalStateException(
                    "Bootstrap username already exists."
            );
        }

        if (accountRepository
                .existsByEmailIgnoreCase(email)) {
            throw new IllegalStateException(
                    "Bootstrap email already exists."
            );
        }

        // ==========================================
        // CREATE FIRST SUPER ADMIN
        // ==========================================

        UserAccount account = new UserAccount();

        account.setUsername(username);
        account.setEmail(email);
        account.setPasswordHash(
                passwordEncoder.encode(password)
        );

        account.setRole(UserRole.SUPER_ADMIN.name());
        account.setAccountStatus("ACTIVE");

        account.setPassengerId(null);
        account.setDriverId(null);

        account.setCreatedAt(LocalDateTime.now());

        accountRepository.saveAndFlush(account);

        System.out.println(
                "SmartMove: Initial Super Admin "
                        + "account created successfully."
        );

        System.out.println(
                "SmartMove: Disable bootstrap "
                        + "after initial setup."
        );
    }
}
