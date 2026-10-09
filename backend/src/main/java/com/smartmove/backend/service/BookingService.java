
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
            PaymentRepository paymentRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.passengerRepository = passengerRepository;
        this.tripRepository = tripRepository;
        this.stopRepository = stopRepository;
        this.paymentRepository = paymentRepository;
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
        List<Booking> activeBookings =
                bookingRepository.findActiveBookingsForTrip(
                        tripId,
                        OCCUPYING_STATUSES
                );

        int maximum = 0;

        for (int segment = boardingOrder;
             segment < destinationOrder;
             segment++) {

            int occupied = 0;

            for (Booking booking : activeBookings) {
                int bookingStart = booking
                        .getBoardingStop()
                        .getStopOrder();

                int bookingEnd = booking
                        .getDestinationStop()
                        .getStopOrder();

                if (bookingStart <= segment
                        && bookingEnd > segment) {
                    occupied += booking.getSeatCount();
                }
            }

            maximum = Math.max(maximum, occupied);
        }

        return maximum;
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
        if (request == null) {
            throw badRequest(
                    "Booking details are required"
            );
        }

        validateSeatCount(request.seatCount());

        if (request.tripId() == null) {
            throw badRequest("Trip ID is required");
        }

        // Serialize competing reservations for the
        // same trip using an Oracle row lock.
        Trip trip = tripRepository
                .findByIdForUpdate(request.tripId())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Trip not found"
                        )
                );

        if (!"SCHEDULED".equalsIgnoreCase(
                trip.getStatus()
        )) {
            throw conflict(
                    "This trip is not open for booking"
            );
        }

        if (!trip.getDepartureTime().isAfter(
                LocalDateTime.now()
        )) {
            throw conflict(
                    "Booking is closed for this trip"
            );
        }

        if (!"ACTIVE".equalsIgnoreCase(
                trip.getRoute().getStatus()
        )) {
            throw conflict(
                    "The route is not currently active"
            );
        }

        Passenger passenger =
                findPassenger(request.passengerId());

        RouteStop boarding = findStopOnTrip(
                trip,
                request.boardingStopId()
        );

        RouteStop destination = findStopOnTrip(
                trip,
                request.destinationStopId()
        );

        validateStopOrder(boarding, destination);

        SeatAvailability availability =
                getSeatAvailability(
                        trip.getId(),
                        boarding.getId(),
                        destination.getId()
                );

        if (request.seatCount()
                > availability.availableSeats()) {
            throw conflict(
                    "Not enough seats are available"
            );
        }

        BigDecimal farePerSeat = calculateSegmentFare(
                trip,
                boarding,
                destination
        );

        Booking booking = new Booking();

        booking.setPassenger(passenger);
        booking.setTrip(trip);
        booking.setBoardingStop(boarding);
        booking.setDestinationStop(destination);
        booking.setSeatCount(request.seatCount());
        booking.setTotalFare(
                farePerSeat.multiply(
                        BigDecimal.valueOf(
                                request.seatCount()
                        )
                )
        );

        // Payment is not automatically completed.
        // Seats are temporarily reserved as PENDING.
        booking.setStatus("PENDING");

        booking.setBookingReference(
                generateBookingReference()
        );

        return toProfile(
                bookingRepository.save(booking)
        );
    }

    // ==========================================
    // PAYMENT WORKFLOW: CONFIRM BOOKING
    // ==========================================

    @Transactional
    public BookingProfile confirmBooking(
            Long bookingId
    ) {
        Booking booking = lockBooking(bookingId);

        if ("CONFIRMED".equalsIgnoreCase(
                booking.getStatus()
        )) {
            return toProfile(booking);
        }

        if (!"PENDING".equalsIgnoreCase(
                booking.getStatus()
        )) {
            throw conflict(
                    "Only pending bookings can be confirmed"
            );
        }

        // Confirmation is restricted to bookings
        // with an existing completed payment record.
        boolean paid = paymentRepository
                .findByBookingIdOrderByCreatedAtDesc(
                        bookingId
                )
                .stream()
                .anyMatch(payment ->
                        "COMPLETED".equalsIgnoreCase(
                                payment.getStatus()
                        )
                                && payment.getAmount() != null
                                && payment.getAmount().compareTo(
                                booking.getTotalFare()
                        ) == 0
                );

        if (!paid) {
            throw conflict(
                    "A completed matching payment is required"
            );
        }

        booking.setStatus("CONFIRMED");

        return toProfile(
                bookingRepository.save(booking)
        );
    }

    // ==========================================
    // PASSENGER: CANCEL BOOKING
    // ==========================================

    @Transactional
    public BookingProfile cancelBooking(
            Long bookingId
    ) {
        Booking booking = lockBooking(bookingId);

        if ("CANCELLED".equalsIgnoreCase(
                booking.getStatus()
        )) {
            return toProfile(booking);
        }

        if (!"PENDING".equalsIgnoreCase(
                booking.getStatus()
        )) {
            throw conflict(
                    "Only unpaid pending bookings can be cancelled here"
            );
        }

        if (!booking.getTrip()
                .getDepartureTime()
                .isAfter(LocalDateTime.now())) {
            throw conflict(
                    "Trip departure time has passed"
            );
        }

        boolean hasSuccessfulPayment =
                paymentRepository
                        .findByBookingIdOrderByCreatedAtDesc(
                                bookingId
                        )
                        .stream()
                        .anyMatch(payment ->
                                "COMPLETED".equalsIgnoreCase(
                                        payment.getStatus()
                                )
                                        || "REFUNDED".equalsIgnoreCase(
                                        payment.getStatus()
                                )
                        );

        if (hasSuccessfulPayment) {
            throw conflict(
                    "Paid bookings require the refund workflow"
            );
        }

        booking.setStatus("CANCELLED");
        booking.setCancelledAt(LocalDateTime.now());

        return toProfile(
                bookingRepository.save(booking)
        );
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
        BigDecimal routeDistance =
                trip.getRoute().getDistanceKm();

        BigDecimal boardingDistance =
                boarding.getDistanceFromStartKm();

        BigDecimal destinationDistance =
                destination.getDistanceFromStartKm();

        if (routeDistance == null
                || routeDistance.signum() <= 0
                || boardingDistance == null
                || destinationDistance == null) {
            throw badRequest(
                    "Route distance information is incomplete"
            );
        }

        BigDecimal segmentDistance =
                destinationDistance.subtract(
                        boardingDistance
                );

        if (segmentDistance.signum() <= 0
                || segmentDistance.compareTo(
                routeDistance
        ) > 0) {
            throw badRequest(
                    "Invalid route segment distance"
            );
        }

        return trip.getFare()
                .multiply(segmentDistance)
                .divide(
                        routeDistance,
                        2,
                        RoundingMode.HALF_UP
                );
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
