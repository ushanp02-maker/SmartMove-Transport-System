package com.smartmove.backend.service;

import org.springframework.stereotype.Service;
import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;

/** Calls Oracle SYS_REFCURSOR PL/SQL procedures; does not recalculate report data in Java. */
@Service
public class PlsqlReportService {
    private final DataSource dataSource;
    private static final Map<String, String> PROCEDURES = Map.of(
        "routes", "route_popularity",
        "revenue", "revenue",
        "passenger-history", "passenger_history",
        "maintenance-due", "maintenance_due",
        "drivers", "driver_performance",
        "bookings", "booking_summary",
        "fleet", "fleet_utilization",
        "daily-revenue", "daily_revenue",
        "maintenance-cost", "maintenance_cost"
    );
    public PlsqlReportService(DataSource dataSource) { this.dataSource = dataSource; }

    public List<Map<String, Object>> report(String type, LocalDate start, LocalDate end,
                                              Long passengerId) {
        String procedure = PROCEDURES.get(type);
        if (procedure == null) throw new IllegalArgumentException("Unknown report: " + type);
        if (List.of("revenue", "bookings", "daily-revenue", "maintenance-cost").contains(type)
                && (start == null || end == null || start.isAfter(end))) {
            throw new IllegalArgumentException("Valid start and end dates are required");
        }
        if ("passenger-history".equals(type) && passengerId == null) {
            throw new IllegalArgumentException("Passenger ID is required");
        }
        boolean dateRange = List.of("revenue", "bookings", "daily-revenue", "maintenance-cost").contains(type);
        boolean oneDate = "maintenance-due".equals(type);
        boolean oneId = "passenger-history".equals(type);
        int params = dateRange ? 3 : oneDate || oneId ? 2 : 1;
        String placeholders = String.join(",", Collections.nCopies(params, "?"));
        try (Connection conn = dataSource.getConnection();
             CallableStatement call = conn.prepareCall("{call sm_reports." + procedure + "(" + placeholders + ")}")) {
            if (dateRange) {
                call.setDate(1, Date.valueOf(start));
                call.setDate(2, Date.valueOf(end));
            } else if (oneDate) {
                call.setDate(1, Date.valueOf(end == null ? LocalDate.now().plusDays(30) : end));
            } else if (oneId) {
                call.setLong(1, passengerId);
            }
            call.registerOutParameter(params, Types.REF_CURSOR);
            call.execute();
            List<Map<String, Object>> rows = new ArrayList<>();
            try (ResultSet rs = (ResultSet) call.getObject(params)) {
                ResultSetMetaData md = rs.getMetaData();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= md.getColumnCount(); i++) {
                        Object value = rs.getObject(i);
                        if (value instanceof Timestamp ts) value = ts.toLocalDateTime();
                        if (value instanceof Date date) value = date.toLocalDate();
                        row.put(md.getColumnLabel(i).toLowerCase(Locale.ROOT), value);
                    }
                    rows.add(row);
                }
            }
            return rows;
        } catch (SQLException ex) {
            throw new IllegalStateException("Oracle PL/SQL report " + type +
                " failed. Ensure backend/database/smartmove_plsql.sql has been installed.", ex);
        }
    }
}
