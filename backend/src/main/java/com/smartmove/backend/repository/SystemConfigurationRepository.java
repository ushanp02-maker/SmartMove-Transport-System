
package com.smartmove.backend.repository;

import com.smartmove.backend.entity.SystemConfiguration;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SystemConfigurationRepository
        extends JpaRepository<SystemConfiguration, Long> {

    // ==========================================
    // CONFIGURATION LOOKUP BY KEY
    // ==========================================

    Optional<SystemConfiguration> findByConfigKeyIgnoreCase(
            String configKey
    );

    Optional<SystemConfiguration>
    findByConfigKeyIgnoreCaseAndActiveTrue(
            String configKey
    );

    boolean existsByConfigKeyIgnoreCase(
            String configKey
    );

    boolean existsByConfigKeyIgnoreCaseAndIdNot(
            String configKey,
            Long id
    );

    // ==========================================
    // CONFIGURATION LOOKUP BY CATEGORY
    // ==========================================

    List<SystemConfiguration>
    findByCategoryIgnoreCaseOrderByConfigKeyAsc(
            String category
    );

    Page<SystemConfiguration>
    findByCategoryIgnoreCase(
            String category,
            Pageable pageable
    );

    // ==========================================
    // ACTIVE AND INACTIVE CONFIGURATIONS
    // ==========================================

    List<SystemConfiguration>
    findByActiveTrueOrderByCategoryAscConfigKeyAsc();

    List<SystemConfiguration>
    findByActiveFalseOrderByCategoryAscConfigKeyAsc();

    Page<SystemConfiguration> findByActive(
            Boolean active,
            Pageable pageable
    );

    // ==========================================
    // CONFIGURATIONS BY ACCESS LEVEL
    // ==========================================

    List<SystemConfiguration>
    findBySuperAdminOnlyTrueOrderByConfigKeyAsc();

    List<SystemConfiguration>
    findBySuperAdminOnlyFalseOrderByConfigKeyAsc();

    Page<SystemConfiguration> findBySuperAdminOnly(
            Boolean superAdminOnly,
            Pageable pageable
    );

    // ==========================================
    // SYSTEM-DEFINED CONFIGURATIONS
    // ==========================================

    List<SystemConfiguration>
    findBySystemDefinedTrueOrderByConfigKeyAsc();

    List<SystemConfiguration>
    findBySystemDefinedFalseOrderByConfigKeyAsc();

    // ==========================================
    // CONFIGURATION SEARCH
    // ==========================================

    List<SystemConfiguration>
    findByConfigKeyContainingIgnoreCaseOrderByConfigKeyAsc(
            String keyword
    );

    List<SystemConfiguration>
    findByDisplayNameContainingIgnoreCaseOrderByDisplayNameAsc(
            String keyword
    );

    Page<SystemConfiguration>
    findByConfigKeyContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(
            String configKey,
            String displayName,
            Pageable pageable
    );

    // ==========================================
    // CONFIGURATIONS UPDATED BY AN ACCOUNT
    // ==========================================

    List<SystemConfiguration>
    findByUpdatedByIdOrderByUpdatedAtDesc(
            Long accountId
    );

    Page<SystemConfiguration>
    findByUpdatedByIdOrderByUpdatedAtDesc(
            Long accountId,
            Pageable pageable
    );

    // ==========================================
    // CONFIGURATIONS CREATED BY AN ACCOUNT
    // ==========================================

    List<SystemConfiguration>
    findByCreatedByIdOrderByCreatedAtDesc(
            Long accountId
    );

    // ==========================================
    // CONFIGURATIONS UPDATED WITHIN A PERIOD
    // ==========================================

    List<SystemConfiguration>
    findByUpdatedAtBetweenOrderByUpdatedAtDesc(
            LocalDateTime start,
            LocalDateTime end
    );

    Page<SystemConfiguration>
    findByUpdatedAtBetween(
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable
    );

    // ==========================================
    // RECENTLY UPDATED CONFIGURATIONS
    // ==========================================

    List<SystemConfiguration>
    findTop20ByOrderByUpdatedAtDesc();

    // ==========================================
    // COUNTS AND STATISTICS
    // ==========================================

    long countByActiveTrue();

    long countByActiveFalse();

    long countBySuperAdminOnlyTrue();

    long countBySystemDefinedTrue();

    long countByCategoryIgnoreCase(
            String category
    );

    long countByUpdatedAtBetween(
            LocalDateTime start,
            LocalDateTime end
    );

    // ==========================================
    // CATEGORY STATISTICS
    // ==========================================

    @Query("""
        SELECT c.category, COUNT(c)
        FROM SystemConfiguration c
        GROUP BY c.category
        ORDER BY c.category ASC
        """)
    List<Object[]> countConfigurationsByCategory();

    // ==========================================
    // VALUE TYPE STATISTICS
    // ==========================================

    @Query("""
        SELECT c.valueType, COUNT(c)
        FROM SystemConfiguration c
        GROUP BY c.valueType
        ORDER BY c.valueType ASC
        """)
    List<Object[]> countConfigurationsByValueType();

    // ==========================================
    // ACTIVE CONFIGURATIONS BY CATEGORY
    // ==========================================

    @Query("""
        SELECT c
        FROM SystemConfiguration c
        WHERE UPPER(c.category) = UPPER(:category)
          AND c.active = true
        ORDER BY c.configKey ASC
        """)
    List<SystemConfiguration> findActiveByCategory(
            @Param("category") String category
    );

    // ==========================================
    // ADMIN-VISIBLE CONFIGURATIONS
    // ==========================================

    @Query("""
        SELECT c
        FROM SystemConfiguration c
        WHERE c.superAdminOnly = false
        ORDER BY c.category ASC, c.configKey ASC
        """)
    List<SystemConfiguration> findAdminVisibleConfigurations();

    @Query("""
        SELECT c
        FROM SystemConfiguration c
        WHERE c.superAdminOnly = false
        """)
    Page<SystemConfiguration> findAdminVisibleConfigurations(
            Pageable pageable
    );

    // ==========================================
    // ADMIN-VISIBLE CONFIGURATIONS BY CATEGORY
    // ==========================================

    @Query("""
        SELECT c
        FROM SystemConfiguration c
        WHERE c.superAdminOnly = false
          AND UPPER(c.category) = UPPER(:category)
        ORDER BY c.configKey ASC
        """)
    List<SystemConfiguration>
    findAdminVisibleByCategory(
            @Param("category") String category
    );

    // ==========================================
    // ACTIVE CONFIGURATION VALUES
    // ==========================================

    @Query("""
        SELECT c.configValue
        FROM SystemConfiguration c
        WHERE UPPER(c.configKey) = UPPER(:configKey)
          AND c.active = true
        """)
    Optional<String> findActiveValueByKey(
            @Param("configKey") String configKey
    );

    // ==========================================
    // CONFIGURATION ROW LOCK
    // ==========================================

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT c
        FROM SystemConfiguration c
        WHERE c.id = :configurationId
        """)
    Optional<SystemConfiguration> findByIdForUpdate(
            @Param("configurationId") Long configurationId
    );

    // ==========================================
    // CONFIGURATION KEY LOCK
    // ==========================================

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT c
        FROM SystemConfiguration c
        WHERE UPPER(c.configKey) = UPPER(:configKey)
        """)
    Optional<SystemConfiguration> findByKeyForUpdate(
            @Param("configKey") String configKey
    );

    // ==========================================
    // ALL CONFIGURATIONS SORTED
    // ==========================================

    List<SystemConfiguration>
    findAllByOrderByCategoryAscConfigKeyAsc();

    Page<SystemConfiguration>
    findAllByOrderByCategoryAscConfigKeyAsc(
            Pageable pageable
    );
}
