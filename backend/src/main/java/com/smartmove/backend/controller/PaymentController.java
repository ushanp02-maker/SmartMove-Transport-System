
package com.smartmove.backend.controller;

import com.smartmove.backend.service.PaymentService;
import com.smartmove.backend.service.PaymentService.CreatePaymentRequest;
import com.smartmove.backend.service.PaymentService.PaymentProfile;
import com.smartmove.backend.service.PaymentService.PaymentStatistics;
import com.smartmove.backend.service.PaymentService.RefundRequest;
import com.smartmove.backend.service.PaymentService.RevenueByMethod;
import com.smartmove.backend.service.PaymentService.RevenueByRoute;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

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
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@Validated
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // ==========================================
    // ADMIN: ALL PAYMENTS
    // ==========================================

    @GetMapping
    public ResponseEntity<List<PaymentProfile>> getAllPayments() {
        return ResponseEntity.ok(
                paymentService.getAllPayments()
        );
    }

    // ==========================================
    // ADMIN: PAGINATED PAYMENTS
    // ==========================================

    @GetMapping("/page")
    public ResponseEntity<Page<PaymentProfile>> getPaymentsPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        if (page < 0 || size < 1 || size > 100) {
            throw badRequest(
                    "Page must be nonnegative and size must be between 1 and 100"
            );
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Direction.DESC,
                        "createdAt"
                )
        );

        return ResponseEntity.ok(
                paymentService.getPayments(pageable)
        );
    }

    // ==========================================
    // ADMIN: PAYMENT STATISTICS
    // ==========================================

    @GetMapping("/stats")
    public ResponseEntity<PaymentStatistics> getPaymentStatistics() {
        return ResponseEntity.ok(
                paymentService.getPaymentStatistics()
        );
    }

    // ==========================================
    // ADMIN: PAYMENT COUNT
    // ==========================================

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> countPayments() {
        return ResponseEntity.ok(
                Map.of(
                        "totalPayments",
                        paymentService.getPaymentStatistics()
                                .totalPayments()
                )
        );
    }

    // ==========================================
    // ADMIN: FILTER PAYMENTS BY STATUS
    // ==========================================

    @GetMapping("/status/{status}")
    public ResponseEntity<List<PaymentProfile>> getPaymentsByStatus(
            @PathVariable String status
    ) {
        return ResponseEntity.ok(
                paymentService.getPaymentsByStatus(status)
        );
    }

    // ==========================================
    // ADMIN: FILTER PAYMENTS BY METHOD
    // ==========================================

    @GetMapping("/method/{method}")
    public ResponseEntity<List<PaymentProfile>> getPaymentsByMethod(
            @PathVariable String method
    ) {
        return ResponseEntity.ok(
                paymentService.getPaymentsByMethod(method)
        );
    }

    // ==========================================
    // ADMIN: PAYMENTS WITHIN DATE RANGE
    // ==========================================

    @GetMapping("/between")
    public ResponseEntity<List<PaymentProfile>> getPaymentsBetween(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime start,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime end
    ) {
        return ResponseEntity.ok(
                paymentService.getPaymentsBetween(start, end)
        );
    }

    // ==========================================
    // PASSENGER: PAYMENT HISTORY
    // ==========================================

    @GetMapping("/passenger/{passengerId}")
    public ResponseEntity<List<PaymentProfile>> getPassengerPayments(
            @PathVariable @Positive Long passengerId
    ) {
        return ResponseEntity.ok(
                paymentService.getPassengerPayments(passengerId)
        );
    }

    // ==========================================
    // BOOKING PAYMENT HISTORY
    // ==========================================

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<List<PaymentProfile>> getBookingPayments(
            @PathVariable @Positive Long bookingId
    ) {
        return ResponseEntity.ok(
                paymentService.getBookingPayments(bookingId)
        );
    }

    // ==========================================
    // TRANSACTION LOOKUP BY REFERENCE
    // ==========================================

    @GetMapping("/reference/{reference}")
    public ResponseEntity<PaymentProfile> getPaymentByReference(
            @PathVariable String reference
    ) {
        return ResponseEntity.ok(
                paymentService.getPaymentByReference(reference)
        );
    }

    // ==========================================
    // ADMIN: REVENUE BY ROUTE
    // ==========================================

    @GetMapping("/reports/revenue-by-route")
    public ResponseEntity<List<RevenueByRoute>> getRevenueByRoute() {
        return ResponseEntity.ok(
                paymentService.getRevenueByRoute()
        );
    }

    // ==========================================
    // ADMIN: REVENUE BY PAYMENT METHOD
    // ==========================================

    @GetMapping("/reports/revenue-by-method")
    public ResponseEntity<List<RevenueByMethod>> getRevenueByMethod() {
        return ResponseEntity.ok(
                paymentService.getRevenueByMethod()
        );
    }

    // ==========================================
    // ADMIN: NET REVENUE BY DATE RANGE
    // ==========================================

    @GetMapping("/reports/net-revenue")
    public ResponseEntity<Map<String, Object>> getNetRevenueBetween(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime start,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime end
    ) {
        BigDecimal netRevenue =
                paymentService.getNetRevenueBetween(start, end);

        return ResponseEntity.ok(
                Map.of(
                        "start", start.toString(),
                        "end", end.toString(),
                        "currency", "LKR",
                        "netRevenue", netRevenue
                )
        );
    }

    // ==========================================
    // PAYMENT LOOKUP BY ID
    // ==========================================

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentProfile> getPayment(
            @PathVariable @Positive Long paymentId
    ) {
        return ResponseEntity.ok(
                paymentService.getPaymentProfile(paymentId)
        );
    }

    // ==========================================
    // CREATE PAYMENT RECORD
    // ==========================================

    @PostMapping
    public ResponseEntity<PaymentProfile> createPayment(
            @Valid @RequestBody CreatePaymentPayload request
    ) {
        PaymentProfile payment = paymentService.createPayment(
                new CreatePaymentRequest(
                        request.bookingId(),
                        request.paymentMethod(),
                        request.notes()
                )
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(payment);
    }

    // ==========================================
    // ADMIN / TRUSTED SIMULATION:
    // COMPLETE PAYMENT
    // ==========================================

    @PatchMapping("/{paymentId}/complete")
    public ResponseEntity<PaymentProfile> completePayment(
            @PathVariable @Positive Long paymentId
    ) {
        return ResponseEntity.ok(
                paymentService.completePayment(paymentId)
        );
    }

    // ==========================================
    // ADMIN / TRUSTED SIMULATION:
    // FAIL PAYMENT
    // ==========================================

    @PatchMapping("/{paymentId}/fail")
    public ResponseEntity<PaymentProfile> failPayment(
            @PathVariable @Positive Long paymentId,
            @Valid @RequestBody FailPaymentPayload request
    ) {
        return ResponseEntity.ok(
                paymentService.failPayment(
                        paymentId,
                        request.reason()
                )
        );
    }

    // ==========================================
    // ADMIN: REFUND PAYMENT
    // ==========================================

    @PatchMapping("/{paymentId}/refund")
    public ResponseEntity<PaymentProfile> refundPayment(
            @PathVariable @Positive Long paymentId,
            @Valid @RequestBody RefundPaymentPayload request
    ) {
        return ResponseEntity.ok(
                paymentService.refundPayment(
                        paymentId,
                        new RefundRequest(
                                request.amount(),
                                request.reason()
                        )
                )
        );
    }

    // ==========================================
    // REQUEST DTOs
    // ==========================================

    public record CreatePaymentPayload(

            @NotNull(message = "Booking ID is required")
            @Positive
            Long bookingId,

            String paymentMethod,

            @Size(max = 500)
            String notes

    ) {
    }

    public record FailPaymentPayload(

            @Size(max = 500)
            String reason

    ) {
    }

    public record RefundPaymentPayload(

            @NotNull(message = "Refund amount is required")
            @DecimalMin(
                    value = "0.00",
                    inclusive = false
            )
            BigDecimal amount,

            @Size(max = 500)
            String reason

    ) {
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
