
package com.smartmove.backend.repository;

import com.smartmove.backend.entity.Booking;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // ==========================================
    // BOOKING LOOKUPS
    // ==========================================

    Optional<Booking> findByBookingReference(
            String bookingReference
    );

    boolean existsByBookingReference(
            String bookingReference
    );

    // ==========================================
    // TRIP BOOKINGS
    // ==========================================

    List<Booking> findByTripId(
            Long tripId
    );

    boolean existsByTripId(
            Long tripId
    );

    List<Booking> findByTripIdOrderByBookedAtDesc(
            Long tripId
    );

    // ==========================================
    // ACTIVE BOOKINGS FOR A TRIP
    // ==========================================

    @Query("""
            SELECT b
            FROM Booking b
            WHERE b.trip.id = :tripId
              AND UPPER(b.status) IN :statuses
            ORDER BY b.bookedAt DESC
            """)
    List<Booking> findActiveBookingsForTrip(
            @Param("tripId") Long tripId,
            @Param("statuses") Set<String> statuses
    );

    // ==========================================
    // PASSENGER BOOKING HISTORY
    // ==========================================

    List<Booking> findByPassengerIdOrderByBookedAtDesc(
            Long passengerId
    );

    Page<Booking> findByPassengerIdOrderByBookedAtDesc(
            Long passengerId,
            Pageable pageable
    );

    List<Booking>
    findByPassengerIdAndStatusIgnoreCaseOrderByBookedAtDesc(
            Long passengerId,
            String status
    );

    // ==========================================
    // BOOKINGS BY STATUS
    // ==========================================

    List<Booking> findByStatusIgnoreCase(
            String status
    );

    Page<Booking> findByStatusIgnoreCase(
            String status,
            Pageable pageable
    );

    // Missing method required by BookingService.java
    List<Booking> findByStatusIgnoreCaseOrderByBookedAtDesc(
            String status
    );

    long countByStatusIgnoreCase(
            String status
    );

    // ==========================================
    // BOOKING COUNTS
    // ==========================================

    long countByPassengerId(
            Long passengerId
    );

    long countByTripId(
            Long tripId
    );

    long countByTripIdAndStatusIgnoreCase(
            Long tripId,
            String status
    );

    // ==========================================
    // SEAT OCCUPANCY BETWEEN ROUTE STOPS
    // ==========================================

    @Query("""
            SELECT COALESCE(SUM(b.seatCount), 0)
            FROM Booking b
            WHERE b.trip.id = :tripId
              AND b.status IN :statuses
              AND b.boardingStop.stopOrder < :destinationOrder
              AND b.destinationStop.stopOrder > :boardingOrder
            """)
    Long countOccupiedSeatsBetweenStops(
            @Param("tripId") Long tripId,
            @Param("boardingOrder") Integer boardingOrder,
            @Param("destinationOrder") Integer destinationOrder,
            @Param("statuses") List<String> statuses
    );

    // ==========================================
    // LOCK BOOKING DURING STATE CHANGES
    // ==========================================

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT b
            FROM Booking b
            WHERE b.id = :bookingId
            """)
    Optional<Booking> findByIdForUpdate(
            @Param("bookingId") Long bookingId
    );
}
