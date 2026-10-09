
package com.smartmove.backend.controller;

import com.smartmove.backend.entity.UserRole;
import com.smartmove.backend.service.CurrentUserService;
import com.smartmove.backend.service.CurrentUserService.CurrentAccountProfile;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class CurrentUserController {

    private final CurrentUserService currentUserService;

    public CurrentUserController(
            CurrentUserService currentUserService
    ) {
        this.currentUserService = currentUserService;
    }

    // ==========================================
    // CURRENT AUTHENTICATED ACCOUNT
    // ==========================================

    @GetMapping("/me")
    public ResponseEntity<CurrentAccountProfile> getMyAccount() {

        return ResponseEntity.ok(
                currentUserService.getCurrentAccountProfile()
        );
    }

    // ==========================================
    // CURRENT USER ROLE
    // ==========================================

    @GetMapping("/me/role")
    public ResponseEntity<Map<String, Object>> getMyRole() {

        CurrentAccountProfile profile =
                currentUserService.getCurrentAccountProfile();

        String role = profile.role();

        return ResponseEntity.ok(
                Map.of(
                        "accountId", profile.accountId(),
                        "role", role,
                        "isPassenger",
                        UserRole.PASSENGER.name().equals(role),
                        "isDriver",
                        UserRole.DRIVER.name().equals(role),
                        "isAdmin",
                        UserRole.ADMIN.name().equals(role),
                        "isSuperAdmin",
                        UserRole.SUPER_ADMIN.name().equals(role),
                        "isAdministrative",
                        UserRole.ADMIN.name().equals(role)
                                || UserRole.SUPER_ADMIN.name()
                                .equals(role)
                )
        );
    }

    // ==========================================
    // CURRENT PASSENGER PROFILE LINK
    // ==========================================

    @GetMapping("/me/passenger")
    public ResponseEntity<Map<String, Object>> getMyPassengerLink() {

        currentUserService.requirePassenger();

        Long passengerId =
                currentUserService.getCurrentPassengerId();

        return ResponseEntity.ok(
                Map.of(
                        "accountId",
                        currentUserService.getCurrentAccountId(),
                        "passengerId",
                        passengerId,
                        "role",
                        UserRole.PASSENGER.name()
                )
        );
    }

    // ==========================================
    // CURRENT DRIVER PROFILE LINK
    // ==========================================

    @GetMapping("/me/driver")
    public ResponseEntity<Map<String, Object>> getMyDriverLink() {

        currentUserService.requireDriver();

        Long driverId =
                currentUserService.getCurrentDriverId();

        return ResponseEntity.ok(
                Map.of(
                        "accountId",
                        currentUserService.getCurrentAccountId(),
                        "driverId",
                        driverId,
                        "role",
                        UserRole.DRIVER.name()
                )
        );
    }

    // ==========================================
    // CURRENT USER DASHBOARD DESTINATION
    // ==========================================

    @GetMapping("/me/dashboard")
    public ResponseEntity<Map<String, String>> getMyDashboard() {

        CurrentAccountProfile profile =
                currentUserService.getCurrentAccountProfile();

        String dashboard = switch (profile.role()) {

            case "PASSENGER" -> "passenger";

            case "DRIVER" -> "driver";

            case "ADMIN" -> "admin";

            case "SUPER_ADMIN" -> "super-admin";

            default -> throw new IllegalStateException(
                    "Unsupported account role"
            );
        };

        return ResponseEntity.ok(
                Map.of(
                        "role", profile.role(),
                        "dashboard", dashboard
                )
        );
    }

    // ==========================================
    // ACCOUNT SESSION CHECK
    // ==========================================

    @GetMapping("/me/session")
    public ResponseEntity<Map<String, Object>> checkMySession() {

        CurrentAccountProfile profile =
                currentUserService.getCurrentAccountProfile();

        return ResponseEntity.ok(
                Map.of(
                        "authenticated", true,
                        "accountId", profile.accountId(),
                        "username", profile.username(),
                        "role", profile.role(),
                        "accountStatus", profile.accountStatus()
                )
        );
    }
}
