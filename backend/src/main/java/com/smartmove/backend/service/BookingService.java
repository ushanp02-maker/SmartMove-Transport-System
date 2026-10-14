
package com.smartmove.backend.service;

import com.smartmove.backend.entity.Booking;
import com.smartmove.backend.entity.Passenger;
import com.smartmove.backend.entity.Payment;
import com.smartmove.backend.entity.RouteStop;
import com.smartmove.backend.entity.Trip;

import com.smartmove.backend.repository.BookingRepository;
import com.smartmove.backend.repository.PassengerRepository;
import com.smartmove.backend.repository.PaymentRepository;
import com.smartmove.backend.repository.RouteStopRepository;
import com.smartmove.backend.repository.TripRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class BookingService {

    private static final Set<String> OCCUPYING_STATUSES =
            Set.of("PENDING", "CONFIRMED");

    private final BookingPaymentPlsqlGateway plsql;
    private final PortalNotificationService notifications;
    private final BookingRepository bookingRepository;
    private final PassengerRepository passengerRepository;
    private final TripRepository tripRepository;
    private final RouteStopRepository stopRepository;
    private final PaymentRepository paymentRepository;

    public BookingService(
            BookingRepository bookingRepository,
            PassengerRepository passengerRepository,
            TripRepository tripRepository,
            RouteStopRepository stopRepository,
            PaymentRepository paymentRepository,
            BookingPaymentPlsqlGateway plsql,
            PortalNotificationService notifications
    ) {
        this.bookingRepository = bookingRepository;
        this.passengerRepository = passengerRepository;
        this.tripRepository = tripRepository;
        this.stopRepository = stopRepository;
        this.paymentRepository = paymentRepository;
        this.plsql = plsql;
        this.notifications = notifications;
    }

    // ==========================================
    // DATA TRANSFER OBJECTS
    // ==========================================

    public record BookingProfile(
            Long id,
            String bookingReference,
            Long passengerId,
            String passengerName,
            Long tripId,
            String routeName,
            Long boardingStopId,
            String boardingStop,
            Long destinationStopId,
            String destinationStop,
            Integer seatCount,
            BigDecimal totalFare,
            String status,
            LocalDateTime departureTime,
            LocalDateTime arrivalTime,
            LocalDateTime bookedAt,
            LocalDateTime cancelledAt
    ) {
    }

    public record CreateBookingRequest(
            Long passengerId,
            Long tripId,
            Long boardingStopId,
            Long destinationStopId,
            Integer seatCount
    ) {
    }

    public record SeatAvailability(
            Long tripId,
            Long boardingStopId,
            Long destinationStopId,
            Integer totalCapacity,
            Integer occupiedSeats,
            Integer availableSeats
    ) {
    }

    public record BookingQuote(
            Long tripId,
            Long boardingStopId,
            Long destinationStopId,
            Integer seatCount,
            BigDecimal farePerSeat,
            BigDecimal totalFare,
            Integer availableSeats
    ) {
    }

    public record BookingStatistics(
            long totalBookings,
            long pendingBookings,
            long confirmedBookings,
            long cancelledBookings,
            long expiredBookings,
            long completedTripsWithBookings
    ) {
    }

    // ==========================================
    // BOOKING LOOKUP
    // ==========================================

    @Transactional(readOnly = true)
    public Booking getBookingById(Long bookingId) {
        if (bookingId == null) {
            throw badRequest("Booking ID is required");
        }

        return bookingRepository.findById(bookingId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Booking not found"
                        )
                );
    }

    @Transactional(readOnly = true)
    public BookingProfile getBookingProfile(Long bookingId) {
        return toProfile(getBookingById(bookingId));
    }

    @Transactional(readOnly = true)
    public BookingProfile getBookingByReference(
            String reference
    ) {
        if (reference == null || reference.isBlank()) {
            throw badRequest(
                    "Booking reference is required"
            );
        }

        Booking booking = bookingRepository
                .findByBookingReference(reference.trim())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Booking not found"
                        )
                );

        return toProfile(booking);
    }

    // ==========================================
    // PASSENGER: SEAT AVAILABILITY
    // ==========================================

    @Transactional(readOnly = true)
    public SeatAvailability getSeatAvailability(
            Long tripId,
            Long boardingStopId,
            Long destinationStopId
    ) {
        Trip trip = findTrip(tripId);

        RouteStop boarding = findStopOnTrip(
                trip, boardingStopId
        );

        RouteStop destination = findStopOnTrip(
                trip, destinationStopId
        );

        validateStopOrder(boarding, destination);

        int capacity = trip.getVehicle()
                .getSeatingCapacity();

        int maxOccupied = calculateMaximumOccupancy(
                tripId,
                boarding.getStopOrder(),
                destination.getStopOrder()
        );

        return new SeatAvailability(
                tripId,
                boardingStopId,
                destinationStopId,
                capacity,
                maxOccupied,
                Math.max(0, capacity - maxOccupied)
        );
    }

    // ==========================================
    // MAXIMUM OCCUPANCY PER SEGMENT
    // ==========================================

    private int calculateMaximumOccupancy(
            Long tripId,
            int boardingOrder,
            int destinationOrder
    ) {
        return plsql.maximumOccupied(tripId, boardingOrder, destinationOrder);
    }

    // ==========================================
    // PASSENGER: BOOKING QUOTE
    // ==========================================

    @Transactional(readOnly = true)
    public BookingQuote calculateBookingQuote(
            Long tripId,
            Long boardingStopId,
            Long destinationStopId,
            Integer seatCount
    ) {
        validateSeatCount(seatCount);

        Trip trip = findTrip(tripId);

        RouteStop boarding = findStopOnTrip(
                trip, boardingStopId
        );

        RouteStop destination = findStopOnTrip(
                trip, destinationStopId
        );

        validateStopOrder(boarding, destination);

        SeatAvailability availability =
                getSeatAvailability(
                        tripId,
                        boardingStopId,
                        destinationStopId
                );

        BigDecimal farePerSeat = calculateSegmentFare(
                trip,
                boarding,
                destination
        );

        BigDecimal totalFare = farePerSeat.multiply(
                BigDecimal.valueOf(seatCount)
        );

        return new BookingQuote(
                tripId,
                boardingStopId,
                destinationStopId,
                seatCount,
                farePerSeat,
                totalFare,
                availability.availableSeats()
        );
    }

    // ==========================================
    // PASSENGER: CREATE BOOKING
    // ==========================================

    @Transactional
    public BookingProfile createBooking(
            CreateBookingRequest request
    ) {
        if (request == null) throw badRequest("Booking details are required");
        validateSeatCount(request.seatCount());
        if (request.tripId() == null || request.passengerId() == null ||
            request.boardingStopId() == null || request.destinationStopId() == null) {
            throw badRequest("Passenger, trip and route stops are required");
        }
        Long id = plsql.createBooking(request.passengerId(),request.tripId(),
                request.boardingStopId(),request.destinationStopId(),request.seatCount());
        var saved = getBookingById(id);
        notifications.notifyPassenger(saved.getPassenger().getId(), "Booking created",
            "Booking #" + id + " was created. Complete payment to confirm your seat.",
            "BOOKING", "/passenger/bookings");
        return toProfile(saved);
    }

    // ==========================================
    // PAYMENT WORKFLOW: CONFIRM BOOKING
    // ==========================================

    @Transactional
    public BookingProfile confirmBooking(
            Long bookingId
    ) {
        if (bookingId == null) throw badRequest("Booking ID is required");
        plsql.confirmBooking(bookingId);
        var saved = getBookingById(bookingId);
        notifications.notifyPassenger(saved.getPassenger().getId(), "Booking confirmed",
            "Your booking #" + bookingId + " is confirmed.", "BOOKING", "/passenger/bookings");
        return toProfile(saved);
    }

    // ==========================================
    // PASSENGER: CANCEL BOOKING
    // ==========================================

    @Transactional
    public BookingProfile cancelBooking(
            Long bookingId
    ) {
        if (bookingId == null) throw badRequest("Booking ID is required");
        plsql.cancelBooking(bookingId);
        var saved = getBookingById(bookingId);
        notifications.notifyPassenger(saved.getPassenger().getId(), "Booking cancelled",
            "Your booking #" + bookingId + " was cancelled.", "BOOKING", "/passenger/bookings");
        return toProfile(saved);
    }

    // ==========================================
    // PASSENGER: BOOKING HISTORY
    // ==========================================

    @Transactional(readOnly = true)
    public List<BookingProfile> getPassengerBookings(
            Long passengerId
    ) {
        findPassenger(passengerId);

        return bookingRepository
                .findByPassengerIdOrderByBookedAtDesc(
                        passengerId
                )
                .stream()
                .map(this::toProfile)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingProfile> getPassengerBookingsByStatus(
            Long passengerId,
            String status
    ) {
        findPassenger(passengerId);

        return bookingRepository
                .findByPassengerIdAndStatusIgnoreCaseOrderByBookedAtDesc(
                        passengerId,
                        requireText(status, "Booking status")
                )
                .stream()
                .map(this::toProfile)
                .toList();
    }

    // ==========================================
    // ADMIN: TRIP BOOKINGS
    // ==========================================

    @Transactional(readOnly = true)
    public List<BookingProfile> getTripBookings(
            Long tripId
    ) {
        findTrip(tripId);

        return bookingRepository
                .findByTripIdOrderByBookedAtDesc(
                        tripId
                )
                .stream()
                .map(this::toProfile)
                .toList();
    }

    // ==========================================
    // ADMIN: ALL BOOKINGS
    // ==========================================

    @Transactional(readOnly = true)
    public List<BookingProfile> getAllBookings() {
        return bookingRepository.findAll()
                .stream()
                .map(this::toProfile)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<BookingProfile> getBookings(
            Pageable pageable
    ) {
        return bookingRepository.findAll(pageable)
                .map(this::toProfile);
    }

    @Transactional(readOnly = true)
    public List<BookingProfile> getBookingsByStatus(
            String status
    ) {
        return bookingRepository
                .findByStatusIgnoreCaseOrderByBookedAtDesc(
                        requireText(status, "Booking status")
                )
                .stream()
                .map(this::toProfile)
                .toList();
    }

    // ==========================================
    // ADMIN: BOOKING STATISTICS
    // ==========================================

    @Transactional(readOnly = true)
    public BookingStatistics getBookingStatistics() {
        List<Booking> bookings =
                bookingRepository.findAll();

        long completedTripBookings =
                bookings.stream()
                        .filter(booking ->
                                "COMPLETED".equalsIgnoreCase(
                                        booking.getTrip().getStatus()
                                )
                        )
                        .count();

        return new BookingStatistics(
                bookings.size(),
                countStatus(bookings, "PENDING"),
                countStatus(bookings, "CONFIRMED"),
                countStatus(bookings, "CANCELLED"),
                countStatus(bookings, "EXPIRED"),
                completedTripBookings
        );
    }

    // ==========================================
    // FARE CALCULATION
    // ==========================================

    private BigDecimal calculateSegmentFare(
            Trip trip,
            RouteStop boarding,
            RouteStop destination
    ) {
        return plsql.segmentFare(trip.getId(), boarding.getId(), destination.getId());
    }

    // ==========================================
    // ENTITY LOOKUPS
    // ==========================================

    private Trip findTrip(Long tripId) {
        if (tripId == null) {
            throw badRequest("Trip ID is required");
        }

        return tripRepository.findById(tripId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Trip not found"
                        )
                );
    }

    private Passenger findPassenger(Long passengerId) {
        if (passengerId == null) {
            throw badRequest(
                    "Passenger ID is required"
            );
        }

        return passengerRepository.findById(passengerId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Passenger not found"
                        )
                );
    }

    private RouteStop findStopOnTrip(
            Trip trip,
            Long stopId
    ) {
        if (stopId == null) {
            throw badRequest(
                    "Route stop ID is required"
            );
        }

        RouteStop stop = stopRepository
                .findById(stopId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Route stop not found"
                        )
                );

        if (!trip.getRoute().getId().equals(
                stop.getRoute().getId()
        )) {
            throw badRequest(
                    "Stop does not belong to the trip route"
            );
        }

        return stop;
    }

    private Booking lockBooking(Long bookingId) {
        if (bookingId == null) {
            throw badRequest(
                    "Booking ID is required"
            );
        }

        return bookingRepository
                .findByIdForUpdate(bookingId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Booking not found"
                        )
                );
    }

    // ==========================================
    // VALIDATION HELPERS
    // ==========================================

    private void validateStopOrder(
            RouteStop boarding,
            RouteStop destination
    ) {
        if (boarding.getStopOrder()
                >= destination.getStopOrder()) {
            throw badRequest(
                    "Destination must come after boarding stop"
            );
        }
    }

    private void validateSeatCount(Integer seats) {
        if (seats == null || seats < 1
                || seats > 20) {
            throw badRequest(
                    "Seat count must be between 1 and 20"
            );
        }
    }

    private String requireText(
            String value,
            String field
    ) {
        if (value == null || value.isBlank()) {
            throw badRequest(
                    field + " is required"
            );
        }

        return value.trim();
    }

    private long countStatus(
            List<Booking> bookings,
            String status
    ) {
        return bookings.stream()
                .filter(booking ->
                        status.equalsIgnoreCase(
                                booking.getStatus()
                        )
                )
                .count();
    }

    private String generateBookingReference() {
        // UUID-based references make collisions
        // extremely unlikely. Oracle's unique
        // constraint provides final protection.
        return "SM-" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .toUpperCase(Locale.ROOT);
    }

    // ==========================================
    // ENTITY TO DTO
    // ==========================================

    private BookingProfile toProfile(Booking booking) {
        return new BookingProfile(
                booking.getId(),
                booking.getBookingReference(),
                booking.getPassenger().getId(),
                booking.getPassenger().getName(),
                booking.getTrip().getId(),
                booking.getTrip().getRoute().getName(),
                booking.getBoardingStop().getId(),
                booking.getBoardingStop().getStopName(),
                booking.getDestinationStop().getId(),
                booking.getDestinationStop().getStopName(),
                booking.getSeatCount(),
                booking.getTotalFare(),
                booking.getStatus(),
                booking.getTrip().getDepartureTime(),
                booking.getTrip().getArrivalTime(),
                booking.getBookedAt(),
                booking.getCancelledAt()
        );
    }

    private ResponseStatusException badRequest(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }

    private ResponseStatusException conflict(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                message
        );
    }
}
