
package com.smartmove.backend.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "driver_locations")
@CompoundIndexes({

        // Efficiently retrieve the latest GPS update
        // for a particular trip.
        @CompoundIndex(
                name = "idx_location_trip_time",
                def = "{'tripId': 1, 'recordedAt': -1}"
        ),

        // Retrieve a driver's location history.
        @CompoundIndex(
                name = "idx_location_driver_time",
                def = "{'driverId': 1, 'recordedAt': -1}"
        ),

        // Retrieve vehicle tracking history.
        @CompoundIndex(
                name = "idx_location_vehicle_time",
                def = "{'vehicleId': 1, 'recordedAt': -1}"
        )
})
public class DriverLocation {

    // ==========================================
    // MONGODB DOCUMENT ID
    // ==========================================

    @Id
    private String id;

    // ==========================================
    // ORACLE ENTITY REFERENCES
    // ==========================================

    @Indexed
    @Field("tripId")
    private Long tripId;

    @Field("driverId")
    private Long driverId;

    @Field("vehicleId")
    private Long vehicleId;

    // ==========================================
    // GPS COORDINATES
    // ==========================================

    @Field("latitude")
    private Double latitude;

    @Field("longitude")
    private Double longitude;

    // GPS-reported accuracy in metres.
    @Field("accuracyMeters")
    private Double accuracyMeters;

    // Current movement speed in km/h.
    @Field("speedKmh")
    private Double speedKmh;

    // Heading in degrees, 0-360.
    // 0 = North, 90 = East.
    @Field("headingDegrees")
    private Double headingDegrees;

    // GPS-reported altitude in metres.
    @Field("altitudeMeters")
    private Double altitudeMeters;

    // ==========================================
    // TRIP TRACKING STATUS
    // ==========================================

    // Examples:
    // SCHEDULED, IN_PROGRESS, PAUSED,
    // COMPLETED, CANCELLED.
    @Field("tripStatus")
    private String tripStatus;

    // Indicates whether the device reported
    // that the vehicle was moving.
    @Field("moving")
    private Boolean moving;

    // ==========================================
    // TRACKING SOURCE
    // ==========================================

    // Examples:
    // DRIVER_PHONE, VEHICLE_GPS, ADMIN_SIMULATION.
    @Field("source")
    private String source;

    // Optional device identifier.
    // Avoid storing personally identifying
    // hardware IDs unnecessarily.
    @Field("deviceId")
    private String deviceId;

    // ==========================================
    // LOCATION UPDATE INFORMATION
    // ==========================================

    // Timestamp reported by the GPS device.
    @Indexed
    @Field("recordedAt")
    private LocalDateTime recordedAt;

    // Timestamp when the backend received
    // this update.
    @Field("receivedAt")
    private LocalDateTime receivedAt;

    // Optional distance from previous
    // accepted GPS update, in kilometres.
    @Field("distanceFromPreviousKm")
    private Double distanceFromPreviousKm;

    // ==========================================
    // ADDITIONAL INFORMATION
    // ==========================================

    @Field("notes")
    private String notes;

    @CreatedDate
    @Field("createdAt")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Field("updatedAt")
    private LocalDateTime updatedAt;

    // ==========================================
    // CONSTRUCTORS
    // ==========================================

    public DriverLocation() {
    }

    public DriverLocation(
            Long tripId,
            Long driverId,
            Long vehicleId,
            Double latitude,
            Double longitude
    ) {
        this.tripId = tripId;
        this.driverId = driverId;
        this.vehicleId = vehicleId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.source = "DRIVER_PHONE";
        this.receivedAt = LocalDateTime.now();
    }

    // ==========================================
    // DOCUMENT ID
    // ==========================================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    // ==========================================
    // ORACLE REFERENCES
    // ==========================================

    public Long getTripId() {
        return tripId;
    }

    public void setTripId(Long tripId) {
        this.tripId = tripId;
    }

    public Long getDriverId() {
        return driverId;
    }

    public void setDriverId(Long driverId) {
        this.driverId = driverId;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    // ==========================================
    // GPS COORDINATES
    // ==========================================

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getAccuracyMeters() {
        return accuracyMeters;
    }

    public void setAccuracyMeters(Double accuracyMeters) {
        this.accuracyMeters = accuracyMeters;
    }

    public Double getSpeedKmh() {
        return speedKmh;
    }

    public void setSpeedKmh(Double speedKmh) {
        this.speedKmh = speedKmh;
    }

    public Double getHeadingDegrees() {
        return headingDegrees;
    }

    public void setHeadingDegrees(Double headingDegrees) {
        this.headingDegrees = headingDegrees;
    }

    public Double getAltitudeMeters() {
        return altitudeMeters;
    }

    public void setAltitudeMeters(Double altitudeMeters) {
        this.altitudeMeters = altitudeMeters;
    }

    // ==========================================
    // TRACKING STATUS
    // ==========================================

    public String getTripStatus() {
        return tripStatus;
    }

    public void setTripStatus(String tripStatus) {
        this.tripStatus = tripStatus;
    }

    public Boolean getMoving() {
        return moving;
    }

    public void setMoving(Boolean moving) {
        this.moving = moving;
    }

    // ==========================================
    // TRACKING SOURCE
    // ==========================================

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    // ==========================================
    // TIMESTAMPS
    // ==========================================

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }

    public Double getDistanceFromPreviousKm() {
        return distanceFromPreviousKm;
    }

    public void setDistanceFromPreviousKm(
            Double distanceFromPreviousKm
    ) {
        this.distanceFromPreviousKm =
                distanceFromPreviousKm;
    }

    // ==========================================
    // ADDITIONAL INFORMATION
    // ==========================================

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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
