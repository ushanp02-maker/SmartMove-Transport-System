
package com.smartmove.backend.repository;

import com.smartmove.backend.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository
        extends JpaRepository<Vehicle, Long> {

    Optional<Vehicle> findByRegistrationNumberIgnoreCase(
            String registrationNumber
    );

    boolean existsByRegistrationNumberIgnoreCase(
            String registrationNumber
    );

    List<Vehicle> findByStatusIgnoreCase(String status);

    List<Vehicle> findByVehicleTypeIgnoreCase(String vehicleType);

    List<Vehicle> findBySeatingCapacityGreaterThanEqual(
            Integer minimumCapacity
    );

    List<Vehicle> findByNameContainingIgnoreCase(String name);

    List<Vehicle> findByNextServiceDateBefore(LocalDate date);

    List<Vehicle> findByNextServiceDateLessThanEqual(
            LocalDate date
    );

    List<Vehicle> findByStatusIgnoreCaseAndSeatingCapacityGreaterThanEqual(
            String status,
            Integer minimumCapacity
    );
}
