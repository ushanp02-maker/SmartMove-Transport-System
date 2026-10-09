
package com.smartmove.backend.repository;

import com.smartmove.backend.document.VehicleDocument;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleDocumentRepository
        extends MongoRepository<VehicleDocument, String> {

    // ==========================================
    // VEHICLE DOCUMENT MANAGEMENT
    // ==========================================

    // Retrieve all documents belonging to a vehicle.
    List<VehicleDocument> findByVehicleIdOrderByCreatedAtDesc(
            Long vehicleId
    );

    // Retrieve documents by vehicle and document type.
    List<VehicleDocument>
    findByVehicleIdAndDocumentTypeIgnoreCaseOrderByCreatedAtDesc(
            Long vehicleId,
            String documentType
    );

    // Find a document by its certificate/policy number.
    Optional<VehicleDocument> findByDocumentNumber(
            String documentNumber
    );

    // Find documents by their issuing authority.
    List<VehicleDocument>
    findByIssuingAuthorityContainingIgnoreCase(
            String issuingAuthority
    );

    // Search document titles.
    Page<VehicleDocument> findByTitleContainingIgnoreCase(
            String keyword,
            Pageable pageable
    );

    // Retrieve documents uploaded by a user.
    List<VehicleDocument> findByUploadedByUserIdOrderByCreatedAtDesc(
            Long userId
    );

    // Count documents belonging to a vehicle.
    long countByVehicleId(Long vehicleId);

    // ==========================================
    // DOCUMENT VERIFICATION
    // ==========================================

    // Admin: documents awaiting verification.
    Page<VehicleDocument>
    findByVerificationStatusIgnoreCaseOrderByCreatedAtAsc(
            String verificationStatus,
            Pageable pageable
    );

    // Admin: filter by verification status.
    Page<VehicleDocument> findByVerificationStatusIgnoreCase(
            String verificationStatus,
            Pageable pageable
    );

    // Documents verified by a particular administrator.
    List<VehicleDocument>
    findByVerifiedByUserIdOrderByVerifiedAtDesc(
            Long verifiedByUserId
    );

    // Count pending, verified or rejected documents.
    long countByVerificationStatusIgnoreCase(
            String verificationStatus
    );

    // ==========================================
    // DOCUMENT STATUS
    // ==========================================

    // Filter by document status.
    Page<VehicleDocument> findByStatusIgnoreCase(
            String status,
            Pageable pageable
    );

    // Retrieve documents of a particular type.
    List<VehicleDocument> findByDocumentTypeIgnoreCase(
            String documentType
    );

    // Retrieve active documents for a vehicle.
    List<VehicleDocument>
    findByVehicleIdAndStatusIgnoreCaseOrderByCreatedAtDesc(
            Long vehicleId,
            String status
    );

    // ==========================================
    // EXPIRY TRACKING
    // ==========================================

    // Documents that have already expired,
    // regardless of their stored status.
    List<VehicleDocument> findByExpiryDateBeforeOrderByExpiryDateAsc(
            LocalDate today
    );

    // Documents expiring within a date range.
    List<VehicleDocument> findByExpiryDateBetweenOrderByExpiryDateAsc(
            LocalDate startDate,
            LocalDate endDate
    );

    // Active documents expiring within a date range.
    List<VehicleDocument>
    findByStatusIgnoreCaseAndExpiryDateBetweenOrderByExpiryDateAsc(
            String status,
            LocalDate startDate,
            LocalDate endDate
    );

    // Find active documents that are already expired.
    @Query("""
        {
          'status': 'ACTIVE',
          'expiryDate': { '$lt': ?0 }
        }
        """)
    List<VehicleDocument> findExpiredActiveDocuments(
            LocalDate today
    );

    // Find active, verified documents that are
    // valid on the supplied date.
    //
    // Documents with no expiry date are considered
    // non-expiring for this query.
    @Query("""
        {
          'vehicleId': ?0,
          'documentType': ?1,
          'status': 'ACTIVE',
          'verificationStatus': 'VERIFIED',
          '$or': [
            { 'expiryDate': null },
            { 'expiryDate': { '$gte': ?2 } }
          ]
        }
        """)
    List<VehicleDocument> findValidVerifiedDocuments(
            Long vehicleId,
            String documentType,
            LocalDate today
    );

    // ==========================================
    // EXPIRY REMINDERS
    // ==========================================

    // Find documents eligible for expiry reminders.
    // The service layer will apply each document's
    // reminderDaysBeforeExpiry setting.
    @Query("""
        {
          'status': 'ACTIVE',
          'expiryReminderEnabled': true,
          'expiryDate': {
            '$gte': ?0,
            '$lte': ?1
          }
        }
        """)
    List<VehicleDocument> findExpiryReminderCandidates(
            LocalDate today,
            LocalDate endDate
    );

    // ==========================================
    // VEHICLE COMPLIANCE
    // ==========================================

    // Retrieve all verified documents for a vehicle.
    List<VehicleDocument>
    findByVehicleIdAndVerificationStatusIgnoreCase(
            Long vehicleId,
            String verificationStatus
    );

    // Count valid verified documents of a given type.
    @Query(
            value = """
            {
              'vehicleId': ?0,
              'documentType': ?1,
              'status': 'ACTIVE',
              'verificationStatus': 'VERIFIED',
              '$or': [
                { 'expiryDate': null },
                { 'expiryDate': { '$gte': ?2 } }
              ]
            }
            """,
            count = true
    )
    long countValidVerifiedDocuments(
            Long vehicleId,
            String documentType,
            LocalDate today
    );

    // ==========================================
    // ADMIN REPORTS
    // ==========================================

    // All documents, newest first.
    Page<VehicleDocument> findAllByOrderByCreatedAtDesc(
            Pageable pageable
    );

    // Filter documents by type and verification status.
    Page<VehicleDocument>
    findByDocumentTypeIgnoreCaseAndVerificationStatusIgnoreCase(
            String documentType,
            String verificationStatus,
            Pageable pageable
    );

    // Count documents of a given type.
    long countByDocumentTypeIgnoreCase(
            String documentType
    );

    // Count documents with a given status.
    long countByStatusIgnoreCase(
            String status
    );
}
