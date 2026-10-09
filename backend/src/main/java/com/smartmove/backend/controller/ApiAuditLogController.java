
package com.smartmove.backend.controller;

import com.smartmove.backend.service.ApiAuditLogService;
import com.smartmove.backend.service.ApiAuditLogService.AuditLogProfile;
import com.smartmove.backend.service.ApiAuditLogService.AuditStatistics;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import org.springframework.format.annotation.DateTimeFormat;

import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.validation.annotation.Validated;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/audit-logs")
@Validated
public class ApiAuditLogController {

    private final ApiAuditLogService auditLogService;

    public ApiAuditLogController(
            ApiAuditLogService auditLogService
    ) {
        this.auditLogService = auditLogService;
    }

    // ==========================================
    // API INFORMATION
    // ==========================================

    @GetMapping("/info")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getInfo() {

        return ResponseEntity.ok(
                Map.of(
                        "module", "SmartMove Audit Management",
                        "access", "SUPER_ADMIN",
                        "readOnly", true,
                        "database", "Oracle",
                        "description",
                        "Administrative and system activity history"
                )
        );
    }

    // ==========================================
    // RECENT AUDIT ACTIVITY
    // ==========================================

    @GetMapping("/recent")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<AuditLogProfile>>
    getRecentActivity() {

        return ResponseEntity.ok(
                auditLogService.getRecentActivity()
        );
    }

    // ==========================================
    // PAGINATED AUDIT HISTORY
    // ==========================================

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<AuditLogProfile>>
    getAuditHistory(
            @RequestParam(defaultValue = "0")
            @Min(0)
            int page,

            @RequestParam(defaultValue = "20")
            @Min(1)
            @Max(200)
            int size
    ) {
        Pageable pageable = createPageable(page, size);

        return ResponseEntity.ok(
                auditLogService.getAuditHistory(pageable)
        );
    }

    // ==========================================
    // AUDIT STATISTICS
    // ==========================================

    @GetMapping("/statistics")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<AuditStatistics>
    getStatistics() {

        return ResponseEntity.ok(
                auditLogService.getStatistics()
        );
    }

    // ==========================================
    // AUDIT HISTORY BY ACCOUNT
    // ==========================================

    @GetMapping("/actors/{accountId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<AuditLogProfile>>
    getByActor(
            @PathVariable
            @Positive
            Long accountId
    ) {
        return ResponseEntity.ok(
                auditLogService.getByActor(accountId)
        );
    }

    // ==========================================
    // PAGINATED AUDIT HISTORY BY ACCOUNT
    // ==========================================

    @GetMapping("/actors/{accountId}/page")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<AuditLogProfile>>
    getByActorPage(
            @PathVariable
            @Positive
            Long accountId,

            @RequestParam(defaultValue = "0")
            @Min(0)
            int page,

            @RequestParam(defaultValue = "20")
            @Min(1)
            @Max(200)
            int size
    ) {
        return ResponseEntity.ok(
                auditLogService.getByActor(
                        accountId,
                        createPageable(page, size)
                )
        );
    }

    // ==========================================
    // AUDIT HISTORY BY USERNAME
    // ==========================================

    @GetMapping("/usernames/{username}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<AuditLogProfile>>
    getByUsername(
            @PathVariable
            @NotBlank
            String username
    ) {
        return ResponseEntity.ok(
                auditLogService.getByUsername(username)
        );
    }

    // ==========================================
    // AUDIT HISTORY BY ROLE
    // ==========================================

    @GetMapping("/roles/{role}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<AuditLogProfile>>
    getByRole(
            @PathVariable
            @NotBlank
            String role
    ) {
        return ResponseEntity.ok(
                auditLogService.getByRole(role)
        );
    }

    // ==========================================
    // AUDIT HISTORY BY ACTION
    // ==========================================

    @GetMapping("/actions/{action}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<AuditLogProfile>>
    getByAction(
            @PathVariable
            @NotBlank
            String action
    ) {
        return ResponseEntity.ok(
                auditLogService.getByAction(action)
        );
    }

    // ==========================================
    // AUDIT HISTORY BY ENTITY TYPE
    // ==========================================

    @GetMapping("/entities/{entityType}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<AuditLogProfile>>
    getByEntityType(
            @PathVariable
            @NotBlank
            String entityType
    ) {
        return ResponseEntity.ok(
                auditLogService.getByEntityType(entityType)
        );
    }

    // ==========================================
    // AUDIT HISTORY FOR A SPECIFIC RECORD
    // ==========================================

    @GetMapping("/entities/{entityType}/{entityId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<AuditLogProfile>>
    getEntityHistory(
            @PathVariable
            @NotBlank
            String entityType,

            @PathVariable
            @NotBlank
            String entityId
    ) {
        return ResponseEntity.ok(
                auditLogService.getEntityHistory(
                        entityType,
                        entityId
                )
        );
    }

    // ==========================================
    // AUDIT HISTORY BY RESULT
    // ==========================================

    @GetMapping("/results/{result}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<AuditLogProfile>>
    getByResult(
            @PathVariable
            @NotBlank
            String result
    ) {
        return ResponseEntity.ok(
                auditLogService.getByResult(result)
        );
    }

    // ==========================================
    // AUDIT HISTORY BY DATE RANGE
    // ==========================================

    @GetMapping("/date-range")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<AuditLogProfile>>
    getByDateRange(
            @RequestParam
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME
            )
            LocalDateTime start,

            @RequestParam
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME
            )
            LocalDateTime end
    ) {
        return ResponseEntity.ok(
                auditLogService.getByDateRange(
                        start,
                        end
                )
        );
    }

    // ==========================================
    // PAGINATED AUDIT HISTORY BY DATE RANGE
    // ==========================================

    @GetMapping("/date-range/page")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<AuditLogProfile>>
    getByDateRangePage(
            @RequestParam
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME
            )
            LocalDateTime start,

            @RequestParam
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME
            )
            LocalDateTime end,

            @RequestParam(defaultValue = "0")
            @Min(0)
            int page,

            @RequestParam(defaultValue = "20")
            @Min(1)
            @Max(200)
            int size
    ) {
        return ResponseEntity.ok(
                auditLogService.getByDateRange(
                        start,
                        end,
                        createPageable(page, size)
                )
        );
    }

    // ==========================================
    // AUDIT HISTORY BY CORRELATION ID
    // ==========================================

    @GetMapping("/correlations/{correlationId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<AuditLogProfile>>
    getByCorrelationId(
            @PathVariable
            @NotBlank
            String correlationId
    ) {
        return ResponseEntity.ok(
                auditLogService.getByCorrelationId(
                        correlationId
                )
        );
    }

    // ==========================================
    // FAILED / DENIED ACTIONS BY ACCOUNT
    // ==========================================

    @GetMapping("/actors/{accountId}/failures")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<AuditLogProfile>>
    getFailedActionsByActor(
            @PathVariable
            @Positive
            Long accountId
    ) {
        return ResponseEntity.ok(
                auditLogService.getFailedActionsByActor(
                        accountId
                )
        );
    }

    // ==========================================
    // SINGLE AUDIT RECORD
    // ==========================================

    @GetMapping("/{auditId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<AuditLogProfile>
    getAuditById(
            @PathVariable
            @Positive
            Long auditId
    ) {
        return ResponseEntity.ok(
                auditLogService.getAuditById(auditId)
        );
    }

    // ==========================================
    // PAGINATION HELPER
    // ==========================================

    private Pageable createPageable(
            int page,
            int size
    ) {
        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page number cannot be negative"
            );
        }

        if (size < 1 || size > 200) {
            throw new IllegalArgumentException(
                    "Page size must be between 1 and 200"
            );
        }

        return PageRequest.of(page, size);
    }
}
