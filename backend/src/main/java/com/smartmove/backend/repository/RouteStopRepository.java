
package com.smartmove.backend.repository;

import com.smartmove.backend.entity.RouteStop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RouteStopRepository
        extends JpaRepository<RouteStop, Long> {

    // Get all stops for a route in travel order.
    List<RouteStop> findByRouteIdOrderByStopOrderAsc(
            Long routeId
    );

    // Find a specific stop on a route.
    Optional<RouteStop> findByRouteIdAndStopOrder(
            Long routeId,
            Integer stopOrder
    );

    // Find stops by name.
    List<RouteStop> findByStopNameContainingIgnoreCase(
            String stopName
    );

    // Check whether a stop order already exists.
    boolean existsByRouteIdAndStopOrder(
            Long routeId,
            Integer stopOrder
    );

    // Count stops belonging to a route.
    long countByRouteId(Long routeId);

    // Find routes passing through a boarding and
    // destination stop in the correct travel order.
    @Query("""
        SELECT DISTINCT boarding.route.id
        FROM RouteStop boarding, RouteStop destination
        WHERE boarding.route.id = destination.route.id
          AND LOWER(boarding.stopName) = LOWER(:boardingStop)
          AND LOWER(destination.stopName) = LOWER(:destinationStop)
          AND boarding.stopOrder < destination.stopOrder
        """)
    List<Long> findRouteIdsBetweenStops(
            @Param("boardingStop") String boardingStop,
            @Param("destinationStop") String destinationStop
    );

    // Find every stop associated with a given route.
    @Query("""
        SELECT s
        FROM RouteStop s
        WHERE s.route.id = :routeId
        ORDER BY s.stopOrder ASC
        """)
    List<RouteStop> getOrderedStops(
            @Param("routeId") Long routeId
    );
}
