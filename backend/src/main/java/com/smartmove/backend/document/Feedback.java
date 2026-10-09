
package com.smartmove.backend.document;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "feedback")
@CompoundIndexes({
        @CompoundIndex(
                name = "feedback_trip_created_idx",
                def = "{'tripId': 1, 'createdAt': -1}"
        ),
        @CompoundIndex(
                name = "feedback_driver_created_idx",
                def = "{'driverId': 1, 'createdAt': -1}"
        ),
        @CompoundIndex(
                name = "feedback_status_created_idx",
                def = "{'status': 1, 'createdAt': -1}"
        )
})
public class Feedback {

    @Id
    private String id;

    // References to records stored in Oracle.
    // These are identifiers, not JPA relationships.

    @Indexed
    private Long passengerId;

    private Long tripId;

    private Long driverId;

    private Long routeId;

    @Indexed(unique = true, sparse = true)
    private Long bookingId;

    // Overall rating: 1 to 5.
    private Integer rating;

    // Optional individual ratings: 1 to 5.
    private Integer driverRating;

    private Integer vehicleRating;

    private Integer punctualityRating;

    private Integer comfortRating;

    // Passenger review details.
    private String title;

    private String comment;

    // Optional categories such as:
    // DRIVER_BEHAVIOUR, CLEANLINESS,
    // PUNCTUALITY, COMFORT, OTHER.
    private String category;

    // PENDING, APPROVED, REJECTED, HIDDEN.
    @Indexed
    private String status = "PENDING";

    // Admin moderation details.
    private Long moderatedByUserId;

    private LocalDateTime moderatedAt;

    private String moderationNotes;

    // Admin response to the passenger's review.
    private String adminResponse;

    private LocalDateTime adminRespondedAt;

    // Whether the passenger has edited their review.
    private Boolean edited = false;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public Feedback() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getPassengerId() {
        return passengerId;
    }

    public void setPassengerId(Long passengerId) {
        this.passengerId = passengerId;
    }

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

    public Long getRouteId() {
        return routeId;
    }

    public void setRouteId(Long routeId) {
        this.routeId = routeId;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public Integer getDriverRating() {
        return driverRating;
    }

    public void setDriverRating(Integer driverRating) {
        this.driverRating = driverRating;
    }

    public Integer getVehicleRating() {
        return vehicleRating;
    }

    public void setVehicleRating(Integer vehicleRating) {
        this.vehicleRating = vehicleRating;
    }

    public Integer getPunctualityRating() {
        return punctualityRating;
    }

    public void setPunctualityRating(
            Integer punctualityRating
    ) {
        this.punctualityRating = punctualityRating;
    }

    public Integer getComfortRating() {
        return comfortRating;
    }

    public void setComfortRating(Integer comfortRating) {
        this.comfortRating = comfortRating;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getModeratedByUserId() {
        return moderatedByUserId;
    }

    public void setModeratedByUserId(
            Long moderatedByUserId
    ) {
        this.moderatedByUserId = moderatedByUserId;
    }

    public LocalDateTime getModeratedAt() {
        return moderatedAt;
    }

    public void setModeratedAt(
            LocalDateTime moderatedAt
    ) {
        this.moderatedAt = moderatedAt;
    }

    public String getModerationNotes() {
        return moderationNotes;
    }

    public void setModerationNotes(
            String moderationNotes
    ) {
        this.moderationNotes = moderationNotes;
    }

    public String getAdminResponse() {
        return adminResponse;
    }

    public void setAdminResponse(String adminResponse) {
        this.adminResponse = adminResponse;
    }

    public LocalDateTime getAdminRespondedAt() {
        return adminRespondedAt;
    }

    public void setAdminRespondedAt(
            LocalDateTime adminRespondedAt
    ) {
        this.adminRespondedAt = adminRespondedAt;
    }

    public Boolean getEdited() {
        return edited;
    }

    public void setEdited(Boolean edited) {
        this.edited = edited;
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
