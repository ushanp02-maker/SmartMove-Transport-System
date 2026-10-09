
package com.smartmove.backend.controller;

import com.smartmove.backend.service.AnnouncementService;
import com.smartmove.backend.service.AnnouncementService.AnnouncementProfile;
import com.smartmove.backend.service.AnnouncementService.PublicAnnouncement;
import com.smartmove.backend.service.AnnouncementService.CreateAnnouncementRequest;
import com.smartmove.backend.service.AnnouncementService.UpdateAnnouncementRequest;
import com.smartmove.backend.service.AnnouncementService.AnnouncementStatistics;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/announcements")
@Validated
public class AnnouncementController {

    private final AnnouncementService announcementService;

    public AnnouncementController(
            AnnouncementService announcementService
    ) {
        this.announcementService = announcementService;
    }

    // ==========================================
    // PUBLIC: ACTIVE ANNOUNCEMENTS
    // ==========================================

    @GetMapping("/public")
    public ResponseEntity<List<PublicAnnouncement>>
    getActiveAnnouncements() {
        return ResponseEntity.ok(
                announcementService.getActiveAnnouncements()
        );
    }

    // ==========================================
    // PUBLIC: ANNOUNCEMENTS BY AUDIENCE
    // ==========================================

    @GetMapping("/public/audience/{audience}")
    public ResponseEntity<List<PublicAnnouncement>>
    getActiveForAudience(
            @PathVariable @NotBlank String audience
    ) {
        return ResponseEntity.ok(
                announcementService.getActiveForAudience(
                        audience
                )
        );
    }

    // ==========================================
    // PUBLIC: ROUTE ANNOUNCEMENTS
    // ==========================================

    @GetMapping("/public/route/{routeId}")
    public ResponseEntity<List<PublicAnnouncement>>
    getActiveForRoute(
            @PathVariable @Positive Long routeId,
            @RequestParam(defaultValue = "PASSENGER")
            String audience
    ) {
        return ResponseEntity.ok(
                announcementService.getActiveForRoute(
                        routeId,
                        audience
                )
        );
    }

    // ==========================================
    // PUBLIC: TRIP ANNOUNCEMENTS
    // ==========================================

    @GetMapping("/public/trip/{tripId}")
    public ResponseEntity<List<PublicAnnouncement>>
    getActiveForTrip(
            @PathVariable @Positive Long tripId,
            @RequestParam(defaultValue = "PASSENGER")
            String audience
    ) {
        return ResponseEntity.ok(
                announcementService.getActiveForTrip(
                        tripId,
                        audience
                )
        );
    }

    // ==========================================
    // ADMIN: ALL ANNOUNCEMENTS
    // ==========================================

    @GetMapping
    public ResponseEntity<Page<AnnouncementProfile>>
    getAllAnnouncements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                announcementService.getAllAnnouncements(
                        createPageable(page, size)
                )
        );
    }

    // ==========================================
    // ADMIN: ANNOUNCEMENT STATISTICS
    // ==========================================

    @GetMapping("/stats")
    public ResponseEntity<AnnouncementStatistics>
    getStatistics() {
        return ResponseEntity.ok(
                announcementService.getStatistics()
        );
    }

    // ==========================================
    // ADMIN: ANNOUNCEMENT COUNT
    // ==========================================

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>>
    getAnnouncementCount() {
        return ResponseEntity.ok(
                Map.of(
                        "totalAnnouncements",
                        announcementService
                                .getStatistics()
                                .totalAnnouncements()
                )
        );
    }

    // ==========================================
    // ADMIN: SEARCH ANNOUNCEMENTS
    // ==========================================

    @GetMapping("/search")
    public ResponseEntity<Page<AnnouncementProfile>>
    searchAnnouncements(
            @RequestParam(defaultValue = "")
            String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                announcementService.searchAnnouncements(
                        keyword,
                        createPageable(page, size)
                )
        );
    }

    // ==========================================
    // ADMIN: FILTER BY STATUS
    // ==========================================

    @GetMapping("/status/{status}")
    public ResponseEntity<Page<AnnouncementProfile>>
    getByStatus(
            @PathVariable @NotBlank String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                announcementService.getByStatus(
                        status,
                        createPageable(page, size)
                )
        );
    }

    // ==========================================
    // ADMIN: FILTER BY CATEGORY
    // ==========================================

    @GetMapping("/category/{category}")
    public ResponseEntity<Page<AnnouncementProfile>>
    getByCategory(
            @PathVariable @NotBlank String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                announcementService.getByCategory(
                        category,
                        createPageable(page, size)
                )
        );
    }

    // ==========================================
    // ADMIN: FILTER BY PRIORITY
    // ==========================================

    @GetMapping("/priority/{priority}")
    public ResponseEntity<Page<AnnouncementProfile>>
    getByPriority(
            @PathVariable @NotBlank String priority,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                announcementService.getByPriority(
                        priority,
                        createPageable(page, size)
                )
        );
    }

    // ==========================================
    // ADMIN: ANNOUNCEMENTS BY CREATOR
    // ==========================================

    @GetMapping("/created-by/{userId}")
    public ResponseEntity<List<AnnouncementProfile>>
    getCreatedByUser(
            @PathVariable @Positive Long userId
    ) {
        return ResponseEntity.ok(
                announcementService.getCreatedByUser(
                        userId
                )
        );
    }

    // ==========================================
    // ADMIN: ANNOUNCEMENTS AFFECTING ROUTE
    // ==========================================

    @GetMapping("/affected/route/{routeId}")
    public ResponseEntity<List<AnnouncementProfile>>
    getByAffectedRoute(
            @PathVariable @Positive Long routeId
    ) {
        return ResponseEntity.ok(
                announcementService.getByAffectedRoute(
                        routeId
                )
        );
    }

    // ==========================================
    // ADMIN: ANNOUNCEMENTS AFFECTING TRIP
    // ==========================================

    @GetMapping("/affected/trip/{tripId}")
    public ResponseEntity<List<AnnouncementProfile>>
    getByAffectedTrip(
            @PathVariable @Positive Long tripId
    ) {
        return ResponseEntity.ok(
                announcementService.getByAffectedTrip(
                        tripId
                )
        );
    }

    // ==========================================
    // ADMIN: ANNOUNCEMENT DETAILS
    // ==========================================

    @GetMapping("/{announcementId}")
    public ResponseEntity<AnnouncementProfile>
    getAnnouncementById(
            @PathVariable String announcementId
    ) {
        return ResponseEntity.ok(
                announcementService.getAnnouncementById(
                        announcementId
                )
        );
    }

    // ==========================================
    // ADMIN: CREATE ANNOUNCEMENT
    // ==========================================

    @PostMapping
    public ResponseEntity<AnnouncementProfile>
    createAnnouncement(
            @Valid @RequestBody CreateAnnouncementPayload request
    ) {
        AnnouncementProfile announcement =
                announcementService.createAnnouncement(
                        new CreateAnnouncementRequest(
                                request.title(),
                                request.message(),
                                request.category(),
                                request.priority(),
                                request.targetAudience(),
                                request.affectedRouteIds(),
                                request.affectedTripIds(),
                                request.affectedCities(),
                                request.publishAt(),
                                request.expiresAt(),
                                request.actionLabel(),
                                request.actionUrl(),
                                request.attachmentUrl(),
                                request.pinned(),
                                request.important(),
                                request.internalNotes(),
                                request.createdByUserId()
                        )
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(announcement);
    }

    // ==========================================
    // ADMIN: UPDATE ANNOUNCEMENT
    // ==========================================

    @PutMapping("/{announcementId}")
    public ResponseEntity<AnnouncementProfile>
    updateAnnouncement(
            @PathVariable String announcementId,
            @Valid @RequestBody UpdateAnnouncementPayload request
    ) {
        return ResponseEntity.ok(
                announcementService.updateAnnouncement(
                        announcementId,
                        new UpdateAnnouncementRequest(
                                request.title(),
                                request.message(),
                                request.category(),
                                request.priority(),
                                request.targetAudience(),
                                request.affectedRouteIds(),
                                request.affectedTripIds(),
                                request.affectedCities(),
                                request.publishAt(),
                                request.expiresAt(),
                                request.actionLabel(),
                                request.actionUrl(),
                                request.attachmentUrl(),
                                request.pinned(),
                                request.important(),
                                request.internalNotes(),
                                request.updatedByUserId()
                        )
                )
        );
    }

    // ==========================================
    // ADMIN: PUBLISH IMMEDIATELY
    // ==========================================

    @PatchMapping("/{announcementId}/publish")
    public ResponseEntity<AnnouncementProfile>
    publishAnnouncement(
            @PathVariable String announcementId,
            @Valid @RequestBody ActorPayload request
    ) {
        return ResponseEntity.ok(
                announcementService.publishAnnouncement(
                        announcementId,
                        request.userId()
                )
        );
    }

    // ==========================================
    // ADMIN: SCHEDULE PUBLICATION
    // ==========================================

    @PatchMapping("/{announcementId}/schedule")
    public ResponseEntity<AnnouncementProfile>
    scheduleAnnouncement(
            @PathVariable String announcementId,
            @Valid @RequestBody ScheduleAnnouncementPayload request
    ) {
        return ResponseEntity.ok(
                announcementService.scheduleAnnouncement(
                        announcementId,
                        request.publishAt(),
                        request.userId()
                )
        );
    }

    // ==========================================
    // ADMIN: ARCHIVE ANNOUNCEMENT
    // ==========================================

    @PatchMapping("/{announcementId}/archive")
    public ResponseEntity<AnnouncementProfile>
    archiveAnnouncement(
            @PathVariable String announcementId,
            @Valid @RequestBody ActorPayload request
    ) {
        return ResponseEntity.ok(
                announcementService.archiveAnnouncement(
                        announcementId,
                        request.userId()
                )
        );
    }

    // ==========================================
    // ADMIN: PROCESS SCHEDULED PUBLICATIONS
    // ==========================================

    @PostMapping("/jobs/publish-due")
    public ResponseEntity<Map<String, Integer>>
    publishDueAnnouncements() {
        int published =
                announcementService.publishDueAnnouncements();

        return ResponseEntity.ok(
                Map.of("publishedCount", published)
        );
    }

    // ==========================================
    // ADMIN: ARCHIVE EXPIRED ANNOUNCEMENTS
    // ==========================================

    @PostMapping("/jobs/archive-expired")
    public ResponseEntity<Map<String, Integer>>
    archiveExpiredAnnouncements() {
        int archived =
                announcementService.archiveExpiredAnnouncements();

        return ResponseEntity.ok(
                Map.of("archivedCount", archived)
        );
    }

    // ==========================================
    // REQUEST DTOs
    // ==========================================

    public record CreateAnnouncementPayload(

            @NotBlank(message = "Title is required")
            @Size(max = 200)
            String title,

            @NotBlank(message = "Message is required")
            @Size(max = 5000)
            String message,

            String category,

            String priority,

            String targetAudience,

            List<@Positive Long> affectedRouteIds,

            List<@Positive Long> affectedTripIds,

            List<@NotBlank String> affectedCities,

            LocalDateTime publishAt,

            LocalDateTime expiresAt,

            @Size(max = 100)
            String actionLabel,

            @Size(max = 1000)
            String actionUrl,

            @Size(max = 1000)
            String attachmentUrl,

            Boolean pinned,

            Boolean important,

            @Size(max = 2000)
            String internalNotes,

            @NotNull(message = "Creator user ID is required")
            @Positive
            Long createdByUserId

    ) {
    }

    public record UpdateAnnouncementPayload(

            @Size(max = 200)
            String title,

            @Size(max = 5000)
            String message,

            String category,

            String priority,

            String targetAudience,

            List<@Positive Long> affectedRouteIds,

            List<@Positive Long> affectedTripIds,

            List<@NotBlank String> affectedCities,

            LocalDateTime publishAt,

            LocalDateTime expiresAt,

            @Size(max = 100)
            String actionLabel,

            @Size(max = 1000)
            String actionUrl,

            @Size(max = 1000)
            String attachmentUrl,

            Boolean pinned,

            Boolean important,

            @Size(max = 2000)
            String internalNotes,

            @NotNull(message = "Updater user ID is required")
            @Positive
            Long updatedByUserId

    ) {
    }

    public record ActorPayload(

            @NotNull(message = "User ID is required")
            @Positive
            Long userId

    ) {
    }

    public record ScheduleAnnouncementPayload(

            @NotNull(message = "Publication time is required")
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME
            )
            LocalDateTime publishAt,

            @NotNull(message = "User ID is required")
            @Positive
            Long userId

    ) {
    }

    // ==========================================
    // PAGINATION HELPER
    // ==========================================

    private Pageable createPageable(
            int page,
            int size
    ) {
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
