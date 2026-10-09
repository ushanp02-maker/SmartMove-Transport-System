
package com.smartmove.backend.controller;

import com.smartmove.backend.service.CurrentUserService;
import com.smartmove.backend.service.OnDemandTripRequestService;

import com.smartmove.backend.service.OnDemandTripRequestService.CreateOnDemandRequest;
import com.smartmove.backend.service.OnDemandTripRequestService.UpdateOnDemandRequest;
import com.smartmove.backend.service.OnDemandTripRequestService.ReviewRequest;
import com.smartmove.backend.service.OnDemandTripRequestService.AssignTripRequest;
import com.smartmove.backend.service.OnDemandTripRequestService.CancelRequest;
import com.smartmove.backend.service.OnDemandTripRequestService.OnDemandRequestProfile;
import com.smartmove.backend.service.OnDemandTripRequestService.OnDemandStatistics;
import com.smartmove.backend.service.OnDemandTripRequestService.DistanceEstimate;

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
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/on-demand")
@Validated
public class OnDemandTripRequestController {

    private final OnDemandTripRequestService requestService;
    private final CurrentUserService currentUserService;

    public OnDemandTripRequestController(
            OnDemandTripRequestService requestService,
            CurrentUserService currentUserService
    ) {
        this.requestService = requestService;
        this.currentUserService = currentUserService;
    }

    // ==========================================
    // PUBLIC: STRAIGHT-LINE DISTANCE ESTIMATE
    // ==========================================

    @GetMapping("/estimate-distance")
    public ResponseEntity<DistanceEstimate> estimateDistance(
            @RequestParam @NotNull
            @DecimalMin("-90.0") @DecimalMax("90.0")
            Double pickupLatitude,

            @RequestParam @NotNull
            @DecimalMin("-180.0") @DecimalMax("180.0")
            Double pickupLongitude,

            @RequestParam @NotNull
            @DecimalMin("-90.0") @DecimalMax("90.0")
            Double destinationLatitude,

            @RequestParam @NotNull
            @DecimalMin("-180.0") @DecimalMax("180.0")
            Double destinationLongitude
    ) {
        return ResponseEntity.ok(
                requestService.estimateDistance(
                        pickupLatitude,
                        pickupLongitude,
                        destinationLatitude,
                        destinationLongitude
                )
        );
    }

    // ==========================================
    // PASSENGER: CREATE REQUEST
    // ==========================================

    @PostMapping("/requests")
    public ResponseEntity<OnDemandRequestProfile> createRequest(
            @Valid @RequestBody OnDemandRequestPayload request
    ) {
        currentUserService.requirePassenger();

        OnDemandRequestProfile created =
                requestService.createRequest(
                        toCreateRequest(request)
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(created);
    }

    // ==========================================
    // PASSENGER: UPDATE OWN PENDING REQUEST
    // ==========================================

    @PutMapping("/requests/{requestId}")
    public ResponseEntity<OnDemandRequestProfile> updateRequest(
            @PathVariable @Positive Long requestId,
            @Valid @RequestBody OnDemandRequestPayload request
    ) {
        currentUserService.requirePassenger();

        return ResponseEntity.ok(
                requestService.updateRequest(
                        requestId,
                        toUpdateRequest(request)
                )
        );
    }

    // ==========================================
    // PASSENGER: CANCEL OWN REQUEST
    // ==========================================

    @PatchMapping("/requests/{requestId}/cancel")
    public ResponseEntity<OnDemandRequestProfile> cancelMyRequest(
            @PathVariable @Positive Long requestId,
            @RequestBody(required = false) CancelPayload request
    ) {
        currentUserService.requirePassenger();

        return ResponseEntity.ok(
                requestService.cancelMyRequest(
                        requestId,
                        new CancelRequest(
                                request == null
                                        ? null
                                        : request.reason()
                        )
                )
        );
    }

    // ==========================================
    // PASSENGER: MY REQUEST HISTORY
    // ==========================================

    @GetMapping("/my-requests")
    public ResponseEntity<List<OnDemandRequestProfile>>
    getMyRequests() {
        currentUserService.requirePassenger();

        return ResponseEntity.ok(
                requestService.getMyRequests()
        );
    }

    // ==========================================
    // PASSENGER: PAGINATED REQUEST HISTORY
    // ==========================================

    @GetMapping("/my-requests/page")
    public ResponseEntity<Page<OnDemandRequestProfile>>
    getMyRequestsPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        currentUserService.requirePassenger();

        return ResponseEntity.ok(
                requestService.getMyRequestsPage(
                        createPageable(page, size)
                )
        );
    }

    // ==========================================
    // PASSENGER: MY REQUESTS BY STATUS
    // ==========================================

    @GetMapping("/my-requests/status/{status}")
    public ResponseEntity<List<OnDemandRequestProfile>>
    getMyRequestsByStatus(
            @PathVariable String status
    ) {
        currentUserService.requirePassenger();

        return ResponseEntity.ok(
                requestService.getMyRequestsByStatus(status)
        );
    }

    // ==========================================
    // OWNER / ADMIN: REQUEST DETAILS
    // ==========================================

    @GetMapping("/requests/{requestId}")
    public ResponseEntity<OnDemandRequestProfile>
    getRequestById(
            @PathVariable @Positive Long requestId
    ) {
        return ResponseEntity.ok(
                requestService.getRequestById(requestId)
        );
    }

    // ==========================================
    // ADMIN: ALL REQUESTS
    // ==========================================

    @GetMapping("/admin/requests")
    public ResponseEntity<List<OnDemandRequestProfile>>
    getAllRequests() {
        currentUserService.requireAdmin();

        return ResponseEntity.ok(
                requestService.getAllRequests()
        );
    }

    // ==========================================
    // ADMIN: PAGINATED REQUESTS
    // ==========================================

    @GetMapping("/admin/requests/page")
    public ResponseEntity<Page<OnDemandRequestProfile>>
    getRequestsPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        currentUserService.requireAdmin();

        return ResponseEntity.ok(
                requestService.getRequestsPage(
                        createPageable(page, size)
                )
        );
    }

    // ==========================================
    // ADMIN: PENDING APPROVAL QUEUE
    // ==========================================

    @GetMapping("/admin/requests/pending")
    public ResponseEntity<List<OnDemandRequestProfile>>
    getPendingRequests() {
        currentUserService.requireAdmin();

        return ResponseEntity.ok(
                requestService.getPendingRequests()
        );
    }

    // ==========================================
    // ADMIN: APPROVED BUT UNASSIGNED
    // ==========================================

    @GetMapping("/admin/requests/unassigned")
    public ResponseEntity<List<OnDemandRequestProfile>>
    getApprovedUnassignedRequests() {
        currentUserService.requireAdmin();

        return ResponseEntity.ok(
                requestService.getApprovedUnassignedRequests()
        );
    }

    // ==========================================
    // ADMIN: REQUESTS BY STATUS
    // ==========================================

    @GetMapping("/admin/requests/status/{status}")
    public ResponseEntity<List<OnDemandRequestProfile>>
    getByStatus(
            @PathVariable String status
    ) {
        currentUserService.requireAdmin();

        return ResponseEntity.ok(
                requestService.getByStatus(status)
        );
    }

    // ==========================================
    // ADMIN: REQUESTS BY SERVICE TYPE
    // ==========================================

    @GetMapping("/admin/requests/service/{serviceType}")
    public ResponseEntity<List<OnDemandRequestProfile>>
    getByServiceType(
            @PathVariable String serviceType
    ) {
        currentUserService.requireAdmin();

        return ResponseEntity.ok(
                requestService.getByServiceType(serviceType)
        );
    }

    // ==========================================
    // ADMIN: SEARCH PICKUP LOCATIONS
    // ==========================================

    @GetMapping("/admin/requests/search/pickup")
    public ResponseEntity<List<OnDemandRequestProfile>>
    searchByPickup(
            @RequestParam String query
    ) {
        currentUserService.requireAdmin();

        return ResponseEntity.ok(
                requestService.searchByPickup(query)
        );
    }

    // ==========================================
    // ADMIN: SEARCH DESTINATIONS
    // ==========================================

    @GetMapping("/admin/requests/search/destination")
    public ResponseEntity<List<OnDemandRequestProfile>>
    searchByDestination(
            @RequestParam String query
    ) {
        currentUserService.requireAdmin();

        return ResponseEntity.ok(
                requestService.searchByDestination(query)
        );
    }

    // ==========================================
    // ADMIN: REQUESTS BY PICKUP DATE RANGE
    // ==========================================

    @GetMapping("/admin/requests/between")
    public ResponseEntity<List<OnDemandRequestProfile>>
    getByPickupPeriod(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime start,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime end
    ) {
        currentUserService.requireAdmin();

        return ResponseEntity.ok(
                requestService.getByPickupPeriod(
                        start,
                        end
                )
        );
    }

    // ==========================================
    // ADMIN: REQUESTS ASSIGNED TO TRIP
    // ==========================================

    @GetMapping("/admin/trips/{tripId}/requests")
    public ResponseEntity<List<OnDemandRequestProfile>>
    getByTrip(
            @PathVariable @Positive Long tripId
    ) {
        currentUserService.requireAdmin();

        return ResponseEntity.ok(
                requestService.getByTrip(tripId)
        );
    }

    // ==========================================
    // ADMIN: APPROVE REQUEST
    // ==========================================

    @PatchMapping("/admin/requests/{requestId}/approve")
    public ResponseEntity<OnDemandRequestProfile>
    approveRequest(
            @PathVariable @Positive Long requestId,
            @Valid @RequestBody ApprovalPayload request
    ) {
        currentUserService.requireAdmin();

        return ResponseEntity.ok(
                requestService.approveRequest(
                        requestId,
                        new ReviewRequest(
                                request.reviewNotes(),
                                request.approvedFare()
                        )
                )
        );
    }

    // ==========================================
    // ADMIN: REJECT REQUEST
    // ==========================================

    @PatchMapping("/admin/requests/{requestId}/reject")
    public ResponseEntity<OnDemandRequestProfile>
    rejectRequest(
            @PathVariable @Positive Long requestId,
            @Valid @RequestBody RejectionPayload request
    ) {
        currentUserService.requireAdmin();

        return ResponseEntity.ok(
                requestService.rejectRequest(
                        requestId,
                        new ReviewRequest(
                                request.reviewNotes(),
                                null
                        )
                )
        );
    }

    // ==========================================
    // ADMIN: ASSIGN TRIP
    // ==========================================

    @PatchMapping("/admin/requests/{requestId}/assign")
    public ResponseEntity<OnDemandRequestProfile>
    assignTrip(
            @PathVariable @Positive Long requestId,
            @Valid @RequestBody AssignmentPayload request
    ) {
        currentUserService.requireAdmin();

        return ResponseEntity.ok(
                requestService.assignTrip(
                        requestId,
                        new AssignTripRequest(
                                request.tripId()
                        )
                )
        );
    }

    // ==========================================
    // ADMIN: COMPLETE REQUEST
    // ==========================================

    @PatchMapping("/admin/requests/{requestId}/complete")
    public ResponseEntity<OnDemandRequestProfile>
    completeRequest(
            @PathVariable @Positive Long requestId
    ) {
        currentUserService.requireAdmin();

        return ResponseEntity.ok(
                requestService.completeRequest(requestId)
        );
    }

    // ==========================================
    // ADMIN: CANCEL REQUEST
    // ==========================================

    @PatchMapping("/admin/requests/{requestId}/cancel")
    public ResponseEntity<OnDemandRequestProfile>
    cancelByAdmin(
            @PathVariable @Positive Long requestId,
            @Valid @RequestBody CancelPayload request
    ) {
        currentUserService.requireAdmin();

        return ResponseEntity.ok(
                requestService.cancelByAdmin(
                        requestId,
                        new CancelRequest(request.reason())
                )
        );
    }

    // ==========================================
    // ADMIN: REQUEST STATISTICS
    // ==========================================

    @GetMapping("/admin/stats")
    public ResponseEntity<OnDemandStatistics>
    getStatistics() {
        currentUserService.requireAdmin();

        return ResponseEntity.ok(
                requestService.getStatistics()
        );
    }

    // ==========================================
    // REQUEST BODY: MAP-BASED TRIP REQUEST
    // ==========================================

    public record OnDemandRequestPayload(

            @NotBlank
            @Size(max = 250)
            String pickupAddress,

            @NotNull
            @DecimalMin("-90.0")
            @DecimalMax("90.0")
            Double pickupLatitude,

            @NotNull
            @DecimalMin("-180.0")
            @DecimalMax("180.0")
            Double pickupLongitude,

            @NotBlank
            @Size(max = 250)
            String destinationAddress,

            @NotNull
            @DecimalMin("-90.0")
            @DecimalMax("90.0")
            Double destinationLatitude,

            @NotNull
            @DecimalMin("-180.0")
            @DecimalMax("180.0")
            Double destinationLongitude,

            @NotNull
            @Future
            LocalDateTime requestedPickupTime,

            @NotNull
            @Min(1)
            @Max(1000)
            Integer passengerCount,

            @Pattern(
                    regexp = "(?i)STANDARD|PREMIUM|STAFF|OTHER"
            )
            String serviceType,

            @Size(max = 2000)
            String specialRequirements

    ) {}

    // ==========================================
    // REQUEST BODY: ADMIN APPROVAL
    // ==========================================

    public record ApprovalPayload(

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
    // REQUEST BODY: CANCELLATION
    // ==========================================

    public record CancelPayload(

            @NotBlank
            @Size(max = 1000)
            String reason

    ) {}

    // ==========================================
    // DTO CONVERSION
    // ==========================================

    private CreateOnDemandRequest toCreateRequest(
            OnDemandRequestPayload request
    ) {
        return new CreateOnDemandRequest(
                request.pickupAddress(),
                request.pickupLatitude(),
                request.pickupLongitude(),
                request.destinationAddress(),
                request.destinationLatitude(),
                request.destinationLongitude(),
                request.requestedPickupTime(),
                request.passengerCount(),
                request.serviceType(),
                request.specialRequirements()
        );
    }

    private UpdateOnDemandRequest toUpdateRequest(
            OnDemandRequestPayload request
    ) {
        return new UpdateOnDemandRequest(
                request.pickupAddress(),
                request.pickupLatitude(),
                request.pickupLongitude(),
                request.destinationAddress(),
                request.destinationLatitude(),
                request.destinationLongitude(),
                request.requestedPickupTime(),
                request.passengerCount(),
                request.serviceType(),
                request.specialRequirements()
        );
    }

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
