
package com.smartmove.backend.controller;

import com.smartmove.backend.entity.UserAccount;
import com.smartmove.backend.repository.UserAccountRepository;
import com.smartmove.backend.service.AdminService;

import com.smartmove.backend.service.AdminService.AccountProfile;
import com.smartmove.backend.service.AdminService.AccountStatistics;
import com.smartmove.backend.service.AdminService.CreateAdminRequest;
import com.smartmove.backend.service.AdminService.CreateDriverAccountRequest;
import com.smartmove.backend.service.AdminService.ChangeRoleRequest;
import com.smartmove.backend.service.AdminService.ChangeAccountStatusRequest;
import com.smartmove.backend.service.AdminService.ResetPasswordRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@Validated
public class AdminController {

    private final AdminService adminService;
    private final UserAccountRepository accountRepository;

    public AdminController(
            AdminService adminService,
            UserAccountRepository accountRepository
    ) {
        this.adminService = adminService;
        this.accountRepository = accountRepository;
    }

    // ==========================================
    // ADMIN: GET ACCOUNT DETAILS
    // ==========================================

    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<AccountProfile> getAccount(
            Authentication authentication,
            @PathVariable @Positive Long accountId
    ) {
        return ResponseEntity.ok(
                adminService.getAccount(
                        currentAccountId(authentication),
                        accountId
                )
        );
    }

    // ==========================================
    // ADMIN: LIST ALL ACCOUNTS
    // ==========================================

    @GetMapping("/accounts")
    public ResponseEntity<Page<AccountProfile>> getAllAccounts(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                adminService.getAllAccounts(
                        currentAccountId(authentication),
                        createPageable(page, size)
                )
        );
    }

    // ==========================================
    // ADMIN: SEARCH ACCOUNTS
    // ==========================================

    @GetMapping("/accounts/search")
    public ResponseEntity<List<AccountProfile>> searchAccounts(
            Authentication authentication,
            @RequestParam(defaultValue = "") String keyword
    ) {
        return ResponseEntity.ok(
                adminService.searchAccounts(
                        currentAccountId(authentication),
                        keyword
                )
        );
    }

    // ==========================================
    // ADMIN: FILTER ACCOUNTS BY ROLE
    // ==========================================

    @GetMapping("/accounts/role/{role}")
    public ResponseEntity<List<AccountProfile>> getAccountsByRole(
            Authentication authentication,
            @PathVariable @NotBlank String role
    ) {
        return ResponseEntity.ok(
                adminService.getAccountsByRole(
                        currentAccountId(authentication),
                        role
                )
        );
    }

    // ==========================================
    // ADMIN: FILTER ACCOUNTS BY STATUS
    // ==========================================

    @GetMapping("/accounts/status/{status}")
    public ResponseEntity<List<AccountProfile>> getAccountsByStatus(
            Authentication authentication,
            @PathVariable @NotBlank String status
    ) {
        return ResponseEntity.ok(
                adminService.getAccountsByStatus(
                        currentAccountId(authentication),
                        status
                )
        );
    }

    // ==========================================
    // SUPER ADMIN: ACCOUNT STATISTICS
    // ==========================================

    @GetMapping("/accounts/stats")
    public ResponseEntity<AccountStatistics> getAccountStatistics(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                adminService.getAccountStatistics(
                        currentAccountId(authentication)
                )
        );
    }

    // ==========================================
    // SUPER ADMIN: CREATE ADMIN ACCOUNT
    // ==========================================

    @PostMapping("/accounts/admin")
    public ResponseEntity<AccountProfile> createAdmin(
            Authentication authentication,
            @Valid @RequestBody CreateAdminPayload request
    ) {
        AccountProfile account = adminService.createAdmin(
                currentAccountId(authentication),
                new CreateAdminRequest(
                        request.username(),
                        request.email(),
                        request.password(),
                        request.role()
                )
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(account);
    }

    // ==========================================
    // ADMIN: CREATE DRIVER LOGIN ACCOUNT
    // ==========================================

    @PostMapping("/accounts/driver")
    public ResponseEntity<AccountProfile> createDriverAccount(
            Authentication authentication,
            @Valid @RequestBody CreateDriverAccountPayload request
    ) {
        AccountProfile account = adminService.createDriverAccount(
                currentAccountId(authentication),
                new CreateDriverAccountRequest(
                        request.driverId(),
                        request.username(),
                        request.password()
                )
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(account);
    }

    // ==========================================
    // SUPER ADMIN: CHANGE ACCOUNT ROLE
    // ==========================================

    @PatchMapping("/accounts/{accountId}/role")
    public ResponseEntity<AccountProfile> changeAccountRole(
            Authentication authentication,
            @PathVariable @Positive Long accountId,
            @Valid @RequestBody ChangeRolePayload request
    ) {
        return ResponseEntity.ok(
                adminService.changeAccountRole(
                        currentAccountId(authentication),
                        accountId,
                        new ChangeRoleRequest(
                                request.newRole()
                        )
                )
        );
    }

    // ==========================================
    // ADMIN: CHANGE ACCOUNT STATUS
    // ==========================================

    @PatchMapping("/accounts/{accountId}/status")
    public ResponseEntity<AccountProfile> changeAccountStatus(
            Authentication authentication,
            @PathVariable @Positive Long accountId,
            @Valid @RequestBody ChangeAccountStatusPayload request
    ) {
        return ResponseEntity.ok(
                adminService.changeAccountStatus(
                        currentAccountId(authentication),
                        accountId,
                        new ChangeAccountStatusRequest(
                                request.newStatus()
                        )
                )
        );
    }

    // ==========================================
    // ADMIN: RESET ACCOUNT PASSWORD
    // ==========================================

    @PatchMapping("/accounts/{accountId}/password/reset")
    public ResponseEntity<AccountProfile> resetAccountPassword(
            Authentication authentication,
            @PathVariable @Positive Long accountId,
            @Valid @RequestBody ResetPasswordPayload request
    ) {
        return ResponseEntity.ok(
                adminService.resetAccountPassword(
                        currentAccountId(authentication),
                        accountId,
                        new ResetPasswordRequest(
                                request.newPassword()
                        )
                )
        );
    }

    // ==========================================
    // REQUEST DTOs
    // ==========================================

    public record CreateAdminPayload(

            @NotBlank(message = "Username is required")
            @Size(min = 3, max = 100)
            String username,

            @NotBlank(message = "Email is required")
            @Email
            @Size(max = 180)
            String email,

            @NotBlank(message = "Password is required")
            @Size(min = 8, max = 128)
            String password,

            @NotBlank(message = "Role is required")
            String role

    ) {}

    public record CreateDriverAccountPayload(

            @NotNull(message = "Driver ID is required")
            @Positive
            Long driverId,

            @NotBlank(message = "Username is required")
            @Size(min = 3, max = 100)
            String username,

            @NotBlank(message = "Password is required")
            @Size(min = 8, max = 128)
            String password

    ) {}

    public record ChangeRolePayload(

            @NotBlank(message = "New role is required")
            String newRole

    ) {}

    public record ChangeAccountStatusPayload(

            @NotBlank(message = "New status is required")
            String newStatus

    ) {}

    public record ResetPasswordPayload(

            @NotBlank(message = "New password is required")
            @Size(min = 8, max = 128)
            String newPassword

    ) {}

    // ==========================================
    // AUTHENTICATED ACCOUNT RESOLUTION
    // ==========================================

    private Long currentAccountId(
            Authentication authentication
    ) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || authentication.getName().isBlank()
                || "anonymousUser".equalsIgnoreCase(
                authentication.getName()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication is required"
            );
        }

        String username = authentication.getName();

        UserAccount account = accountRepository
                .findByUsernameIgnoreCase(username)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Authenticated account not found"
                        )
                );

        if (!"ACTIVE".equalsIgnoreCase(
                account.getAccountStatus()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Account is not active"
            );
        }

        return account.getId();
    }

    // ==========================================
    // PAGINATION HELPER
    // ==========================================

    private Pageable createPageable(
            int page,
            int size
    ) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page must be nonnegative and size must be between 1 and 100"
            );
        }

        return PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Direction.DESC,
                        "createdAt"
                )
        );
    }
}
