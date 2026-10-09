
package com.smartmove.backend.controller;

import com.smartmove.backend.service.ReportService;

import com.smartmove.backend.service.ReportService.DashboardReport;
import com.smartmove.backend.service.ReportService.RevenueReport;
import com.smartmove.backend.service.ReportService.DailyRevenueReport;
import com.smartmove.backend.service.ReportService.BookingSummaryReport;
import com.smartmove.backend.service.ReportService.RoutePerformanceReport;
import com.smartmove.backend.service.ReportService.DriverPerformanceReport;
import com.smartmove.backend.service.ReportService.PassengerActivityReport;
import com.smartmove.backend.service.ReportService.FleetUtilizationReport;
import com.smartmove.backend.service.ReportService.MaintenanceCostReport;

import jakarta.validation.constraints.NotNull;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@Validated
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    // ==========================================
    // ADMIN: DASHBOARD OVERVIEW
    // ==========================================

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardReport> getDashboardReport() {
        return ResponseEntity.ok(
                reportService.getDashboardReport()
        );
    }

    // ==========================================
    // ADMIN: REVENUE SUMMARY
    // ==========================================

    @GetMapping("/revenue")
    public ResponseEntity<RevenueReport> getRevenueReport(
            @RequestParam
            @NotNull
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam
            @NotNull
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) {
        return ResponseEntity.ok(
                reportService.getRevenueReport(
                        startDate,
                        endDate
                )
        );
    }

    // ==========================================
    // ADMIN: DAILY REVENUE BREAKDOWN
    // ==========================================

    @GetMapping("/revenue/daily")
    public ResponseEntity<List<DailyRevenueReport>>
    getDailyRevenueReport(
            @RequestParam
            @NotNull
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam
            @NotNull
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) {
        return ResponseEntity.ok(
                reportService.getDailyRevenueReport(
                        startDate,
                        endDate
                )
        );
    }

    // ==========================================
    // ADMIN: BOOKING SUMMARY
    // ==========================================

    @GetMapping("/bookings")
    public ResponseEntity<BookingSummaryReport>
    getBookingSummaryReport(
            @RequestParam
            @NotNull
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam
            @NotNull
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) {
        return ResponseEntity.ok(
                reportService.getBookingSummaryReport(
                        startDate,
                        endDate
                )
        );
    }

    // ==========================================
    // ADMIN: ROUTE PERFORMANCE
    // ==========================================

    @GetMapping("/routes")
    public ResponseEntity<List<RoutePerformanceReport>>
    getRoutePerformanceReport() {
        return ResponseEntity.ok(
                reportService.getRoutePerformanceReport()
        );
    }

    // ==========================================
    // ADMIN: DRIVER PERFORMANCE
    // ==========================================

    @GetMapping("/drivers")
    public ResponseEntity<List<DriverPerformanceReport>>
    getDriverPerformanceReport() {
        return ResponseEntity.ok(
                reportService.getDriverPerformanceReport()
        );
    }

    // ==========================================
    // ADMIN: PASSENGER ACTIVITY
    // ==========================================

    @GetMapping("/passengers")
    public ResponseEntity<List<PassengerActivityReport>>
    getPassengerActivityReport() {
        return ResponseEntity.ok(
                reportService.getPassengerActivityReport()
        );
    }

    // ==========================================
    // ADMIN: FLEET UTILIZATION
    // ==========================================

    @GetMapping("/fleet")
    public ResponseEntity<List<FleetUtilizationReport>>
    getFleetUtilizationReport() {
        return ResponseEntity.ok(
                reportService.getFleetUtilizationReport()
        );
    }

    // ==========================================
    // ADMIN: MAINTENANCE COST REPORT
    // ==========================================

    @GetMapping("/maintenance")
    public ResponseEntity<MaintenanceCostReport>
    getMaintenanceCostReport(
            @RequestParam
            @NotNull
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam
            @NotNull
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) {
        return ResponseEntity.ok(
                reportService.getMaintenanceCostReport(
                        startDate,
                        endDate
                )
        );
    }
}
