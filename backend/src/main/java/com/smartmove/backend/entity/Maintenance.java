
package com.smartmove.backend.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "MAINTENANCE",
        indexes = {
                @Index(
                        name = "IDX_MAINTENANCE_VEHICLE",
                        columnList = "VEHICLE_ID"
                ),
                @Index(
                        name = "IDX_MAINTENANCE_STATUS",
                        columnList = "STATUS"
                ),
                @Index(
                        name = "IDX_MAINTENANCE_SCHEDULED",
                        columnList = "SCHEDULED_DATE"
                ),
                @Index(
                        name = "IDX_MAINTENANCE_COMPLETED",
                        columnList = "COMPLETED_DATE"
                )
        }
)
public class Maintenance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Vehicle undergoing maintenance
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "VEHICLE_ID", nullable = false)
    private Vehicle vehicle;

    // Examples: ROUTINE_SERVICE, REPAIR, INSPECTION,
    // TYRE_REPLACEMENT, EMERGENCY_REPAIR
    @Column(
            name = "MAINTENANCE_TYPE",
            nullable = false,
            length = 50
    )
    private String maintenanceType;

    @Column(
            name = "DESCRIPTION",
            nullable = false,
            length = 1000
    )
    private String description;

    // SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED
    @Column(
            name = "STATUS",
            nullable = false,
            length = 30
    )
    private String status = "SCHEDULED";

    @Column(
            name = "PRIORITY",
            nullable = false,
            length = 20
    )
    private String priority = "NORMAL";

    @Column(name = "SCHEDULED_DATE", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "STARTED_DATE")
    private LocalDate startedDate;

    @Column(name = "COMPLETED_DATE")
    private LocalDate completedDate;

    // Service provider or workshop
    @Column(name = "SERVICE_PROVIDER", length = 150)
    private String serviceProvider;

    // Estimated cost before maintenance
    @Column(
            name = "ESTIMATED_COST",
            precision = 12,
            scale = 2
    )
    private BigDecimal estimatedCost;

    // Actual cost after maintenance
    @Column(
            name = "ACTUAL_COST",
            precision = 12,
            scale = 2
    )
    private BigDecimal actualCost;

    @Column(
            name = "CURRENCY",
            nullable = false,
            length = 3
    )
    private String currency = "LKR";

    // Mileage recorded when maintenance is performed
    @Column(name = "ODOMETER_READING")
    private Integer odometerReading;

    // Suggested date for the vehicle's next service
    @Column(name = "NEXT_SERVICE_DATE")
    private LocalDate nextServiceDate;

    // Optional notes from the workshop or administrator
    @Column(name = "NOTES", length = 1000)
    private String notes;

    // User account that created the record
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REPORTED_BY_USER_ID")
    private UserAccount reportedBy;

    @Column(
            name = "CREATED_AT",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    public Maintenance() {
    }

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now;

        if (status == null) {
            status = "SCHEDULED";
        }

        if (priority == null) {
            priority = "NORMAL";
        }

        if (currency == null) {
            currency = "LKR";
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public String getMaintenanceType() {
        return maintenanceType;
    }

    public void setMaintenanceType(String maintenanceType) {
        this.maintenanceType = maintenanceType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public LocalDate getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(LocalDate scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    public LocalDate getStartedDate() {
        return startedDate;
    }

    public void setStartedDate(LocalDate startedDate) {
        this.startedDate = startedDate;
    }

    public LocalDate getCompletedDate() {
        return completedDate;
    }

    public void setCompletedDate(LocalDate completedDate) {
        this.completedDate = completedDate;
    }

    public String getServiceProvider() {
        return serviceProvider;
    }

    public void setServiceProvider(String serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    public BigDecimal getEstimatedCost() {
        return estimatedCost;
    }

    public void setEstimatedCost(BigDecimal estimatedCost) {
        this.estimatedCost = estimatedCost;
    }

    public BigDecimal getActualCost() {
        return actualCost;
    }

    public void setActualCost(BigDecimal actualCost) {
        this.actualCost = actualCost;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Integer getOdometerReading() {
        return odometerReading;
    }

    public void setOdometerReading(Integer odometerReading) {
        this.odometerReading = odometerReading;
    }

    public LocalDate getNextServiceDate() {
        return nextServiceDate;
    }

    public void setNextServiceDate(LocalDate nextServiceDate) {
        this.nextServiceDate = nextServiceDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public UserAccount getReportedBy() {
        return reportedBy;
    }

    public void setReportedBy(UserAccount reportedBy) {
        this.reportedBy = reportedBy;
    }

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
