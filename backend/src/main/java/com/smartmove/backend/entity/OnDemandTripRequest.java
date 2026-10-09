
package com.smartmove.backend.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "ON_DEMAND_TRIP_REQUESTS",
        indexes = {
                @Index(
                        name = "IDX_OD_REQUEST_PASSENGER",
                        columnList = "PASSENGER_ID"
                ),
                @Index(
                        name = "IDX_OD_REQUEST_STATUS",
                        columnList = "STATUS"
                ),
                @Index(
                        name = "IDX_OD_REQUEST_PICKUP",
                        columnList = "REQUESTED_PICKUP_TIME"
                ),
                @Index(
                        name = "IDX_OD_REQUEST_TRIP",
                        columnList = "ASSIGNED_TRIP_ID"
                )
        }
)
public class OnDemandTripRequest {

    // ==========================================
    // PRIMARY KEY
    // ==========================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==========================================
    // REQUESTING PASSENGER
    // ==========================================

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "PASSENGER_ID",
            nullable = false
    )
    private Passenger passenger;

    // ==========================================
    // PICKUP LOCATION
    // ==========================================

    @Column(
            name = "PICKUP_ADDRESS",
            nullable = false,
            length = 250
    )
    private String pickupAddress;

    @Column(
            name = "PICKUP_LATITUDE",
            nullable = false
    )
    private Double pickupLatitude;

    @Column(
            name = "PICKUP_LONGITUDE",
            nullable = false
    )
    private Double pickupLongitude;

    // ==========================================
    // DESTINATION LOCATION
    // ==========================================

    @Column(
            name = "DESTINATION_ADDRESS",
            nullable = false,
            length = 250
    )
    private String destinationAddress;

    @Column(
            name = "DESTINATION_LATITUDE",
            nullable = false
    )
    private Double destinationLatitude;

    @Column(
            name = "DESTINATION_LONGITUDE",
            nullable = false
    )
    private Double destinationLongitude;

    // ==========================================
    // JOURNEY DETAILS
    // ==========================================

    @Column(
            name = "REQUESTED_PICKUP_TIME",
            nullable = false
    )
    private LocalDateTime requestedPickupTime;

    @Column(
            name = "PASSENGER_COUNT",
            nullable = false
    )
    private Integer passengerCount = 1;

    // STANDARD, PREMIUM, STAFF, OTHER
    @Column(
            name = "SERVICE_TYPE",
            nullable = false,
            length = 30
    )
    private String serviceType = "STANDARD";

    @Column(
            name = "SPECIAL_REQUIREMENTS",
            length = 2000
    )
    private String specialRequirements;

    // ==========================================
    // ESTIMATED JOURNEY INFORMATION
    // ==========================================

    @Column(
            name = "ESTIMATED_DISTANCE_KM",
            precision = 10,
            scale = 2
    )
    private BigDecimal estimatedDistanceKm;

    @Column(name = "ESTIMATED_DURATION_MINUTES")
    private Integer estimatedDurationMinutes;

    @Column(
            name = "ESTIMATED_FARE",
            precision = 12,
            scale = 2
    )
    private BigDecimal estimatedFare;

    @Column(
            name = "APPROVED_FARE",
            precision = 12,
            scale = 2
    )
    private BigDecimal approvedFare;

    @Column(
            name = "CURRENCY",
            nullable = false,
            length = 10
    )
    private String currency = "LKR";

    // ==========================================
    // APPROVAL WORKFLOW
    // ==========================================

    // PENDING, APPROVED, REJECTED,
    // ASSIGNED, CANCELLED, COMPLETED
    @Column(
            name = "STATUS",
            nullable = false,
            length = 30
    )
    private String status = "PENDING";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REVIEWED_BY_ACCOUNT_ID")
    private UserAccount reviewedBy;

    @Column(name = "REVIEWED_AT")
    private LocalDateTime reviewedAt;

    @Column(
            name = "REVIEW_NOTES",
            length = 2000
    )
    private String reviewNotes;

    // ==========================================
    // TRIP ASSIGNMENT
    // ==========================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ASSIGNED_TRIP_ID")
    private Trip assignedTrip;

    @Column(name = "ASSIGNED_AT")
    private LocalDateTime assignedAt;

    // ==========================================
    // CANCELLATION
    // ==========================================

    @Column(name = "CANCELLED_AT")
    private LocalDateTime cancelledAt;

    @Column(
            name = "CANCELLATION_REASON",
            length = 1000
    )
    private String cancellationReason;

    // ==========================================
    // AUDIT INFORMATION
    // ==========================================

    @Column(
            name = "CREATED_AT",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "UPDATED_AT",
            nullable = false
    )
    private LocalDateTime updatedAt;

    // ==========================================
    // CONSTRUCTORS
    // ==========================================

    public OnDemandTripRequest() {
    }

    // ==========================================
    // JPA LIFECYCLE CALLBACKS
    // ==========================================

    @PrePersist
    public void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now;

        if (status == null) {
            status = "PENDING";
        }

        if (passengerCount == null) {
            passengerCount = 1;
        }

        if (serviceType == null) {
            serviceType = "STANDARD";
        }

        if (currency == null) {
            currency = "LKR";
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ==========================================
    // PRIMARY KEY
    // ==========================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    // ==========================================
    // PASSENGER
    // ==========================================

    public Passenger getPassenger() {
        return passenger;
    }

    public void setPassenger(Passenger passenger) {
        this.passenger = passenger;
    }

    // ==========================================
    // PICKUP LOCATION
    // ==========================================

    public String getPickupAddress() {
        return pickupAddress;
    }

    public void setPickupAddress(String pickupAddress) {
        this.pickupAddress = pickupAddress;
    }

    public Double getPickupLatitude() {
        return pickupLatitude;
    }

    public void setPickupLatitude(Double pickupLatitude) {
        this.pickupLatitude = pickupLatitude;
    }

    public Double getPickupLongitude() {
        return pickupLongitude;
    }

    public void setPickupLongitude(Double pickupLongitude) {
        this.pickupLongitude = pickupLongitude;
    }

    // ==========================================
    // DESTINATION LOCATION
    // ==========================================

    public String getDestinationAddress() {
        return destinationAddress;
    }

    public void setDestinationAddress(String destinationAddress) {
        this.destinationAddress = destinationAddress;
    }

    public Double getDestinationLatitude() {
        return destinationLatitude;
    }

    public void setDestinationLatitude(
            Double destinationLatitude
    ) {
        this.destinationLatitude = destinationLatitude;
    }

    public Double getDestinationLongitude() {
        return destinationLongitude;
    }

    public void setDestinationLongitude(
            Double destinationLongitude
    ) {
        this.destinationLongitude = destinationLongitude;
    }

    // ==========================================
    // JOURNEY DETAILS
    // ==========================================

    public LocalDateTime getRequestedPickupTime() {
        return requestedPickupTime;
    }

    public void setRequestedPickupTime(
            LocalDateTime requestedPickupTime
    ) {
        this.requestedPickupTime = requestedPickupTime;
    }

    public Integer getPassengerCount() {
        return passengerCount;
    }

    public void setPassengerCount(Integer passengerCount) {
        this.passengerCount = passengerCount;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getSpecialRequirements() {
        return specialRequirements;
    }

    public void setSpecialRequirements(
            String specialRequirements
    ) {
        this.specialRequirements = specialRequirements;
    }

    // ==========================================
    // ESTIMATED JOURNEY INFORMATION
    // ==========================================

    public BigDecimal getEstimatedDistanceKm() {
        return estimatedDistanceKm;
    }

    public void setEstimatedDistanceKm(
            BigDecimal estimatedDistanceKm
    ) {
        this.estimatedDistanceKm = estimatedDistanceKm;
    }

    public Integer getEstimatedDurationMinutes() {
        return estimatedDurationMinutes;
    }

    public void setEstimatedDurationMinutes(
            Integer estimatedDurationMinutes
    ) {
        this.estimatedDurationMinutes =
                estimatedDurationMinutes;
    }

    public BigDecimal getEstimatedFare() {
        return estimatedFare;
    }

    public void setEstimatedFare(
            BigDecimal estimatedFare
    ) {
        this.estimatedFare = estimatedFare;
    }

    public BigDecimal getApprovedFare() {
        return approvedFare;
    }

    public void setApprovedFare(
            BigDecimal approvedFare
    ) {
        this.approvedFare = approvedFare;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    // ==========================================
    // APPROVAL
    // ==========================================

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public UserAccount getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(UserAccount reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getReviewNotes() {
        return reviewNotes;
    }

    public void setReviewNotes(String reviewNotes) {
        this.reviewNotes = reviewNotes;
    }

    // ==========================================
    // ASSIGNED TRIP
    // ==========================================

    public Trip getAssignedTrip() {
        return assignedTrip;
    }

    public void setAssignedTrip(Trip assignedTrip) {
        this.assignedTrip = assignedTrip;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    // ==========================================
    // CANCELLATION
    // ==========================================

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(
            String cancellationReason
    ) {
        this.cancellationReason = cancellationReason;
    }

    // ==========================================
    // AUDIT INFORMATION
    // ==========================================

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
