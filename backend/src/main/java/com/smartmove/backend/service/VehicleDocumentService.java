
package com.smartmove.backend.service;

import com.smartmove.backend.document.VehicleDocument;
import com.smartmove.backend.repository.VehicleDocumentRepository;
import com.smartmove.backend.repository.VehicleRepository;
import com.smartmove.backend.repository.UserAccountRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class VehicleDocumentService {

    private static final Set<String> DOCUMENT_TYPES = Set.of(
            "REGISTRATION",
            "INSURANCE",
            "REVENUE_LICENSE",
            "FITNESS_CERTIFICATE",
            "INSPECTION",
            "EMISSION_TEST",
            "OTHER"
    );

    private static final Set<String> STATUSES = Set.of(
            "ACTIVE", "EXPIRED", "REVOKED", "ARCHIVED"
    );

    private static final Set<String> VERIFICATION_STATUSES = Set.of(
            "PENDING", "VERIFIED", "REJECTED"
    );

    private static final Set<String> REQUIRED_TYPES = Set.of(
            "REGISTRATION",
            "INSURANCE",
            "REVENUE_LICENSE",
            "FITNESS_CERTIFICATE"
    );

    private final VehicleDocumentRepository documentRepository;
    private final VehicleRepository vehicleRepository;
    private final UserAccountRepository userAccountRepository;

    public VehicleDocumentService(
            VehicleDocumentRepository documentRepository,
            VehicleRepository vehicleRepository,
            UserAccountRepository userAccountRepository
    ) {
        this.documentRepository = documentRepository;
        this.vehicleRepository = vehicleRepository;
        this.userAccountRepository = userAccountRepository;
    }

    // ==========================================
    // DATA TRANSFER OBJECTS
    // ==========================================

    public record VehicleDocumentProfile(
            String id,
            Long vehicleId,
            String documentType,
            String title,
            String description,
            String documentNumber,
            String issuingAuthority,
            LocalDate issueDate,
            LocalDate expiryDate,
            String status,
            String verificationStatus,
            String originalFileName,
            String storedFileName,
            String fileUrl,
            String contentType,
            Long fileSizeBytes,
            String fileChecksum,
            Long uploadedByUserId,
            Long verifiedByUserId,
            LocalDateTime verifiedAt,
            String verificationNotes,
            Boolean expiryReminderEnabled,
            Integer reminderDaysBeforeExpiry,
            List<String> tags,
            String internalNotes,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            boolean expired,
            boolean expiringSoon
    ) {}

    public record CreateVehicleDocumentRequest(
            Long vehicleId,
            String documentType,
            String title,
            String description,
            String documentNumber,
            String issuingAuthority,
            LocalDate issueDate,
            LocalDate expiryDate,
            String originalFileName,
            String storedFileName,
            String fileUrl,
            String contentType,
            Long fileSizeBytes,
            String fileChecksum,
            Boolean expiryReminderEnabled,
            Integer reminderDaysBeforeExpiry,
            List<String> tags,
            String internalNotes,
            Long uploadedByUserId
    ) {}

    public record UpdateVehicleDocumentRequest(
            String title,
            String description,
            String documentNumber,
            String issuingAuthority,
            LocalDate issueDate,
            LocalDate expiryDate,
            Boolean expiryReminderEnabled,
            Integer reminderDaysBeforeExpiry,
            List<String> tags,
            String internalNotes
    ) {}

    public record VerifyDocumentRequest(
            Long verifiedByUserId,
            String verificationStatus,
            String verificationNotes
    ) {}

    public record VehicleDocumentCompliance(
            Long vehicleId,
            boolean compliant,
            List<String> validDocumentTypes,
            List<String> missingOrInvalidTypes
    ) {}

    public record VehicleDocumentStatistics(
            long totalDocuments,
            long activeDocuments,
            long expiredStatusDocuments,
            long revokedDocuments,
            long archivedDocuments,
            long pendingVerification,
            long verifiedDocuments,
            long rejectedDocuments,
            long currentlyExpired,
            long expiringWithin30Days
    ) {}

    // ==========================================
    // CREATE DOCUMENT RECORD
    // ==========================================

    public VehicleDocumentProfile createDocument(
            CreateVehicleDocumentRequest request
    ) {
        if (request == null) {
            throw badRequest("Document details are required");
        }

        requireVehicle(request.vehicleId());
        requireUser(request.uploadedByUserId());

        String type = normalize(
                request.documentType(),
                DOCUMENT_TYPES,
                "Document type"
        );

        validateDates(
                request.issueDate(),
                request.expiryDate()
        );

        validateReminderDays(
                request.reminderDaysBeforeExpiry()
        );

        if (request.fileSizeBytes() != null
                && request.fileSizeBytes() <= 0) {
            throw badRequest("File size must be positive");
        }

        String documentNumber = optionalText(
                request.documentNumber(), 150
        );

        if (documentNumber != null
                && documentRepository
                .findByDocumentNumber(documentNumber)
                .isPresent()) {
            throw conflict("Document number already exists");
        }

        VehicleDocument document = new VehicleDocument();

        document.setVehicleId(request.vehicleId());
        document.setDocumentType(type);
        document.setTitle(
                requiredText(request.title(), "Title", 200)
        );
        document.setDescription(
                optionalText(request.description(), 2000)
        );
        document.setDocumentNumber(documentNumber);
        document.setIssuingAuthority(
                optionalText(request.issuingAuthority(), 200)
        );
        document.setIssueDate(request.issueDate());
        document.setExpiryDate(request.expiryDate());

        // This service records file metadata.
        // It does not upload or store actual file bytes.
        document.setOriginalFileName(
                optionalText(request.originalFileName(), 255)
        );
        document.setStoredFileName(
                optionalText(request.storedFileName(), 255)
        );
        document.setFileUrl(
                optionalText(request.fileUrl(), 1000)
        );
        document.setContentType(
                optionalText(request.contentType(), 100)
        );
        document.setFileSizeBytes(request.fileSizeBytes());
        document.setFileChecksum(
                optionalText(request.fileChecksum(), 128)
        );

        document.setUploadedByUserId(
                request.uploadedByUserId()
        );
        document.setVerificationStatus("PENDING");

        document.setStatus(
                isExpired(document) ? "EXPIRED" : "ACTIVE"
        );

        document.setExpiryReminderEnabled(
                request.expiryReminderEnabled() == null
                        ? true
                        : request.expiryReminderEnabled()
        );

        document.setReminderDaysBeforeExpiry(
                request.reminderDaysBeforeExpiry() == null
                        ? 30
                        : request.reminderDaysBeforeExpiry()
        );

        document.setTags(validateTags(request.tags()));
        document.setInternalNotes(
                optionalText(request.internalNotes(), 2000)
        );

        LocalDateTime now = LocalDateTime.now();
        document.setCreatedAt(now);
        document.setUpdatedAt(now);

        return toProfile(
                documentRepository.save(document)
        );
    }

    // ==========================================
    // UPDATE DOCUMENT DETAILS
    // ==========================================

    public VehicleDocumentProfile updateDocument(
            String documentId,
            UpdateVehicleDocumentRequest request
    ) {
        if (request == null) {
            throw badRequest("Update details are required");
        }

        VehicleDocument document = findDocument(documentId);

        if (!"ACTIVE".equals(document.getStatus())) {
            throw conflict(
                    "Only active documents can be edited"
            );
        }

        if ("VERIFIED".equals(
                document.getVerificationStatus()
        )) {
            throw conflict(
                    "Verified documents cannot be edited. "
                            + "Archive and create a replacement record."
            );
        }

        if (request.title() != null) {
            document.setTitle(
                    requiredText(request.title(), "Title", 200)
            );
        }

        if (request.description() != null) {
            document.setDescription(
                    optionalText(request.description(), 2000)
            );
        }

        if (request.documentNumber() != null) {
            String number = optionalText(
                    request.documentNumber(), 150
            );

            documentRepository.findByDocumentNumber(number)
                    .ifPresent(existing -> {
                        if (!existing.getId().equals(documentId)) {
                            throw conflict(
                                    "Document number already exists"
                            );
                        }
                    });

            document.setDocumentNumber(number);
        }

        if (request.issuingAuthority() != null) {
            document.setIssuingAuthority(
                    optionalText(request.issuingAuthority(), 200)
            );
        }

        LocalDate issueDate = request.issueDate() == null
                ? document.getIssueDate()
                : request.issueDate();

        LocalDate expiryDate = request.expiryDate() == null
                ? document.getExpiryDate()
                : request.expiryDate();

        validateDates(issueDate, expiryDate);

        document.setIssueDate(issueDate);
        document.setExpiryDate(expiryDate);

        if (request.expiryReminderEnabled() != null) {
            document.setExpiryReminderEnabled(
                    request.expiryReminderEnabled()
            );
        }

        if (request.reminderDaysBeforeExpiry() != null) {
            validateReminderDays(
                    request.reminderDaysBeforeExpiry()
            );
            document.setReminderDaysBeforeExpiry(
                    request.reminderDaysBeforeExpiry()
            );
        }

        if (request.tags() != null) {
            document.setTags(
                    validateTags(request.tags())
            );
        }

        if (request.internalNotes() != null) {
            document.setInternalNotes(
                    optionalText(request.internalNotes(), 2000)
            );
        }

        // Updated documents require verification again.
        document.setVerificationStatus("PENDING");
        document.setVerifiedByUserId(null);
        document.setVerifiedAt(null);
        document.setVerificationNotes(null);
        document.setUpdatedAt(LocalDateTime.now());

        if (isExpired(document)) {
            document.setStatus("EXPIRED");
        }

        return toProfile(
                documentRepository.save(document)
        );
    }

    // ==========================================
    // ADMIN: VERIFY OR REJECT DOCUMENT
    // ==========================================

    public VehicleDocumentProfile verifyDocument(
            String documentId,
            VerifyDocumentRequest request
    ) {
        if (request == null) {
            throw badRequest("Verification details are required");
        }

        requireUser(request.verifiedByUserId());

        String outcome = normalize(
                request.verificationStatus(),
                VERIFICATION_STATUSES,
                "Verification status"
        );

        if ("PENDING".equals(outcome)) {
            throw badRequest(
                    "Verification outcome must be VERIFIED or REJECTED"
            );
        }

        VehicleDocument document = findDocument(documentId);

        if (!"ACTIVE".equals(document.getStatus())
                || isExpired(document)) {
            throw conflict(
                    "Expired, revoked or archived documents "
                            + "cannot be verified"
            );
        }

        if (!"PENDING".equals(
                document.getVerificationStatus()
        )) {
            throw conflict(
                    "Only pending documents can be verified"
            );
        }

        document.setVerificationStatus(outcome);
        document.setVerifiedByUserId(
                request.verifiedByUserId()
        );
        document.setVerifiedAt(LocalDateTime.now());
        document.setVerificationNotes(
                optionalText(request.verificationNotes(), 1000)
        );
        document.setUpdatedAt(LocalDateTime.now());

        return toProfile(
                documentRepository.save(document)
        );
    }

    // ==========================================
    // ADMIN: REVOKE DOCUMENT
    // ==========================================

    public VehicleDocumentProfile revokeDocument(
            String documentId,
            String reason
    ) {
        VehicleDocument document = findDocument(documentId);

        if ("ARCHIVED".equals(document.getStatus())) {
            throw conflict("Archived documents cannot be revoked");
        }

        document.setStatus("REVOKED");
        document.setVerificationStatus("REJECTED");
        document.setVerificationNotes(
                requiredText(reason, "Revocation reason", 1000)
        );
        document.setUpdatedAt(LocalDateTime.now());

        return toProfile(
                documentRepository.save(document)
        );
    }

    // ==========================================
    // ADMIN: ARCHIVE DOCUMENT
    // ==========================================

    public VehicleDocumentProfile archiveDocument(
            String documentId
    ) {
        VehicleDocument document = findDocument(documentId);

        document.setStatus("ARCHIVED");
        document.setUpdatedAt(LocalDateTime.now());

        return toProfile(
                documentRepository.save(document)
        );
    }

    // ==========================================
    // AUTOMATIC EXPIRY PROCESSING
    // ==========================================

    public int markExpiredDocuments() {
        List<VehicleDocument> expired =
                documentRepository.findExpiredActiveDocuments(
                        LocalDate.now()
                );

        int updated = 0;

        for (VehicleDocument document : expired) {
            document.setStatus("EXPIRED");
            document.setUpdatedAt(LocalDateTime.now());
            documentRepository.save(document);
            updated++;
        }

        return updated;
    }

    // ==========================================
    // DOCUMENT LOOKUPS
    // ==========================================

    public VehicleDocumentProfile getDocumentById(
            String documentId
    ) {
        return toProfile(findDocument(documentId));
    }

    public List<VehicleDocumentProfile> getVehicleDocuments(
            Long vehicleId
    ) {
        requireVehicle(vehicleId);

        return documentRepository
                .findByVehicleIdOrderByCreatedAtDesc(vehicleId)
                .stream()
                .map(this::toProfile)
                .toList();
    }

    public List<VehicleDocumentProfile> getVehicleDocumentsByType(
            Long vehicleId,
            String documentType
    ) {
        requireVehicle(vehicleId);

        return documentRepository
                .findByVehicleIdAndDocumentTypeIgnoreCaseOrderByCreatedAtDesc(
                        vehicleId,
                        normalize(
                                documentType,
                                DOCUMENT_TYPES,
                                "Document type"
                        )
                )
                .stream()
                .map(this::toProfile)
                .toList();
    }

    public Page<VehicleDocumentProfile> getAllDocuments(
            Pageable pageable
    ) {
        return documentRepository
                .findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toProfile);
    }

    public Page<VehicleDocumentProfile> getByStatus(
            String status,
            Pageable pageable
    ) {
        return documentRepository
                .findByStatusIgnoreCase(
                        normalize(status, STATUSES, "Status"),
                        pageable
                )
                .map(this::toProfile);
    }

    public Page<VehicleDocumentProfile> getByVerificationStatus(
            String status,
            Pageable pageable
    ) {
        return documentRepository
                .findByVerificationStatusIgnoreCase(
                        normalize(
                                status,
                                VERIFICATION_STATUSES,
                                "Verification status"
                        ),
                        pageable
                )
                .map(this::toProfile);
    }

    public Page<VehicleDocumentProfile> getPendingVerification(
            Pageable pageable
    ) {
        return documentRepository
                .findByVerificationStatusIgnoreCaseOrderByCreatedAtAsc(
                        "PENDING",
                        pageable
                )
                .map(this::toProfile);
    }

    public Page<VehicleDocumentProfile> searchDocuments(
            String keyword,
            Pageable pageable
    ) {
        if (keyword == null || keyword.isBlank()) {
            return getAllDocuments(pageable);
        }

        return documentRepository
                .findByTitleContainingIgnoreCase(
                        keyword.trim(),
                        pageable
                )
                .map(this::toProfile);
    }

    public List<VehicleDocumentProfile> getUploadedByUser(
            Long userId
    ) {
        requireUser(userId);

        return documentRepository
                .findByUploadedByUserIdOrderByCreatedAtDesc(
                        userId
                )
                .stream()
                .map(this::toProfile)
                .toList();
    }

    // ==========================================
    // EXPIRY REPORTS
    // ==========================================

    public List<VehicleDocumentProfile> getExpiredDocuments() {
        return documentRepository
                .findByExpiryDateBeforeOrderByExpiryDateAsc(
                        LocalDate.now()
                )
                .stream()
                .map(this::toProfile)
                .toList();
    }

    public List<VehicleDocumentProfile> getExpiringDocuments(
            int daysAhead
    ) {
        validateDaysAhead(daysAhead);

        LocalDate today = LocalDate.now();

        return documentRepository
                .findByStatusIgnoreCaseAndExpiryDateBetweenOrderByExpiryDateAsc(
                        "ACTIVE",
                        today,
                        today.plusDays(daysAhead)
                )
                .stream()
                .map(this::toProfile)
                .toList();
    }

    public List<VehicleDocumentProfile> getExpiryReminders(
            int daysAhead
    ) {
        validateDaysAhead(daysAhead);

        LocalDate today = LocalDate.now();

        return documentRepository
                .findExpiryReminderCandidates(
                        today,
                        today.plusDays(daysAhead)
                )
                .stream()
                .filter(document ->
                        document.getExpiryDate() != null
                                && !document.getExpiryDate().isAfter(
                                today.plusDays(
                                        document.getReminderDaysBeforeExpiry() == null
                                                ? 30
                                                : document.getReminderDaysBeforeExpiry()
                                )
                        )
                )
                .map(this::toProfile)
                .toList();
    }

    // ==========================================
    // VEHICLE DOCUMENT COMPLIANCE
    // ==========================================

    public VehicleDocumentCompliance getVehicleDocumentCompliance(
            Long vehicleId
    ) {
        requireVehicle(vehicleId);

        LocalDate today = LocalDate.now();

        List<String> valid = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        for (String type : REQUIRED_TYPES.stream().sorted().toList()) {
            long count = documentRepository
                    .countValidVerifiedDocuments(
                            vehicleId,
                            type,
                            today
                    );

            if (count > 0) {
                valid.add(type);
            } else {
                missing.add(type);
            }
        }

        return new VehicleDocumentCompliance(
                vehicleId,
                missing.isEmpty(),
                valid,
                missing
        );
    }

    // ==========================================
    // ADMIN: DOCUMENT STATISTICS
    // ==========================================

    public VehicleDocumentStatistics getStatistics() {
        LocalDate today = LocalDate.now();

        return new VehicleDocumentStatistics(
                documentRepository.count(),
                documentRepository.countByStatusIgnoreCase(
                        "ACTIVE"
                ),
                documentRepository.countByStatusIgnoreCase(
                        "EXPIRED"
                ),
                documentRepository.countByStatusIgnoreCase(
                        "REVOKED"
                ),
                documentRepository.countByStatusIgnoreCase(
                        "ARCHIVED"
                ),
                documentRepository.countByVerificationStatusIgnoreCase(
                        "PENDING"
                ),
                documentRepository.countByVerificationStatusIgnoreCase(
                        "VERIFIED"
                ),
                documentRepository.countByVerificationStatusIgnoreCase(
                        "REJECTED"
                ),
                documentRepository
                        .findByExpiryDateBeforeOrderByExpiryDateAsc(
                                today
                        )
                        .size(),
                documentRepository
                        .findByStatusIgnoreCaseAndExpiryDateBetweenOrderByExpiryDateAsc(
                                "ACTIVE",
                                today,
                                today.plusDays(30)
                        )
                        .size()
        );
    }

    // ==========================================
    // INTERNAL HELPERS
    // ==========================================

    private VehicleDocument findDocument(String id) {
        if (id == null || id.isBlank()) {
            throw badRequest("Document ID is required");
        }

        return documentRepository.findById(id)
                .orElseThrow(() ->
                        notFound("Vehicle document not found")
                );
    }

    private void requireVehicle(Long vehicleId) {
        if (vehicleId == null || vehicleId <= 0
                || !vehicleRepository.existsById(vehicleId)) {
            throw notFound("Vehicle not found");
        }
    }

    private void requireUser(Long userId) {
        if (userId == null || userId <= 0
                || !userAccountRepository.existsById(userId)) {
            throw notFound("User account not found");
        }
    }

    private void validateDates(
            LocalDate issueDate,
            LocalDate expiryDate
    ) {
        if (issueDate != null
                && issueDate.isAfter(LocalDate.now())) {
            throw badRequest(
                    "Issue date cannot be in the future"
            );
        }

        if (issueDate != null
                && expiryDate != null
                && expiryDate.isBefore(issueDate)) {
            throw badRequest(
                    "Expiry date cannot be before issue date"
            );
        }
    }

    private void validateReminderDays(Integer days) {
        if (days != null && (days < 0 || days > 365)) {
            throw badRequest(
                    "Reminder days must be between 0 and 365"
            );
        }
    }

    private void validateDaysAhead(int days) {
        if (days < 0 || days > 365) {
            throw badRequest(
                    "Days ahead must be between 0 and 365"
            );
        }
    }

    private boolean isExpired(VehicleDocument document) {
        return document.getExpiryDate() != null
                && document.getExpiryDate()
                .isBefore(LocalDate.now());
    }

    private boolean isExpiringSoon(VehicleDocument document) {
        if (document.getExpiryDate() == null
                || isExpired(document)) {
            return false;
        }

        return !document.getExpiryDate().isAfter(
                LocalDate.now().plusDays(30)
        );
    }

    private String normalize(
            String value,
            Set<String> allowed,
            String field
    ) {
        if (value == null || value.isBlank()) {
            throw badRequest(field + " is required");
        }

        String result = value.trim()
                .toUpperCase(Locale.ROOT);

        if (!allowed.contains(result)) {
            throw badRequest(
                    "Invalid " + field.toLowerCase(Locale.ROOT)
            );
        }

        return result;
    }

    private String requiredText(
            String value,
            String field,
            int maxLength
    ) {
        if (value == null || value.isBlank()) {
            throw badRequest(field + " is required");
        }

        String result = value.trim();

        if (result.length() > maxLength) {
            throw badRequest(field + " is too long");
        }

        return result;
    }

    private String optionalText(
            String value,
            int maxLength
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String result = value.trim();

        if (result.length() > maxLength) {
            throw badRequest("Text exceeds maximum length");
        }

        return result;
    }

    private List<String> validateTags(List<String> tags) {
        if (tags == null) {
            return new ArrayList<>();
        }

        if (tags.size() > 20) {
            throw badRequest("Maximum 20 tags allowed");
        }

        List<String> result = new ArrayList<>();

        for (String tag : tags) {
            String value = requiredText(
                    tag, "Tag", 50
            );

            if (!result.contains(value)) {
                result.add(value);
            }
        }

        return result;
    }

    private VehicleDocumentProfile toProfile(
            VehicleDocument d
    ) {
        return new VehicleDocumentProfile(
                d.getId(),
                d.getVehicleId(),
                d.getDocumentType(),
                d.getTitle(),
                d.getDescription(),
                d.getDocumentNumber(),
                d.getIssuingAuthority(),
                d.getIssueDate(),
                d.getExpiryDate(),
                d.getStatus(),
                d.getVerificationStatus(),
                d.getOriginalFileName(),
                d.getStoredFileName(),
                d.getFileUrl(),
                d.getContentType(),
                d.getFileSizeBytes(),
                d.getFileChecksum(),
                d.getUploadedByUserId(),
                d.getVerifiedByUserId(),
                d.getVerifiedAt(),
                d.getVerificationNotes(),
                d.getExpiryReminderEnabled(),
                d.getReminderDaysBeforeExpiry(),
                d.getTags(),
                d.getInternalNotes(),
                d.getCreatedAt(),
                d.getUpdatedAt(),
                isExpired(d),
                isExpiringSoon(d)
        );
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST, message
        );
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(
                HttpStatus.CONFLICT, message
        );
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND, message
        );
    }
}
