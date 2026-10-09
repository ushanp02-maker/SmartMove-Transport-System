
package com.smartmove.backend.repository;

import com.smartmove.backend.entity.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository
        extends JpaRepository<Payment, Long> {

    // Find a payment using its transaction reference.
    Optional<Payment> findByTransactionReference(
            String transactionReference
    );

    // Prevent duplicate transaction references.
    boolean existsByTransactionReference(
            String transactionReference
    );

    // Get all payments belonging to a booking.
    List<Payment> findByBookingIdOrderByCreatedAtDesc(
            Long bookingId
    );

    // Passenger payment history.
    List<Payment> findByBookingPassengerIdOrderByCreatedAtDesc(
            Long passengerId
    );

    // Admin: view payments by status.
    List<Payment> findByStatusIgnoreCaseOrderByCreatedAtDesc(
            String status
    );

    // Admin: view payments by method.
    List<Payment> findByPaymentMethodIgnoreCaseOrderByCreatedAtDesc(
            String paymentMethod
    );

    // Admin: payments created during a date range.
    List<Payment> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime start,
            LocalDateTime end
    );

    // Find completed payments during a date range.
    List<Payment> findByStatusIgnoreCaseAndPaymentDateBetweenOrderByPaymentDateDesc(
            String status,
            LocalDateTime start,
            LocalDateTime end
    );

    // Count payments by status.
    long countByStatusIgnoreCase(String status);

    // Count payments belonging to a booking.
    long countByBookingId(Long bookingId);

    // Lock payment before changing its status.
    // Used for completion, failure and refund processing.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT p FROM Payment p
        WHERE p.id = :paymentId
        """)
    Optional<Payment> findByIdForUpdate(
            @Param("paymentId") Long paymentId
    );

    // Retrieve payment together with its booking,
    // passenger and trip details.
    @Query("""
        SELECT p FROM Payment p
        JOIN FETCH p.booking b
        JOIN FETCH b.passenger
        JOIN FETCH b.trip t
        JOIN FETCH t.route
        WHERE p.id = :paymentId
        """)
    Optional<Payment> findDetailedById(
            @Param("paymentId") Long paymentId
    );

    // Total successfully completed payments.
    // Gross revenue before refunds.
    @Query("""
        SELECT COALESCE(SUM(p.amount), 0)
        FROM Payment p
        WHERE p.status IN ('COMPLETED', 'REFUNDED')
        """)
    BigDecimal calculateGrossRevenue();

    // Gross revenue for a particular date range.
    @Query("""
        SELECT COALESCE(SUM(p.amount), 0)
        FROM Payment p
        WHERE p.status IN ('COMPLETED', 'REFUNDED')
          AND p.paymentDate >= :start
          AND p.paymentDate < :end
        """)
    BigDecimal calculateGrossRevenueBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // Total refunded amount.
    @Query("""
        SELECT COALESCE(SUM(p.refundAmount), 0)
        FROM Payment p
        WHERE p.refundAmount IS NOT NULL
        """)
    BigDecimal calculateTotalRefunds();

    // Refunds issued during a date range.
    @Query("""
        SELECT COALESCE(SUM(p.refundAmount), 0)
        FROM Payment p
        WHERE p.refundedAt >= :start
          AND p.refundedAt < :end
        """)
    BigDecimal calculateRefundsBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // Revenue grouped by route.
    // Each result contains:
    // [route ID, route name, gross revenue]
    @Query("""
        SELECT t.route.id,
               t.route.name,
               SUM(p.amount)
        FROM Payment p
        JOIN p.booking b
        JOIN b.trip t
        WHERE p.status IN ('COMPLETED', 'REFUNDED')
        GROUP BY t.route.id, t.route.name
        ORDER BY SUM(p.amount) DESC
        """)
    List<Object[]> calculateRevenueByRoute();

    // Revenue grouped by payment method.
    // Each result contains:
    // [payment method, gross revenue]
    @Query("""
        SELECT p.paymentMethod,
               SUM(p.amount)
        FROM Payment p
        WHERE p.status IN ('COMPLETED', 'REFUNDED')
        GROUP BY p.paymentMethod
        ORDER BY SUM(p.amount) DESC
        """)
    List<Object[]> calculateRevenueByPaymentMethod();

    // Completed payment records for a particular trip.
    @Query("""
        SELECT p FROM Payment p
        JOIN FETCH p.booking b
        WHERE b.trip.id = :tripId
          AND p.status IN ('COMPLETED', 'REFUNDED')
        ORDER BY p.paymentDate DESC
        """)
    List<Payment> findSuccessfulPaymentsByTrip(
            @Param("tripId") Long tripId
    );
}
