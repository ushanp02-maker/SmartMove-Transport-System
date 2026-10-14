
package com.smartmove.backend.repository;

import com.smartmove.backend.entity.Driver;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverRepository
        extends JpaRepository<Driver, Long> {

    // ==========================================
    // DRIVER LOOKUP
    // ==========================================

    Optional<Driver> findByEmailIgnoreCase(
            String email
    );

    Optional<Driver> findByLicenseNumber(
            String licenseNumber
    );

    // ==========================================
    // DUPLICATE VALIDATION
    // ==========================================

    boolean existsByEmailIgnoreCase(
            String email
    );

    boolean existsByLicenseNumber(
            String licenseNumber
    );

    // Used by DriverService.java
    boolean existsByLicenseNumberIgnoreCase(
            String licenseNumber
    );

    // ==========================================
    // DRIVER STATUS
    // ==========================================

    List<Driver> findByStatusIgnoreCase(
            String status
    );

    // ==========================================
    // DRIVER EXPERIENCE
    // ==========================================

    List<Driver> findByExperienceYearsGreaterThanEqual(
            Integer years
    );

    // ==========================================
    // DRIVER SEARCH
    // ==========================================

    List<Driver> findByNameContainingIgnoreCase(
            String name
    );
}
