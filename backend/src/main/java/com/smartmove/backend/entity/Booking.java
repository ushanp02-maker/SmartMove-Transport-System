
package com.smartmove.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "BOOKINGS",
        indexes = {
                @Index(
                        name = "IDX_BOOKING_PASSENGER",
                        columnList = "PASSENGER_ID"
                ),
                @Index(
                        name = "IDX_BOOKING_TRIP",
                        columnList = "TRIP_ID"
                ),
                @Index(
                        name = "IDX_BOOKING_STATUS",
                        columnList = "STATUS"
                )
        }
)
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PASSENGER_ID", nullable = false)
    private Passenger passenger;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "TRIP_ID", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "BOARDING_STOP_ID", nullable = false)
    private RouteStop boardingStop;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "DESTINATION_STOP_ID", nullable = false)
    private RouteStop destinationStop;

    @Column(name = "SEAT_COUNT", nullable = false)
    private Integer seatCount;

    @Column(
            name = "TOTAL_FARE",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal totalFare;

    @Column(name = "STATUS", nullable = false, length = 30)
    private String status = "PENDING";

    @Column(name = "BOOKING_REFERENCE", unique = true, length = 50)
    private String bookingReference;

    @Column(name = "BOOKED_AT", nullable = false)
    private LocalDateTime bookedAt;

    @Column(name = "CANCELLED_AT")
    private LocalDateTime cancelledAt;

    public Booking() {
    }

    @PrePersist
    public void onCreate() {
        if (bookedAt == null) {
            bookedAt = LocalDateTime.now();
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

    public Passenger getPassenger() {
        return passenger;
    }

    public void setPassenger(Passenger passenger) {
        this.passenger = passenger;
    }

    public Trip getTrip() {
        return trip;
    }

    public void setTrip(Trip trip) {
        this.trip = trip;
    }

    public RouteStop getBoardingStop() {
        return boardingStop;
    }

    public void setBoardingStop(RouteStop boardingStop) {
        this.boardingStop = boardingStop;
    }

    public RouteStop getDestinationStop() {
        return destinationStop;
    }

    public void setDestinationStop(RouteStop destinationStop) {
        this.destinationStop = destinationStop;
    }

    public Integer getSeatCount() {
        return seatCount;
    }

    public void setSeatCount(Integer seatCount) {
        this.seatCount = seatCount;
    }

    public BigDecimal getTotalFare() {
        return totalFare;
    }

    public void setTotalFare(BigDecimal totalFare) {
        this.totalFare = totalFare;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getBookingReference() {
        return bookingReference;
    }

    public void setBookingReference(String bookingReference) {
        this.bookingReference = bookingReference;
    }

    public LocalDateTime getBookedAt() {
        return bookedAt;
    }

    public void setBookedAt(LocalDateTime bookedAt) {
        this.bookedAt = bookedAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }
}
