
package com.smartmove.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.math.BigDecimal;

@Entity
@Table(
        name = "STAFF_TRANSPORT_REQUESTS",
        indexes = {
                @Index(
                        name = "IDX_STAFF_REQ_PASSENGER",
                        columnList = "PASSENGER_ID"
                ),
                @Index(
                        name = "IDX_STAFF_REQ_STATUS",
                        columnList = "STATUS"
                ),
                @Index(
                        name = "IDX_STAFF_REQ_DATE",
                        columnList = "REQUESTED_DATE"
                ),
                @Index(
                        name = "IDX_STAFF_REQ_TRIP",
                        columnList = "ASSIGNED_TRIP_ID"
                )
        }
)
public class StaffTransportRequest {

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
    // ORGANIZATION INFORMATION
    // ==========================================

    @Column(
            name = "ORGANIZATION_NAME",
            nullable = false,
            length = 150
    )
    private String organizationName;

    @Column(
            name = "CONTACT_PERSON",
            length = 120
    )
    private String contactPerson;

    @Column(
            name = "CONTACT_PHONE",
            length = 30
    )
    private String contactPhone;

    @Column(
            name = "CONTACT_EMAIL",
            length = 150
    )
    private String contactEmail;

    // ==========================================
    // TRANSPORT REQUEST DETAILS
    // ==========================================

    @Column(
            name = "ORIGIN",
            nullable = false,
            length = 200
    )
    private String origin;

    @Column(
            name = "DESTINATION",
            nullable = false,
            length = 200
    )
    private String destination;

    @Column(name = "ORIGIN_LATITUDE")
    private Double originLatitude;

    @Column(name = "ORIGIN_LONGITUDE")
    private Double originLongitude;

    @Column(name = "DEST_LATITUDE")
    private Double destinationLatitude;

    @Column(name = "DEST_LONGITUDE")
    private Double destinationLongitude;

    @Column(
            name = "REQUESTED_DATE",
            nullable = false
    )
    private LocalDate requestedDate;

    @Column(
            name = "PICKUP_TIME",
            nullable = false
    )
    private LocalTime pickupTime;

    @Column(name = "RETURN_TIME")
    private LocalTime returnTime;

    @Column(
            name = "PASSENGER_COUNT",
            nullable = false
    )
    private Integer passengerCount;

    // ONE_WAY or ROUND_TRIP
    @Column(
            name = "JOURNEY_TYPE",
            nullable = false,
            length = 30
    )
    private String journeyType = "ONE_WAY";

    // ONE_TIME, DAILY, WEEKDAYS, WEEKLY
    @Column(
            name = "FREQUENCY",
            nullable = false,
            length = 30
    )
    private String frequency = "ONE_TIME";

    @Column(name = "SERVICE_END_DATE")
    private LocalDate serviceEndDate;

    @Column(
            name = "SPECIAL_REQUIREMENTS",
            length = 2000
    )
    private String specialRequirements;

    // ==========================================
    // REQUEST APPROVAL
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
    // PRICING
    // ==========================================

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
    // AUDIT FIELDS
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

    @Column(name = "CANCELLED_AT")
    private LocalDateTime cancelledAt;

    // ==========================================
    // CONSTRUCTORS
    // ==========================================

    public StaffTransportRequest() {
    }

    // ==========================================
    // LIFECYCLE CALLBACKS
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

        if (journeyType == null) {
            journeyType = "ONE_WAY";
        }

        if (frequency == null) {
            frequency = "ONE_TIME";
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
    // ORGANIZATION INFORMATION
    // ==========================================

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    // ==========================================
    // ORIGIN AND DESTINATION
    // ==========================================

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public Double getOriginLatitude() {
        return originLatitude;
    }

    public void setOriginLatitude(Double originLatitude) {
        this.originLatitude = originLatitude;
    }

    public Double getOriginLongitude() {
        return originLongitude;
    }

    public void setOriginLongitude(Double originLongitude) {
        this.originLongitude = originLongitude;
    }

    public Double getDestinationLatitude() {
        return destinationLatitude;
    }

    public void setDestinationLatitude(Double destinationLatitude) {
        this.destinationLatitude = destinationLatitude;
    }

    public Double getDestinationLongitude() {
        return destinationLongitude;
    }

    public void setDestinationLongitude(Double destinationLongitude) {
        this.destinationLongitude = destinationLongitude;
    }

    // ==========================================
    // REQUESTED JOURNEY
    // ==========================================

    public LocalDate getRequestedDate() {
        return requestedDate;
    }

    public void setRequestedDate(LocalDate requestedDate) {
        this.requestedDate = requestedDate;
    }

    public LocalTime getPickupTime() {
        return pickupTime;
    }

    public void setPickupTime(LocalTime pickupTime) {
        this.pickupTime = pickupTime;
    }

    public LocalTime getReturnTime() {
        return returnTime;
    }

    public void setReturnTime(LocalTime returnTime) {
        this.returnTime = returnTime;
    }

    public Integer getPassengerCount() {
        return passengerCount;
    }

    public void setPassengerCount(Integer passengerCount) {
        this.passengerCount = passengerCount;
    }

    public String getJourneyType() {
        return journeyType;
    }

    public void setJourneyType(String journeyType) {
        this.journeyType = journeyType;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public LocalDate getServiceEndDate() {
        return serviceEndDate;
    }

    public void setServiceEndDate(LocalDate serviceEndDate) {
        this.serviceEndDate = serviceEndDate;
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
    // TRIP ASSIGNMENT
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
    // PRICING
    // ==========================================

    public BigDecimal getEstimatedFare() {
        return estimatedFare;
    }

    public void setEstimatedFare(BigDecimal estimatedFare) {
        this.estimatedFare = estimatedFare;
    }

    public BigDecimal getApprovedFare() {
        return approvedFare;
    }

    public void setApprovedFare(BigDecimal approvedFare) {
        this.approvedFare = approvedFare;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    // ==========================================
    // AUDIT FIELDS
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

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }
}
