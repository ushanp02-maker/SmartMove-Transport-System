package com.smartmove.backend.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document(collection = "driver_issues")
public class DriverIssue {
    @Id
    private String id;
    private Long driverId;
    private Long reportedByAccountId;
    private Long tripId;
    private Long vehicleId;
    private String description;
    private String priority;
    private String status;
    private Instant createdAt;

    public DriverIssue() {}
    public DriverIssue(Long driverId, Long reportedByAccountId, Long tripId, Long vehicleId, String description, String priority) {
        this.driverId=driverId;
        this.reportedByAccountId=reportedByAccountId;
        this.tripId=tripId;
        this.vehicleId=vehicleId;
        this.description=description;
        this.priority=priority;
        this.status="OPEN";
        this.createdAt=Instant.now();
    }
    public String getId(){return id;}
    public Long getDriverId(){return driverId;}
    public Long getReportedByAccountId(){return reportedByAccountId;}
    public Long getTripId(){return tripId;}
    public Long getVehicleId(){return vehicleId;}
    public String getDescription(){return description;}
    public String getPriority(){return priority;}
    public String getStatus(){return status;}
    public Instant getCreatedAt(){return createdAt;}
    public void setStatus(String status){this.status=status;}
}
