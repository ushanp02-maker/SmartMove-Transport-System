
package com.smartmove.backend.document;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "announcements")
@CompoundIndexes({
        @CompoundIndex(
                name = "announcement_status_publish_idx",
                def = "{'status': 1, 'publishAt': -1}"
        ),
        @CompoundIndex(
                name = "announcement_audience_publish_idx",
                def = "{'targetAudience': 1, 'publishAt': -1}"
        ),
        @CompoundIndex(
                name = "announcement_category_publish_idx",
                def = "{'category': 1, 'publishAt': -1}"
        )
})
public class Announcement {

    @Id
    private String id;

    @Indexed
    private String title;

    private String message;

    // GENERAL, SERVICE_UPDATE, ROUTE_DISRUPTION,
    // DELAY, MAINTENANCE, EMERGENCY, POLICY_UPDATE
    private String category = "GENERAL";

    // LOW, NORMAL, HIGH, URGENT
    private String priority = "NORMAL";

    // DRAFT, SCHEDULED, PUBLISHED, ARCHIVED
    private String status = "DRAFT";

    // ALL, PASSENGER, DRIVER, ADMIN
    private String targetAudience = "ALL";

    // Oracle route identifiers affected by the notice.
    private List<Long> affectedRouteIds = new ArrayList<>();

    // Oracle trip identifiers affected by the notice.
    private List<Long> affectedTripIds = new ArrayList<>();

    // Optional geographical targeting.
    private List<String> affectedCities = new ArrayList<>();

    // UserAccount ID from Oracle.
    private Long createdByUserId;

    private Long updatedByUserId;

    // Time at which the announcement becomes visible.
    private LocalDateTime publishAt;

    // Time after which it should no longer appear.
    private LocalDateTime expiresAt;

    // Actual time the announcement was published.
    private LocalDateTime publishedAt;

    // Optional additional information.
    private String actionLabel;

    private String actionUrl;

    // Can be used for an image or attachment URL.
    private String attachmentUrl;

    // Whether the notice should be highlighted.
    private Boolean pinned = false;

    // Whether the notice should be shown prominently.
    private Boolean important = false;

    // Used for administrative audit information.
    private String internalNotes;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public Announcement() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTargetAudience() {
        return targetAudience;
    }

    public void setTargetAudience(String targetAudience) {
        this.targetAudience = targetAudience;
    }

    public List<Long> getAffectedRouteIds() {
        return affectedRouteIds;
    }

    public void setAffectedRouteIds(
            List<Long> affectedRouteIds
    ) {
        this.affectedRouteIds = affectedRouteIds;
    }

    public List<Long> getAffectedTripIds() {
        return affectedTripIds;
    }

    public void setAffectedTripIds(
            List<Long> affectedTripIds
    ) {
        this.affectedTripIds = affectedTripIds;
    }

    public List<String> getAffectedCities() {
        return affectedCities;
    }

    public void setAffectedCities(
            List<String> affectedCities
    ) {
        this.affectedCities = affectedCities;
    }

    public Long getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(
            Long createdByUserId
    ) {
        this.createdByUserId = createdByUserId;
    }

    public Long getUpdatedByUserId() {
        return updatedByUserId;
    }

    public void setUpdatedByUserId(
            Long updatedByUserId
    ) {
        this.updatedByUserId = updatedByUserId;
    }

    public LocalDateTime getPublishAt() {
        return publishAt;
    }

    public void setPublishAt(
            LocalDateTime publishAt
    ) {
        this.publishAt = publishAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(
            LocalDateTime expiresAt
    ) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(
            LocalDateTime publishedAt
    ) {
        this.publishedAt = publishedAt;
    }

    public String getActionLabel() {
        return actionLabel;
    }

    public void setActionLabel(
            String actionLabel
    ) {
        this.actionLabel = actionLabel;
    }

    public String getActionUrl() {
        return actionUrl;
    }

    public void setActionUrl(
            String actionUrl
    ) {
        this.actionUrl = actionUrl;
    }

    public String getAttachmentUrl() {
        return attachmentUrl;
    }

    public void setAttachmentUrl(
            String attachmentUrl
    ) {
        this.attachmentUrl = attachmentUrl;
    }

    public Boolean getPinned() {
        return pinned;
    }

    public void setPinned(Boolean pinned) {
        this.pinned = pinned;
    }

    public Boolean getImportant() {
        return important;
    }

    public void setImportant(Boolean important) {
        this.important = important;
    }

    public String getInternalNotes() {
        return internalNotes;
    }

    public void setInternalNotes(
            String internalNotes
    ) {
        this.internalNotes = internalNotes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(
            LocalDateTime createdAt
    ) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(
            LocalDateTime updatedAt
    ) {
        this.updatedAt = updatedAt;
    }
}
