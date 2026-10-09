
package com.smartmove.backend.service;

import com.smartmove.backend.document.Announcement;
import com.smartmove.backend.repository.AnnouncementRepository;
import com.smartmove.backend.repository.RouteRepository;
import com.smartmove.backend.repository.TripRepository;
import com.smartmove.backend.repository.UserAccountRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AnnouncementService {

    private static final Set<String> CATEGORIES = Set.of(
            "GENERAL",
            "SERVICE_UPDATE",
            "ROUTE_DISRUPTION",
            "DELAY",
            "MAINTENANCE",
            "EMERGENCY",
            "POLICY_UPDATE"
    );

    private static final Set<String> PRIORITIES = Set.of(
            "LOW", "NORMAL", "HIGH", "URGENT"
    );

    private static final Set<String> AUDIENCES = Set.of(
            "ALL", "PASSENGER", "DRIVER", "ADMIN"
    );

    private static final Set<String> STATUSES = Set.of(
            "DRAFT", "SCHEDULED", "PUBLISHED", "ARCHIVED"
    );

    private final AnnouncementRepository announcementRepository;
    private final RouteRepository routeRepository;
    private final TripRepository tripRepository;
    private final UserAccountRepository userAccountRepository;

    public AnnouncementService(
            AnnouncementRepository announcementRepository,
            RouteRepository routeRepository,
            TripRepository tripRepository,
            UserAccountRepository userAccountRepository
    ) {
        this.announcementRepository = announcementRepository;
        this.routeRepository = routeRepository;
        this.tripRepository = tripRepository;
        this.userAccountRepository = userAccountRepository;
    }

    // ==========================================
    // DATA TRANSFER OBJECTS
    // ==========================================

    public record AnnouncementProfile(
            String id,
            String title,
            String message,
            String category,
            String priority,
            String status,
            String targetAudience,
            List<Long> affectedRouteIds,
            List<Long> affectedTripIds,
            List<String> affectedCities,
            Long createdByUserId,
            Long updatedByUserId,
            LocalDateTime publishAt,
            LocalDateTime expiresAt,
            LocalDateTime publishedAt,
            String actionLabel,
            String actionUrl,
            String attachmentUrl,
            Boolean pinned,
            Boolean important,
            String internalNotes,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record PublicAnnouncement(
            String id,
            String title,
            String message,
            String category,
            String priority,
            String targetAudience,
            List<Long> affectedRouteIds,
            List<Long> affectedTripIds,
            List<String> affectedCities,
            LocalDateTime publishedAt,
            LocalDateTime expiresAt,
            String actionLabel,
            String actionUrl,
            String attachmentUrl,
            Boolean pinned,
            Boolean important
    ) {
    }

    public record CreateAnnouncementRequest(
            String title,
            String message,
            String category,
            String priority,
            String targetAudience,
            List<Long> affectedRouteIds,
            List<Long> affectedTripIds,
            List<String> affectedCities,
            LocalDateTime publishAt,
            LocalDateTime expiresAt,
            String actionLabel,
            String actionUrl,
            String attachmentUrl,
            Boolean pinned,
            Boolean important,
            String internalNotes,
            Long createdByUserId
    ) {
    }

    public record UpdateAnnouncementRequest(
            String title,
            String message,
            String category,
            String priority,
            String targetAudience,
            List<Long> affectedRouteIds,
            List<Long> affectedTripIds,
            List<String> affectedCities,
            LocalDateTime publishAt,
            LocalDateTime expiresAt,
            String actionLabel,
            String actionUrl,
            String attachmentUrl,
            Boolean pinned,
            Boolean important,
            String internalNotes,
            Long updatedByUserId
    ) {
    }

    public record AnnouncementStatistics(
            long totalAnnouncements,
            long draftAnnouncements,
            long scheduledAnnouncements,
            long publishedAnnouncements,
            long archivedAnnouncements,
            long currentlyActiveAnnouncements,
            long urgentAnnouncements,
            long passengerAnnouncements,
            long driverAnnouncements
    ) {
    }

    // ==========================================
    // ADMIN: CREATE ANNOUNCEMENT
    // ==========================================

    public AnnouncementProfile createAnnouncement(
            CreateAnnouncementRequest request
    ) {
        if (request == null) {
            throw badRequest("Announcement details are required");
        }

        validateUser(request.createdByUserId());

        Announcement announcement = new Announcement();

        announcement.setTitle(
                requiredText(request.title(), "Title", 200)
        );
        announcement.setMessage(
                requiredText(request.message(), "Message", 5000)
        );
        announcement.setCategory(
                normalize(
                        request.category(),
                        CATEGORIES,
                        "GENERAL"
                )
        );
        announcement.setPriority(
                normalize(
                        request.priority(),
                        PRIORITIES,
                        "NORMAL"
                )
        );
        announcement.setTargetAudience(
                normalize(
                        request.targetAudience(),
                        AUDIENCES,
                        "ALL"
                )
        );

        announcement.setAffectedRouteIds(
                validateRouteIds(request.affectedRouteIds())
        );
        announcement.setAffectedTripIds(
                validateTripIds(request.affectedTripIds())
        );
        announcement.setAffectedCities(
                normalizeCities(request.affectedCities())
        );

        validateDates(
                request.publishAt(),
                request.expiresAt()
        );

        announcement.setPublishAt(request.publishAt());
        announcement.setExpiresAt(request.expiresAt());
        announcement.setActionLabel(
                optionalText(request.actionLabel(), 100)
        );
        announcement.setActionUrl(
                optionalText(request.actionUrl(), 1000)
        );
        announcement.setAttachmentUrl(
                optionalText(request.attachmentUrl(), 1000)
        );
        announcement.setPinned(
                Boolean.TRUE.equals(request.pinned())
        );
        announcement.setImportant(
                Boolean.TRUE.equals(request.important())
        );
        announcement.setInternalNotes(
                optionalText(request.internalNotes(), 2000)
        );

        announcement.setCreatedByUserId(
                request.createdByUserId()
        );
        announcement.setStatus("DRAFT");

        LocalDateTime now = LocalDateTime.now();
        announcement.setCreatedAt(now);
        announcement.setUpdatedAt(now);

        return toProfile(
                announcementRepository.save(announcement)
        );
    }

    // ==========================================
    // ADMIN: UPDATE ANNOUNCEMENT
    // ==========================================

    public AnnouncementProfile updateAnnouncement(
            String announcementId,
            UpdateAnnouncementRequest request
    ) {
        if (request == null) {
            throw badRequest("Update details are required");
        }

        Announcement announcement = findAnnouncement(
                announcementId
        );

        if (!Set.of("DRAFT", "SCHEDULED").contains(
                announcement.getStatus()
        )) {
            throw conflict(
                    "Only draft or scheduled announcements can be edited"
            );
        }

        validateUser(request.updatedByUserId());

        if (request.title() != null) {
            announcement.setTitle(
                    requiredText(request.title(), "Title", 200)
            );
        }

        if (request.message() != null) {
            announcement.setMessage(
                    requiredText(request.message(), "Message", 5000)
            );
        }

        if (request.category() != null) {
            announcement.setCategory(
                    normalize(
                            request.category(),
                            CATEGORIES,
                            "GENERAL"
                    )
            );
        }

        if (request.priority() != null) {
            announcement.setPriority(
                    normalize(
                            request.priority(),
                            PRIORITIES,
                            "NORMAL"
                    )
            );
        }

        if (request.targetAudience() != null) {
            announcement.setTargetAudience(
                    normalize(
                            request.targetAudience(),
                            AUDIENCES,
                            "ALL"
                    )
            );
        }

        if (request.affectedRouteIds() != null) {
            announcement.setAffectedRouteIds(
                    validateRouteIds(request.affectedRouteIds())
            );
        }

        if (request.affectedTripIds() != null) {
            announcement.setAffectedTripIds(
                    validateTripIds(request.affectedTripIds())
            );
        }

        if (request.affectedCities() != null) {
            announcement.setAffectedCities(
                    normalizeCities(request.affectedCities())
            );
        }

        LocalDateTime publishAt =
                request.publishAt() != null
                        ? request.publishAt()
                        : announcement.getPublishAt();

        LocalDateTime expiresAt =
                request.expiresAt() != null
                        ? request.expiresAt()
                        : announcement.getExpiresAt();

        validateDates(publishAt, expiresAt);

        announcement.setPublishAt(publishAt);
        announcement.setExpiresAt(expiresAt);

        if (request.actionLabel() != null) {
            announcement.setActionLabel(
                    optionalText(request.actionLabel(), 100)
            );
        }

        if (request.actionUrl() != null) {
            announcement.setActionUrl(
                    optionalText(request.actionUrl(), 1000)
            );
        }

        if (request.attachmentUrl() != null) {
            announcement.setAttachmentUrl(
                    optionalText(request.attachmentUrl(), 1000)
            );
        }

        if (request.pinned() != null) {
            announcement.setPinned(request.pinned());
        }

        if (request.important() != null) {
            announcement.setImportant(request.important());
        }

        if (request.internalNotes() != null) {
            announcement.setInternalNotes(
                    optionalText(request.internalNotes(), 2000)
            );
        }

        announcement.setUpdatedByUserId(
                request.updatedByUserId()
        );
        announcement.setUpdatedAt(LocalDateTime.now());

        return toProfile(
                announcementRepository.save(announcement)
        );
    }

    // ==========================================
    // ADMIN: PUBLISH NOW
    // ==========================================

    public AnnouncementProfile publishAnnouncement(
            String announcementId,
            Long userId
    ) {
        validateUser(userId);

        Announcement announcement = findAnnouncement(
                announcementId
        );

        if (!Set.of("DRAFT", "SCHEDULED").contains(
                announcement.getStatus()
        )) {
            throw conflict(
                    "Only draft or scheduled announcements can be published"
            );
        }

        LocalDateTime now = LocalDateTime.now();

        if (announcement.getExpiresAt() != null
                && !announcement.getExpiresAt().isAfter(now)) {
            throw conflict(
                    "Cannot publish an already expired announcement"
            );
        }

        announcement.setStatus("PUBLISHED");
        announcement.setPublishAt(now);
        announcement.setPublishedAt(now);
        announcement.setUpdatedByUserId(userId);
        announcement.setUpdatedAt(now);

        return toProfile(
                announcementRepository.save(announcement)
        );
    }

    // ==========================================
    // ADMIN: SCHEDULE PUBLICATION
    // ==========================================

    public AnnouncementProfile scheduleAnnouncement(
            String announcementId,
            LocalDateTime publishAt,
            Long userId
    ) {
        validateUser(userId);

        Announcement announcement = findAnnouncement(
                announcementId
        );

        if (!Set.of("DRAFT", "SCHEDULED").contains(
                announcement.getStatus()
        )) {
            throw conflict(
                    "Only draft or scheduled announcements can be scheduled"
            );
        }

        if (publishAt == null
                || !publishAt.isAfter(LocalDateTime.now())) {
            throw badRequest(
                    "Publication time must be in the future"
            );
        }

        validateDates(
                publishAt,
                announcement.getExpiresAt()
        );

        announcement.setStatus("SCHEDULED");
        announcement.setPublishAt(publishAt);
        announcement.setPublishedAt(null);
        announcement.setUpdatedByUserId(userId);
        announcement.setUpdatedAt(LocalDateTime.now());

        return toProfile(
                announcementRepository.save(announcement)
        );
    }

    // ==========================================
    // ADMIN: ARCHIVE ANNOUNCEMENT
    // ==========================================

    public AnnouncementProfile archiveAnnouncement(
            String announcementId,
            Long userId
    ) {
        validateUser(userId);

        Announcement announcement = findAnnouncement(
                announcementId
        );

        if ("ARCHIVED".equals(announcement.getStatus())) {
            return toProfile(announcement);
        }

        announcement.setStatus("ARCHIVED");
        announcement.setUpdatedByUserId(userId);
        announcement.setUpdatedAt(LocalDateTime.now());

        return toProfile(
                announcementRepository.save(announcement)
        );
    }

    // ==========================================
    // AUTOMATIC PUBLICATION PROCESSING
    // ==========================================

    public int publishDueAnnouncements() {
        LocalDateTime now = LocalDateTime.now();

        List<Announcement> due =
                announcementRepository
                        .findByStatusAndPublishAtLessThanEqual(
                                "SCHEDULED",
                                now
                        );

        int published = 0;

        for (Announcement announcement : due) {
            // Skip announcements whose expiry has passed.
            if (announcement.getExpiresAt() != null
                    && !announcement.getExpiresAt().isAfter(now)) {
                announcement.setStatus("ARCHIVED");
            } else {
                announcement.setStatus("PUBLISHED");
                announcement.setPublishedAt(
                        announcement.getPublishAt()
                );
                published++;
            }

            announcement.setUpdatedAt(now);
            announcementRepository.save(announcement);
        }

        return published;
    }

    public int archiveExpiredAnnouncements() {
        LocalDateTime now = LocalDateTime.now();

        List<Announcement> expired =
                announcementRepository
                        .findByStatusAndExpiresAtLessThanEqual(
                                "PUBLISHED",
                                now
                        );

        for (Announcement announcement : expired) {
            announcement.setStatus("ARCHIVED");
            announcement.setUpdatedAt(now);
            announcementRepository.save(announcement);
        }

        return expired.size();
    }

    // ==========================================
    // PUBLIC: ACTIVE ANNOUNCEMENTS
    // ==========================================

    public List<PublicAnnouncement> getActiveAnnouncements() {
        return sortPublic(
                announcementRepository
                        .findActiveAnnouncements(
                                LocalDateTime.now()
                        )
        );
    }

    public List<PublicAnnouncement> getActiveForAudience(
            String audience
    ) {
        return sortPublic(
                announcementRepository
                        .findActiveAnnouncementsForAudience(
                                normalizeAudience(audience),
                                LocalDateTime.now()
                        )
        );
    }

    public List<PublicAnnouncement> getActiveForRoute(
            Long routeId,
            String audience
    ) {
        if (routeId == null
                || !routeRepository.existsById(routeId)) {
            throw notFound("Route not found");
        }

        return sortPublic(
                announcementRepository
                        .findActiveAnnouncementsForRoute(
                                routeId,
                                normalizeAudience(audience),
                                LocalDateTime.now()
                        )
        );
    }

    public List<PublicAnnouncement> getActiveForTrip(
            Long tripId,
            String audience
    ) {
        if (tripId == null
                || !tripRepository.existsById(tripId)) {
            throw notFound("Trip not found");
        }

        return sortPublic(
                announcementRepository
                        .findActiveAnnouncementsForTrip(
                                tripId,
                                normalizeAudience(audience),
                                LocalDateTime.now()
                        )
        );
    }

    // ==========================================
    // ADMIN: LIST AND SEARCH
    // ==========================================

    public Page<AnnouncementProfile> getAllAnnouncements(
            Pageable pageable
    ) {
        return announcementRepository
                .findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toProfile);
    }

    public Page<AnnouncementProfile> getByStatus(
            String status,
            Pageable pageable
    ) {
        return announcementRepository
                .findByStatusIgnoreCase(
                        normalizeStatus(status),
                        pageable
                )
                .map(this::toProfile);
    }

    public Page<AnnouncementProfile> getByCategory(
            String category,
            Pageable pageable
    ) {
        return announcementRepository
                .findByCategoryIgnoreCase(
                        normalize(
                                category,
                                CATEGORIES,
                                "GENERAL"
                        ),
                        pageable
                )
                .map(this::toProfile);
    }

    public Page<AnnouncementProfile> getByPriority(
            String priority,
            Pageable pageable
    ) {
        return announcementRepository
                .findByPriorityIgnoreCase(
                        normalize(
                                priority,
                                PRIORITIES,
                                "NORMAL"
                        ),
                        pageable
                )
                .map(this::toProfile);
    }

    public Page<AnnouncementProfile> searchAnnouncements(
            String keyword,
            Pageable pageable
    ) {
        if (keyword == null || keyword.isBlank()) {
            return getAllAnnouncements(pageable);
        }

        return announcementRepository
                .findByTitleContainingIgnoreCase(
                        keyword.trim(),
                        pageable
                )
                .map(this::toProfile);
    }

    public List<AnnouncementProfile> getCreatedByUser(
            Long userId
    ) {
        validateUser(userId);

        return announcementRepository
                .findByCreatedByUserIdOrderByCreatedAtDesc(
                        userId
                )
                .stream()
                .map(this::toProfile)
                .toList();
    }

    public List<AnnouncementProfile> getByAffectedRoute(
            Long routeId
    ) {
        return announcementRepository
                .findByAffectedRouteIdsContaining(routeId)
                .stream()
                .map(this::toProfile)
                .toList();
    }

    public List<AnnouncementProfile> getByAffectedTrip(
            Long tripId
    ) {
        return announcementRepository
                .findByAffectedTripIdsContaining(tripId)
                .stream()
                .map(this::toProfile)
                .toList();
    }

    public AnnouncementProfile getAnnouncementById(
            String announcementId
    ) {
        return toProfile(findAnnouncement(announcementId));
    }

    // ==========================================
    // ADMIN: STATISTICS
    // ==========================================

    public AnnouncementStatistics getStatistics() {
        return new AnnouncementStatistics(
                announcementRepository.count(),
                announcementRepository.countByStatusIgnoreCase(
                        "DRAFT"
                ),
                announcementRepository.countByStatusIgnoreCase(
                        "SCHEDULED"
                ),
                announcementRepository.countByStatusIgnoreCase(
                        "PUBLISHED"
                ),
                announcementRepository.countByStatusIgnoreCase(
                        "ARCHIVED"
                ),
                announcementRepository
                        .findActiveAnnouncements(
                                LocalDateTime.now()
                        )
                        .size(),
                announcementRepository
                        .countByPriorityIgnoreCase("URGENT"),
                announcementRepository
                        .countByTargetAudienceIgnoreCase(
                                "PASSENGER"
                        ),
                announcementRepository
                        .countByTargetAudienceIgnoreCase(
                                "DRIVER"
                        )
        );
    }

    // ==========================================
    // VALIDATION HELPERS
    // ==========================================

    private Announcement findAnnouncement(String id) {
        if (id == null || id.isBlank()) {
            throw badRequest("Announcement ID is required");
        }

        return announcementRepository.findById(id)
                .orElseThrow(() ->
                        notFound("Announcement not found")
                );
    }

    private void validateUser(Long userId) {
        if (userId == null || userId <= 0) {
            throw badRequest("User ID is required");
        }

        if (!userAccountRepository.existsById(userId)) {
            throw notFound("User account not found");
        }
    }

    private List<Long> validateRouteIds(List<Long> ids) {
        if (ids == null) {
            return new ArrayList<>();
        }

        List<Long> distinct = ids.stream()
                .distinct()
                .toList();

        for (Long id : distinct) {
            if (id == null || id <= 0
                    || !routeRepository.existsById(id)) {
                throw badRequest(
                        "Invalid route ID: " + id
                );
            }
        }

        return new ArrayList<>(distinct);
    }

    private List<Long> validateTripIds(List<Long> ids) {
        if (ids == null) {
            return new ArrayList<>();
        }

        List<Long> distinct = ids.stream()
                .distinct()
                .toList();

        for (Long id : distinct) {
            if (id == null || id <= 0
                    || !tripRepository.existsById(id)) {
                throw badRequest(
                        "Invalid trip ID: " + id
                );
            }
        }

        return new ArrayList<>(distinct);
    }

    private List<String> normalizeCities(
            List<String> cities
    ) {
        if (cities == null) {
            return new ArrayList<>();
        }

        List<String> result = new ArrayList<>();

        for (String city : cities) {
            if (city == null || city.isBlank()) {
                throw badRequest(
                        "City names cannot be empty"
                );
            }

            String normalized = city.trim();

            if (normalized.length() > 100) {
                throw badRequest("City name is too long");
            }

            if (!result.contains(normalized)) {
                result.add(normalized);
            }
        }

        return result;
    }

    private void validateDates(
            LocalDateTime publishAt,
            LocalDateTime expiresAt
    ) {
        if (expiresAt != null
                && publishAt != null
                && !expiresAt.isAfter(publishAt)) {
            throw badRequest(
                    "Expiration time must be after publication time"
            );
        }

        if (expiresAt != null
                && !expiresAt.isAfter(LocalDateTime.now())) {
            throw badRequest(
                    "Expiration time must be in the future"
            );
        }
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
            throw badRequest("Text is too long");
        }

        return result;
    }

    private String normalize(
            String value,
            Set<String> allowed,
            String defaultValue
    ) {
        String normalized = value == null
                || value.isBlank()
                ? defaultValue
                : value.trim().toUpperCase(Locale.ROOT);

        if (!allowed.contains(normalized)) {
            throw badRequest(
                    "Unsupported value: " + normalized
            );
        }

        return normalized;
    }

    private String normalizeAudience(String audience) {
        return normalize(
                audience,
                AUDIENCES,
                "PASSENGER"
        );
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            throw badRequest("Status is required");
        }

        return normalize(status, STATUSES, "DRAFT");
    }

    // ==========================================
    // RESPONSE MAPPING
    // ==========================================

    private AnnouncementProfile toProfile(
            Announcement a
    ) {
        return new AnnouncementProfile(
                a.getId(),
                a.getTitle(),
                a.getMessage(),
                a.getCategory(),
                a.getPriority(),
                a.getStatus(),
                a.getTargetAudience(),
                a.getAffectedRouteIds(),
                a.getAffectedTripIds(),
                a.getAffectedCities(),
                a.getCreatedByUserId(),
                a.getUpdatedByUserId(),
                a.getPublishAt(),
                a.getExpiresAt(),
                a.getPublishedAt(),
                a.getActionLabel(),
                a.getActionUrl(),
                a.getAttachmentUrl(),
                a.getPinned(),
                a.getImportant(),
                a.getInternalNotes(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }

    private PublicAnnouncement toPublicAnnouncement(
            Announcement a
    ) {
        return new PublicAnnouncement(
                a.getId(),
                a.getTitle(),
                a.getMessage(),
                a.getCategory(),
                a.getPriority(),
                a.getTargetAudience(),
                a.getAffectedRouteIds(),
                a.getAffectedTripIds(),
                a.getAffectedCities(),
                a.getPublishedAt(),
                a.getExpiresAt(),
                a.getActionLabel(),
                a.getActionUrl(),
                a.getAttachmentUrl(),
                a.getPinned(),
                a.getImportant()
        );
    }

    private List<PublicAnnouncement> sortPublic(
            List<Announcement> announcements
    ) {
        return announcements.stream()
                .sorted(
                        Comparator
                                .comparing(
                                        (Announcement a) ->
                                                Boolean.TRUE.equals(
                                                        a.getPinned()
                                                )
                                )
                                .reversed()
                                .thenComparing(
                                        a -> priorityRank(
                                                a.getPriority()
                                        ),
                                        Comparator.reverseOrder()
                                )
                                .thenComparing(
                                        Announcement::getPublishedAt,
                                        Comparator.nullsLast(
                                                Comparator.reverseOrder()
                                        )
                                )
                )
                .map(this::toPublicAnnouncement)
                .toList();
    }

    private int priorityRank(String priority) {
        if (priority == null) {
            return 0;
        }

        return switch (priority.toUpperCase(Locale.ROOT)) {
            case "URGENT" -> 4;
            case "HIGH" -> 3;
            case "NORMAL" -> 2;
            case "LOW" -> 1;
            default -> 0;
        };
    }

    private ResponseStatusException badRequest(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }

    private ResponseStatusException conflict(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                message
        );
    }

    private ResponseStatusException notFound(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                message
        );
    }
}
