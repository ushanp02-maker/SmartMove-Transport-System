
package com.smartmove.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(
        name = "ROUTE_STOPS",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "UK_ROUTE_STOP_ORDER",
                        columnNames = {"ROUTE_ID", "STOP_ORDER"}
                )
        },
        indexes = {
                @Index(
                        name = "IDX_ROUTE_STOPS_ROUTE",
                        columnList = "ROUTE_ID"
                )
        }
)
public class RouteStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ROUTE_ID", nullable = false)
    private Route route;

    @Column(name = "STOP_NAME", nullable = false, length = 120)
    private String stopName;

    @Column(name = "STOP_ORDER", nullable = false)
    private Integer stopOrder;

    @Column(name = "LATITUDE", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "LONGITUDE", precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "DISTANCE_FROM_START_KM", precision = 10, scale = 2)
    private BigDecimal distanceFromStartKm;

    @Column(name = "MINUTES_FROM_START")
    private Integer minutesFromStart;

    public RouteStop() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Route getRoute() {
        return route;
    }

    public void setRoute(Route route) {
        this.route = route;
    }

    public String getStopName() {
        return stopName;
    }

    public void setStopName(String stopName) {
        this.stopName = stopName;
    }

    public Integer getStopOrder() {
        return stopOrder;
    }

    public void setStopOrder(Integer stopOrder) {
        this.stopOrder = stopOrder;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public BigDecimal getDistanceFromStartKm() {
        return distanceFromStartKm;
    }

    public void setDistanceFromStartKm(
            BigDecimal distanceFromStartKm
    ) {
        this.distanceFromStartKm = distanceFromStartKm;
    }

    public Integer getMinutesFromStart() {
        return minutesFromStart;
    }

    public void setMinutesFromStart(Integer minutesFromStart) {
        this.minutesFromStart = minutesFromStart;
    }
}
