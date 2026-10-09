
package com.smartmove.backend.document;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "vehicle_documents")
@CompoundIndexes({
        @CompoundIndex(
                name = "vehicle_document_type_idx",
                def = "{'vehicleId': 1, 'documentType': 1}"
        ),
        @CompoundIndex(
                name = "vehicle_document_expiry_idx",
                def = "{'status': 1, 'expiryDate': 1}"
        ),
        @CompoundIndex(
                name = "vehicle_document_verification_idx",
                def = "{'verificationStatus': 1, 'createdAt': -1}"
        )
})
public class VehicleDocument {

    @Id
    private String id;

    // References Vehicle.id from Oracle.
    @Indexed
    private Long vehicleId;

    // REGISTRATION, INSURANCE, REVENUE_LICENSE,
    // FITNESS_CERTIFICATE, INSPECTION,
    // EMISSION_TEST, OTHER
    private String documentType;

    private String title;

    private String description;

    // Optional certificate or policy number.
    private String documentNumber;

    // Organization that issued the document.
    private String issuingAuthority;

    private LocalDate issueDate;

    private LocalDate expiryDate;

    // ACTIVE, EXPIRED, REVOKED, ARCHIVED
    private String status = "ACTIVE";

    // PENDING, VERIFIED, REJECTED
    private String verificationStatus = "PENDING";

    // File metadata. The actual file will be
    // stored separately from this document.
    private String originalFileName;

    private String storedFileName;

    private String fileUrl;

    private String contentType;

    private Long fileSizeBytes;

    // Optional integrity checksum.
    private String fileChecksum;

    // Oracle UserAccount IDs.
    private Long uploadedByUserId;

    private Long verifiedByUserId;

    private LocalDateTime verifiedAt;

    private String verificationNotes;

    // Reminder settings.
    private Boolean expiryReminderEnabled = true;

    private Integer reminderDaysBeforeExpiry = 30;

    // Optional tags for searching.
    private List<String> tags = new ArrayList<>();

    // Additional notes for administrators.
    private String internalNotes;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public VehicleDocument() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public String getIssuingAuthority() {
        return issuingAuthority;
    }

    public void setIssuingAuthority(String issuingAuthority) {
        this.issuingAuthority = issuingAuthority;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getStoredFileName() {
        return storedFileName;
    }

    public void setStoredFileName(String storedFileName) {
        this.storedFileName = storedFileName;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(Long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public String getFileChecksum() {
        return fileChecksum;
    }

    public void setFileChecksum(String fileChecksum) {
        this.fileChecksum = fileChecksum;
    }

    public Long getUploadedByUserId() {
        return uploadedByUserId;
    }

    public void setUploadedByUserId(Long uploadedByUserId) {
        this.uploadedByUserId = uploadedByUserId;
    }

    public Long getVerifiedByUserId() {
        return verifiedByUserId;
    }

    public void setVerifiedByUserId(Long verifiedByUserId) {
        this.verifiedByUserId = verifiedByUserId;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public String getVerificationNotes() {
        return verificationNotes;
    }

    public void setVerificationNotes(String verificationNotes) {
        this.verificationNotes = verificationNotes;
    }

    public Boolean getExpiryReminderEnabled() {
        return expiryReminderEnabled;
    }

    public void setExpiryReminderEnabled(
            Boolean expiryReminderEnabled
    ) {
        this.expiryReminderEnabled = expiryReminderEnabled;
    }

    public Integer getReminderDaysBeforeExpiry() {
        return reminderDaysBeforeExpiry;
    }

    public void setReminderDaysBeforeExpiry(
            Integer reminderDaysBeforeExpiry
    ) {
        this.reminderDaysBeforeExpiry = reminderDaysBeforeExpiry;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getInternalNotes() {
        return internalNotes;
    }

    public void setInternalNotes(String internalNotes) {
        this.internalNotes = internalNotes;
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
