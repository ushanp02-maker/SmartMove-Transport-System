
package com.smartmove.backend.controller;

import com.smartmove.backend.service.BookingService;
import com.smartmove.backend.service.CurrentUserService;
import com.smartmove.backend.service.BookingService.BookingProfile;
import com.smartmove.backend.service.BookingService.BookingQuote;
import com.smartmove.backend.service.BookingService.BookingStatistics;
import com.smartmove.backend.service.BookingService.CreateBookingRequest;
import com.smartmove.backend.service.BookingService.SeatAvailability;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
@Validated
public class BookingController {

    private final BookingService bookingService;
    private final CurrentUserService currentUserService;

    public BookingController(BookingService bookingService, CurrentUserService currentUserService) {
        this.bookingService = bookingService;
        this.currentUserService = currentUserService;
    }

    // ==========================================
    // ADMIN: ALL BOOKINGS
    // ==========================================

    @GetMapping
    public ResponseEntity<List<BookingProfile>> getAllBookings() {
        currentUserService.requireAdmin();
        return ResponseEntity.ok(
                bookingService.getAllBookings()
        );
    }

    // ==========================================
    // ADMIN: PAGINATED BOOKINGS
    // ==========================================

    @GetMapping("/page")
    public ResponseEntity<Page<BookingProfile>> getBookingsPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        currentUserService.requireAdmin();
        if (page < 0 || size < 1 || size > 100) {
            throw badRequest(
                    "Page must be nonnegative and size must be between 1 and 100"
            );
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Direction.DESC,
                        "bookedAt"
                )
        );

        return ResponseEntity.ok(
                bookingService.getBookings(pageable)
        );
    }

    // ==========================================
    // PASSENGER: SEAT AVAILABILITY
    // ==========================================

    @GetMapping("/availability")
    public ResponseEntity<SeatAvailability> getSeatAvailability(
            @RequestParam @Positive Long tripId,
            @RequestParam @Positive Long boardingStopId,
            @RequestParam @Positive Long destinationStopId
    ) {
        return ResponseEntity.ok(
                bookingService.getSeatAvailability(
                        tripId,
                        boardingStopId,
                        destinationStopId
                )
        );
    }

    // ==========================================
    // PASSENGER: BOOKING QUOTATION
    // ==========================================

    @GetMapping("/quote")
    public ResponseEntity<BookingQuote> getBookingQuote(
            @RequestParam @Positive Long tripId,
            @RequestParam @Positive Long boardingStopId,
            @RequestParam @Positive Long destinationStopId,
            @RequestParam @Min(1) @Max(20) Integer seatCount
    ) {
        return ResponseEntity.ok(
                bookingService.calculateBookingQuote(
                        tripId,
                        boardingStopId,
                        destinationStopId,
                        seatCount
                )
        );
    }

    // ==========================================
    // ADMIN: BOOKING STATISTICS
    // ==========================================

    @GetMapping("/stats")
    public ResponseEntity<BookingStatistics> getBookingStatistics() {
        currentUserService.requireAdmin();
        return ResponseEntity.ok(
                bookingService.getBookingStatistics()
        );
    }

    // ==========================================
    // ADMIN: BOOKING COUNT
    // ==========================================

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> countBookings() {
        currentUserService.requireAdmin();
        return ResponseEntity.ok(
                Map.of(
                        "totalBookings",
                        bookingService.getBookingStatistics()
                                .totalBookings()
                )
        );
    }

    // ==========================================
    // ADMIN: FILTER BOOKINGS BY STATUS
    // ==========================================

    @GetMapping("/status/{status}")
    public ResponseEntity<List<BookingProfile>> getBookingsByStatus(
            @PathVariable @NotBlank String status
    ) {
        currentUserService.requireAdmin();
        return ResponseEntity.ok(
                bookingService.getBookingsByStatus(status)
        );
    }

    // ==========================================
    // PASSENGER: BOOKING HISTORY
    // ==========================================

    @GetMapping("/passenger/{passengerId}")
    public ResponseEntity<List<BookingProfile>> getPassengerBookings(
            @PathVariable @Positive Long passengerId
    ) {
        return ResponseEntity.ok(
                bookingService.getPassengerBookings(
                        verifiedPassenger(passengerId)
                )
        );
    }

    // ==========================================
    // PASSENGER: BOOKINGS BY STATUS
    // ==========================================

    @GetMapping("/passenger/{passengerId}/status/{status}")
    public ResponseEntity<List<BookingProfile>> getPassengerBookingsByStatus(
            @PathVariable @Positive Long passengerId,
            @PathVariable @NotBlank String status
    ) {
        return ResponseEntity.ok(
                bookingService.getPassengerBookingsByStatus(
                        verifiedPassenger(passengerId),
                        status
                )
        );
    }

    // ==========================================
    // ADMIN: BOOKINGS FOR A TRIP
    // ==========================================

    @GetMapping("/trip/{tripId}")
    public ResponseEntity<List<BookingProfile>> getTripBookings(
            @PathVariable @Positive Long tripId
    ) {
        currentUserService.requireAdmin();
        return ResponseEntity.ok(
                bookingService.getTripBookings(tripId)
        );
    }

    // ==========================================
    // BOOKING LOOKUP BY REFERENCE
    // ==========================================

    @GetMapping("/reference/{reference}")
    public ResponseEntity<BookingProfile> getBookingByReference(
            @PathVariable @NotBlank String reference
    ) {
        return ResponseEntity.ok(
                ownedBooking(bookingService.getBookingByReference(reference))
        );
    }

    // ==========================================
    // BOOKING LOOKUP BY ID
    // ==========================================

    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingProfile> getBooking(
            @PathVariable @Positive Long bookingId
    ) {
        return ResponseEntity.ok(
                ownedBooking(bookingService.getBookingProfile(bookingId))
        );
    }

    // ==========================================
    // PASSENGER: CREATE BOOKING
    // ==========================================

    @PostMapping
    public ResponseEntity<BookingProfile> createBooking(
            @Valid @RequestBody CreateBookingPayload request
    ) {
        BookingProfile booking = bookingService.createBooking(
                new CreateBookingRequest(
                        verifiedPassenger(request.passengerId()),
                        request.tripId(),
                        request.boardingStopId(),
                        request.destinationStopId(),
                        request.seatCount()
                )
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(booking);
    }

    // ==========================================
    // PAYMENT WORKFLOW: CONFIRM BOOKING
    // ==========================================

    @PatchMapping("/{bookingId}/confirm")
    public ResponseEntity<BookingProfile> confirmBooking(
            @PathVariable @Positive Long bookingId
    ) {
        return ResponseEntity.ok(
                bookingService.confirmBooking(
                        verifiedBookingId(bookingId)
                )
        );
    }

    // ==========================================
    // PASSENGER: CANCEL UNPAID BOOKING
    // ==========================================

    @PatchMapping("/{bookingId}/cancel")
    public ResponseEntity<BookingProfile> cancelBooking(
            @PathVariable @Positive Long bookingId
    ) {
        return ResponseEntity.ok(
                bookingService.cancelBooking(
                        verifiedBookingId(bookingId)
                )
        );
    }

    private Long verifiedPassenger(Long passengerId) {
        currentUserService.requirePassengerOwnership(passengerId);
        return passengerId;
    }

    private BookingProfile ownedBooking(BookingProfile booking) {
        currentUserService.requirePassengerOwnership(booking.passengerId());
        return booking;
    }

    private Long verifiedBookingId(Long bookingId) {
        ownedBooking(bookingService.getBookingProfile(bookingId));
        return bookingId;
    }

    // ==========================================
    // REQUEST DTO
    // ==========================================

    public record CreateBookingPayload(

            @NotNull(message = "Passenger ID is required")
            @Positive
            Long passengerId,

            @NotNull(message = "Trip ID is required")
            @Positive
            Long tripId,

            @NotNull(message = "Boarding stop is required")
            @Positive
            Long boardingStopId,

            @NotNull(message = "Destination stop is required")
            @Positive
            Long destinationStopId,

            @NotNull(message = "Seat count is required")
            @Min(1)
            @Max(20)
            Integer seatCount

    ) {
    }

    // ==========================================
    // ERROR HELPER
    // ==========================================

    private ResponseStatusException badRequest(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }
}
