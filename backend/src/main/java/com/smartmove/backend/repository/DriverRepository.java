
package com.smartmove.backend.repository;

import com.smartmove.backend.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverRepository
        extends JpaRepository<Driver, Long> {

    Optional<Driver> findByEmailIgnoreCase(String email);

    Optional<Driver> findByLicenseNumber(String licenseNumber);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByLicenseNumber(String licenseNumber);

    List<Driver> findByStatusIgnoreCase(String status);

    List<Driver> findByExperienceYearsGreaterThanEqual(
            Integer years
    );

    List<Driver> findByNameContainingIgnoreCase(String name);
}
