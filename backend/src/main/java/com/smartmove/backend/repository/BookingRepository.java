
package com.smartmove.backend.repository;

import com.smartmove.backend.entity.Booking;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository
        extends JpaRepository<Booking, Long> {

    // Passenger booking history
    List<Booking> findByPassengerIdOrderByBookedAtDesc(
            Long passengerId
    );

    // Bookings for a particular trip
    List<Booking> findByTripIdOrderByBookedAtDesc(
            Long tripId
    );

    // Find ticket using its booking reference
    Optional<Booking> findByBookingReference(
            String bookingReference
    );

    // Check duplicate booking references
    boolean existsByBookingReference(
            String bookingReference
    );

    // Admin: filter bookings by status
    List<Booking> findByStatusIgnoreCaseOrderByBookedAtDesc(
            String status
    );

    // Passenger: bookings with a particular status
    List<Booking> findByPassengerIdAndStatusIgnoreCaseOrderByBookedAtDesc(
            Long passengerId,
            String status
    );

    // Admin: bookings made during a period
    List<Booking> findByBookedAtBetweenOrderByBookedAtDesc(
            LocalDateTime start,
            LocalDateTime end
    );

    // Count bookings belonging to a passenger
    long countByPassengerId(Long passengerId);

    // Count bookings for a trip
    long countByTripId(Long tripId);

    // Calculate occupied seats on a particular journey segment.
    //
    // A booking overlaps the requested segment when:
    // booking boarding order < requested destination order
    // AND booking destination order > requested boarding order.
    //
    // Cancelled or expired bookings are excluded by the
    // activeStatuses parameter.
    @Query("""
        SELECT COALESCE(SUM(b.seatCount), 0)
        FROM Booking b
        WHERE b.trip.id = :tripId
          AND b.status IN :activeStatuses
          AND b.boardingStop.stopOrder < :destinationOrder
          AND b.destinationStop.stopOrder > :boardingOrder
        """)
    Long countOccupiedSeatsBetweenStops(
            @Param("tripId") Long tripId,
            @Param("boardingOrder") Integer boardingOrder,
            @Param("destinationOrder") Integer destinationOrder,
            @Param("activeStatuses")
            Collection<String> activeStatuses
    );

    // Retrieve all active bookings for a trip.
    // Used for seat allocation, occupancy and reports.
    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.boardingStop
        JOIN FETCH b.destinationStop
        WHERE b.trip.id = :tripId
          AND b.status IN :activeStatuses
        ORDER BY b.boardingStop.stopOrder ASC
        """)
    List<Booking> findActiveBookingsForTrip(
            @Param("tripId") Long tripId,
            @Param("activeStatuses")
            Collection<String> activeStatuses
    );

    // Lock an existing booking for payment or cancellation.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT b FROM Booking b
        WHERE b.id = :bookingId
        """)
    Optional<Booking> findByIdForUpdate(
            @Param("bookingId") Long bookingId
    );

    // Passenger travel history for coursework reporting.
    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.trip t
        JOIN FETCH t.route
        WHERE b.passenger.id = :passengerId
          AND t.status = 'COMPLETED'
          AND b.status = 'CONFIRMED'
        ORDER BY t.departureTime DESC
        """)
    List<Booking> findPassengerTravelHistory(
            @Param("passengerId") Long passengerId
    );
}
