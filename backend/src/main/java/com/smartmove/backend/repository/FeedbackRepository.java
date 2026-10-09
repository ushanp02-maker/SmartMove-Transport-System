
package com.smartmove.backend.repository;

import com.smartmove.backend.document.Feedback;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeedbackRepository
        extends MongoRepository<Feedback, String> {

    // ==========================================
    // PASSENGER FEEDBACK
    // ==========================================

    // Passenger: view personal feedback history.
    List<Feedback> findByPassengerIdOrderByCreatedAtDesc(
            Long passengerId
    );

    // Passenger: find an existing review for a booking.
    Optional<Feedback> findByBookingId(
            Long bookingId
    );

    // Prevent duplicate reviews for the same booking.
    boolean existsByBookingId(
            Long bookingId
    );

    // ==========================================
    // TRIP, ROUTE AND DRIVER FEEDBACK
    // ==========================================

    // Retrieve all reviews for a particular trip.
    List<Feedback> findByTripIdOrderByCreatedAtDesc(
            Long tripId
    );

    // Retrieve all reviews for a particular route.
    List<Feedback> findByRouteIdOrderByCreatedAtDesc(
            Long routeId
    );

    // Retrieve all reviews for a particular driver.
    List<Feedback> findByDriverIdOrderByCreatedAtDesc(
            Long driverId
    );

    // ==========================================
    // PUBLIC FEEDBACK
    // ==========================================

    // Public: retrieve approved reviews with pagination.
    // Also used by the admin moderation queue
    // with the appropriate requested status.
    Page<Feedback> findByStatusIgnoreCase(
            String status,
            Pageable pageable
    );

    // Public: approved reviews for a route.
    Page<Feedback> findByRouteIdAndStatusIgnoreCase(
            Long routeId,
            String status,
            Pageable pageable
    );

    // Public: approved reviews for a driver.
    Page<Feedback> findByDriverIdAndStatusIgnoreCase(
            Long driverId,
            String status,
            Pageable pageable
    );

    // ==========================================
    // ADMIN FEEDBACK MANAGEMENT
    // ==========================================

    // Admin: filter reviews by moderation status.
    List<Feedback> findByStatusIgnoreCaseOrderByCreatedAtDesc(
            String status
    );

    // Admin: retrieve all reviews with pagination.
    Page<Feedback> findAllByOrderByCreatedAtDesc(
            Pageable pageable
    );

    // Admin: retrieve reviews handled by a moderator.
    List<Feedback> findByModeratedByUserIdOrderByModeratedAtDesc(
            Long moderatedByUserId
    );

    // Count reviews awaiting moderation or
    // belonging to another status.
    long countByStatusIgnoreCase(
            String status
    );

    // ==========================================
    // FEEDBACK STATISTICS
    // ==========================================

    // Count reviews submitted by a passenger.
    long countByPassengerId(
            Long passengerId
    );

    // Count approved reviews for a driver.
    long countByDriverIdAndStatusIgnoreCase(
            Long driverId,
            String status
    );

    // Count approved reviews for a route.
    long countByRouteIdAndStatusIgnoreCase(
            Long routeId,
            String status
    );

    // Retrieve reviews above a minimum rating.
    List<Feedback>
    findByStatusIgnoreCaseAndRatingGreaterThanEqualOrderByCreatedAtDesc(
            String status,
            Integer minimumRating
    );

    // Retrieve reviews below a maximum rating.
    List<Feedback>
    findByStatusIgnoreCaseAndRatingLessThanEqualOrderByCreatedAtDesc(
            String status,
            Integer maximumRating
    );

    // ==========================================
    // DRIVER AND ROUTE RATINGS
    // ==========================================

    // Retrieve approved driver ratings.
    // The service layer will calculate the average.
    @Query(
            value = "{ 'driverId': ?0, 'status': 'APPROVED' }",
            fields = "{ 'rating': 1 }"
    )
    List<Feedback> findApprovedDriverRatings(
            Long driverId
    );

    // Retrieve approved route ratings.
    // The service layer will calculate the average.
    @Query(
            value = "{ 'routeId': ?0, 'status': 'APPROVED' }",
            fields = "{ 'rating': 1 }"
    )
    List<Feedback> findApprovedRouteRatings(
            Long routeId
    );

    // ==========================================
    // CATEGORY AND TRIP FILTERING
    // ==========================================

    // Retrieve feedback by category and status.
    List<Feedback>
    findByCategoryIgnoreCaseAndStatusIgnoreCaseOrderByCreatedAtDesc(
            String category,
            String status
    );

    // Retrieve feedback for a trip with a given status.
    List<Feedback>
    findByTripIdAndStatusIgnoreCaseOrderByCreatedAtDesc(
            Long tripId,
            String status
    );
}
