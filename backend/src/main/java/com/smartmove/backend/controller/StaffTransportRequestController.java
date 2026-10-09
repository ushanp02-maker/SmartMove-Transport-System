
package com.smartmove.backend.controller;

import com.smartmove.backend.service.CurrentUserService;
import com.smartmove.backend.service.StaffTransportRequestService;

import com.smartmove.backend.service.StaffTransportRequestService.CreateStaffRequest;
import com.smartmove.backend.service.StaffTransportRequestService.UpdateStaffRequest;
import com.smartmove.backend.service.StaffTransportRequestService.ReviewRequest;
import com.smartmove.backend.service.StaffTransportRequestService.AssignTripRequest;
import com.smartmove.backend.service.StaffTransportRequestService.StaffRequestProfile;
import com.smartmove.backend.service.StaffTransportRequestService.StaffRequestStatistics;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/staff-transport")
@Validated
public class StaffTransportRequestController {

    private final StaffTransportRequestService staffService;
    private final CurrentUserService currentUserService;

    public StaffTransportRequestController(
            StaffTransportRequestService staffService,
            CurrentUserService currentUserService
    ) {
        this.staffService = staffService;
        this.currentUserService = currentUserService;
    }

    // ==========================================
    // PASSENGER: CREATE STAFF TRANSPORT REQUEST
    // ==========================================

    @PostMapping("/requests")
    public ResponseEntity<StaffRequestProfile> createRequest(
            @Valid @RequestBody StaffRequestPayload request
    ) {
        currentUserService.requirePassenger();

        StaffRequestProfile created = staffService.createRequest(
                new CreateStaffRequest(
                        request.organizationName(),
                        request.contactPerson(),
                        request.contactPhone(),
                        request.contactEmail(),
                        request.origin(),
                        request.destination(),
                        request.originLatitude(),
                        request.originLongitude(),
                        request.destinationLatitude(),
                        request.destinationLongitude(),
                        request.requestedDate(),
                        request.pickupTime(),
                        request.returnTime(),
                        request.passengerCount(),
                        request.journeyType(),
                        request.frequency(),
                        request.serviceEndDate(),
                        request.specialRequirements()
                )
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(created);
    }

    // ==========================================
    // PASSENGER: UPDATE OWN PENDING REQUEST
    // ==========================================

    @PutMapping("/requests/{requestId}")
    public ResponseEntity<StaffRequestProfile> updateRequest(
            @PathVariable @Positive Long requestId,
            @Valid @RequestBody StaffRequestPayload request
    ) {
        currentUserService.requirePassenger();

        return ResponseEntity.ok(
                staffService.updateRequest(
                        requestId,
                        new UpdateStaffRequest(
                                request.organizationName(),
                                request.contactPerson(),
                                request.contactPhone(),
                                request.contactEmail(),
                                request.origin(),
                                request.destination(),
                                request.originLatitude(),
                                request.originLongitude(),
                                request.destinationLatitude(),
                                request.destinationLongitude(),
                                request.requestedDate(),
                                request.pickupTime(),
                                request.returnTime(),
                                request.passengerCount(),
                                request.journeyType(),
                                request.frequency(),
                                request.serviceEndDate(),
                                request.specialRequirements()
                        )
                )
        );
    }

    // ==========================================
    // PASSENGER: CANCEL OWN REQUEST
    // ==========================================

    @PatchMapping("/requests/{requestId}/cancel")
    public ResponseEntity<StaffRequestProfile> cancelMyRequest(
            @PathVariable @Positive Long requestId
    ) {
        currentUserService.requirePassenger();

        return ResponseEntity.ok(
                staffService.cancelMyRequest(requestId)
        );
    }

    // ==========================================
    // PASSENGER: MY REQUESTS
    // ==========================================

    @GetMapping("/my-requests")
    public ResponseEntity<List<StaffRequestProfile>> getMyRequests() {

        currentUserService.requirePassenger();

        return ResponseEntity.ok(
                staffService.getMyRequests()
        );
    }

    // ==========================================
    // PASSENGER: PAGINATED REQUEST HISTORY
    // ==========================================

    @GetMapping("/my-requests/page")
    public ResponseEntity<Page<StaffRequestProfile>> getMyRequestsPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        currentUserService.requirePassenger();

        return ResponseEntity.ok(
                staffService.getMyRequestsPage(
                        createPageable(page, size)
                )
        );
    }

    // ==========================================
    // OWNER / ADMIN: GET REQUEST BY ID
    // ==========================================

    @GetMapping("/requests/{requestId}")
    public ResponseEntity<StaffRequestProfile> getRequestById(
            @PathVariable @Positive Long requestId
    ) {
        return ResponseEntity.ok(
                staffService.getRequestById(requestId)
        );
    }

    // ==========================================
    // ADMIN: ALL REQUESTS
    // ==========================================

    @GetMapping("/admin/requests")
    public ResponseEntity<List<StaffRequestProfile>> getAllRequests() {

        currentUserService.requireAdmin();

        return ResponseEntity.ok(
                staffService.getAllRequests()
        );
    }

    // ==========================================
    // ADMIN: PAGINATED REQUESTS
    // ==========================================

    @GetMapping("/admin/requests/page")
    public ResponseEntity<Page<StaffRequestProfile>> getRequestsPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        currentUserService.requireAdmin();

        return ResponseEntity.ok(
                staffService.getRequestsPage(
                        createPageable(page, size)
                )
        );
    }

    // ==========================================
    // ADMIN: PENDING REQUESTS
    // ==========================================

    @GetMapping("/admin/requests/pending")
    public ResponseEntity<List<StaffRequestProfile>> getPendingRequests() {

        currentUserService.requireAdmin();

        return ResponseEntity.ok(
                staffService.getPendingRequests()
        );
    }

    // ==========================================
    // ADMIN: APPROVED, NOT YET ASSIGNED
    // ==========================================

    @GetMapping("/admin/requests/unassigned")
    public ResponseEntity<List<StaffRequestProfile>>
    getApprovedUnassignedRequests() {

        currentUserService.requireAdmin();

        return ResponseEntity.ok(
                staffService.getApprovedUnassignedRequests()
        );
    }

    // ==========================================
    // ADMIN: FILTER BY STATUS
    // ==========================================

    @GetMapping("/admin/requests/status/{status}")
    public ResponseEntity<List<StaffRequestProfile>> getByStatus(
            @PathVariable String status
    ) {
        currentUserService.requireAdmin();

        return ResponseEntity.ok(
                staffService.getRequestsByStatus(status)
        );
    }

    // ==========================================
    // ADMIN: SEARCH BY ORGANIZATION
    // ==========================================

    @GetMapping("/admin/requests/search")
    public ResponseEntity<List<StaffRequestProfile>> searchByOrganization(
            @RequestParam String organization
    ) {
        currentUserService.requireAdmin();

        return ResponseEntity.ok(
                staffService.searchByOrganization(organization)
        );
    }

    // ==========================================
    // ADMIN: FILTER BY REQUESTED DATE RANGE
    // ==========================================

    @GetMapping("/admin/requests/between")
    public ResponseEntity<List<StaffRequestProfile>> getByDateRange(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) {
        currentUserService.requireAdmin();

        return ResponseEntity.ok(
                staffService.getRequestsByDateRange(
                        startDate,
                        endDate
                )
        );
    }

    // ==========================================
    // ADMIN: REQUESTS ASSIGNED TO A TRIP
    // ==========================================

    @GetMapping("/admin/trips/{tripId}/requests")
    public ResponseEntity<List<StaffRequestProfile>> getByTrip(
            @PathVariable @Positive Long tripId
    ) {
        currentUserService.requireAdmin();

        return ResponseEntity.ok(
                staffService.getRequestsByTrip(tripId)
        );
    }

    // ==========================================
    // ADMIN: APPROVE REQUEST
    // ==========================================

    @PatchMapping("/admin/requests/{requestId}/approve")
    public ResponseEntity<StaffRequestProfile> approveRequest(
            @PathVariable @Positive Long requestId,
            @Valid @RequestBody ReviewPayload review
    ) {
        currentUserService.requireAdmin();

        return ResponseEntity.ok(
                staffService.approveRequest(
                        requestId,
                        new ReviewRequest(
                                review.reviewNotes(),
                                review.approvedFare()
                        )
                )
        );
    }

    // ==========================================
    // ADMIN: REJECT REQUEST
    // ==========================================

    @PatchMapping("/admin/requests/{requestId}/reject")
    public ResponseEntity<StaffRequestProfile> rejectRequest(
            @PathVariable @Positive Long requestId,
            @Valid @RequestBody RejectionPayload review
    ) {
        currentUserService.requireAdmin();

        return ResponseEntity.ok(
                staffService.rejectRequest(
                        requestId,
                        new ReviewRequest(
                                review.reviewNotes(),
                                null
                        )
                )
        );
    }

    // ==========================================
    // ADMIN: ASSIGN TRIP
    // ==========================================

    @PatchMapping("/admin/requests/{requestId}/assign")
    public ResponseEntity<StaffRequestProfile> assignTrip(
            @PathVariable @Positive Long requestId,
            @Valid @RequestBody AssignmentPayload request
    ) {
        currentUserService.requireAdmin();

        return ResponseEntity.ok(
                staffService.assignTrip(
                        requestId,
                        new AssignTripRequest(
                                request.tripId()
                        )
                )
        );
    }

    // ==========================================
    // ADMIN: MARK REQUEST COMPLETED
    // ==========================================

    @PatchMapping("/admin/requests/{requestId}/complete")
    public ResponseEntity<StaffRequestProfile> completeRequest(
            @PathVariable @Positive Long requestId
    ) {
        currentUserService.requireAdmin();

        return ResponseEntity.ok(
                staffService.completeRequest(requestId)
        );
    }

    // ==========================================
    // ADMIN: CANCEL REQUEST
    // ==========================================

    @PatchMapping("/admin/requests/{requestId}/cancel")
    public ResponseEntity<StaffRequestProfile> cancelByAdmin(
            @PathVariable @Positive Long requestId,
            @Valid @RequestBody CancellationPayload request
    ) {
        currentUserService.requireAdmin();

        return ResponseEntity.ok(
                staffService.cancelRequestByAdmin(
                        requestId,
                        request.reason()
                )
        );
    }

    // ==========================================
    // ADMIN: REQUEST STATISTICS
    // ==========================================

    @GetMapping("/admin/stats")
    public ResponseEntity<StaffRequestStatistics> getStatistics() {

        currentUserService.requireAdmin();

        return ResponseEntity.ok(
                staffService.getStatistics()
        );
    }

    // ==========================================
    // REQUEST BODY: STAFF TRANSPORT DETAILS
    // ==========================================

    public record StaffRequestPayload(

            @NotBlank
            @Size(max = 150)
            String organizationName,

            @Size(max = 120)
            String contactPerson,

            @Size(max = 30)
            String contactPhone,

            @Email
            @Size(max = 150)
            String contactEmail,

            @NotBlank
            @Size(max = 200)
            String origin,

            @NotBlank
            @Size(max = 200)
            String destination,

            @DecimalMin("-90.0")
            @DecimalMax("90.0")
            Double originLatitude,

            @DecimalMin("-180.0")
            @DecimalMax("180.0")
            Double originLongitude,

            @DecimalMin("-90.0")
            @DecimalMax("90.0")
            Double destinationLatitude,

            @DecimalMin("-180.0")
            @DecimalMax("180.0")
            Double destinationLongitude,

            @NotNull
            @FutureOrPresent
            LocalDate requestedDate,

            @NotNull
            LocalTime pickupTime,

            LocalTime returnTime,

            @NotNull
            @Min(1)
            @Max(1000)
            Integer passengerCount,

            @Pattern(
                    regexp = "(?i)ONE_WAY|ROUND_TRIP"
            )
            String journeyType,

            @Pattern(
                    regexp = "(?i)ONE_TIME|DAILY|WEEKDAYS|WEEKLY"
            )
            String frequency,

            LocalDate serviceEndDate,

            @Size(max = 2000)
            String specialRequirements

    ) {}

    // ==========================================
    // REQUEST BODY: ADMIN APPROVAL
    // ==========================================

    public record ReviewPayload(

            @Size(max = 2000)
            String reviewNotes,

            @DecimalMin("0.0")
            BigDecimal approvedFare

    ) {}

    // ==========================================
    // REQUEST BODY: ADMIN REJECTION
    // ==========================================

    public record RejectionPayload(

            @NotBlank
            @Size(max = 2000)
            String reviewNotes

    ) {}

    // ==========================================
    // REQUEST BODY: TRIP ASSIGNMENT
    // ==========================================

    public record AssignmentPayload(

            @NotNull
            @Positive
            Long tripId

    ) {}

    // ==========================================
    // REQUEST BODY: ADMIN CANCELLATION
    // ==========================================

    public record CancellationPayload(

            @NotBlank
            @Size(max = 2000)
            String reason

    ) {}

    // ==========================================
    // PAGINATION HELPER
    // ==========================================

    private Pageable createPageable(
            int page,
            int size
    ) {
        if (page < 0 || size < 1 || size > 200) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page must be nonnegative and size "
                            + "must be between 1 and 200"
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
