
package com.smartmove.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "PAYMENTS",
        indexes = {
                @Index(
                        name = "IDX_PAYMENT_BOOKING",
                        columnList = "BOOKING_ID"
                ),
                @Index(
                        name = "IDX_PAYMENT_STATUS",
                        columnList = "STATUS"
                ),
                @Index(
                        name = "IDX_PAYMENT_DATE",
                        columnList = "PAYMENT_DATE"
                )
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "UK_PAYMENT_TRANSACTION",
                        columnNames = "TRANSACTION_REFERENCE"
                )
        }
)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "BOOKING_ID", nullable = false)
    private Booking booking;

    @Column(
            name = "AMOUNT",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal amount;

    @Column(name = "CURRENCY", nullable = false, length = 3)
    private String currency = "LKR";

    @Column(name = "PAYMENT_METHOD", nullable = false, length = 40)
    private String paymentMethod = "SIMULATED";

    @Column(name = "STATUS", nullable = false, length = 30)
    private String status = "PENDING";

    @Column(
            name = "TRANSACTION_REFERENCE",
            unique = true,
            length = 80
    )
    private String transactionReference;

    @Column(name = "PAYMENT_DATE")
    private LocalDateTime paymentDate;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "REFUNDED_AT")
    private LocalDateTime refundedAt;

    @Column(
            name = "REFUND_AMOUNT",
            precision = 12,
            scale = 2
    )
    private BigDecimal refundAmount;

    @Column(name = "NOTES", length = 500)
    private String notes;

    public Payment() {
    }

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (currency == null) {
            currency = "LKR";
        }

        if (paymentMethod == null) {
            paymentMethod = "SIMULATED";
        }

        if (status == null) {
            status = "PENDING";
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(
            String transactionReference
    ) {
        this.transactionReference = transactionReference;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDateTime paymentDate) {
        this.paymentDate = paymentDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getRefundedAt() {
        return refundedAt;
    }

    public void setRefundedAt(LocalDateTime refundedAt) {
        this.refundedAt = refundedAt;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
