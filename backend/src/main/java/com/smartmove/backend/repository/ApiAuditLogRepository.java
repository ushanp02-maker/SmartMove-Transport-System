
package com.smartmove.backend.repository;

import com.smartmove.backend.entity.ApiAuditLog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ApiAuditLogRepository
        extends JpaRepository<ApiAuditLog, Long> {

    // ==========================================
    // RECENT AUDIT HISTORY
    // ==========================================

    List<ApiAuditLog> findTop50ByOrderByCreatedAtDesc();

    List<ApiAuditLog> findTop100ByOrderByCreatedAtDesc();

    Page<ApiAuditLog> findAllByOrderByCreatedAtDesc(
            Pageable pageable
    );

    // ==========================================
    // AUDIT HISTORY BY ACTOR
    // ==========================================

    List<ApiAuditLog> findByActorIdOrderByCreatedAtDesc(
            Long accountId
    );

    Page<ApiAuditLog> findByActorIdOrderByCreatedAtDesc(
            Long accountId,
            Pageable pageable
    );

    List<ApiAuditLog>
    findByActorUsernameIgnoreCaseOrderByCreatedAtDesc(
            String username
    );

    // ==========================================
    // AUDIT HISTORY BY ROLE
    // ==========================================

    List<ApiAuditLog> findByActorRoleIgnoreCaseOrderByCreatedAtDesc(
            String role
    );

    Page<ApiAuditLog> findByActorRoleIgnoreCaseOrderByCreatedAtDesc(
            String role,
            Pageable pageable
    );

    // ==========================================
    // AUDIT HISTORY BY ACTION
    // ==========================================

    List<ApiAuditLog> findByActionIgnoreCaseOrderByCreatedAtDesc(
            String action
    );

    Page<ApiAuditLog> findByActionIgnoreCaseOrderByCreatedAtDesc(
            String action,
            Pageable pageable
    );

    // ==========================================
    // AUDIT HISTORY BY ENTITY TYPE
    // ==========================================

    List<ApiAuditLog> findByEntityTypeIgnoreCaseOrderByCreatedAtDesc(
            String entityType
    );

    Page<ApiAuditLog> findByEntityTypeIgnoreCaseOrderByCreatedAtDesc(
            String entityType,
            Pageable pageable
    );

    // ==========================================
    // AUDIT HISTORY FOR SPECIFIC RECORD
    // ==========================================

    List<ApiAuditLog>
    findByEntityTypeIgnoreCaseAndEntityIdOrderByCreatedAtDesc(
            String entityType,
            String entityId
    );

    Page<ApiAuditLog>
    findByEntityTypeIgnoreCaseAndEntityIdOrderByCreatedAtDesc(
            String entityType,
            String entityId,
            Pageable pageable
    );

    // ==========================================
    // AUDIT HISTORY BY RESULT
    // ==========================================

    List<ApiAuditLog> findByResultIgnoreCaseOrderByCreatedAtDesc(
            String result
    );

    Page<ApiAuditLog> findByResultIgnoreCaseOrderByCreatedAtDesc(
            String result,
            Pageable pageable
    );

    // ==========================================
    // AUDIT HISTORY BY HTTP STATUS
    // ==========================================

    List<ApiAuditLog> findByHttpStatusOrderByCreatedAtDesc(
            Integer httpStatus
    );

    // ==========================================
    // AUDIT HISTORY BY REQUEST PATH
    // ==========================================

    List<ApiAuditLog>
    findByRequestPathContainingIgnoreCaseOrderByCreatedAtDesc(
            String requestPath
    );

    // ==========================================
    // AUDIT HISTORY BY CORRELATION ID
    // ==========================================

    List<ApiAuditLog> findByCorrelationIdOrderByCreatedAtAsc(
            String correlationId
    );

    // ==========================================
    // AUDIT HISTORY WITHIN DATE RANGE
    // ==========================================

    List<ApiAuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    Page<ApiAuditLog> findByCreatedAtBetween(
            LocalDateTime startTime,
            LocalDateTime endTime,
            Pageable pageable
    );

    // ==========================================
    // ACTOR HISTORY WITHIN DATE RANGE
    // ==========================================

    List<ApiAuditLog>
    findByActorIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long accountId,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    // ==========================================
    // ENTITY HISTORY WITHIN DATE RANGE
    // ==========================================

    List<ApiAuditLog>
    findByEntityTypeIgnoreCaseAndCreatedAtBetweenOrderByCreatedAtDesc(
            String entityType,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    // ==========================================
    // ACTION HISTORY WITHIN DATE RANGE
    // ==========================================

    List<ApiAuditLog>
    findByActionIgnoreCaseAndCreatedAtBetweenOrderByCreatedAtDesc(
            String action,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    // ==========================================
    // COUNTS BY ACTION, ENTITY AND RESULT
    // ==========================================

    long countByActionIgnoreCase(String action);

    long countByEntityTypeIgnoreCase(String entityType);

    long countByResultIgnoreCase(String result);

    long countByActorId(Long accountId);

    long countByActorRoleIgnoreCase(String role);

    // ==========================================
    // COUNTS WITHIN DATE RANGE
    // ==========================================

    long countByCreatedAtBetween(
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    long countByResultIgnoreCaseAndCreatedAtBetween(
            String result,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    // ==========================================
    // COUNT RECENT FAILED ACTIONS
    // ==========================================

    @Query("""
        SELECT COUNT(a)
        FROM ApiAuditLog a
        WHERE a.result IN ('FAILURE', 'DENIED')
          AND a.createdAt >= :since
        """)
    long countRecentUnsuccessfulActions(
            @Param("since") LocalDateTime since
    );

    // ==========================================
    // ACTION COUNTS FOR REPORTS
    // ==========================================

    @Query("""
        SELECT a.action, COUNT(a)
        FROM ApiAuditLog a
        GROUP BY a.action
        ORDER BY COUNT(a) DESC
        """)
    List<Object[]> countActionsGrouped();

    // ==========================================
    // ENTITY TYPE COUNTS FOR REPORTS
    // ==========================================

    @Query("""
        SELECT a.entityType, COUNT(a)
        FROM ApiAuditLog a
        GROUP BY a.entityType
        ORDER BY COUNT(a) DESC
        """)
    List<Object[]> countEntitiesGrouped();

    // ==========================================
    // RESULT COUNTS FOR REPORTS
    // ==========================================

    @Query("""
        SELECT a.result, COUNT(a)
        FROM ApiAuditLog a
        GROUP BY a.result
        ORDER BY COUNT(a) DESC
        """)
    List<Object[]> countResultsGrouped();

    // ==========================================
    // ACTOR ACTIVITY COUNTS
    // ==========================================

    @Query("""
        SELECT a.actorUsername, COUNT(a)
        FROM ApiAuditLog a
        WHERE a.actorUsername IS NOT NULL
        GROUP BY a.actorUsername
        ORDER BY COUNT(a) DESC
        """)
    List<Object[]> countActionsByUsername();

    // ==========================================
    // AUDIT LOGS FOR AN ENTITY
    // ==========================================

    @Query("""
        SELECT a
        FROM ApiAuditLog a
        WHERE a.entityType = :entityType
          AND a.entityId = :entityId
        ORDER BY a.createdAt DESC
        """)
    List<ApiAuditLog> findEntityAuditHistory(
            @Param("entityType") String entityType,
            @Param("entityId") String entityId
    );

    // ==========================================
    // FAILED ACTIONS BY ACTOR
    // ==========================================

    @Query("""
        SELECT a
        FROM ApiAuditLog a
        WHERE a.actor.id = :accountId
          AND a.result IN ('FAILURE', 'DENIED')
        ORDER BY a.createdAt DESC
        """)
    List<ApiAuditLog> findUnsuccessfulActionsByActor(
            @Param("accountId") Long accountId
    );
}
