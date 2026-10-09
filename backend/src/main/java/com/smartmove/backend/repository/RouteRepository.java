
package com.smartmove.backend.repository;

import com.smartmove.backend.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RouteRepository
        extends JpaRepository<Route, Long> {

    List<Route> findByStatusIgnoreCase(String status);

    List<Route> findByServiceTypeIgnoreCase(String serviceType);

    List<Route> findByOriginContainingIgnoreCase(String origin);

    List<Route> findByDestinationContainingIgnoreCase(
            String destination
    );

    List<Route> findByOriginContainingIgnoreCaseAndDestinationContainingIgnoreCase(
            String origin,
            String destination
    );

    List<Route> findByStatusIgnoreCaseAndServiceTypeIgnoreCase(
            String status,
            String serviceType
    );

    List<Route> findByNameContainingIgnoreCase(String name);
}
