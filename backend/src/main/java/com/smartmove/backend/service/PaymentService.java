
package com.smartmove.backend.service;

import com.smartmove.backend.entity.Booking;
import com.smartmove.backend.entity.Payment;
import com.smartmove.backend.repository.BookingRepository;
import com.smartmove.backend.repository.PaymentRepository;

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
import java.util.UUID;

@Service
public class PaymentService {

    private static final String CURRENCY = "LKR";

    private static final Set<String> PAYMENT_METHODS =
            Set.of(
                    "SIMULATED",
                    "CASH",
                    "BANK_TRANSFER",
                    "CARD_SIMULATED"
            );

    private final BookingPaymentPlsqlGateway plsql;
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;

    public PaymentService(
            PaymentRepository paymentRepository,
            BookingRepository bookingRepository,
            BookingPaymentPlsqlGateway plsql
    ) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.plsql = plsql;
    }

    // ==========================================
    // DATA TRANSFER OBJECTS
    // ==========================================

    public record PaymentProfile(
            Long id,
            Long bookingId,
            String bookingReference,
            Long passengerId,
            String passengerName,
            Long tripId,
            BigDecimal amount,
            String currency,
            String paymentMethod,
            String status,
            String transactionReference,
            LocalDateTime paymentDate,
            LocalDateTime createdAt,
            LocalDateTime refundedAt,
            BigDecimal refundAmount,
            String notes
    ) {
    }

    public record CreatePaymentRequest(
            Long bookingId,
            String paymentMethod,
            String notes
    ) {
    }

    public record RefundRequest(
            BigDecimal amount,
            String reason
    ) {
    }

    public record PaymentStatistics(
            long totalPayments,
            long pendingPayments,
            long completedPayments,
            long failedPayments,
            long refundedPayments,
            BigDecimal grossRevenue,
            BigDecimal totalRefunds,
            BigDecimal netRevenue
    ) {
    }

    public record RevenueByRoute(
            Long routeId,
            String routeName,
            BigDecimal grossRevenue
    ) {
    }

    public record RevenueByMethod(
            String paymentMethod,
            BigDecimal grossRevenue
    ) {
    }

    // ==========================================
    // PAYMENT LOOKUPS
    // ==========================================

    @Transactional(readOnly = true)
    public Payment getPaymentById(Long paymentId) {
        if (paymentId == null) {
            throw badRequest("Payment ID is required");
        }

        return paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Payment not found"
                        )
                );
    }

    @Transactional(readOnly = true)
    public PaymentProfile getPaymentProfile(Long paymentId) {
        return toProfile(getPaymentById(paymentId));
    }

    @Transactional(readOnly = true)
    public PaymentProfile getPaymentByReference(
            String transactionReference
    ) {
        if (transactionReference == null
                || transactionReference.isBlank()) {
            throw badRequest(
                    "Transaction reference is required"
            );
        }

        Payment payment = paymentRepository
                .findByTransactionReference(
                        transactionReference.trim()
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Payment not found"
                        )
                );

        return toProfile(payment);
    }

    // ==========================================
    // CREATE PAYMENT RECORD
    // ==========================================

    @Transactional
    public PaymentProfile createPayment(
            CreatePaymentRequest request
    ) {
        if (request == null || request.bookingId() == null) {
            throw badRequest("Booking ID is required");
        }
        String method = normalizePaymentMethod(request.paymentMethod());
        Long id = plsql.createPayment(request.bookingId(),method,request.notes());
        return toProfile(getPaymentById(id));
    }

    // ==========================================
    // COMPLETE SIMULATED PAYMENT
    // ==========================================

    @Transactional
    public PaymentProfile completePayment(Long paymentId) {
        if (paymentId == null) throw badRequest("Payment ID is required");
        plsql.completePayment(paymentId);
        return toProfile(getPaymentById(paymentId));
    }

    // ==========================================
    // FAIL PENDING PAYMENT
    // ==========================================

    @Transactional
    public PaymentProfile failPayment(
            Long paymentId,
            String reason
    ) {
        if (paymentId == null) throw badRequest("Payment ID is required");
        plsql.failPayment(paymentId,reason);
        return toProfile(getPaymentById(paymentId));
    }

    // ==========================================
    // REFUND COMPLETED PAYMENT
    // ==========================================

    @Transactional
    public PaymentProfile refundPayment(
            Long paymentId,
            RefundRequest request
    ) {
        if (paymentId == null) throw badRequest("Payment ID is required");
        if (request == null || request.amount() == null) throw badRequest("Refund amount is required");
        plsql.refundPayment(paymentId,request.amount(),request.reason());
        return toProfile(getPaymentById(paymentId));
    }

    // ==========================================
    // PASSENGER PAYMENT HISTORY
    // ==========================================

    @Transactional(readOnly = true)
    public List<PaymentProfile> getPassengerPayments(
            Long passengerId
    ) {
        if (passengerId == null) {
            throw badRequest(
                    "Passenger ID is required"
            );
        }

        return paymentRepository
                .findByBookingPassengerIdOrderByCreatedAtDesc(
                        passengerId
                )
                .stream()
                .map(this::toProfile)
                .toList();
    }

    // ==========================================
    // BOOKING PAYMENT HISTORY
    // ==========================================

    @Transactional(readOnly = true)
    public List<PaymentProfile> getBookingPayments(
            Long bookingId
    ) {
        if (bookingId == null) {
            throw badRequest(
                    "Booking ID is required"
            );
        }

        return paymentRepository
                .findByBookingIdOrderByCreatedAtDesc(
                        bookingId
                )
                .stream()
                .map(this::toProfile)
                .toList();
    }

    // ==========================================
    // ADMIN: REMOVE UNSUCCESSFUL PAYMENT ATTEMPT
    // ==========================================

    @Transactional
    public void deleteUnsuccessfulPayment(Long paymentId) {
        if (paymentId == null) throw badRequest("Payment ID is required");
        plsql.deletePayment(paymentId);
    }

    // ==========================================
    // ADMIN: ALL PAYMENTS
    // ==========================================

    @Transactional(readOnly = true)
    public List<PaymentProfile> getAllPayments() {
        return paymentRepository.findAll()
                .stream()
                .map(this::toProfile)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<PaymentProfile> getPayments(
            Pageable pageable
    ) {
        return paymentRepository.findAll(pageable)
                .map(this::toProfile);
    }

    @Transactional(readOnly = true)
    public List<PaymentProfile> getPaymentsByStatus(
            String status
    ) {
        return paymentRepository
                .findByStatusIgnoreCaseOrderByCreatedAtDesc(
                        requireText(status, "Payment status")
                )
                .stream()
                .map(this::toProfile)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentProfile> getPaymentsByMethod(
            String method
    ) {
        return paymentRepository
                .findByPaymentMethodIgnoreCaseOrderByCreatedAtDesc(
                        normalizePaymentMethod(method)
                )
                .stream()
                .map(this::toProfile)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentProfile> getPaymentsBetween(
            LocalDateTime start,
            LocalDateTime end
    ) {
        if (start == null || end == null
                || !start.isBefore(end)) {
            throw badRequest(
                    "Invalid payment date range"
            );
        }

        return paymentRepository
                .findByCreatedAtBetweenOrderByCreatedAtDesc(
                        start,
                        end
                )
                .stream()
                .map(this::toProfile)
                .toList();
    }

    // ==========================================
    // ADMIN: PAYMENT STATISTICS
    // ==========================================

    @Transactional(readOnly = true)
    public PaymentStatistics getPaymentStatistics() {
        BigDecimal gross =
                paymentRepository.calculateGrossRevenue();

        BigDecimal refunds =
                paymentRepository.calculateTotalRefunds();

        return new PaymentStatistics(
                paymentRepository.count(),
                paymentRepository.countByStatusIgnoreCase(
                        "PENDING"
                ),
                paymentRepository.countByStatusIgnoreCase(
                        "COMPLETED"
                ),
                paymentRepository.countByStatusIgnoreCase(
                        "FAILED"
                ),
                paymentRepository.countByStatusIgnoreCase(
                        "REFUNDED"
                ),
                gross,
                refunds,
                gross.subtract(refunds)
        );
    }

    // ==========================================
    // ADMIN: REVENUE BY ROUTE
    // ==========================================

    @Transactional(readOnly = true)
    public List<RevenueByRoute> getRevenueByRoute() {
        return paymentRepository.calculateRevenueByRoute()
                .stream()
                .map(row -> new RevenueByRoute(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        (BigDecimal) row[2]
                ))
                .toList();
    }

    // ==========================================
    // ADMIN: REVENUE BY PAYMENT METHOD
    // ==========================================

    @Transactional(readOnly = true)
    public List<RevenueByMethod> getRevenueByMethod() {
        return paymentRepository
                .calculateRevenueByPaymentMethod()
                .stream()
                .map(row -> new RevenueByMethod(
                        (String) row[0],
                        (BigDecimal) row[1]
                ))
                .toList();
    }

    // ==========================================
    // ADMIN: REVENUE BY DATE RANGE
    // ==========================================

    @Transactional(readOnly = true)
    public BigDecimal getNetRevenueBetween(
            LocalDateTime start,
            LocalDateTime end
    ) {
        if (start == null || end == null
                || !start.isBefore(end)) {
            throw badRequest(
                    "Invalid revenue date range"
            );
        }

        BigDecimal gross =
                paymentRepository
                        .calculateGrossRevenueBetween(
                                start,
                                end
                        );

        BigDecimal refunds =
                paymentRepository.calculateRefundsBetween(
                        start,
                        end
                );

        return gross.subtract(refunds);
    }

    // ==========================================
    // ENTITY LOOKUPS
    // ==========================================

    private Booking lockBooking(Long bookingId) {
        return bookingRepository
                .findByIdForUpdate(bookingId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Booking not found"
                        )
                );
    }

    private Payment lockPayment(Long paymentId) {
        if (paymentId == null) {
            throw badRequest(
                    "Payment ID is required"
            );
        }

        return paymentRepository
                .findByIdForUpdate(paymentId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Payment not found"
                        )
                );
    }

    // ==========================================
    // VALIDATION HELPERS
    // ==========================================

    private String normalizePaymentMethod(
            String method
    ) {
        String normalized = method == null
                || method.isBlank()
                ? "SIMULATED"
                : method.trim().toUpperCase(Locale.ROOT);

        if (!PAYMENT_METHODS.contains(normalized)) {
            throw badRequest(
                    "Unsupported payment method"
            );
        }

        return normalized;
    }

    private String requireText(
            String value,
            String field
    ) {
        if (value == null || value.isBlank()) {
            throw badRequest(
                    field + " is required"
            );
        }

        return value.trim();
    }

    private String generateTransactionReference() {
        return "TXN-" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .toUpperCase(Locale.ROOT);
    }

    // ==========================================
    // ENTITY TO DTO
    // ==========================================

    private PaymentProfile toProfile(Payment payment) {
        Booking booking = payment.getBooking();

        return new PaymentProfile(
                payment.getId(),
                booking.getId(),
                booking.getBookingReference(),
                booking.getPassenger().getId(),
                booking.getPassenger().getName(),
                booking.getTrip().getId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPaymentMethod(),
                payment.getStatus(),
                payment.getTransactionReference(),
                payment.getPaymentDate(),
                payment.getCreatedAt(),
                payment.getRefundedAt(),
                payment.getRefundAmount(),
                payment.getNotes()
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

    private ResponseStatusException conflict(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                message
        );
    }
}
