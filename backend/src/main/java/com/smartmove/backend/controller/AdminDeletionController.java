package com.smartmove.backend.controller;

import com.smartmove.backend.service.CurrentUserService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * Admin-only removal. Historical trip/booking data and linked login accounts
 * are deliberately protected from destructive deletion.
 */
@RestController
@RequestMapping("/api/admin/delete")
public class AdminDeletionController {
    private final JdbcTemplate jdbc;
    private final CurrentUserService currentUser;

    public AdminDeletionController(JdbcTemplate jdbc, CurrentUserService currentUser) {
        this.jdbc = jdbc;
        this.currentUser = currentUser;
    }

    private long count(String sql, Long id) {
        Long value = jdbc.queryForObject(sql, Long.class, id);
        return value == null ? 0 : value;
    }

    private void guard(boolean used, String explanation) {
        if (used) throw new ResponseStatusException(HttpStatus.CONFLICT, explanation);
    }

    private void delete(String table, Long id) {
        try {
            int removed = jdbc.update("DELETE FROM " + table + " WHERE ID = ?", id);
            if (removed == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found");
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "This record is referenced by other data. Deactivate it instead to preserve history.", ex);
        }
    }

    @DeleteMapping("/passengers/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void passenger(@PathVariable Long id) {
        currentUser.requireAdmin();
        guard(count("SELECT COUNT(*) FROM USER_ACCOUNTS WHERE PASSENGER_ID = ?", id) > 0,
            "Passenger has a login account. Disable that account before attempting removal.");
        guard(count("SELECT COUNT(*) FROM BOOKINGS WHERE PASSENGER_ID = ?", id) > 0,
            "Passenger has booking history. Preserve the record and disable their account instead.");
        delete("PASSENGERS", id);
    }

    @DeleteMapping("/drivers/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void driver(@PathVariable Long id) {
        currentUser.requireAdmin();
        guard(count("SELECT COUNT(*) FROM USER_ACCOUNTS WHERE DRIVER_ID = ?", id) > 0,
            "Driver has a linked login account. Disable the account before attempting removal.");
        guard(count("SELECT COUNT(*) FROM TRIPS WHERE DRIVER_ID = ?", id) > 0,
            "Driver has assigned trips. Mark the driver inactive to preserve trip history.");
        delete("DRIVERS", id);
    }

    @DeleteMapping("/vehicles/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void vehicle(@PathVariable Long id) {
        currentUser.requireAdmin();
        guard(count("SELECT COUNT(*) FROM TRIPS WHERE VEHICLE_ID = ?", id) > 0,
            "Vehicle has trip history. Set its status to unavailable or maintenance instead.");
        delete("VEHICLES", id);
    }

    @DeleteMapping("/routes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void route(@PathVariable Long id) {
        currentUser.requireAdmin();
        guard(count("SELECT COUNT(*) FROM TRIPS WHERE ROUTE_ID = ?", id) > 0,
            "Route has trip history. Deactivate it instead of deleting historical data.");
        // Route stops belong to the route and can be deleted only when no trips refer to it.
        jdbc.update("DELETE FROM ROUTE_STOPS WHERE ROUTE_ID = ?", id);
        delete("ROUTES", id);
    }
}
