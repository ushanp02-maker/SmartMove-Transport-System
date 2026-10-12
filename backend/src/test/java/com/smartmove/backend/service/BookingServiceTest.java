package com.smartmove.backend.service;

import com.smartmove.backend.entity.Trip;
import com.smartmove.backend.entity.Booking;
import com.smartmove.backend.repository.BookingRepository;
import com.smartmove.backend.repository.PassengerRepository;
import com.smartmove.backend.repository.PaymentRepository;
import com.smartmove.backend.repository.RouteStopRepository;
import com.smartmove.backend.repository.TripRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BookingServiceTest {

    private BookingRepository bookingRepository;
    private PassengerRepository passengerRepository;
    private TripRepository tripRepository;
    private RouteStopRepository stopRepository;
    private PaymentRepository paymentRepository;

    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepository.class);
        passengerRepository = mock(PassengerRepository.class);
        tripRepository = mock(TripRepository.class);
        stopRepository = mock(RouteStopRepository.class);
        paymentRepository = mock(PaymentRepository.class);

        bookingService = new BookingService(
                bookingRepository,
                passengerRepository,
                tripRepository,
                stopRepository,
                paymentRepository
        );
    }

    @Test
    void createBookingShouldRejectNullRequest() {
        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> bookingService.createBooking(null)
                );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        verifyNoInteractions(
                bookingRepository,
                passengerRepository,
                tripRepository,
                stopRepository,
                paymentRepository
        );
    }

    @Test
    void createBookingShouldRejectNullTripId() {
        BookingService.CreateBookingRequest request =
                new BookingService.CreateBookingRequest(
                        1L,
                        null,
                        10L,
                        20L,
                        1
                );

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> bookingService.createBooking(request)
                );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        verifyNoInteractions(tripRepository);
    }

    @Test
    void createBookingShouldRejectNonScheduledTrip() {
        Trip trip = mock(Trip.class);

        when(trip.getStatus()).thenReturn("CANCELLED");
        when(tripRepository.findByIdForUpdate(2L))
                .thenReturn(Optional.of(trip));

        BookingService.CreateBookingRequest request =
                new BookingService.CreateBookingRequest(
                        1L,
                        2L,
                        10L,
                        20L,
                        1
                );

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> bookingService.createBooking(request)
                );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBookingShouldRejectDuplicateOverlappingBooking() {
        com.smartmove.backend.entity.Route route =
                mock(com.smartmove.backend.entity.Route.class);

        Trip trip = mock(Trip.class);

        when(trip.getId()).thenReturn(2L);
        when(trip.getStatus()).thenReturn("SCHEDULED");
        when(trip.getDepartureTime())
                .thenReturn(java.time.LocalDateTime.now().plusDays(1));
        when(trip.getRoute()).thenReturn(route);
        when(route.getStatus()).thenReturn("ACTIVE");

        when(tripRepository.findByIdForUpdate(2L))
                .thenReturn(Optional.of(trip));

        com.smartmove.backend.entity.Passenger passenger =
                mock(com.smartmove.backend.entity.Passenger.class);

        when(passenger.getId()).thenReturn(1L);
        when(passengerRepository.findById(1L))
                .thenReturn(Optional.of(passenger));

        com.smartmove.backend.entity.RouteStop boarding =
                mock(com.smartmove.backend.entity.RouteStop.class);

        com.smartmove.backend.entity.RouteStop destination =
                mock(com.smartmove.backend.entity.RouteStop.class);

        when(boarding.getStopOrder()).thenReturn(1);
        when(destination.getStopOrder()).thenReturn(3);

        when(boarding.getRoute()).thenReturn(route);
        when(destination.getRoute()).thenReturn(route);

        when(stopRepository.findById(10L))
                .thenReturn(Optional.of(boarding));

        when(stopRepository.findById(20L))
                .thenReturn(Optional.of(destination));

        when(bookingRepository.existsOverlappingActiveBooking(
                1L, 2L, 1, 3
        )).thenReturn(true);

        BookingService.CreateBookingRequest request =
                new BookingService.CreateBookingRequest(
                        1L, 2L, 10L, 20L, 1
                );

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> bookingService.createBooking(request)
                );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void cancelBookingShouldRejectNullId() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.cancelBooking(null)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(bookingRepository);
    }

    @Test
    void cancelBookingShouldRejectConfirmedBooking() {
        com.smartmove.backend.entity.Booking booking =
                mock(com.smartmove.backend.entity.Booking.class);

        when(booking.getStatus()).thenReturn("CONFIRMED");

        when(bookingRepository.findByIdForUpdate(5L))
                .thenReturn(Optional.of(booking));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.cancelBooking(5L)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void cancelBookingShouldRejectAfterDeparture() {
        com.smartmove.backend.entity.Booking booking =
                mock(com.smartmove.backend.entity.Booking.class);

        Trip trip = mock(Trip.class);

        when(booking.getStatus()).thenReturn("PENDING");
        when(booking.getTrip()).thenReturn(trip);

        when(trip.getDepartureTime()).thenReturn(
                java.time.LocalDateTime.now().minusHours(1)
        );

        when(bookingRepository.findByIdForUpdate(6L))
                .thenReturn(Optional.of(booking));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.cancelBooking(6L)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void getBookingByIdShouldRejectNullId() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.getBookingById(null)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(bookingRepository);
    }

    @Test
    void getBookingByIdShouldReturnNotFound() {
        when(bookingRepository.findById(999L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.getBookingById(999L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(bookingRepository).findById(999L);
    }

    @Test
    void getBookingByReferenceShouldRejectBlankReference() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.getBookingByReference("   ")
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(bookingRepository);
    }

    @Test
    void getBookingByReferenceShouldReturnNotFound() {
        when(bookingRepository.findByBookingReference("BK-UNKNOWN"))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.getBookingByReference("BK-UNKNOWN")
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(bookingRepository).findByBookingReference("BK-UNKNOWN");
    }

    @Test
    void getPassengerBookingsShouldRejectNullPassengerId() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.getPassengerBookings(null)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(passengerRepository, bookingRepository);
    }

    @Test
    void getPassengerBookingsShouldReturnNotFoundForUnknownPassenger() {
        when(passengerRepository.findById(999L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.getPassengerBookings(999L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(bookingRepository, never())
                .findByPassengerIdOrderByBookedAtDesc(anyLong());
    }

    @Test
    void getPassengerBookingsShouldReturnEmptyListWhenNoBookingsExist() {
        com.smartmove.backend.entity.Passenger passenger = mock(com.smartmove.backend.entity.Passenger.class);

        when(passengerRepository.findById(1L))
                .thenReturn(Optional.of(passenger));

        when(bookingRepository.findByPassengerIdOrderByBookedAtDesc(1L))
                .thenReturn(java.util.List.of());

        var result = bookingService.getPassengerBookings(1L);

        assertTrue(result.isEmpty());
        verify(bookingRepository)
                .findByPassengerIdOrderByBookedAtDesc(1L);
    }

    @Test
    void getPassengerBookingsByStatusShouldRejectBlankStatus() {
        com.smartmove.backend.entity.Passenger passenger = mock(com.smartmove.backend.entity.Passenger.class);

        when(passengerRepository.findById(1L))
                .thenReturn(Optional.of(passenger));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.getPassengerBookingsByStatus(1L, "   ")
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        verify(bookingRepository, never())
                .findByPassengerIdAndStatusIgnoreCaseOrderByBookedAtDesc(
                        anyLong(),
                        anyString()
                );
    }

    @Test
    void getTripBookingsShouldRejectNullTripId() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.getTripBookings(null)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(tripRepository, bookingRepository);
    }

    @Test
    void getTripBookingsShouldReturnNotFoundForUnknownTrip() {
        when(tripRepository.findById(999L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.getTripBookings(999L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verify(tripRepository).findById(999L);
        verify(bookingRepository, never())
                .findByTripIdOrderByBookedAtDesc(anyLong());
    }

    @Test
    void getTripBookingsShouldReturnEmptyListWhenNoBookingsExist() {
        Trip trip = mock(Trip.class);

        when(tripRepository.findById(1L))
                .thenReturn(Optional.of(trip));

        when(bookingRepository.findByTripIdOrderByBookedAtDesc(1L))
                .thenReturn(java.util.List.of());

        var result = bookingService.getTripBookings(1L);

        assertTrue(result.isEmpty());

        verify(bookingRepository)
                .findByTripIdOrderByBookedAtDesc(1L);
    }

    @Test
    void getSeatAvailabilityShouldRejectNullTripId() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.getSeatAvailability(null, 1L, 2L)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(tripRepository, bookingRepository);
    }

    @Test
    void getSeatAvailabilityShouldRejectUnscheduledTrip() {
        Trip trip = mock(Trip.class);

        when(tripRepository.findById(1L))
                .thenReturn(Optional.of(trip));
        when(trip.getStatus()).thenReturn("CANCELLED");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.getSeatAvailability(1L, 1L, 2L)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verifyNoInteractions(bookingRepository);
    }

    @Test
    void getSeatAvailabilityShouldRejectTripWithoutDepartureTime() {
        Trip trip = mock(Trip.class);

        when(tripRepository.findById(1L))
                .thenReturn(Optional.of(trip));
        when(trip.getStatus()).thenReturn("SCHEDULED");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.getSeatAvailability(1L, 1L, 2L)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verifyNoInteractions(bookingRepository);
    }

    @Test
    void getSeatAvailabilityShouldRejectInactiveRoute() {
        Trip trip = mock(Trip.class);
        com.smartmove.backend.entity.Route route =
                mock(com.smartmove.backend.entity.Route.class);

        when(tripRepository.findById(1L))
                .thenReturn(Optional.of(trip));
        when(trip.getStatus()).thenReturn("SCHEDULED");
        when(trip.getDepartureTime())
                .thenReturn(java.time.LocalDateTime.now().plusDays(1));
        when(trip.getRoute()).thenReturn(route);
        when(route.getStatus()).thenReturn("INACTIVE");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.getSeatAvailability(1L, 10L, 20L)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verifyNoInteractions(stopRepository, bookingRepository);
    }

    @Test
    void getSeatAvailabilityShouldRejectNullBoardingStopId() {
        Trip trip = mock(Trip.class);
        com.smartmove.backend.entity.Route route =
                mock(com.smartmove.backend.entity.Route.class);

        when(tripRepository.findById(1L))
                .thenReturn(Optional.of(trip));
        when(trip.getStatus()).thenReturn("SCHEDULED");
        when(trip.getDepartureTime())
                .thenReturn(java.time.LocalDateTime.now().plusDays(1));
        when(trip.getRoute()).thenReturn(route);
        when(route.getStatus()).thenReturn("ACTIVE");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.getSeatAvailability(1L, null, 20L)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(stopRepository, bookingRepository);
    }

    @Test
    void getSeatAvailabilityShouldRejectReverseStopOrder() {
        Trip trip = mock(Trip.class);
        com.smartmove.backend.entity.Route route =
                mock(com.smartmove.backend.entity.Route.class);
        com.smartmove.backend.entity.RouteStop boarding =
                mock(com.smartmove.backend.entity.RouteStop.class);
        com.smartmove.backend.entity.RouteStop destination =
                mock(com.smartmove.backend.entity.RouteStop.class);

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(trip.getStatus()).thenReturn("SCHEDULED");
        when(trip.getDepartureTime())
                .thenReturn(java.time.LocalDateTime.now().plusDays(1));
        when(trip.getRoute()).thenReturn(route);
        when(route.getStatus()).thenReturn("ACTIVE");
        when(route.getId()).thenReturn(100L);

        when(stopRepository.findById(10L)).thenReturn(Optional.of(boarding));
        when(stopRepository.findById(20L)).thenReturn(Optional.of(destination));

        when(boarding.getRoute()).thenReturn(route);
        when(destination.getRoute()).thenReturn(route);
        when(boarding.getStopOrder()).thenReturn(5);
        when(destination.getStopOrder()).thenReturn(3);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.getSeatAvailability(1L, 10L, 20L)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(bookingRepository);
    }

    @Test
    void getSeatAvailabilityShouldRejectSameStopOrder() {
        Trip trip = mock(Trip.class);
        com.smartmove.backend.entity.Route route =
                mock(com.smartmove.backend.entity.Route.class);
        com.smartmove.backend.entity.RouteStop boarding =
                mock(com.smartmove.backend.entity.RouteStop.class);
        com.smartmove.backend.entity.RouteStop destination =
                mock(com.smartmove.backend.entity.RouteStop.class);

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(trip.getStatus()).thenReturn("SCHEDULED");
        when(trip.getDepartureTime())
                .thenReturn(java.time.LocalDateTime.now().plusDays(1));
        when(trip.getRoute()).thenReturn(route);
        when(route.getStatus()).thenReturn("ACTIVE");
        when(route.getId()).thenReturn(100L);

        when(stopRepository.findById(10L)).thenReturn(Optional.of(boarding));
        when(stopRepository.findById(20L)).thenReturn(Optional.of(destination));

        when(boarding.getRoute()).thenReturn(route);
        when(destination.getRoute()).thenReturn(route);
        when(boarding.getStopOrder()).thenReturn(3);
        when(destination.getStopOrder()).thenReturn(3);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.getSeatAvailability(1L, 10L, 20L)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(bookingRepository);
    }

    private void setupSeatAvailabilityTrip(
            Trip trip,
            com.smartmove.backend.entity.Route route,
            com.smartmove.backend.entity.RouteStop boarding,
            com.smartmove.backend.entity.RouteStop destination,
            com.smartmove.backend.entity.Vehicle vehicle) {

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(trip.getStatus()).thenReturn("SCHEDULED");
        when(trip.getDepartureTime())
                .thenReturn(java.time.LocalDateTime.now().plusDays(1));
        when(trip.getRoute()).thenReturn(route);
        when(route.getStatus()).thenReturn("ACTIVE");
        when(route.getId()).thenReturn(100L);

        when(stopRepository.findById(10L)).thenReturn(Optional.of(boarding));
        when(stopRepository.findById(20L)).thenReturn(Optional.of(destination));

        when(boarding.getRoute()).thenReturn(route);
        when(destination.getRoute()).thenReturn(route);
        when(boarding.getStopOrder()).thenReturn(1);
        when(destination.getStopOrder()).thenReturn(4);

        when(trip.getVehicle()).thenReturn(vehicle);
        when(vehicle.getSeatingCapacity()).thenReturn(40);
    }

    @Test
    void getSeatAvailabilityShouldReturnFullCapacityWithoutBookings() {
        Trip trip = mock(Trip.class);
        com.smartmove.backend.entity.Route route =
                mock(com.smartmove.backend.entity.Route.class);
        com.smartmove.backend.entity.RouteStop boarding =
                mock(com.smartmove.backend.entity.RouteStop.class);
        com.smartmove.backend.entity.RouteStop destination =
                mock(com.smartmove.backend.entity.RouteStop.class);
        com.smartmove.backend.entity.Vehicle vehicle =
                mock(com.smartmove.backend.entity.Vehicle.class);

        setupSeatAvailabilityTrip(
                trip, route, boarding, destination, vehicle);

        when(bookingRepository.findActiveBookingsForTrip(
                eq(1L), anySet()))
                .thenReturn(java.util.List.of());

        var result = bookingService.getSeatAvailability(1L, 10L, 20L);

        assertEquals(40, result.totalCapacity());
        assertEquals(0, result.occupiedSeats());
        assertEquals(40, result.availableSeats());
    }

    @Test
    void getSeatAvailabilityShouldCountOverlappingBookings() {
        Trip trip = mock(Trip.class);
        com.smartmove.backend.entity.Route route =
                mock(com.smartmove.backend.entity.Route.class);
        com.smartmove.backend.entity.RouteStop boarding =
                mock(com.smartmove.backend.entity.RouteStop.class);
        com.smartmove.backend.entity.RouteStop destination =
                mock(com.smartmove.backend.entity.RouteStop.class);
        com.smartmove.backend.entity.Vehicle vehicle =
                mock(com.smartmove.backend.entity.Vehicle.class);

        setupSeatAvailabilityTrip(
                trip, route, boarding, destination, vehicle);

        com.smartmove.backend.entity.RouteStop stop2 =
                mock(com.smartmove.backend.entity.RouteStop.class);
        com.smartmove.backend.entity.RouteStop stop3 =
                mock(com.smartmove.backend.entity.RouteStop.class);

        when(stop2.getStopOrder()).thenReturn(2);
        when(stop3.getStopOrder()).thenReturn(3);

        Booking bookingA = mock(Booking.class);
        when(bookingA.getBoardingStop()).thenReturn(boarding);
        when(bookingA.getDestinationStop()).thenReturn(stop3);
        when(bookingA.getSeatCount()).thenReturn(10);

        Booking bookingB = mock(Booking.class);
        when(bookingB.getBoardingStop()).thenReturn(stop2);
        when(bookingB.getDestinationStop()).thenReturn(destination);
        when(bookingB.getSeatCount()).thenReturn(15);

        when(bookingRepository.findActiveBookingsForTrip(
                eq(1L), anySet()))
                .thenReturn(java.util.List.of(bookingA, bookingB));

        var result = bookingService.getSeatAvailability(1L, 10L, 20L);

        assertEquals(40, result.totalCapacity());
        assertEquals(25, result.occupiedSeats());
        assertEquals(15, result.availableSeats());
    }

    @Test
    void getSeatAvailabilityShouldReuseSeatsAcrossNonOverlappingBookings() {
        Trip trip = mock(Trip.class);
        com.smartmove.backend.entity.Route route =
                mock(com.smartmove.backend.entity.Route.class);
        com.smartmove.backend.entity.RouteStop boarding =
                mock(com.smartmove.backend.entity.RouteStop.class);
        com.smartmove.backend.entity.RouteStop destination =
                mock(com.smartmove.backend.entity.RouteStop.class);
        com.smartmove.backend.entity.Vehicle vehicle =
                mock(com.smartmove.backend.entity.Vehicle.class);

        setupSeatAvailabilityTrip(
                trip, route, boarding, destination, vehicle);

        com.smartmove.backend.entity.RouteStop stop2 =
                mock(com.smartmove.backend.entity.RouteStop.class);
        when(stop2.getStopOrder()).thenReturn(2);

        Booking bookingA = mock(Booking.class);
        when(bookingA.getBoardingStop()).thenReturn(boarding);
        when(bookingA.getDestinationStop()).thenReturn(stop2);
        when(bookingA.getSeatCount()).thenReturn(10);

        Booking bookingB = mock(Booking.class);
        when(bookingB.getBoardingStop()).thenReturn(stop2);
        when(bookingB.getDestinationStop()).thenReturn(destination);
        when(bookingB.getSeatCount()).thenReturn(15);

        when(bookingRepository.findActiveBookingsForTrip(
                eq(1L), anySet()))
                .thenReturn(java.util.List.of(bookingA, bookingB));

        var result = bookingService.getSeatAvailability(1L, 10L, 20L);

        assertEquals(40, result.totalCapacity());
        assertEquals(15, result.occupiedSeats());
        assertEquals(25, result.availableSeats());
    }

    @Test
    void getSeatAvailabilityShouldNeverReturnNegativeAvailableSeats() {
        Trip trip = mock(Trip.class);
        com.smartmove.backend.entity.Route route =
                mock(com.smartmove.backend.entity.Route.class);
        com.smartmove.backend.entity.RouteStop boarding =
                mock(com.smartmove.backend.entity.RouteStop.class);
        com.smartmove.backend.entity.RouteStop destination =
                mock(com.smartmove.backend.entity.RouteStop.class);
        com.smartmove.backend.entity.Vehicle vehicle =
                mock(com.smartmove.backend.entity.Vehicle.class);

        setupSeatAvailabilityTrip(
                trip, route, boarding, destination, vehicle);

        Booking booking = mock(Booking.class);
        when(booking.getBoardingStop()).thenReturn(boarding);
        when(booking.getDestinationStop()).thenReturn(destination);
        when(booking.getSeatCount()).thenReturn(45);

        when(bookingRepository.findActiveBookingsForTrip(
                eq(1L), anySet()))
                .thenReturn(java.util.List.of(booking));

        var result = bookingService.getSeatAvailability(1L, 10L, 20L);

        assertEquals(40, result.totalCapacity());
        assertEquals(45, result.occupiedSeats());
        assertEquals(0, result.availableSeats());
    }

    @Test
    void getSeatAvailabilityShouldRejectStopFromDifferentRoute() {
        Trip trip = mock(Trip.class);
        com.smartmove.backend.entity.Route route =
                mock(com.smartmove.backend.entity.Route.class);
        com.smartmove.backend.entity.Route otherRoute =
                mock(com.smartmove.backend.entity.Route.class);
        com.smartmove.backend.entity.RouteStop boarding =
                mock(com.smartmove.backend.entity.RouteStop.class);

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(trip.getStatus()).thenReturn("SCHEDULED");
        when(trip.getDepartureTime())
                .thenReturn(java.time.LocalDateTime.now().plusDays(1));
        when(trip.getRoute()).thenReturn(route);
        when(route.getStatus()).thenReturn("ACTIVE");
        when(route.getId()).thenReturn(100L);

        when(stopRepository.findById(10L))
                .thenReturn(Optional.of(boarding));
        when(boarding.getRoute()).thenReturn(otherRoute);
        when(otherRoute.getId()).thenReturn(200L);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.getSeatAvailability(1L, 10L, 20L)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(bookingRepository);
    }

    @Test
    void getSeatAvailabilityShouldRejectUnknownBoardingStop() {
        Trip trip = mock(Trip.class);
        com.smartmove.backend.entity.Route route =
                mock(com.smartmove.backend.entity.Route.class);

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(trip.getStatus()).thenReturn("SCHEDULED");
        when(trip.getDepartureTime())
                .thenReturn(java.time.LocalDateTime.now().plusDays(1));
        when(trip.getRoute()).thenReturn(route);
        when(route.getStatus()).thenReturn("ACTIVE");

        when(stopRepository.findById(10L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.getSeatAvailability(1L, 10L, 20L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verifyNoInteractions(bookingRepository);
    }

    private void setupCreateBookingCapacityScenario(
            int occupiedSeats,
            boolean duplicateBooking) {

        Trip trip = mock(Trip.class);
        com.smartmove.backend.entity.Route route =
                mock(com.smartmove.backend.entity.Route.class);
        com.smartmove.backend.entity.RouteStop boarding =
                mock(com.smartmove.backend.entity.RouteStop.class);
        com.smartmove.backend.entity.RouteStop destination =
                mock(com.smartmove.backend.entity.RouteStop.class);
        com.smartmove.backend.entity.Vehicle vehicle =
                mock(com.smartmove.backend.entity.Vehicle.class);
        com.smartmove.backend.entity.Passenger passenger =
                mock(com.smartmove.backend.entity.Passenger.class);

        when(tripRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(trip));

        when(trip.getStatus()).thenReturn("SCHEDULED");
        when(trip.getDepartureTime())
                .thenReturn(java.time.LocalDateTime.now().plusDays(1));
        when(trip.getRoute()).thenReturn(route);
        when(route.getStatus()).thenReturn("ACTIVE");

        when(passengerRepository.findById(5L))
                .thenReturn(Optional.of(passenger));

        when(stopRepository.findById(10L))
                .thenReturn(Optional.of(boarding));
        when(stopRepository.findById(20L))
                .thenReturn(Optional.of(destination));

        when(route.getId()).thenReturn(100L);
        when(boarding.getRoute()).thenReturn(route);
        when(destination.getRoute()).thenReturn(route);

        when(boarding.getStopOrder()).thenReturn(1);
        when(destination.getStopOrder()).thenReturn(4);

        when(passenger.getId()).thenReturn(5L);
        when(trip.getId()).thenReturn(1L);
        when(boarding.getId()).thenReturn(10L);
        when(destination.getId()).thenReturn(20L);

        when(bookingRepository.existsOverlappingActiveBooking(
                5L, 1L, 1, 4))
                .thenReturn(duplicateBooking);

        if (!duplicateBooking) {
            when(tripRepository.findById(1L))
                    .thenReturn(Optional.of(trip));

            when(trip.getVehicle()).thenReturn(vehicle);
            when(vehicle.getSeatingCapacity()).thenReturn(40);

            Booking existingBooking = mock(Booking.class);
            when(existingBooking.getBoardingStop()).thenReturn(boarding);
            when(existingBooking.getDestinationStop()).thenReturn(destination);
            when(existingBooking.getSeatCount()).thenReturn(occupiedSeats);

            when(bookingRepository.findActiveBookingsForTrip(
                    eq(1L), anySet()))
                    .thenReturn(java.util.List.of(existingBooking));
        }
    }

    @Test
    void createBookingShouldRejectWhenRequestedSeatsExceedAvailability() {
        setupCreateBookingCapacityScenario(38, false);

        var request = new BookingService.CreateBookingRequest(
                5L, 1L, 10L, 20L, 3
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.createBooking(request)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Not enough seats"));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void createBookingShouldRejectWhenNoSeatsAvailable() {
        setupCreateBookingCapacityScenario(40, false);

        var request = new BookingService.CreateBookingRequest(
                5L, 1L, 10L, 20L, 1
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.createBooking(request)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void createBookingShouldRejectDuplicateBeforeCheckingCapacity() {
        setupCreateBookingCapacityScenario(0, true);

        var request = new BookingService.CreateBookingRequest(
                5L, 1L, 10L, 20L, 2
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.createBooking(request)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertTrue(exception.getReason().contains("active booking"));

        verify(bookingRepository, never())
                .findActiveBookingsForTrip(anyLong(), anySet());

        verify(bookingRepository, never())
                .save(any(Booking.class));
    }

    @Test
    void calculateBookingQuoteShouldCalculateCorrectSegmentFare() {
        Trip trip = mock(Trip.class);
        com.smartmove.backend.entity.Route route =
                mock(com.smartmove.backend.entity.Route.class);
        com.smartmove.backend.entity.RouteStop boarding =
                mock(com.smartmove.backend.entity.RouteStop.class);
        com.smartmove.backend.entity.RouteStop destination =
                mock(com.smartmove.backend.entity.RouteStop.class);
        com.smartmove.backend.entity.Vehicle vehicle =
                mock(com.smartmove.backend.entity.Vehicle.class);

        setupSeatAvailabilityTrip(
                trip, route, boarding, destination, vehicle);

        when(trip.getFare())
                .thenReturn(new java.math.BigDecimal("500.00"));

        when(route.getDistanceKm())
                .thenReturn(new java.math.BigDecimal("100.00"));

        when(boarding.getDistanceFromStartKm())
                .thenReturn(new java.math.BigDecimal("20.00"));

        when(destination.getDistanceFromStartKm())
                .thenReturn(new java.math.BigDecimal("60.00"));

        when(bookingRepository.findActiveBookingsForTrip(
                eq(1L), anySet()))
                .thenReturn(java.util.List.of());

        var result = bookingService.calculateBookingQuote(
                1L, 10L, 20L, 2
        );

        assertEquals(2, result.seatCount());

        assertEquals(
                0,
                new java.math.BigDecimal("200.00")
                        .compareTo(result.farePerSeat())
        );

        assertEquals(
                0,
                new java.math.BigDecimal("400.00")
                        .compareTo(result.totalFare())
        );

        assertEquals(40, result.availableSeats());
    }

    private void setupSuccessfulBookingScenario() {
        setupCreateBookingCapacityScenario(30, false);

        Trip trip = tripRepository.findByIdForUpdate(1L).orElseThrow();

        com.smartmove.backend.entity.Route route = trip.getRoute();

        com.smartmove.backend.entity.RouteStop boarding =
                stopRepository.findById(10L).orElseThrow();

        com.smartmove.backend.entity.RouteStop destination =
                stopRepository.findById(20L).orElseThrow();

        when(trip.getFare())
                .thenReturn(new java.math.BigDecimal("500.00"));

        when(route.getDistanceKm())
                .thenReturn(new java.math.BigDecimal("100.00"));

        when(boarding.getDistanceFromStartKm())
                .thenReturn(new java.math.BigDecimal("20.00"));

        when(destination.getDistanceFromStartKm())
                .thenReturn(new java.math.BigDecimal("60.00"));

        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createBookingShouldSavePendingBookingWhenSeatsAvailable() {
        setupSuccessfulBookingScenario();

        var request = new BookingService.CreateBookingRequest(
                5L, 1L, 10L, 20L, 2
        );

        var result = bookingService.createBooking(request);

        org.mockito.ArgumentCaptor<Booking> captor =
                org.mockito.ArgumentCaptor.forClass(Booking.class);

        verify(bookingRepository).save(captor.capture());

        Booking savedBooking = captor.getValue();

        assertEquals("PENDING", savedBooking.getStatus());
        assertEquals("PENDING", result.status());
        assertEquals(2, savedBooking.getSeatCount());
        assertNotNull(savedBooking.getBookingReference());
    }

    @Test
    void createBookingShouldSaveCorrectTotalFare() {
        setupSuccessfulBookingScenario();

        var request = new BookingService.CreateBookingRequest(
                5L, 1L, 10L, 20L, 2
        );

        bookingService.createBooking(request);

        org.mockito.ArgumentCaptor<Booking> captor =
                org.mockito.ArgumentCaptor.forClass(Booking.class);

        verify(bookingRepository).save(captor.capture());

        Booking savedBooking = captor.getValue();

        assertEquals(
                0,
                new java.math.BigDecimal("400.00")
                        .compareTo(savedBooking.getTotalFare())
        );

        assertEquals(2, savedBooking.getSeatCount());
    }

    private Booking setupPaymentConfirmationBooking(String status) {
        Booking booking = new Booking();

        booking.setStatus(status);
        booking.setTotalFare(new java.math.BigDecimal("400.00"));
        booking.setBookingReference("SM-TEST-001");
        booking.setSeatCount(2);

        com.smartmove.backend.entity.Passenger passenger =
                mock(com.smartmove.backend.entity.Passenger.class);
        Trip trip = mock(Trip.class);
        com.smartmove.backend.entity.Route route =
                mock(com.smartmove.backend.entity.Route.class);
        com.smartmove.backend.entity.RouteStop boarding =
                mock(com.smartmove.backend.entity.RouteStop.class);
        com.smartmove.backend.entity.RouteStop destination =
                mock(com.smartmove.backend.entity.RouteStop.class);

        booking.setPassenger(passenger);
        booking.setTrip(trip);
        booking.setBoardingStop(boarding);
        booking.setDestinationStop(destination);

        when(passenger.getId()).thenReturn(5L);
        when(trip.getId()).thenReturn(1L);
        when(trip.getRoute()).thenReturn(route);
        when(boarding.getId()).thenReturn(10L);
        when(destination.getId()).thenReturn(20L);

        when(bookingRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(booking));

        return booking;
    }

    @Test
    void confirmBookingShouldConfirmWhenMatchingPaymentCompleted() {
        Booking booking = setupPaymentConfirmationBooking("PENDING");

        com.smartmove.backend.entity.Payment payment =
                mock(com.smartmove.backend.entity.Payment.class);

        when(payment.getStatus()).thenReturn("COMPLETED");
        when(payment.getAmount())
                .thenReturn(new java.math.BigDecimal("400.00"));

        when(paymentRepository.findByBookingIdOrderByCreatedAtDesc(1L))
                .thenReturn(java.util.List.of(payment));

        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = bookingService.confirmBooking(1L);

        assertEquals("CONFIRMED", booking.getStatus());
        assertEquals("CONFIRMED", result.status());

        verify(bookingRepository).save(booking);
    }

    @Test
    void confirmBookingShouldRejectWithoutMatchingCompletedPayment() {
        Booking booking = setupPaymentConfirmationBooking("PENDING");

        when(paymentRepository.findByBookingIdOrderByCreatedAtDesc(1L))
                .thenReturn(java.util.List.of());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.confirmBooking(1L)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("PENDING", booking.getStatus());

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void confirmBookingShouldReturnAlreadyConfirmedBooking() {
        Booking booking = setupPaymentConfirmationBooking("CONFIRMED");

        var result = bookingService.confirmBooking(1L);

        assertEquals("CONFIRMED", result.status());

        verifyNoInteractions(paymentRepository);
        verify(bookingRepository, never()).save(any(Booking.class));
    }
}
