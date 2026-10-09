
package com.smartmove.backend.controller;

import com.smartmove.backend.entity.UserAccount;
import com.smartmove.backend.service.AuthService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // ==========================================
    // PASSENGER REGISTRATION
    // ==========================================

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        UserAccount account = authService.registerPassenger(
                request.username(),
                request.email(),
                request.password(),
                request.fullName(),
                request.phone(),
                request.city()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toResponse(
                        account,
                        "Passenger registered successfully"
                ));
    }

    // ==========================================
    // LOGIN
    // ==========================================

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        UserAccount account = authService.authenticate(
                request.usernameOrEmail(),
                request.password()
        );

        return ResponseEntity.ok(
                toResponse(account, "Credentials verified")
        );
    }

    // ==========================================
    // API INFORMATION
    // ==========================================

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        return ResponseEntity.ok(Map.of(
                "service", "SmartMove Authentication",
                "registration", "Passenger self-registration",
                "roles", new String[]{
                        "PASSENGER",
                        "DRIVER",
                        "ADMIN",
                        "SUPER_ADMIN"
                },
                "loginStatus",
                "Credential verification only; session authentication pending"
        ));
    }

    // ==========================================
    // SAFE RESPONSE MAPPING
    // ==========================================

    private AuthResponse toResponse(
            UserAccount account,
            String message
    ) {
        return new AuthResponse(
                message,
                account.getId(),
                account.getUsername(),
                account.getEmail(),
                account.getRole(),
                account.getAccountStatus(),
                account.getPassengerId(),
                account.getDriverId()
        );
    }

    // ==========================================
    // REQUEST DTOs
    // ==========================================

    public record RegisterRequest(

            @NotBlank(message = "Username is required")
            String username,

            @NotBlank(message = "Email is required")
            @Email(message = "Invalid email address")
            String email,

            @NotBlank(message = "Password is required")
            @Size(
                    min = 8,
                    max = 128,
                    message = "Password must be 8-128 characters"
            )
            String password,

            @NotBlank(message = "Full name is required")
            String fullName,

            String phone,

            String city
    ) {
    }

    public record LoginRequest(

            @NotBlank(message = "Username or email is required")
            String usernameOrEmail,

            @NotBlank(message = "Password is required")
            String password
    ) {
    }

    // ==========================================
    // RESPONSE DTO
    // ==========================================

    public record AuthResponse(

            String message,

            Long accountId,

            String username,

            String email,

            String role,

            String accountStatus,

            Long passengerId,

            Long driverId
    ) {
    }
}
