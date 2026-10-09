
package com.smartmove.backend.service;

import com.smartmove.backend.document.Feedback;
import com.smartmove.backend.entity.Booking;
import com.smartmove.backend.repository.BookingRepository;
import com.smartmove.backend.repository.FeedbackRepository;
import com.smartmove.backend.repository.UserAccountRepository;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class FeedbackService {

    private static final Set<String> CATEGORIES = Set.of(
            "GENERAL",
            "DRIVER_BEHAVIOUR",
            "CLEANLINESS",
            "PUNCTUALITY",
            "COMFORT",
            "SAFETY",
            "OTHER"
    );

    private static final Set<String> STATUSES = Set.of(
            "PENDING",
            "APPROVED",
            "REJECTED",
            "HIDDEN"
    );

    private final FeedbackRepository feedbackRepository;
    private final BookingRepository bookingRepository;
    private final UserAccountRepository userAccountRepository;

    public FeedbackService(
            FeedbackRepository feedbackRepository,
            BookingRepository bookingRepository,
            UserAccountRepository userAccountRepository
    ) {
        this.feedbackRepository = feedbackRepository;
        this.bookingRepository = bookingRepository;
        this.userAccountRepository = userAccountRepository;
    }

    // ==========================================
    // DATA TRANSFER OBJECTS
    // ==========================================

    public record FeedbackProfile(
            String id,
            Long passengerId,
            Long bookingId,
            Long tripId,
            Long driverId,
            Long routeId,
            Integer rating,
            Integer driverRating,
            Integer vehicleRating,
            Integer punctualityRating,
            Integer comfortRating,
            String title,
            String comment,
            String category,
            String status,
            Long moderatedByUserId,
            LocalDateTime moderatedAt,
            String moderationNotes,
            String adminResponse,
            LocalDateTime adminRespondedAt,
            Boolean edited,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record PublicFeedback(
            String id,
            Long tripId,
            Long driverId,
            Long routeId,
            Integer rating,
            Integer driverRating,
            Integer vehicleRating,
            Integer punctualityRating,
            Integer comfortRating,
            String title,
            String comment,
            String category,
            String adminResponse,
            LocalDateTime createdAt
    ) {
    }

    public record CreateFeedbackRequest(
            Long passengerId,
            Long bookingId,
            Integer rating,
            Integer driverRating,
            Integer vehicleRating,
            Integer punctualityRating,
            Integer comfortRating,
            String title,
            String comment,
            String category
    ) {
    }

    public record UpdateFeedbackRequest(
            Integer rating,
            Integer driverRating,
            Integer vehicleRating,
            Integer punctualityRating,
            Integer comfortRating,
            String title,
            String comment,
            String category
    ) {
    }

    public record ModerateFeedbackRequest(
            String status,
            Long moderatorUserId,
            String moderationNotes
    ) {
    }

    public record AdminResponseRequest(
            Long adminUserId,
            String response
    ) {
    }

    public record RatingSummary(
            Long entityId,
            long reviewCount,
            BigDecimal averageRating,
            long oneStar,
            long twoStars,
            long threeStars,
            long fourStars,
            long fiveStars
    ) {
    }

    public record FeedbackStatistics(
            long totalReviews,
            long pendingReviews,
            long approvedReviews,
            long rejectedReviews,
            long hiddenReviews,
            BigDecimal averageApprovedRating
    ) {
    }

    // ==========================================
    // PASSENGER: CREATE FEEDBACK
    // ==========================================

    @Transactional(readOnly = true)
    public Booking validateReviewableBooking(
            Long bookingId,
            Long passengerId
    ) {
        if (bookingId == null || passengerId == null) {
            throw badRequest(
                    "Booking ID and passenger ID are required"
            );
        }

        Booking booking = bookingRepository
                .findById(bookingId)
                .orElseThrow(() ->
                        notFound("Booking not found")
                );

        if (!booking.getPassenger().getId()
                .equals(passengerId)) {
            throw forbidden(
                    "Booking does not belong to this passenger"
            );
        }

        if (!"CONFIRMED".equalsIgnoreCase(
                booking.getStatus()
        )) {
            throw conflict(
                    "Only confirmed bookings can be reviewed"
            );
        }

        if (!"COMPLETED".equalsIgnoreCase(
                booking.getTrip().getStatus()
        )) {
            throw conflict(
                    "The trip must be completed before reviewing"
            );
        }

        return booking;
    }

    public FeedbackProfile createFeedback(
            CreateFeedbackRequest request
    ) {
        if (request == null) {
            throw badRequest(
                    "Feedback details are required"
            );
        }

        Booking booking = validateReviewableBooking(
                request.bookingId(),
                request.passengerId()
        );

        if (feedbackRepository.existsByBookingId(
                request.bookingId()
        )) {
            throw conflict(
                    "Feedback already exists for this booking"
            );
        }

        validateRating(request.rating(), true);
        validateRating(request.driverRating(), false);
        validateRating(request.vehicleRating(), false);
        validateRating(request.punctualityRating(), false);
        validateRating(request.comfortRating(), false);

        Feedback feedback = new Feedback();

        feedback.setPassengerId(request.passengerId());
        feedback.setBookingId(booking.getId());
        feedback.setTripId(booking.getTrip().getId());
        feedback.setRouteId(
                booking.getTrip().getRoute().getId()
        );

        if (booking.getTrip().getDriver() != null) {
            feedback.setDriverId(
                    booking.getTrip().getDriver().getId()
            );
        }

        feedback.setRating(request.rating());
        feedback.setDriverRating(request.driverRating());
        feedback.setVehicleRating(request.vehicleRating());
        feedback.setPunctualityRating(
                request.punctualityRating()
        );
        feedback.setComfortRating(request.comfortRating());

        feedback.setTitle(
                requiredText(request.title(), "Title", 150)
        );
        feedback.setComment(
                requiredText(request.comment(), "Comment", 3000)
        );
        feedback.setCategory(
                normalizeCategory(request.category())
        );
        feedback.setStatus("PENDING");
        feedback.setEdited(false);

        LocalDateTime now = LocalDateTime.now();
        feedback.setCreatedAt(now);
        feedback.setUpdatedAt(now);

        try {
            return toProfile(
                    feedbackRepository.save(feedback)
            );
        } catch (DuplicateKeyException exception) {
            throw conflict(
                    "Feedback already exists for this booking"
            );
        }
    }

    // ==========================================
    // PASSENGER: EDIT FEEDBACK
    // ==========================================

    public FeedbackProfile updateFeedback(
            String feedbackId,
            Long passengerId,
            UpdateFeedbackRequest request
    ) {
        if (request == null || passengerId == null) {
            throw badRequest(
                    "Passenger ID and update details are required"
            );
        }

        Feedback feedback = findFeedback(feedbackId);

        if (!passengerId.equals(feedback.getPassengerId())) {
            throw forbidden(
                    "You cannot edit another passenger's feedback"
            );
        }

        if (!Set.of("PENDING", "REJECTED").contains(
                feedback.getStatus().toUpperCase(Locale.ROOT)
        )) {
            throw conflict(
                    "Only pending or rejected feedback can be edited"
            );
        }

        if (request.rating() != null) {
            validateRating(request.rating(), true);
            feedback.setRating(request.rating());
        }

        if (request.driverRating() != null) {
            validateRating(request.driverRating(), false);
            feedback.setDriverRating(request.driverRating());
        }

        if (request.vehicleRating() != null) {
            validateRating(request.vehicleRating(), false);
            feedback.setVehicleRating(request.vehicleRating());
        }

        if (request.punctualityRating() != null) {
            validateRating(request.punctualityRating(), false);
            feedback.setPunctualityRating(
                    request.punctualityRating()
            );
        }

        if (request.comfortRating() != null) {
            validateRating(request.comfortRating(), false);
            feedback.setComfortRating(request.comfortRating());
        }

        if (request.title() != null) {
            feedback.setTitle(
                    requiredText(request.title(), "Title", 150)
            );
        }

        if (request.comment() != null) {
            feedback.setComment(
                    requiredText(
                            request.comment(),
                            "Comment",
                            3000
                    )
            );
        }

        if (request.category() != null) {
            feedback.setCategory(
                    normalizeCategory(request.category())
            );
        }

        // Editing requires fresh administrative approval.
        feedback.setStatus("PENDING");
        feedback.setEdited(true);
        feedback.setModeratedByUserId(null);
        feedback.setModeratedAt(null);
        feedback.setModerationNotes(null);
        feedback.setUpdatedAt(LocalDateTime.now());

        return toProfile(
                feedbackRepository.save(feedback)
        );
    }

    // ==========================================
    // PASSENGER: FEEDBACK HISTORY
    // ==========================================

    public List<FeedbackProfile> getPassengerFeedback(
            Long passengerId
    ) {
        if (passengerId == null) {
            throw badRequest("Passenger ID is required");
        }

        return feedbackRepository
                .findByPassengerIdOrderByCreatedAtDesc(
                        passengerId
                )
                .stream()
                .map(this::toProfile)
                .toList();
    }

    public FeedbackProfile getFeedbackByBooking(
            Long bookingId
    ) {
        return toProfile(
                feedbackRepository.findByBookingId(bookingId)
                        .orElseThrow(() ->
                                notFound(
                                        "Feedback not found for booking"
                                )
                        )
        );
    }

    // ==========================================
    // ADMIN: MODERATE FEEDBACK
    // ==========================================

    public FeedbackProfile moderateFeedback(
            String feedbackId,
            ModerateFeedbackRequest request
    ) {
        if (request == null
                || request.moderatorUserId() == null) {
            throw badRequest(
                    "Moderator ID is required"
            );
        }

        userAccountRepository
                .findById(request.moderatorUserId())
                .orElseThrow(() ->
                        notFound("Moderator account not found")
                );

        String status = normalizeStatus(request.status());

        if ("PENDING".equals(status)) {
            throw badRequest(
                    "Moderation outcome must be APPROVED, REJECTED or HIDDEN"
            );
        }

        Feedback feedback = findFeedback(feedbackId);

        feedback.setStatus(status);
        feedback.setModeratedByUserId(
                request.moderatorUserId()
        );
        feedback.setModeratedAt(LocalDateTime.now());
        feedback.setModerationNotes(
                optionalText(request.moderationNotes(), 1000)
        );
        feedback.setUpdatedAt(LocalDateTime.now());

        return toProfile(
                feedbackRepository.save(feedback)
        );
    }

    // ==========================================
    // ADMIN: RESPOND TO FEEDBACK
    // ==========================================

    public FeedbackProfile respondToFeedback(
            String feedbackId,
            AdminResponseRequest request
    ) {
        if (request == null
                || request.adminUserId() == null) {
            throw badRequest(
                    "Admin user ID is required"
            );
        }

        userAccountRepository
                .findById(request.adminUserId())
                .orElseThrow(() ->
                        notFound("Admin account not found")
                );

        Feedback feedback = findFeedback(feedbackId);

        feedback.setAdminResponse(
                requiredText(
                        request.response(),
                        "Admin response",
                        2000
                )
        );
        feedback.setAdminRespondedAt(LocalDateTime.now());
        feedback.setUpdatedAt(LocalDateTime.now());

        return toProfile(
                feedbackRepository.save(feedback)
        );
    }

    // ==========================================
    // PUBLIC: APPROVED FEEDBACK
    // ==========================================

    public Page<PublicFeedback> getApprovedFeedback(
            Pageable pageable
    ) {
        return feedbackRepository
                .findByStatusIgnoreCase(
                        "APPROVED",
                        pageable
                )
                .map(this::toPublicFeedback);
    }

    public Page<PublicFeedback> getApprovedDriverFeedback(
            Long driverId,
            Pageable pageable
    ) {
        return feedbackRepository
                .findByDriverIdAndStatusIgnoreCase(
                        driverId,
                        "APPROVED",
                        pageable
                )
                .map(this::toPublicFeedback);
    }

    public Page<PublicFeedback> getApprovedRouteFeedback(
            Long routeId,
            Pageable pageable
    ) {
        return feedbackRepository
                .findByRouteIdAndStatusIgnoreCase(
                        routeId,
                        "APPROVED",
                        pageable
                )
                .map(this::toPublicFeedback);
    }

    // ==========================================
    // ADMIN: ALL FEEDBACK
    // ==========================================

    public Page<FeedbackProfile> getAllFeedback(
            Pageable pageable
    ) {
        return feedbackRepository
                .findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toProfile);
    }

    public List<FeedbackProfile> getFeedbackByStatus(
            String status
    ) {
        return feedbackRepository
                .findByStatusIgnoreCaseOrderByCreatedAtDesc(
                        normalizeStatus(status)
                )
                .stream()
                .map(this::toProfile)
                .toList();
    }

    public List<FeedbackProfile> getTripFeedback(
            Long tripId
    ) {
        return feedbackRepository
                .findByTripIdOrderByCreatedAtDesc(tripId)
                .stream()
                .map(this::toProfile)
                .toList();
    }

    public List<FeedbackProfile> getFeedbackByCategory(
            String category,
            String status
    ) {
        return feedbackRepository
                .findByCategoryIgnoreCaseAndStatusIgnoreCaseOrderByCreatedAtDesc(
                        normalizeCategory(category),
                        normalizeStatus(status)
                )
                .stream()
                .map(this::toProfile)
                .toList();
    }

    public FeedbackProfile getFeedbackDetails(
            String feedbackId
    ) {
        return toProfile(findFeedback(feedbackId));
    }

    // ==========================================
    // DRIVER AND ROUTE RATING REPORTS
    // ==========================================

    public RatingSummary getDriverRatingSummary(
            Long driverId
    ) {
        if (driverId == null) {
            throw badRequest("Driver ID is required");
        }

        return summarizeRatings(
                driverId,
                feedbackRepository.findApprovedDriverRatings(
                        driverId
                )
        );
    }

    public RatingSummary getRouteRatingSummary(
            Long routeId
    ) {
        if (routeId == null) {
            throw badRequest("Route ID is required");
        }

        return summarizeRatings(
                routeId,
                feedbackRepository.findApprovedRouteRatings(
                        routeId
                )
        );
    }

    public FeedbackStatistics getFeedbackStatistics() {
        List<Feedback> approved = feedbackRepository
                .findByStatusIgnoreCaseOrderByCreatedAtDesc(
                        "APPROVED"
                );

        return new FeedbackStatistics(
                feedbackRepository.count(),
                feedbackRepository.countByStatusIgnoreCase(
                        "PENDING"
                ),
                feedbackRepository.countByStatusIgnoreCase(
                        "APPROVED"
                ),
                feedbackRepository.countByStatusIgnoreCase(
                        "REJECTED"
                ),
                feedbackRepository.countByStatusIgnoreCase(
                        "HIDDEN"
                ),
                averageRating(approved)
        );
    }

    // ==========================================
    // RATING CALCULATIONS
    // ==========================================

    private RatingSummary summarizeRatings(
            Long entityId,
            List<Feedback> feedback
    ) {
        return new RatingSummary(
                entityId,
                feedback.size(),
                averageRating(feedback),
                countStars(feedback, 1),
                countStars(feedback, 2),
                countStars(feedback, 3),
                countStars(feedback, 4),
                countStars(feedback, 5)
        );
    }

    private long countStars(
            List<Feedback> feedback,
            int stars
    ) {
        return feedback.stream()
                .filter(item ->
                        item.getRating() != null
                                && item.getRating() == stars
                )
                .count();
    }

    private BigDecimal averageRating(
            List<Feedback> feedback
    ) {
        double average = feedback.stream()
                .filter(item -> item.getRating() != null)
                .mapToInt(Feedback::getRating)
                .average()
                .orElse(0.0);

        return BigDecimal.valueOf(average)
                .setScale(2, RoundingMode.HALF_UP);
    }

    // ==========================================
    // VALIDATION HELPERS
    // ==========================================

    private Feedback findFeedback(String feedbackId) {
        if (feedbackId == null || feedbackId.isBlank()) {
            throw badRequest("Feedback ID is required");
        }

        return feedbackRepository.findById(feedbackId)
                .orElseThrow(() ->
                        notFound("Feedback not found")
                );
    }

    private void validateRating(
            Integer rating,
            boolean required
    ) {
        if (rating == null) {
            if (required) {
                throw badRequest("Overall rating is required");
            }
            return;
        }

        if (rating < 1 || rating > 5) {
            throw badRequest(
                    "Rating must be between 1 and 5"
            );
        }
    }

    private String normalizeCategory(String category) {
        String value = category == null
                || category.isBlank()
                ? "GENERAL"
                : category.trim().toUpperCase(Locale.ROOT);

        if (!CATEGORIES.contains(value)) {
            throw badRequest(
                    "Unsupported feedback category"
            );
        }

        return value;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            throw badRequest("Feedback status is required");
        }

        String value = status.trim()
                .toUpperCase(Locale.ROOT);

        if (!STATUSES.contains(value)) {
            throw badRequest(
                    "Unsupported feedback status"
            );
        }

        return value;
    }

    private String requiredText(
            String value,
            String field,
            int maxLength
    ) {
        if (value == null || value.isBlank()) {
            throw badRequest(field + " is required");
        }

        String result = value.trim();

        if (result.length() > maxLength) {
            throw badRequest(field + " is too long");
        }

        return result;
    }

    private String optionalText(
            String value,
            int maxLength
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String result = value.trim();

        if (result.length() > maxLength) {
            throw badRequest("Text is too long");
        }

        return result;
    }

    // ==========================================
    // RESPONSE MAPPING
    // ==========================================

    private FeedbackProfile toProfile(Feedback f) {
        return new FeedbackProfile(
                f.getId(),
                f.getPassengerId(),
                f.getBookingId(),
                f.getTripId(),
                f.getDriverId(),
                f.getRouteId(),
                f.getRating(),
                f.getDriverRating(),
                f.getVehicleRating(),
                f.getPunctualityRating(),
                f.getComfortRating(),
                f.getTitle(),
                f.getComment(),
                f.getCategory(),
                f.getStatus(),
                f.getModeratedByUserId(),
                f.getModeratedAt(),
                f.getModerationNotes(),
                f.getAdminResponse(),
                f.getAdminRespondedAt(),
                f.getEdited(),
                f.getCreatedAt(),
                f.getUpdatedAt()
        );
    }

    private PublicFeedback toPublicFeedback(Feedback f) {
        return new PublicFeedback(
                f.getId(),
                f.getTripId(),
                f.getDriverId(),
                f.getRouteId(),
                f.getRating(),
                f.getDriverRating(),
                f.getVehicleRating(),
                f.getPunctualityRating(),
                f.getComfortRating(),
                f.getTitle(),
                f.getComment(),
                f.getCategory(),
                f.getAdminResponse(),
                f.getCreatedAt()
        );
    }

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

    private ResponseStatusException forbidden(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                message
        );
    }

    private ResponseStatusException conflict(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                message
        );
    }
}
