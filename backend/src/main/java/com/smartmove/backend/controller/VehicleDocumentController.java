
package com.smartmove.backend.controller;

import com.smartmove.backend.service.VehicleDocumentService;

import com.smartmove.backend.service.VehicleDocumentService.VehicleDocumentProfile;
import com.smartmove.backend.service.VehicleDocumentService.CreateVehicleDocumentRequest;
import com.smartmove.backend.service.VehicleDocumentService.UpdateVehicleDocumentRequest;
import com.smartmove.backend.service.VehicleDocumentService.VerifyDocumentRequest;
import com.smartmove.backend.service.VehicleDocumentService.VehicleDocumentCompliance;
import com.smartmove.backend.service.VehicleDocumentService.VehicleDocumentStatistics;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vehicle-documents")
@Validated
public class VehicleDocumentController {

    private final VehicleDocumentService documentService;

    public VehicleDocumentController(
            VehicleDocumentService documentService
    ) {
        this.documentService = documentService;
    }

    // ==========================================
    // ADMIN: ALL VEHICLE DOCUMENTS
    // ==========================================

    @GetMapping
    public ResponseEntity<Page<VehicleDocumentProfile>>
    getAllDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                documentService.getAllDocuments(
                        createPageable(page, size)
                )
        );
    }

    // ==========================================
    // ADMIN: DOCUMENT STATISTICS
    // ==========================================

    @GetMapping("/stats")
    public ResponseEntity<VehicleDocumentStatistics>
    getStatistics() {
        return ResponseEntity.ok(
                documentService.getStatistics()
        );
    }

    // ==========================================
    // ADMIN: DOCUMENT COUNT
    // ==========================================

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>>
    getDocumentCount() {
        return ResponseEntity.ok(
                Map.of(
                        "totalDocuments",
                        documentService
                                .getStatistics()
                                .totalDocuments()
                )
        );
    }

    // ==========================================
    // ADMIN: SEARCH DOCUMENTS
    // ==========================================

    @GetMapping("/search")
    public ResponseEntity<Page<VehicleDocumentProfile>>
    searchDocuments(
            @RequestParam(defaultValue = "")
            String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                documentService.searchDocuments(
                        keyword,
                        createPageable(page, size)
                )
        );
    }

    // ==========================================
    // ADMIN: FILTER BY DOCUMENT STATUS
    // ==========================================

    @GetMapping("/status/{status}")
    public ResponseEntity<Page<VehicleDocumentProfile>>
    getByStatus(
            @PathVariable @NotBlank String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                documentService.getByStatus(
                        status,
                        createPageable(page, size)
                )
        );
    }

    // ==========================================
    // ADMIN: FILTER BY VERIFICATION STATUS
    // ==========================================

    @GetMapping("/verification/{status}")
    public ResponseEntity<Page<VehicleDocumentProfile>>
    getByVerificationStatus(
            @PathVariable @NotBlank String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                documentService.getByVerificationStatus(
                        status,
                        createPageable(page, size)
                )
        );
    }

    // ==========================================
    // ADMIN: PENDING VERIFICATION QUEUE
    // ==========================================

    @GetMapping("/verification/pending")
    public ResponseEntity<Page<VehicleDocumentProfile>>
    getPendingVerification(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                documentService.getPendingVerification(
                        createPageable(page, size)
                )
        );
    }

    // ==========================================
    // ADMIN: EXPIRED DOCUMENTS
    // ==========================================

    @GetMapping("/expired")
    public ResponseEntity<List<VehicleDocumentProfile>>
    getExpiredDocuments() {
        return ResponseEntity.ok(
                documentService.getExpiredDocuments()
        );
    }

    // ==========================================
    // ADMIN: EXPIRING DOCUMENTS
    // ==========================================

    @GetMapping("/expiring")
    public ResponseEntity<List<VehicleDocumentProfile>>
    getExpiringDocuments(
            @RequestParam(defaultValue = "30")
            @Min(0)
            @Max(365)
            int days
    ) {
        return ResponseEntity.ok(
                documentService.getExpiringDocuments(days)
        );
    }

    // ==========================================
    // ADMIN: EXPIRY REMINDER CANDIDATES
    // ==========================================

    @GetMapping("/reminders")
    public ResponseEntity<List<VehicleDocumentProfile>>
    getExpiryReminders(
            @RequestParam(defaultValue = "30")
            @Min(0)
            @Max(365)
            int days
    ) {
        return ResponseEntity.ok(
                documentService.getExpiryReminders(days)
        );
    }

    // ==========================================
    // ADMIN: DOCUMENTS UPLOADED BY USER
    // ==========================================

    @GetMapping("/uploaded-by/{userId}")
    public ResponseEntity<List<VehicleDocumentProfile>>
    getUploadedByUser(
            @PathVariable @Positive Long userId
    ) {
        return ResponseEntity.ok(
                documentService.getUploadedByUser(userId)
        );
    }

    // ==========================================
    // ADMIN: VEHICLE DOCUMENT HISTORY
    // ==========================================

    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<List<VehicleDocumentProfile>>
    getVehicleDocuments(
            @PathVariable @Positive Long vehicleId
    ) {
        return ResponseEntity.ok(
                documentService.getVehicleDocuments(vehicleId)
        );
    }

    // ==========================================
    // ADMIN: VEHICLE DOCUMENTS BY TYPE
    // ==========================================

    @GetMapping("/vehicle/{vehicleId}/type/{documentType}")
    public ResponseEntity<List<VehicleDocumentProfile>>
    getVehicleDocumentsByType(
            @PathVariable @Positive Long vehicleId,
            @PathVariable @NotBlank String documentType
    ) {
        return ResponseEntity.ok(
                documentService.getVehicleDocumentsByType(
                        vehicleId,
                        documentType
                )
        );
    }

    // ==========================================
    // ADMIN: DOCUMENT COMPLIANCE
    // ==========================================

    @GetMapping("/vehicle/{vehicleId}/compliance")
    public ResponseEntity<VehicleDocumentCompliance>
    getVehicleDocumentCompliance(
            @PathVariable @Positive Long vehicleId
    ) {
        return ResponseEntity.ok(
                documentService.getVehicleDocumentCompliance(
                        vehicleId
                )
        );
    }

    // ==========================================
    // ADMIN: DOCUMENT DETAILS
    // ==========================================

    @GetMapping("/{documentId}")
    public ResponseEntity<VehicleDocumentProfile>
    getDocumentById(
            @PathVariable String documentId
    ) {
        return ResponseEntity.ok(
                documentService.getDocumentById(documentId)
        );
    }

    // ==========================================
    // ADMIN: REGISTER DOCUMENT METADATA
    // ==========================================

    @PostMapping
    public ResponseEntity<VehicleDocumentProfile>
    createDocument(
            @Valid @RequestBody CreateDocumentPayload request
    ) {
        VehicleDocumentProfile document =
                documentService.createDocument(
                        new CreateVehicleDocumentRequest(
                                request.vehicleId(),
                                request.documentType(),
                                request.title(),
                                request.description(),
                                request.documentNumber(),
                                request.issuingAuthority(),
                                request.issueDate(),
                                request.expiryDate(),
                                request.originalFileName(),
                                request.storedFileName(),
                                request.fileUrl(),
                                request.contentType(),
                                request.fileSizeBytes(),
                                request.fileChecksum(),
                                request.expiryReminderEnabled(),
                                request.reminderDaysBeforeExpiry(),
                                request.tags(),
                                request.internalNotes(),
                                request.uploadedByUserId()
                        )
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(document);
    }

    // ==========================================
    // ADMIN: UPDATE DOCUMENT DETAILS
    // ==========================================

    @PutMapping("/{documentId}")
    public ResponseEntity<VehicleDocumentProfile>
    updateDocument(
            @PathVariable String documentId,
            @Valid @RequestBody UpdateDocumentPayload request
    ) {
        return ResponseEntity.ok(
                documentService.updateDocument(
                        documentId,
                        new UpdateVehicleDocumentRequest(
                                request.title(),
                                request.description(),
                                request.documentNumber(),
                                request.issuingAuthority(),
                                request.issueDate(),
                                request.expiryDate(),
                                request.expiryReminderEnabled(),
                                request.reminderDaysBeforeExpiry(),
                                request.tags(),
                                request.internalNotes()
                        )
                )
        );
    }

    // ==========================================
    // ADMIN: VERIFY OR REJECT DOCUMENT
    // ==========================================

    @PatchMapping("/{documentId}/verify")
    public ResponseEntity<VehicleDocumentProfile>
    verifyDocument(
            @PathVariable String documentId,
            @Valid @RequestBody VerifyDocumentPayload request
    ) {
        return ResponseEntity.ok(
                documentService.verifyDocument(
                        documentId,
                        new VerifyDocumentRequest(
                                request.verifiedByUserId(),
                                request.verificationStatus(),
                                request.verificationNotes()
                        )
                )
        );
    }

    // ==========================================
    // ADMIN: REVOKE DOCUMENT
    // ==========================================

    @PatchMapping("/{documentId}/revoke")
    public ResponseEntity<VehicleDocumentProfile>
    revokeDocument(
            @PathVariable String documentId,
            @Valid @RequestBody RevokeDocumentPayload request
    ) {
        return ResponseEntity.ok(
                documentService.revokeDocument(
                        documentId,
                        request.reason()
                )
        );
    }

    // ==========================================
    // ADMIN: ARCHIVE DOCUMENT
    // ==========================================

    @PatchMapping("/{documentId}/archive")
    public ResponseEntity<VehicleDocumentProfile>
    archiveDocument(
            @PathVariable String documentId
    ) {
        return ResponseEntity.ok(
                documentService.archiveDocument(documentId)
        );
    }

    // ==========================================
    // ADMIN: PROCESS EXPIRED DOCUMENTS
    // ==========================================

    @PostMapping("/jobs/mark-expired")
    public ResponseEntity<Map<String, Integer>>
    markExpiredDocuments() {
        int updated = documentService.markExpiredDocuments();

        return ResponseEntity.ok(
                Map.of("expiredDocumentsUpdated", updated)
        );
    }

    // ==========================================
    // REQUEST DTOs
    // ==========================================

    public record CreateDocumentPayload(

            @NotNull(message = "Vehicle ID is required")
            @Positive
            Long vehicleId,

            @NotBlank(message = "Document type is required")
            String documentType,

            @NotBlank(message = "Document title is required")
            @Size(max = 200)
            String title,

            @Size(max = 2000)
            String description,

            @Size(max = 150)
            String documentNumber,

            @Size(max = 200)
            String issuingAuthority,

            LocalDate issueDate,

            LocalDate expiryDate,

            @Size(max = 255)
            String originalFileName,

            @Size(max = 255)
            String storedFileName,

            @Size(max = 1000)
            String fileUrl,

            @Size(max = 100)
            String contentType,

            @Positive
            Long fileSizeBytes,

            @Size(max = 128)
            String fileChecksum,

            Boolean expiryReminderEnabled,

            @Min(0)
            @Max(365)
            Integer reminderDaysBeforeExpiry,

            @Size(max = 20)
            List<@NotBlank @Size(max = 50) String> tags,

            @Size(max = 2000)
            String internalNotes,

            @NotNull(message = "Uploader user ID is required")
            @Positive
            Long uploadedByUserId

    ) {
    }

    public record UpdateDocumentPayload(

            @Size(max = 200)
            String title,

            @Size(max = 2000)
            String description,

            @Size(max = 150)
            String documentNumber,

            @Size(max = 200)
            String issuingAuthority,

            LocalDate issueDate,

            LocalDate expiryDate,

            Boolean expiryReminderEnabled,

            @Min(0)
            @Max(365)
            Integer reminderDaysBeforeExpiry,

            @Size(max = 20)
            List<@NotBlank @Size(max = 50) String> tags,

            @Size(max = 2000)
            String internalNotes

    ) {
    }

    public record VerifyDocumentPayload(

            @NotNull(message = "Verifier user ID is required")
            @Positive
            Long verifiedByUserId,

            @NotBlank(message = "Verification status is required")
            String verificationStatus,

            @Size(max = 1000)
            String verificationNotes

    ) {
    }

    public record RevokeDocumentPayload(

            @NotBlank(message = "Revocation reason is required")
            @Size(max = 1000)
            String reason

    ) {
    }

    // ==========================================
    // PAGINATION HELPER
    // ==========================================

    private Pageable createPageable(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw badRequest(
                    "Page must be nonnegative and size must be between 1 and 100"
            );
        }

        return PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Direction.DESC,
                        "createdAt"
                )
        );
    }

    // ==========================================
    // ERROR HELPER
    // ==========================================

    private ResponseStatusException badRequest(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }
}
