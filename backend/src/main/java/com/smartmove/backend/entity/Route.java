
package com.smartmove.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "ROUTES")
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "NAME", nullable = false, length = 150)
    private String name;

    @Column(name = "ORIGIN", nullable = false, length = 120)
    private String origin;

    @Column(name = "DESTINATION", nullable = false, length = 120)
    private String destination;

    @Column(name = "DISTANCE_KM", precision = 10, scale = 2)
    private BigDecimal distanceKm;

    @Column(name = "DURATION_MINUTES")
    private Integer durationMinutes;

    @Column(name = "BASE_FARE", nullable = false,
            precision = 12, scale = 2)
    private BigDecimal baseFare;

    @Column(name = "SERVICE_TYPE", nullable = false, length = 30)
    private String serviceType = "COMMUTER";

    @Column(name = "STATUS", nullable = false, length = 30)
    private String status = "ACTIVE";

    public Route() {
    }

    @PrePersist
    public void onCreate() {
        if (serviceType == null) {
            serviceType = "COMMUTER";
        }

        if (status == null) {
            status = "ACTIVE";
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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

    public BigDecimal getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(BigDecimal distanceKm) {
        this.distanceKm = distanceKm;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public BigDecimal getBaseFare() {
        return baseFare;
    }

    public void setBaseFare(BigDecimal baseFare) {
        this.baseFare = baseFare;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
