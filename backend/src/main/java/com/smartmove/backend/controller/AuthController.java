
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
import java.security.SecureRandom;
import java.util.Base64;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import com.smartmove.backend.repository.UserAccountRepository;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final UserAccountRepository accountRepository;
    private static final SecureRandom RANDOM = new SecureRandom();

    public AuthController(AuthService authService, AuthenticationManager authenticationManager,
                          UserAccountRepository accountRepository) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.accountRepository = accountRepository;
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
    public ResponseEntity<SessionResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest
    ) {
        UserAccount account = authService.authenticate(
                request.usernameOrEmail(),
                request.password()
        );

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(account.getUsername(), request.password()));
        HttpSession previous = servletRequest.getSession(false);
        if (previous != null) previous.invalidate();
        HttpSession session = servletRequest.getSession(true);
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String csrfToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        session.setAttribute("SMARTMOVE_CSRF", csrfToken);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return ResponseEntity.ok(toSessionResponse(account, csrfToken));
    }

    @GetMapping("/session")
    public ResponseEntity<SessionResponse> session(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (session == null || authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session expired");
        }
        UserAccount account = accountRepository.findByUsernameIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account not found"));
        if (!"ACTIVE".equalsIgnoreCase(account.getAccountStatus())) {
            session.invalidate();
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account disabled");
        }
        return ResponseEntity.ok(toSessionResponse(account, (String) session.getAttribute("SMARTMOVE_CSRF")));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    private SessionResponse toSessionResponse(UserAccount account, String csrfToken) {
        return new SessionResponse(toResponse(account, "Authenticated"), csrfToken);
    }

    public record SessionResponse(AuthResponse account, String csrfToken) {}

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
                "Server-side session authentication enabled"
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
