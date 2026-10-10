package com.smartmove.backend.controller;

import com.smartmove.backend.service.PlsqlReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Admin-only by SecurityConfig's /api/reports/** rule. */
@RestController
@RequestMapping("/api/reports/plsql")
public class PlsqlReportController {
    private final PlsqlReportService reports;
    public PlsqlReportController(PlsqlReportService reports) { this.reports = reports; }

    @GetMapping("/{type}")
    public List<Map<String, Object>> get(
        @PathVariable String type,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(required = false) Long passengerId
    ) {
        return reports.report(type, startDate, endDate, passengerId);
    }
}
