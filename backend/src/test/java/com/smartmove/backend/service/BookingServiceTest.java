package com.smartmove.backend.service;

import com.smartmove.backend.entity.Trip;
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
}
