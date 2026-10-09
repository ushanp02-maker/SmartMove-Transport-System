
package com.smartmove.backend.config;

import com.smartmove.backend.service.ApiAuditLogService;
import com.smartmove.backend.service.ApiAuditLogService.AuditEvent;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.aop.support.AopUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Set;

@Aspect
@Component
public class AuditLoggingAspect {

    private static final Logger logger =
            LoggerFactory.getLogger(AuditLoggingAspect.class);

    private static final Set<String> AUDITED_SERVICES = Set.of(
            "AdminService",
            "PassengerService",
            "DriverService",
            "VehicleService",
            "RouteService",
            "TripService",
            "BookingService",
            "PaymentService",
            "MaintenanceService",
            "FeedbackService",
            "AnnouncementService",
            "VehicleDocumentService",
            "TripStatusService",
            "StaffTransportRequestService",
            "OnDemandTripRequestService"
    );

    private final ApiAuditLogService auditLogService;

    public AuditLoggingAspect(
            ApiAuditLogService auditLogService
    ) {
        this.auditLogService = auditLogService;
    }

    // ==========================================
    // INTERCEPT BUSINESS SERVICE METHODS
    // ==========================================

    @Around(
            "execution(public * com.smartmove.backend.service.*Service.*(..))"
    )
    public Object auditBusinessOperation(
            ProceedingJoinPoint joinPoint
    ) throws Throwable {

        String serviceName =
                joinPoint.getTarget()
                        .getClass()
                        .getSimpleName();

        if (!AUDITED_SERVICES.contains(serviceName)) {
            return joinPoint.proceed();
        }

        MethodSignature signature =
                (MethodSignature) joinPoint.getSignature();

        String methodName = signature.getName();

        String action = determineAction(methodName);

        if (action == null) {
            return joinPoint.proceed();
        }

        String entityType = determineEntityType(serviceName);

        String entityId = extractEntityId(
                methodName,
                joinPoint.getArgs()
        );

        try {
            Object result = joinPoint.proceed();

            AuditEvent event = new AuditEvent(
                    action,
                    entityType,
                    entityId,
                    "SUCCESS",
                    200,
                    serviceName + "." + methodName
                            + " completed successfully",
                    null,
                    null,
                    null
            );

            recordSuccessSafely(event);

            return result;

        } catch (Throwable exception) {

            int status = determineHttpStatus(exception);

            String result = status == 401 || status == 403
                    ? "DENIED"
                    : "FAILURE";

            AuditEvent event = new AuditEvent(
                    action,
                    entityType,
                    entityId,
                    result,
                    status,
                    serviceName + "." + methodName
                            + " failed",
                    null,
                    null,
                    null
            );

            recordFailureSafely(event);

            throw exception;
        }
    }

    // ==========================================
    // DETERMINE ACTION FROM METHOD NAME
    // ==========================================

    private String determineAction(
            String methodName
    ) {
        String name =
                methodName.toLowerCase(Locale.ROOT);

        if (startsWithAny(
                name,
                "create",
                "register",
                "add",
                "submit"
        )) {
            return "CREATE";
        }

        if (startsWithAny(
                name,
                "update",
                "edit",
                "change",
                "modify",
                "set"
        )) {
            return "UPDATE";
        }

        if (startsWithAny(
                name,
                "delete",
                "remove"
        )) {
            return "DELETE";
        }

        if (name.startsWith("approve")) {
            return "APPROVE";
        }

        if (name.startsWith("reject")) {
            return "REJECT";
        }

        if (startsWithAny(
                name,
                "assign",
                "allocate"
        )) {
            return "ASSIGN";
        }

        if (name.startsWith("cancel")) {
            return "CANCEL";
        }

        if (name.startsWith("refund")) {
            return "REFUND";
        }

        if (name.startsWith("verify")) {
            return "VERIFY";
        }

        if (name.startsWith("publish")) {
            return "PUBLISH";
        }

        if (name.startsWith("start")) {
            return "START";
        }

        if (name.startsWith("pause")) {
            return "PAUSE";
        }

        if (name.startsWith("resume")) {
            return "RESUME";
        }

        if (name.startsWith("complete")) {
            return "COMPLETE";
        }

        if (name.startsWith("resetpassword")
                || name.startsWith("resetaccountpassword")) {
            return "PASSWORD_RESET";
        }

        if (startsWithAny(
                name,
                "activate",
                "deactivate",
                "suspend",
                "enable",
                "disable",
                "mark"
        )) {
            return "STATUS_CHANGE";
        }

        return null;
    }

    // ==========================================
    // DETERMINE ENTITY TYPE
    // ==========================================

    private String determineEntityType(
            String serviceName
    ) {
        return switch (serviceName) {
            case "AdminService" -> "USER_ACCOUNT";
            case "PassengerService" -> "PASSENGER";
            case "DriverService" -> "DRIVER";
            case "VehicleService" -> "VEHICLE";
            case "RouteService" -> "ROUTE";
            case "TripService", "TripStatusService" -> "TRIP";
            case "BookingService" -> "BOOKING";
            case "PaymentService" -> "PAYMENT";
            case "MaintenanceService" -> "MAINTENANCE";
            case "FeedbackService" -> "FEEDBACK";
            case "AnnouncementService" -> "ANNOUNCEMENT";
            case "VehicleDocumentService" -> "VEHICLE_DOCUMENT";
            case "StaffTransportRequestService" ->
                    "STAFF_TRANSPORT_REQUEST";
            case "OnDemandTripRequestService" ->
                    "ON_DEMAND_TRIP_REQUEST";
            default -> "OTHER";
        };
    }

    // ==========================================
    // EXTRACT RECORD ID SAFELY
    // ==========================================

    private String extractEntityId(
            String methodName,
            Object[] arguments
    ) {
        if (arguments == null
                || arguments.length == 0) {
            return null;
        }

        // Only extract a simple numeric ID when
        // the first argument is a Long.
        // Never serialize request DTOs, passwords,
        // tokens or complete method arguments.

        Object firstArgument = arguments[0];

        if (firstArgument instanceof Long id
                && id > 0) {
            return id.toString();
        }

        return null;
    }

    // ==========================================
    // HTTP STATUS FOR FAILED OPERATIONS
    // ==========================================

    private int determineHttpStatus(
            Throwable exception
    ) {
        if (exception instanceof
                ResponseStatusException responseException) {
            return responseException
                    .getStatusCode()
                    .value();
        }

        if (exception instanceof
                org.springframework.security.access
                        .AccessDeniedException) {
            return 403;
        }

        if (exception instanceof
                org.springframework.security.core
                        .AuthenticationException) {
            return 401;
        }

        if (exception instanceof
                IllegalArgumentException) {
            return 400;
        }

        return 500;
    }

    // ==========================================
    // RECORD SUCCESS AFTER TRANSACTION COMMIT
    // ==========================================

    private void recordSuccessSafely(
            AuditEvent event
    ) {
        if (TransactionSynchronizationManager
                .isSynchronizationActive()
                && TransactionSynchronizationManager
                .isActualTransactionActive()) {

            TransactionSynchronizationManager
                    .registerSynchronization(
                            new TransactionSynchronization() {

                                @Override
                                public void afterCommit() {
                                    saveSuccess(event);
                                }
                            }
                    );

        } else {
            saveSuccess(event);
        }
    }

    private void saveSuccess(
            AuditEvent event
    ) {
        try {
            auditLogService.recordSuccess(event);

        } catch (Exception exception) {
            logger.error(
                    "Could not persist successful audit event: {}",
                    event.action(),
                    exception
            );
        }
    }

    // ==========================================
    // RECORD FAILURE IN SEPARATE TRANSACTION
    // ==========================================

    private void recordFailureSafely(
            AuditEvent event
    ) {
        try {
            auditLogService.recordFailure(event);

        } catch (Exception exception) {
            logger.error(
                    "Could not persist failed audit event: {}",
                    event.action(),
                    exception
            );
        }
    }

    // ==========================================
    // STRING HELPER
    // ==========================================

    private boolean startsWithAny(
            String value,
            String... prefixes
    ) {
        for (String prefix : prefixes) {
            if (value.startsWith(prefix)) {
                return true;
            }
        }

        return false;
    }
}
