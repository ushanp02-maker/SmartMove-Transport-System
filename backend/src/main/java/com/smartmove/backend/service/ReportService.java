
package com.smartmove.backend.service;

import com.smartmove.backend.entity.Booking;
import com.smartmove.backend.entity.Driver;
import com.smartmove.backend.entity.Maintenance;
import com.smartmove.backend.entity.Passenger;
import com.smartmove.backend.entity.Payment;
import com.smartmove.backend.entity.Route;
import com.smartmove.backend.entity.Trip;
import com.smartmove.backend.entity.Vehicle;

import com.smartmove.backend.repository.BookingRepository;
import com.smartmove.backend.repository.DriverRepository;
import com.smartmove.backend.repository.MaintenanceRepository;
import com.smartmove.backend.repository.PassengerRepository;
import com.smartmove.backend.repository.PaymentRepository;
import com.smartmove.backend.repository.RouteRepository;
import com.smartmove.backend.repository.TripRepository;
import com.smartmove.backend.repository.VehicleRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private static final Set<String> SUCCESSFUL_PAYMENT_STATUSES =
            Set.of("COMPLETED", "REFUNDED");

    private static final Set<String> ACTIVE_BOOKING_STATUSES =
            Set.of("PENDING", "CONFIRMED");

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final TripRepository tripRepository;
    private final PassengerRepository passengerRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final RouteRepository routeRepository;
    private final MaintenanceRepository maintenanceRepository;

    public ReportService(
            BookingRepository bookingRepository,
            PaymentRepository paymentRepository,
            TripRepository tripRepository,
            PassengerRepository passengerRepository,
            DriverRepository driverRepository,
            VehicleRepository vehicleRepository,
            RouteRepository routeRepository,
            MaintenanceRepository maintenanceRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.tripRepository = tripRepository;
        this.passengerRepository = passengerRepository;
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
        this.routeRepository = routeRepository;
        this.maintenanceRepository = maintenanceRepository;
    }

    // ==========================================
    // REPORT DATA TRANSFER OBJECTS
    // ==========================================

    public record DashboardReport(
            long totalPassengers,
            long totalDrivers,
            long totalVehicles,
            long availableVehicles,
            long totalRoutes,
            long activeRoutes,
            long totalTrips,
            long scheduledTrips,
            long completedTrips,
            long cancelledTrips,
            long totalBookings,
            long confirmedBookings,
            long pendingBookings,
            long cancelledBookings,
            long totalPayments,
            BigDecimal grossRevenue,
            BigDecimal totalRefunds,
            BigDecimal netRevenue,
            BigDecimal maintenanceExpenses
    ) {}

    public record RevenueReport(
            LocalDate startDate,
            LocalDate endDate,
            long successfulPayments,
            long refundedPayments,
            BigDecimal grossRevenue,
            BigDecimal refundsIssued,
            BigDecimal netRevenue
    ) {}

    public record RoutePerformanceReport(
            Long routeId,
            String routeName,
            String origin,
            String destination,
            long totalTrips,
            long completedTrips,
            long cancelledTrips,
            long totalBookings,
            long confirmedBookings,
            long bookedSeats,
            BigDecimal grossRevenue,
            BigDecimal refunds,
            BigDecimal netRevenue
    ) {}

    public record DriverPerformanceReport(
            Long driverId,
            String driverName,
            String status,
            long assignedTrips,
            long completedTrips,
            long cancelledTrips,
            long confirmedBookings,
            long transportedSeats
    ) {}

    public record PassengerActivityReport(
            Long passengerId,
            String passengerName,
            long totalBookings,
            long confirmedBookings,
            long cancelledBookings,
            long completedJourneys,
            BigDecimal totalPaid,
            BigDecimal totalRefunded
    ) {}

    public record FleetUtilizationReport(
            Long vehicleId,
            String registrationNumber,
            String vehicleName,
            String vehicleStatus,
            Integer seatingCapacity,
            long assignedTrips,
            long completedTrips,
            long cancelledTrips,
            long confirmedSeats,
            BigDecimal maintenanceCost
    ) {}

    public record BookingSummaryReport(
            LocalDate startDate,
            LocalDate endDate,
            long totalBookings,
            long pendingBookings,
            long confirmedBookings,
            long cancelledBookings,
            long expiredBookings,
            long totalSeatsBooked,
            BigDecimal totalBookingValue
    ) {}

    public record MaintenanceCostReport(
            LocalDate startDate,
            LocalDate endDate,
            long completedMaintenanceRecords,
            BigDecimal totalActualCost,
            BigDecimal totalEstimatedCost
    ) {}

    public record DailyRevenueReport(
            LocalDate date,
            long successfulPayments,
            BigDecimal grossRevenue,
            BigDecimal refunds,
            BigDecimal netRevenue
    ) {}

    // ==========================================
    // ADMIN DASHBOARD
    // ==========================================

    public DashboardReport getDashboardReport() {

        long totalPassengers = passengerRepository.count();
        long totalDrivers = driverRepository.count();
        long totalVehicles = vehicleRepository.count();
        long totalRoutes = routeRepository.count();
        long totalTrips = tripRepository.count();
        long totalBookings = bookingRepository.count();
        long totalPayments = paymentRepository.count();

        long availableVehicles = vehicleRepository.findAll()
                .stream()
                .filter(v -> statusIs(v.getStatus(), "AVAILABLE"))
                .count();

        long activeRoutes = routeRepository.findAll()
                .stream()
                .filter(r -> statusIs(r.getStatus(), "ACTIVE"))
                .count();

        long scheduledTrips = tripRepository.findAll()
                .stream()
                .filter(t -> statusIs(t.getStatus(), "SCHEDULED"))
                .count();

        long completedTrips = tripRepository.findAll()
                .stream()
                .filter(t -> statusIs(t.getStatus(), "COMPLETED"))
                .count();

        long cancelledTrips = tripRepository.findAll()
                .stream()
                .filter(t -> statusIs(t.getStatus(), "CANCELLED"))
                .count();

        long confirmedBookings = bookingRepository.findAll()
                .stream()
                .filter(b -> statusIs(b.getStatus(), "CONFIRMED"))
                .count();

        long pendingBookings = bookingRepository.findAll()
                .stream()
                .filter(b -> statusIs(b.getStatus(), "PENDING"))
                .count();

        long cancelledBookings = bookingRepository.findAll()
                .stream()
                .filter(b -> statusIs(b.getStatus(), "CANCELLED"))
                .count();

        BigDecimal gross = zeroIfNull(
                paymentRepository.calculateGrossRevenue()
        );

        BigDecimal refunds = zeroIfNull(
                paymentRepository.calculateTotalRefunds()
        );

        BigDecimal maintenance = zeroIfNull(
                maintenanceRepository.calculateTotalMaintenanceCost()
        );

        return new DashboardReport(
                totalPassengers,
                totalDrivers,
                totalVehicles,
                availableVehicles,
                totalRoutes,
                activeRoutes,
                totalTrips,
                scheduledTrips,
                completedTrips,
                cancelledTrips,
                totalBookings,
                confirmedBookings,
                pendingBookings,
                cancelledBookings,
                totalPayments,
                gross,
                refunds,
                gross.subtract(refunds),
                maintenance
        );
    }

    // ==========================================
    // REVENUE REPORT
    // ==========================================

    public RevenueReport getRevenueReport(
            LocalDate startDate,
            LocalDate endDate
    ) {
        validateDateRange(startDate, endDate);

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime endExclusive =
                endDate.plusDays(1).atStartOfDay();

        BigDecimal gross = zeroIfNull(
                paymentRepository.calculateGrossRevenueBetween(
                        start, endExclusive
                )
        );

        BigDecimal refunds = zeroIfNull(
                paymentRepository.calculateRefundsBetween(
                        start, endExclusive
                )
        );

        long successfulPayments = paymentRepository.findAll()
                .stream()
                .filter(p -> successfulPayment(p)
                        && within(
                        p.getPaymentDate(),
                        startDate,
                        endDate
                ))
                .count();

        long refundedPayments = paymentRepository.findAll()
                .stream()
                .filter(p -> p.getRefundedAt() != null
                        && within(
                        p.getRefundedAt(),
                        startDate,
                        endDate
                ))
                .count();

        return new RevenueReport(
                startDate,
                endDate,
                successfulPayments,
                refundedPayments,
                gross,
                refunds,
                gross.subtract(refunds)
        );
    }

    // ==========================================
    // DAILY REVENUE REPORT
    // ==========================================

    public List<DailyRevenueReport> getDailyRevenueReport(
            LocalDate startDate,
            LocalDate endDate
    ) {
        validateDateRange(startDate, endDate);

        List<Payment> payments = paymentRepository.findAll();
        List<DailyRevenueReport> result = new ArrayList<>();

        for (LocalDate date = startDate;
             !date.isAfter(endDate);
             date = date.plusDays(1)) {

            final LocalDate currentDate = date;

            long successfulCount = 0;
            BigDecimal gross = BigDecimal.ZERO;
            BigDecimal refunds = BigDecimal.ZERO;

            for (Payment payment : payments) {
                if (successfulPayment(payment)
                        && payment.getPaymentDate() != null
                        && payment.getPaymentDate()
                        .toLocalDate()
                        .equals(currentDate)) {

                    successfulCount++;
                    gross = gross.add(
                            zeroIfNull(payment.getAmount())
                    );
                }

                if (payment.getRefundedAt() != null
                        && payment.getRefundedAt()
                        .toLocalDate()
                        .equals(currentDate)) {

                    refunds = refunds.add(
                            zeroIfNull(payment.getRefundAmount())
                    );
                }
            }

            result.add(
                    new DailyRevenueReport(
                            currentDate,
                            successfulCount,
                            gross,
                            refunds,
                            gross.subtract(refunds)
                    )
            );
        }

        return result;
    }

    // ==========================================
    // BOOKING SUMMARY REPORT
    // ==========================================

    public BookingSummaryReport getBookingSummaryReport(
            LocalDate startDate,
            LocalDate endDate
    ) {
        validateDateRange(startDate, endDate);

        List<Booking> bookings = bookingRepository.findAll()
                .stream()
                .filter(b -> within(
                        b.getBookedAt(),
                        startDate,
                        endDate
                ))
                .toList();

        long pending = countBookingsByStatus(
                bookings, "PENDING"
        );
        long confirmed = countBookingsByStatus(
                bookings, "CONFIRMED"
        );
        long cancelled = countBookingsByStatus(
                bookings, "CANCELLED"
        );
        long expired = countBookingsByStatus(
                bookings, "EXPIRED"
        );

        long seats = bookings.stream()
                .filter(b -> ACTIVE_BOOKING_STATUSES.contains(
                        normalizedStatus(b.getStatus())
                ))
                .mapToLong(b -> b.getSeatCount() == null
                        ? 0L
                        : b.getSeatCount())
                .sum();

        BigDecimal value = bookings.stream()
                .filter(b -> ACTIVE_BOOKING_STATUSES.contains(
                        normalizedStatus(b.getStatus())
                ))
                .map(b -> zeroIfNull(b.getTotalFare()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new BookingSummaryReport(
                startDate,
                endDate,
                bookings.size(),
                pending,
                confirmed,
                cancelled,
                expired,
                seats,
                value
        );
    }

    // ==========================================
    // ROUTE PERFORMANCE REPORT
    // ==========================================

    public List<RoutePerformanceReport>
    getRoutePerformanceReport() {

        List<Route> routes = routeRepository.findAll();
        List<Trip> trips = tripRepository.findAll();
        List<Booking> bookings = bookingRepository.findAll();
        List<Payment> payments = paymentRepository.findAll();

        List<RoutePerformanceReport> result = new ArrayList<>();

        for (Route route : routes) {

            Long routeId = route.getId();

            List<Trip> routeTrips = trips.stream()
                    .filter(t -> t.getRoute() != null
                            && routeId.equals(
                            t.getRoute().getId()
                    ))
                    .toList();

            Set<Long> tripIds = routeTrips.stream()
                    .map(Trip::getId)
                    .collect(java.util.stream.Collectors.toSet());

            List<Booking> routeBookings = bookings.stream()
                    .filter(b -> b.getTrip() != null
                            && tripIds.contains(
                            b.getTrip().getId()
                    ))
                    .toList();

            Set<Long> bookingIds = routeBookings.stream()
                    .map(Booking::getId)
                    .collect(java.util.stream.Collectors.toSet());

            BigDecimal gross = BigDecimal.ZERO;
            BigDecimal refunds = BigDecimal.ZERO;

            for (Payment payment : payments) {
                if (payment.getBooking() == null
                        || !bookingIds.contains(
                        payment.getBooking().getId()
                )) {
                    continue;
                }

                if (successfulPayment(payment)) {
                    gross = gross.add(
                            zeroIfNull(payment.getAmount())
                    );
                }

                refunds = refunds.add(
                        zeroIfNull(payment.getRefundAmount())
                );
            }

            long confirmedSeats = routeBookings.stream()
                    .filter(b -> statusIs(
                            b.getStatus(), "CONFIRMED"
                    ))
                    .mapToLong(b -> b.getSeatCount() == null
                            ? 0L
                            : b.getSeatCount())
                    .sum();

            result.add(
                    new RoutePerformanceReport(
                            routeId,
                            route.getName(),
                            route.getOrigin(),
                            route.getDestination(),
                            routeTrips.size(),
                            countTripsByStatus(
                                    routeTrips, "COMPLETED"
                            ),
                            countTripsByStatus(
                                    routeTrips, "CANCELLED"
                            ),
                            routeBookings.size(),
                            countBookingsByStatus(
                                    routeBookings, "CONFIRMED"
                            ),
                            confirmedSeats,
                            gross,
                            refunds,
                            gross.subtract(refunds)
                    )
            );
        }

        result.sort(
                Comparator.comparing(
                        RoutePerformanceReport::netRevenue
                ).reversed()
        );

        return result;
    }

    // ==========================================
    // DRIVER PERFORMANCE REPORT
    // ==========================================

    public List<DriverPerformanceReport>
    getDriverPerformanceReport() {

        List<Driver> drivers = driverRepository.findAll();
        List<Trip> trips = tripRepository.findAll();
        List<Booking> bookings = bookingRepository.findAll();

        List<DriverPerformanceReport> result = new ArrayList<>();

        for (Driver driver : drivers) {

            Long driverId = driver.getId();

            List<Trip> driverTrips = trips.stream()
                    .filter(t -> t.getDriver() != null
                            && driverId.equals(
                            t.getDriver().getId()
                    ))
                    .toList();

            Set<Long> tripIds = driverTrips.stream()
                    .map(Trip::getId)
                    .collect(java.util.stream.Collectors.toSet());

            List<Booking> confirmedBookings = bookings.stream()
                    .filter(b -> statusIs(
                            b.getStatus(), "CONFIRMED"
                    ))
                    .filter(b -> b.getTrip() != null
                            && tripIds.contains(
                            b.getTrip().getId()
                    ))
                    .toList();

            long seats = confirmedBookings.stream()
                    .mapToLong(b -> b.getSeatCount() == null
                            ? 0L
                            : b.getSeatCount())
                    .sum();

            result.add(
                    new DriverPerformanceReport(
                            driverId,
                            driver.getName(),
                            driver.getStatus(),
                            driverTrips.size(),
                            countTripsByStatus(
                                    driverTrips, "COMPLETED"
                            ),
                            countTripsByStatus(
                                    driverTrips, "CANCELLED"
                            ),
                            confirmedBookings.size(),
                            seats
                    )
            );
        }

        result.sort(
                Comparator.comparingLong(
                        DriverPerformanceReport::completedTrips
                ).reversed()
        );

        return result;
    }

    // ==========================================
    // PASSENGER ACTIVITY REPORT
    // ==========================================

    public List<PassengerActivityReport>
    getPassengerActivityReport() {

        List<Passenger> passengers =
                passengerRepository.findAll();

        List<Booking> bookings =
                bookingRepository.findAll();

        List<Payment> payments =
                paymentRepository.findAll();

        List<PassengerActivityReport> result =
                new ArrayList<>();

        for (Passenger passenger : passengers) {

            Long passengerId = passenger.getId();

            List<Booking> passengerBookings = bookings.stream()
                    .filter(b -> b.getPassenger() != null
                            && passengerId.equals(
                            b.getPassenger().getId()
                    ))
                    .toList();

            Set<Long> bookingIds = passengerBookings.stream()
                    .map(Booking::getId)
                    .collect(java.util.stream.Collectors.toSet());

            BigDecimal paid = BigDecimal.ZERO;
            BigDecimal refunded = BigDecimal.ZERO;

            for (Payment payment : payments) {
                if (payment.getBooking() == null
                        || !bookingIds.contains(
                        payment.getBooking().getId()
                )) {
                    continue;
                }

                if (successfulPayment(payment)) {
                    paid = paid.add(
                            zeroIfNull(payment.getAmount())
                    );
                }

                refunded = refunded.add(
                        zeroIfNull(payment.getRefundAmount())
                );
            }

            long completedJourneys = passengerBookings.stream()
                    .filter(b -> statusIs(
                            b.getStatus(), "CONFIRMED"
                    ))
                    .filter(b -> b.getTrip() != null
                            && statusIs(
                            b.getTrip().getStatus(),
                            "COMPLETED"
                    ))
                    .count();

            result.add(
                    new PassengerActivityReport(
                            passengerId,
                            passenger.getName(),
                            passengerBookings.size(),
                            countBookingsByStatus(
                                    passengerBookings, "CONFIRMED"
                            ),
                            countBookingsByStatus(
                                    passengerBookings, "CANCELLED"
                            ),
                            completedJourneys,
                            paid,
                            refunded
                    )
            );
        }

        result.sort(
                Comparator.comparingLong(
                        PassengerActivityReport::totalBookings
                ).reversed()
        );

        return result;
    }

    // ==========================================
    // FLEET UTILIZATION REPORT
    // ==========================================

    public List<FleetUtilizationReport>
    getFleetUtilizationReport() {

        List<Vehicle> vehicles = vehicleRepository.findAll();
        List<Trip> trips = tripRepository.findAll();
        List<Booking> bookings = bookingRepository.findAll();

        List<FleetUtilizationReport> result = new ArrayList<>();

        for (Vehicle vehicle : vehicles) {

            Long vehicleId = vehicle.getId();

            List<Trip> vehicleTrips = trips.stream()
                    .filter(t -> t.getVehicle() != null
                            && vehicleId.equals(
                            t.getVehicle().getId()
                    ))
                    .toList();

            Set<Long> tripIds = vehicleTrips.stream()
                    .map(Trip::getId)
                    .collect(java.util.stream.Collectors.toSet());

            long confirmedSeats = bookings.stream()
                    .filter(b -> statusIs(
                            b.getStatus(), "CONFIRMED"
                    ))
                    .filter(b -> b.getTrip() != null
                            && tripIds.contains(
                            b.getTrip().getId()
                    ))
                    .mapToLong(b -> b.getSeatCount() == null
                            ? 0L
                            : b.getSeatCount())
                    .sum();

            BigDecimal maintenanceCost = zeroIfNull(
                    maintenanceRepository
                            .calculateMaintenanceCostByVehicle(
                                    vehicleId
                            )
            );

            result.add(
                    new FleetUtilizationReport(
                            vehicleId,
                            vehicle.getRegistrationNumber(),
                            vehicle.getName(),
                            vehicle.getStatus(),
                            vehicle.getSeatingCapacity(),
                            vehicleTrips.size(),
                            countTripsByStatus(
                                    vehicleTrips, "COMPLETED"
                            ),
                            countTripsByStatus(
                                    vehicleTrips, "CANCELLED"
                            ),
                            confirmedSeats,
                            maintenanceCost
                    )
            );
        }

        result.sort(
                Comparator.comparingLong(
                        FleetUtilizationReport::completedTrips
                ).reversed()
        );

        return result;
    }

    // ==========================================
    // MAINTENANCE COST REPORT
    // ==========================================

    public MaintenanceCostReport getMaintenanceCostReport(
            LocalDate startDate,
            LocalDate endDate
    ) {
        validateDateRange(startDate, endDate);

        List<Maintenance> records =
                maintenanceRepository.findAll()
                        .stream()
                        .filter(m -> statusIs(
                                m.getStatus(), "COMPLETED"
                        ))
                        .filter(m -> m.getCompletedDate() != null
                                && !m.getCompletedDate()
                                .isBefore(startDate)
                                && !m.getCompletedDate()
                                .isAfter(endDate))
                        .toList();

        BigDecimal actual = records.stream()
                .map(m -> zeroIfNull(m.getActualCost()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal estimated = records.stream()
                .map(m -> zeroIfNull(m.getEstimatedCost()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new MaintenanceCostReport(
                startDate,
                endDate,
                records.size(),
                actual,
                estimated
        );
    }

    // ==========================================
    // INTERNAL HELPERS
    // ==========================================

    private void validateDateRange(
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (startDate == null || endDate == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Start date and end date are required"
            );
        }

        if (endDate.isBefore(startDate)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "End date cannot be before start date"
            );
        }

        if (startDate.isBefore(
                endDate.minusYears(5)
        )) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Report period cannot exceed five years"
            );
        }
    }

    private boolean within(
            LocalDateTime timestamp,
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (timestamp == null) {
            return false;
        }

        LocalDate date = timestamp.toLocalDate();

        return !date.isBefore(startDate)
                && !date.isAfter(endDate);
    }

    private String normalizedStatus(String status) {
        return status == null
                ? ""
                : status.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private boolean statusIs(
            String actual,
            String expected
    ) {
        return expected.equals(normalizedStatus(actual));
    }

    private boolean successfulPayment(Payment payment) {
        return SUCCESSFUL_PAYMENT_STATUSES.contains(
                normalizedStatus(payment.getStatus())
        );
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private long countBookingsByStatus(
            List<Booking> bookings,
            String status
    ) {
        return bookings.stream()
                .filter(b -> statusIs(b.getStatus(), status))
                .count();
    }

    private long countTripsByStatus(
            List<Trip> trips,
            String status
    ) {
        return trips.stream()
                .filter(t -> statusIs(t.getStatus(), status))
                .count();
    }
}
