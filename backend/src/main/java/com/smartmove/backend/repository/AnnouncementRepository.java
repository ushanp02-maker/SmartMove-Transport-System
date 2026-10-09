
package com.smartmove.backend.repository;

import com.smartmove.backend.document.Announcement;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AnnouncementRepository
        extends MongoRepository<Announcement, String> {

    // ==========================================
    // ADMIN ANNOUNCEMENT MANAGEMENT
    // ==========================================

    // Retrieve all announcements, newest first.
    Page<Announcement> findAllByOrderByCreatedAtDesc(
            Pageable pageable
    );

    // Filter announcements by status.
    Page<Announcement> findByStatusIgnoreCase(
            String status,
            Pageable pageable
    );

    // Filter announcements by category.
    Page<Announcement> findByCategoryIgnoreCase(
            String category,
            Pageable pageable
    );

    // Filter announcements by priority.
    Page<Announcement> findByPriorityIgnoreCase(
            String priority,
            Pageable pageable
    );

    // Retrieve announcements created by a user.
    List<Announcement> findByCreatedByUserIdOrderByCreatedAtDesc(
            Long userId
    );

    // Search announcement titles.
    Page<Announcement> findByTitleContainingIgnoreCase(
            String keyword,
            Pageable pageable
    );

    // Count announcements by status.
    long countByStatusIgnoreCase(
            String status
    );

    // ==========================================
    // SCHEDULED ANNOUNCEMENTS
    // ==========================================

    // Retrieve announcements ready to publish.
    List<Announcement> findByStatusAndPublishAtLessThanEqual(
            String status,
            LocalDateTime now
    );

    // Retrieve published announcements that expired.
    List<Announcement> findByStatusAndExpiresAtLessThanEqual(
            String status,
            LocalDateTime now
    );

    // ==========================================
    // ACTIVE ANNOUNCEMENTS
    // ==========================================

    // Retrieve currently visible announcements.
    // The caller must pass the current date/time.
    //
    // An announcement is active when:
    // - Status is PUBLISHED
    // - Publish time has been reached, or is absent
    // - Expiration time has not been reached, or is absent
    @Query("""
        {
          'status': 'PUBLISHED',
          '$and': [
            {
              '$or': [
                { 'publishAt': null },
                { 'publishAt': { '$lte': ?0 } }
              ]
            },
            {
              '$or': [
                { 'expiresAt': null },
                { 'expiresAt': { '$gt': ?0 } }
              ]
            }
          ]
        }
        """)
    List<Announcement> findActiveAnnouncements(
            LocalDateTime now
    );

    // ==========================================
    // PASSENGER AND DRIVER ANNOUNCEMENTS
    // ==========================================

    // Retrieve active notices for a specific audience.
    // For example: PASSENGER or DRIVER.
    //
    // Includes announcements intended for ALL.
    @Query("""
        {
          'status': 'PUBLISHED',
          'targetAudience': { '$in': ['ALL', ?0] },
          '$and': [
            {
              '$or': [
                { 'publishAt': null },
                { 'publishAt': { '$lte': ?1 } }
              ]
            },
            {
              '$or': [
                { 'expiresAt': null },
                { 'expiresAt': { '$gt': ?1 } }
              ]
            }
          ]
        }
        """)
    List<Announcement> findActiveAnnouncementsForAudience(
            String audience,
            LocalDateTime now
    );

    // ==========================================
    // ROUTE ANNOUNCEMENTS
    // ==========================================

    // Retrieve active notices affecting a route.
    // Includes route-specific and system-wide notices.
    //
    // Notices with an empty affectedRouteIds list
    // and empty affectedTripIds list are treated
    // as general announcements.
    @Query("""
        {
          'status': 'PUBLISHED',
          'targetAudience': { '$in': ['ALL', ?1] },
          '$and': [
            {
              '$or': [
                { 'publishAt': null },
                { 'publishAt': { '$lte': ?2 } }
              ]
            },
            {
              '$or': [
                { 'expiresAt': null },
                { 'expiresAt': { '$gt': ?2 } }
              ]
            },
            {
              '$or': [
                { 'affectedRouteIds': ?0 },
                {
                  'affectedRouteIds': { '$size': 0 },
                  'affectedTripIds': { '$size': 0 }
                }
              ]
            }
          ]
        }
        """)
    List<Announcement> findActiveAnnouncementsForRoute(
            Long routeId,
            String audience,
            LocalDateTime now
    );

    // ==========================================
    // TRIP ANNOUNCEMENTS
    // ==========================================

    // Retrieve active notices affecting a trip.
    // Includes trip-specific and system-wide notices.
    @Query("""
        {
          'status': 'PUBLISHED',
          'targetAudience': { '$in': ['ALL', ?1] },
          '$and': [
            {
              '$or': [
                { 'publishAt': null },
                { 'publishAt': { '$lte': ?2 } }
              ]
            },
            {
              '$or': [
                { 'expiresAt': null },
                { 'expiresAt': { '$gt': ?2 } }
              ]
            },
            {
              '$or': [
                { 'affectedTripIds': ?0 },
                {
                  'affectedRouteIds': { '$size': 0 },
                  'affectedTripIds': { '$size': 0 }
                }
              ]
            }
          ]
        }
        """)
    List<Announcement> findActiveAnnouncementsForTrip(
            Long tripId,
            String audience,
            LocalDateTime now
    );

    // ==========================================
    // ROUTE AND TRIP DISRUPTIONS
    // ==========================================

    // Find all announcements referencing a route.
    // Includes drafts and archived announcements.
    List<Announcement> findByAffectedRouteIdsContaining(
            Long routeId
    );

    // Find all announcements referencing a trip.
    // Includes drafts and archived announcements.
    List<Announcement> findByAffectedTripIdsContaining(
            Long tripId
    );

    // Retrieve announcements by category and status.
    List<Announcement>
    findByCategoryIgnoreCaseAndStatusIgnoreCaseOrderByCreatedAtDesc(
            String category,
            String status
    );

    // ==========================================
    // DASHBOARD STATISTICS
    // ==========================================

    // Count announcements by audience.
    long countByTargetAudienceIgnoreCase(
            String targetAudience
    );

    // Count announcements by category.
    long countByCategoryIgnoreCase(
            String category
    );

    // Count announcements by priority.
    long countByPriorityIgnoreCase(
            String priority
    );
}
