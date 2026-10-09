
package com.smartmove.backend.service;

import com.smartmove.backend.entity.ApiAuditLog;
import com.smartmove.backend.entity.UserAccount;
import com.smartmove.backend.repository.ApiAuditLogRepository;
import com.smartmove.backend.repository.UserAccountRepository;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ApiAuditLogService {

    private static final Set<String> RESULTS =
            Set.of("SUCCESS", "FAILURE", "DENIED");

    private static final Set<String> ACTIONS = Set.of(
            "CREATE",
            "UPDATE",
            "DELETE",
            "APPROVE",
            "REJECT",
            "ASSIGN",
            "CANCEL",
            "LOGIN",
            "LOGOUT",
            "STATUS_CHANGE",
            "PASSWORD_RESET",
            "REFUND",
            "VERIFY",
            "PUBLISH",
            "START",
            "PAUSE",
            "RESUME",
            "COMPLETE",
            "OTHER"
    );

    private static final Set<String> ENTITY_TYPES = Set.of(
            "USER_ACCOUNT",
            "PASSENGER",
            "DRIVER",
            "VEHICLE",
            "ROUTE",
            "ROUTE_STOP",
            "TRIP",
            "BOOKING",
            "PAYMENT",
            "MAINTENANCE",
            "STAFF_TRANSPORT_REQUEST",
            "ON_DEMAND_TRIP_REQUEST",
            "ANNOUNCEMENT",
            "VEHICLE_DOCUMENT",
            "FEEDBACK",
            "OTHER"
    );

    private final ApiAuditLogRepository auditRepository;
    private final UserAccountRepository accountRepository;
    private final CurrentUserService currentUserService;

    public ApiAuditLogService(
            ApiAuditLogRepository auditRepository,
            UserAccountRepository accountRepository,
            CurrentUserService currentUserService
    ) {
        this.auditRepository = auditRepository;
        this.accountRepository = accountRepository;
        this.currentUserService = currentUserService;
    }

    // ==========================================
    // REQUEST / RESPONSE DTOs
    // ==========================================

    public record AuditEvent(
            String action,
            String entityType,
            String entityId,
            String result,
            Integer httpStatus,
            String description,
            String oldStatus,
            String newStatus,
            String correlationId
    ) {}

    public record AuditLogProfile(
            Long id,
            Long actorAccountId,
            String actorUsername,
            String actorRole,
            String action,
            String entityType,
            String entityId,
            String result,
            Integer httpStatus,
            String description,
            String oldStatus,
            String newStatus,
            String httpMethod,
            String requestPath,
            String ipAddress,
            String userAgent,
            String correlationId,
            LocalDateTime createdAt
    ) {}

    public record AuditGroupCount(
            String name,
            long count
    ) {}

    public record AuditStatistics(
            long totalEvents,
            long successfulEvents,
            long failedEvents,
            long deniedEvents,
            long eventsLast24Hours,
            long unsuccessfulLast24Hours,
            List<AuditGroupCount> actions,
            List<AuditGroupCount> entities,
            List<AuditGroupCount> results,
            List<AuditGroupCount> activeUsers
    ) {}

    // ==========================================
    // RECORD SUCCESSFUL BUSINESS ACTION
    // ==========================================

    @Transactional
    public AuditLogProfile recordSuccess(
            AuditEvent event
    ) {
        if (event == null) {
            throw badRequest("Audit event is required");
        }

        AuditEvent successEvent = new AuditEvent(
                event.action(),
                event.entityType(),
                event.entityId(),
                "SUCCESS",
                event.httpStatus(),
                event.description(),
                event.oldStatus(),
                event.newStatus(),
                event.correlationId()
        );

        return saveAuditEvent(successEvent);
    }

    // ==========================================
    // RECORD FAILED / DENIED ACTION
    // ==========================================

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLogProfile recordFailure(
            AuditEvent event
    ) {
        if (event == null) {
            throw badRequest("Audit event is required");
        }

        String result = normalize(event.result());

        if (!"FAILURE".equals(result)
                && !"DENIED".equals(result)) {
            throw badRequest(
                    "Failure audit result must be "
                            + "FAILURE or DENIED"
            );
        }

        return saveAuditEvent(event);
    }

    // ==========================================
    // RECORD AUDIT EVENT
    // ==========================================

    @Transactional
    public AuditLogProfile recordEvent(
            AuditEvent event
    ) {
        if (event == null) {
            throw badRequest("Audit event is required");
        }

        return saveAuditEvent(event);
    }

    // ==========================================
    // SAVE AUDIT EVENT
    // ==========================================

    private AuditLogProfile saveAuditEvent(
            AuditEvent event
    ) {
        validateEvent(event);

        ApiAuditLog entity = new ApiAuditLog();

        populateCurrentActor(entity);

        entity.setAction(normalize(event.action()));
        entity.setEntityType(normalize(event.entityType()));
        entity.setEntityId(
                limit(clean(event.entityId()), 120)
        );
        entity.setResult(normalize(event.result()));
        entity.setHttpStatus(event.httpStatus());

        entity.setDescription(
                limit(clean(event.description()), 2000)
        );

        entity.setOldStatus(
                limit(clean(event.oldStatus()), 100)
        );

        entity.setNewStatus(
                limit(clean(event.newStatus()), 100)
        );

        entity.setCorrelationId(
                limit(clean(event.correlationId()), 100)
        );

        populateRequestInformation(entity);

        return toProfile(auditRepository.save(entity));
    }

    // ==========================================
    // IDENTIFY CURRENT ACTOR
    // ==========================================

    private void populateCurrentActor(
            ApiAuditLog entity
    ) {
        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(
                authentication.getPrincipal()
        )) {
            entity.setActorUsername("ANONYMOUS");
            entity.setActorRole("ANONYMOUS");
            return;
        }

        String username = authentication.getName();

        if (username == null || username.isBlank()) {
            entity.setActorUsername("UNKNOWN");
            entity.setActorRole("UNKNOWN");
            return;
        }

        UserAccount account = accountRepository
                .findByUsernameIgnoreCase(username)
                .orElseGet(() ->
                        accountRepository
                                .findByEmailIgnoreCase(username)
                                .orElse(null)
                );

        if (account != null) {
            entity.setActor(account);
            entity.setActorUsername(
                    limit(account.getUsername(), 150)
            );
            entity.setActorRole(
                    limit(account.getRole(), 30)
            );
        } else {
            entity.setActorUsername(
                    limit(username, 150)
            );

            entity.setActorRole("UNKNOWN");
        }
    }

    // ==========================================
    // REQUEST METADATA
    // ==========================================

    private void populateRequestInformation(
            ApiAuditLog entity
    ) {
        var attributes =
                org.springframework.web.context.request
                        .RequestContextHolder
                        .getRequestAttributes();

        if (!(attributes instanceof
                org.springframework.web.context.request
                        .ServletRequestAttributes servletAttributes)) {
            return;
        }

        HttpServletRequest request =
                servletAttributes.getRequest();

        entity.setHttpMethod(
                limit(request.getMethod(), 10)
        );

        entity.setRequestPath(
                limit(request.getRequestURI(), 500)
        );

        // Remote address is used directly.
        // Untrusted X-Forwarded-For headers are ignored.
        entity.setIpAddress(
                limit(request.getRemoteAddr(), 45)
        );

        entity.setUserAgent(
                limit(request.getHeader("User-Agent"), 500)
        );
    }

    // ==========================================
    // SUPER ADMIN: RECENT ACTIVITY
    // ==========================================

    @Transactional(readOnly = true)
    public List<AuditLogProfile> getRecentActivity() {

        requireSuperAdmin();

        return auditRepository
                .findTop50ByOrderByCreatedAtDesc()
                .stream()
                .map(this::toProfile)
                .toList();
    }

    // ==========================================
    // SUPER ADMIN: ALL AUDIT HISTORY
    // ==========================================

    @Transactional(readOnly = true)
    public Page<AuditLogProfile> getAuditHistory(
            Pageable pageable
    ) {
        requireSuperAdmin();

        return auditRepository
                .findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toProfile);
    }

    // ==========================================
    // SUPER ADMIN: SINGLE AUDIT RECORD
    // ==========================================

    @Transactional(readOnly = true)
    public AuditLogProfile getAuditById(
            Long auditId
    ) {
        requireSuperAdmin();

        validateId(auditId);

        return toProfile(
                auditRepository.findById(auditId)
                        .orElseThrow(() ->
                                notFound(
                                        "Audit record not found"
                                )
                        )
        );
    }

    // ==========================================
    // SUPER ADMIN: HISTORY BY ACCOUNT
    // ==========================================

    @Transactional(readOnly = true)
    public List<AuditLogProfile> getByActor(
            Long accountId
    ) {
        requireSuperAdmin();
        validateId(accountId);

        return auditRepository
                .findByActorIdOrderByCreatedAtDesc(accountId)
                .stream()
                .map(this::toProfile)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<AuditLogProfile> getByActor(
            Long accountId,
            Pageable pageable
    ) {
        requireSuperAdmin();
        validateId(accountId);

        return auditRepository
                .findByActorIdOrderByCreatedAtDesc(
                        accountId,
                        pageable
                )
                .map(this::toProfile);
    }

    // ==========================================
    // SUPER ADMIN: HISTORY BY USERNAME
    // ==========================================

    @Transactional(readOnly = true)
    public List<AuditLogProfile> getByUsername(
            String username
    ) {
        requireSuperAdmin();

        if (isBlank(username)) {
            throw badRequest("Username is required");
        }

        return auditRepository
                .findByActorUsernameIgnoreCaseOrderByCreatedAtDesc(
                        username.trim()
                )
                .stream()
                .map(this::toProfile)
                .toList();
    }

    // ==========================================
    // SUPER ADMIN: HISTORY BY ROLE
    // ==========================================

    @Transactional(readOnly = true)
    public List<AuditLogProfile> getByRole(
            String role
    ) {
        requireSuperAdmin();

        if (isBlank(role)) {
            throw badRequest("Role is required");
        }

        return auditRepository
                .findByActorRoleIgnoreCaseOrderByCreatedAtDesc(
                        normalize(role)
                )
                .stream()
                .map(this::toProfile)
                .toList();
    }

    // ==========================================
    // SUPER ADMIN: HISTORY BY ACTION
    // ==========================================

    @Transactional(readOnly = true)
    public List<AuditLogProfile> getByAction(
            String action
    ) {
        requireSuperAdmin();

        String validatedAction = validateAction(action);

        return auditRepository
                .findByActionIgnoreCaseOrderByCreatedAtDesc(
                        validatedAction
                )
                .stream()
                .map(this::toProfile)
                .toList();
    }

    // ==========================================
    // SUPER ADMIN: HISTORY BY ENTITY TYPE
    // ==========================================

    @Transactional(readOnly = true)
    public List<AuditLogProfile> getByEntityType(
            String entityType
    ) {
        requireSuperAdmin();

        String validatedType =
                validateEntityType(entityType);

        return auditRepository
                .findByEntityTypeIgnoreCaseOrderByCreatedAtDesc(
                        validatedType
                )
                .stream()
                .map(this::toProfile)
                .toList();
    }

    // ==========================================
    // SUPER ADMIN: HISTORY FOR A RECORD
    // ==========================================

    @Transactional(readOnly = true)
    public List<AuditLogProfile> getEntityHistory(
            String entityType,
            String entityId
    ) {
        requireSuperAdmin();

        if (isBlank(entityId)) {
            throw badRequest("Entity ID is required");
        }

        return auditRepository
                .findEntityAuditHistory(
                        validateEntityType(entityType),
                        entityId.trim()
                )
                .stream()
                .map(this::toProfile)
                .toList();
    }

    // ==========================================
    // SUPER ADMIN: HISTORY BY RESULT
    // ==========================================

    @Transactional(readOnly = true)
    public List<AuditLogProfile> getByResult(
            String result
    ) {
        requireSuperAdmin();

        String validatedResult = validateResult(result);

        return auditRepository
                .findByResultIgnoreCaseOrderByCreatedAtDesc(
                        validatedResult
                )
                .stream()
                .map(this::toProfile)
                .toList();
    }

    // ==========================================
    // SUPER ADMIN: HISTORY BY DATE RANGE
    // ==========================================

    @Transactional(readOnly = true)
    public List<AuditLogProfile> getByDateRange(
            LocalDateTime start,
            LocalDateTime end
    ) {
        requireSuperAdmin();
        validateDateRange(start, end);

        return auditRepository
                .findByCreatedAtBetweenOrderByCreatedAtDesc(
                        start,
                        end
                )
                .stream()
                .map(this::toProfile)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<AuditLogProfile> getByDateRange(
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable
    ) {
        requireSuperAdmin();
        validateDateRange(start, end);

        return auditRepository
                .findByCreatedAtBetween(
                        start,
                        end,
                        pageable
                )
                .map(this::toProfile);
    }

    // ==========================================
    // SUPER ADMIN: CORRELATION ID
    // ==========================================

    @Transactional(readOnly = true)
    public List<AuditLogProfile> getByCorrelationId(
            String correlationId
    ) {
        requireSuperAdmin();

        if (isBlank(correlationId)) {
            throw badRequest(
                    "Correlation ID is required"
            );
        }

        return auditRepository
                .findByCorrelationIdOrderByCreatedAtAsc(
                        correlationId.trim()
                )
                .stream()
                .map(this::toProfile)
                .toList();
    }

    // ==========================================
    // SUPER ADMIN: FAILED ACTIONS
    // ==========================================

    @Transactional(readOnly = true)
    public List<AuditLogProfile> getFailedActionsByActor(
            Long accountId
    ) {
        requireSuperAdmin();
        validateId(accountId);

        return auditRepository
                .findUnsuccessfulActionsByActor(accountId)
                .stream()
                .map(this::toProfile)
                .toList();
    }

    // ==========================================
    // SUPER ADMIN: AUDIT STATISTICS
    // ==========================================

    @Transactional(readOnly = true)
    public AuditStatistics getStatistics() {

        requireSuperAdmin();

        LocalDateTime since =
                LocalDateTime.now().minusHours(24);

        return new AuditStatistics(
                auditRepository.count(),
                auditRepository.countByResultIgnoreCase(
                        "SUCCESS"
                ),
                auditRepository.countByResultIgnoreCase(
                        "FAILURE"
                ),
                auditRepository.countByResultIgnoreCase(
                        "DENIED"
                ),
                auditRepository.countByCreatedAtBetween(
                        since,
                        LocalDateTime.now()
                ),
                auditRepository
                        .countRecentUnsuccessfulActions(since),
                mapGroupCounts(
                        auditRepository.countActionsGrouped()
                ),
                mapGroupCounts(
                        auditRepository.countEntitiesGrouped()
                ),
                mapGroupCounts(
                        auditRepository.countResultsGrouped()
                ),
                mapGroupCounts(
                        auditRepository.countActionsByUsername()
                )
        );
    }

    // ==========================================
    // GROUPED STATISTICS MAPPING
    // ==========================================

    private List<AuditGroupCount> mapGroupCounts(
            List<Object[]> rows
    ) {
        return rows.stream()
                .map(row -> new AuditGroupCount(
                        row[0] == null
                                ? "UNKNOWN"
                                : row[0].toString(),
                        row[1] instanceof Number number
                                ? number.longValue()
                                : 0L
                ))
                .toList();
    }

    // ==========================================
    // RESPONSE MAPPING
    // ==========================================

    private AuditLogProfile toProfile(
            ApiAuditLog entity
    ) {
        return new AuditLogProfile(
                entity.getId(),
                entity.getActor() == null
                        ? null
                        : entity.getActor().getId(),
                entity.getActorUsername(),
                entity.getActorRole(),
                entity.getAction(),
                entity.getEntityType(),
                entity.getEntityId(),
                entity.getResult(),
                entity.getHttpStatus(),
                entity.getDescription(),
                entity.getOldStatus(),
                entity.getNewStatus(),
                entity.getHttpMethod(),
                entity.getRequestPath(),
                entity.getIpAddress(),
                entity.getUserAgent(),
                entity.getCorrelationId(),
                entity.getCreatedAt()
        );
    }

    // ==========================================
    // SUPER ADMIN AUTHORIZATION
    // ==========================================

    private void requireSuperAdmin() {
        currentUserService.requireSuperAdmin();
    }

    // ==========================================
    // VALIDATION
    // ==========================================

    private void validateEvent(
            AuditEvent event
    ) {
        validateAction(event.action());
        validateEntityType(event.entityType());
        validateResult(event.result());

        if (event.httpStatus() != null
                && (event.httpStatus() < 100
                || event.httpStatus() > 599)) {
            throw badRequest(
                    "HTTP status must be between 100 and 599"
            );
        }
    }

    private String validateAction(
            String action
    ) {
        String value = normalize(action);

        if (!ACTIONS.contains(value)) {
            throw badRequest("Invalid audit action");
        }

        return value;
    }

    private String validateEntityType(
            String entityType
    ) {
        String value = normalize(entityType);

        if (!ENTITY_TYPES.contains(value)) {
            throw badRequest("Invalid audit entity type");
        }

        return value;
    }

    private String validateResult(
            String result
    ) {
        String value = normalize(result);

        if (!RESULTS.contains(value)) {
            throw badRequest("Invalid audit result");
        }

        return value;
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw badRequest("Valid ID is required");
        }
    }

    private void validateDateRange(
            LocalDateTime start,
            LocalDateTime end
    ) {
        if (start == null
                || end == null
                || start.isAfter(end)) {
            throw badRequest("Invalid date range");
        }

        if (start.isBefore(end.minusYears(5))) {
            throw badRequest(
                    "Date range cannot exceed five years"
            );
        }
    }

    // ==========================================
    // STRING HELPERS
    // ==========================================

    private String normalize(String value) {
        return value == null
                ? ""
                : value.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }

    private String limit(
            String value,
            int maxLength
    ) {
        if (value == null) {
            return null;
        }

        return value.length() <= maxLength
                ? value
                : value.substring(0, maxLength);
    }

    // ==========================================
    // HTTP ERROR HELPERS
    // ==========================================

    private ResponseStatusException badRequest(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
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
