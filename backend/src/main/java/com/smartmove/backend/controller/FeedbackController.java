
package com.smartmove.backend.controller;

import com.smartmove.backend.service.FeedbackService;
import com.smartmove.backend.service.FeedbackService.FeedbackProfile;
import com.smartmove.backend.service.FeedbackService.PublicFeedback;
import com.smartmove.backend.service.FeedbackService.CreateFeedbackRequest;
import com.smartmove.backend.service.FeedbackService.UpdateFeedbackRequest;
import com.smartmove.backend.service.FeedbackService.ModerateFeedbackRequest;
import com.smartmove.backend.service.FeedbackService.AdminResponseRequest;
import com.smartmove.backend.service.FeedbackService.RatingSummary;
import com.smartmove.backend.service.FeedbackService.FeedbackStatistics;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/feedback")
@Validated
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(
            FeedbackService feedbackService
    ) {
        this.feedbackService = feedbackService;
    }

    // ==========================================
    // PUBLIC: APPROVED REVIEWS
    // ==========================================

    @GetMapping("/public")
    public ResponseEntity<Page<PublicFeedback>>
    getApprovedFeedback(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                feedbackService.getApprovedFeedback(
                        createPageable(page, size)
                )
        );
    }

    // ==========================================
    // PUBLIC: APPROVED DRIVER REVIEWS
    // ==========================================

    @GetMapping("/public/driver/{driverId}")
    public ResponseEntity<Page<PublicFeedback>>
    getApprovedDriverFeedback(
            @PathVariable @Positive Long driverId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                feedbackService.getApprovedDriverFeedback(
                        driverId,
                        createPageable(page, size)
                )
        );
    }

    // ==========================================
    // PUBLIC: APPROVED ROUTE REVIEWS
    // ==========================================

    @GetMapping("/public/route/{routeId}")
    public ResponseEntity<Page<PublicFeedback>>
    getApprovedRouteFeedback(
            @PathVariable @Positive Long routeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                feedbackService.getApprovedRouteFeedback(
                        routeId,
                        createPageable(page, size)
                )
        );
    }

    // ==========================================
    // PUBLIC: DRIVER RATING SUMMARY
    // ==========================================

    @GetMapping("/ratings/driver/{driverId}")
    public ResponseEntity<RatingSummary>
    getDriverRatingSummary(
            @PathVariable @Positive Long driverId
    ) {
        return ResponseEntity.ok(
                feedbackService.getDriverRatingSummary(
                        driverId
                )
        );
    }

    // ==========================================
    // PUBLIC: ROUTE RATING SUMMARY
    // ==========================================

    @GetMapping("/ratings/route/{routeId}")
    public ResponseEntity<RatingSummary>
    getRouteRatingSummary(
            @PathVariable @Positive Long routeId
    ) {
        return ResponseEntity.ok(
                feedbackService.getRouteRatingSummary(
                        routeId
                )
        );
    }

    // ==========================================
    // ADMIN: ALL FEEDBACK
    // ==========================================

    @GetMapping
    public ResponseEntity<Page<FeedbackProfile>>
    getAllFeedback(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                feedbackService.getAllFeedback(
                        createPageable(page, size)
                )
        );
    }

    // ==========================================
    // ADMIN: FEEDBACK STATISTICS
    // ==========================================

    @GetMapping("/stats")
    public ResponseEntity<FeedbackStatistics>
    getFeedbackStatistics() {
        return ResponseEntity.ok(
                feedbackService.getFeedbackStatistics()
        );
    }

    // ==========================================
    // ADMIN: FEEDBACK COUNT
    // ==========================================

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>>
    getFeedbackCount() {
        return ResponseEntity.ok(
                Map.of(
                        "totalFeedback",
                        feedbackService
                                .getFeedbackStatistics()
                                .totalReviews()
                )
        );
    }

    // ==========================================
    // ADMIN: FILTER BY MODERATION STATUS
    // ==========================================

    @GetMapping("/status/{status}")
    public ResponseEntity<List<FeedbackProfile>>
    getFeedbackByStatus(
            @PathVariable @NotBlank String status
    ) {
        return ResponseEntity.ok(
                feedbackService.getFeedbackByStatus(
                        status
                )
        );
    }

    // ==========================================
    // ADMIN: FILTER BY CATEGORY AND STATUS
    // ==========================================

    @GetMapping("/category/{category}")
    public ResponseEntity<List<FeedbackProfile>>
    getFeedbackByCategory(
            @PathVariable @NotBlank String category,
            @RequestParam(defaultValue = "APPROVED")
            String status
    ) {
        return ResponseEntity.ok(
                feedbackService.getFeedbackByCategory(
                        category,
                        status
                )
        );
    }

    // ==========================================
    // ADMIN: TRIP FEEDBACK
    // ==========================================

    @GetMapping("/trip/{tripId}")
    public ResponseEntity<List<FeedbackProfile>>
    getTripFeedback(
            @PathVariable @Positive Long tripId
    ) {
        return ResponseEntity.ok(
                feedbackService.getTripFeedback(
                        tripId
                )
        );
    }

    // ==========================================
    // PASSENGER: PERSONAL FEEDBACK HISTORY
    // ==========================================

    @GetMapping("/passenger/{passengerId}")
    public ResponseEntity<List<FeedbackProfile>>
    getPassengerFeedback(
            @PathVariable @Positive Long passengerId
    ) {
        return ResponseEntity.ok(
                feedbackService.getPassengerFeedback(
                        passengerId
                )
        );
    }

    // ==========================================
    // PASSENGER / ADMIN: REVIEW BY BOOKING
    // ==========================================

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<FeedbackProfile>
    getFeedbackByBooking(
            @PathVariable @Positive Long bookingId
    ) {
        return ResponseEntity.ok(
                feedbackService.getFeedbackByBooking(
                        bookingId
                )
        );
    }

    // ==========================================
    // ADMIN: FEEDBACK DETAILS
    // ==========================================

    @GetMapping("/{feedbackId}")
    public ResponseEntity<FeedbackProfile>
    getFeedbackDetails(
            @PathVariable String feedbackId
    ) {
        return ResponseEntity.ok(
                feedbackService.getFeedbackDetails(
                        feedbackId
                )
        );
    }

    // ==========================================
    // PASSENGER: SUBMIT FEEDBACK
    // ==========================================

    @PostMapping
    public ResponseEntity<FeedbackProfile>
    createFeedback(
            @Valid @RequestBody CreateFeedbackPayload request
    ) {
        FeedbackProfile feedback =
                feedbackService.createFeedback(
                        new CreateFeedbackRequest(
                                request.passengerId(),
                                request.bookingId(),
                                request.rating(),
                                request.driverRating(),
                                request.vehicleRating(),
                                request.punctualityRating(),
                                request.comfortRating(),
                                request.title(),
                                request.comment(),
                                request.category()
                        )
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(feedback);
    }

    // ==========================================
    // PASSENGER: EDIT EXISTING FEEDBACK
    // ==========================================

    @PutMapping("/{feedbackId}")
    public ResponseEntity<FeedbackProfile>
    updateFeedback(
            @PathVariable String feedbackId,
            @RequestParam @Positive Long passengerId,
            @Valid @RequestBody UpdateFeedbackPayload request
    ) {
        return ResponseEntity.ok(
                feedbackService.updateFeedback(
                        feedbackId,
                        passengerId,
                        new UpdateFeedbackRequest(
                                request.rating(),
                                request.driverRating(),
                                request.vehicleRating(),
                                request.punctualityRating(),
                                request.comfortRating(),
                                request.title(),
                                request.comment(),
                                request.category()
                        )
                )
        );
    }

    // ==========================================
    // ADMIN: MODERATE FEEDBACK
    // ==========================================

    @PatchMapping("/{feedbackId}/moderate")
    public ResponseEntity<FeedbackProfile>
    moderateFeedback(
            @PathVariable String feedbackId,
            @Valid @RequestBody ModerateFeedbackPayload request
    ) {
        return ResponseEntity.ok(
                feedbackService.moderateFeedback(
                        feedbackId,
                        new ModerateFeedbackRequest(
                                request.status(),
                                request.moderatorUserId(),
                                request.moderationNotes()
                        )
                )
        );
    }

    // ==========================================
    // ADMIN: RESPOND TO FEEDBACK
    // ==========================================

    @PatchMapping("/{feedbackId}/respond")
    public ResponseEntity<FeedbackProfile>
    respondToFeedback(
            @PathVariable String feedbackId,
            @Valid @RequestBody AdminResponsePayload request
    ) {
        return ResponseEntity.ok(
                feedbackService.respondToFeedback(
                        feedbackId,
                        new AdminResponseRequest(
                                request.adminUserId(),
                                request.response()
                        )
                )
        );
    }

    // ==========================================
    // REQUEST DTOs
    // ==========================================

    public record CreateFeedbackPayload(

            @NotNull(message = "Passenger ID is required")
            @Positive
            Long passengerId,

            @NotNull(message = "Booking ID is required")
            @Positive
            Long bookingId,

            @NotNull(message = "Overall rating is required")
            @Min(1)
            @Max(5)
            Integer rating,

            @Min(1)
            @Max(5)
            Integer driverRating,

            @Min(1)
            @Max(5)
            Integer vehicleRating,

            @Min(1)
            @Max(5)
            Integer punctualityRating,

            @Min(1)
            @Max(5)
            Integer comfortRating,

            @NotBlank(message = "Title is required")
            @Size(max = 150)
            String title,

            @NotBlank(message = "Comment is required")
            @Size(max = 3000)
            String comment,

            String category

    ) {
    }

    public record UpdateFeedbackPayload(

            @Min(1)
            @Max(5)
            Integer rating,

            @Min(1)
            @Max(5)
            Integer driverRating,

            @Min(1)
            @Max(5)
            Integer vehicleRating,

            @Min(1)
            @Max(5)
            Integer punctualityRating,

            @Min(1)
            @Max(5)
            Integer comfortRating,

            @Size(max = 150)
            String title,

            @Size(max = 3000)
            String comment,

            String category

    ) {
    }

    public record ModerateFeedbackPayload(

            @NotBlank(message = "Moderation status is required")
            String status,

            @NotNull(message = "Moderator user ID is required")
            @Positive
            Long moderatorUserId,

            @Size(max = 1000)
            String moderationNotes

    ) {
    }

    public record AdminResponsePayload(

            @NotNull(message = "Admin user ID is required")
            @Positive
            Long adminUserId,

            @NotBlank(message = "Response is required")
            @Size(max = 2000)
            String response

    ) {
    }

    // ==========================================
    // PAGINATION HELPER
    // ==========================================

    private Pageable createPageable(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw badRequest(
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

    // ==========================================
    // ERROR HELPER
    // ==========================================

    private ResponseStatusException badRequest(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }
}
