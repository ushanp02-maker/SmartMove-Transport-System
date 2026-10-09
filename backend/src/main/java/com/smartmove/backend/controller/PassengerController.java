
package com.smartmove.backend.controller;

import com.smartmove.backend.service.PassengerService;
import com.smartmove.backend.service.PassengerService.PassengerProfile;
import com.smartmove.backend.service.PassengerService.PassengerUpdateRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

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
@RequestMapping("/api/passengers")
@Validated
public class PassengerController {

    private final PassengerService passengerService;

    public PassengerController(
            PassengerService passengerService
    ) {
        this.passengerService = passengerService;
    }

    // ==========================================
    // PASSENGER PROFILE
    // ==========================================

    // Retrieve a passenger profile by ID.
    //
    // Must be restricted to the profile owner
    // or an authorized administrator when
    // authentication is integrated.

    @GetMapping("/{passengerId}")
    public ResponseEntity<PassengerProfile> getPassenger(
            @PathVariable Long passengerId
    ) {
        return ResponseEntity.ok(
                passengerService.getPassengerProfile(
                        passengerId
                )
        );
    }

    // ==========================================
    // ACCOUNT-LINKED PROFILE
    // ==========================================

    // Temporary account-ID-based profile lookup.
    //
    // IMPORTANT:
    // Do not expose this route to untrusted clients
    // until server-side authentication and ownership
    // verification are implemented.

    @GetMapping("/account/{accountId}")
    public ResponseEntity<PassengerProfile>
    getPassengerByAccount(
            @PathVariable Long accountId
    ) {
        return ResponseEntity.ok(
                passengerService.getProfileByAccountId(
                        accountId
                )
        );
    }

    // ==========================================
    // UPDATE PASSENGER PROFILE
    // ==========================================

    @PutMapping("/{passengerId}")
    public ResponseEntity<PassengerProfile>
    updatePassenger(
            @PathVariable Long passengerId,
            @Valid @RequestBody UpdatePassengerRequest request
    ) {
        return ResponseEntity.ok(
                passengerService.updatePassengerProfile(
                        passengerId,
                        new PassengerUpdateRequest(
                                request.name(),
                                request.phone(),
                                request.city()
                        )
                )
        );
    }

    // ==========================================
    // ACCOUNT-LINKED PROFILE UPDATE
    // ==========================================

    // The account ID must be verified against
    // the authenticated identity before this
    // endpoint can be safely exposed.

    @PutMapping("/account/{accountId}")
    public ResponseEntity<PassengerProfile>
    updatePassengerByAccount(
            @PathVariable Long accountId,
            @Valid @RequestBody UpdatePassengerRequest request
    ) {
        return ResponseEntity.ok(
                passengerService.updateProfileByAccountId(
                        accountId,
                        new PassengerUpdateRequest(
                                request.name(),
                                request.phone(),
                                request.city()
                        )
                )
        );
    }

    // ==========================================
    // ADMIN: ALL PASSENGERS
    // ==========================================

    @GetMapping
    public ResponseEntity<List<PassengerProfile>>
    getAllPassengers() {
        return ResponseEntity.ok(
                passengerService.getAllPassengers()
        );
    }

    // ==========================================
    // ADMIN: PAGINATED PASSENGERS
    // ==========================================

    @GetMapping("/page")
    public ResponseEntity<Page<PassengerProfile>>
    getPassengersPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page must be nonnegative and size must be between 1 and 100"
            );
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Direction.DESC,
                        "id"
                )
        );

        return ResponseEntity.ok(
                passengerService.getPassengers(pageable)
        );
    }

    // ==========================================
    // ADMIN: SEARCH PASSENGERS
    // ==========================================

    @GetMapping("/search")
    public ResponseEntity<List<PassengerProfile>>
    searchPassengers(
            @RequestParam(required = false) String keyword
    ) {
        return ResponseEntity.ok(
                passengerService.searchPassengers(keyword)
        );
    }

    // ==========================================
    // ADMIN: PASSENGER STATISTICS
    // ==========================================

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>>
    getPassengerStatistics() {
        return ResponseEntity.ok(
                Map.of(
                        "totalPassengers",
                        passengerService.countPassengers()
                )
        );
    }

    // ==========================================
    // REQUEST DTO
    // ==========================================

    public record UpdatePassengerRequest(

            @Size(
                    max = 150,
                    message = "Name cannot exceed 150 characters"
            )
            String name,

            @Size(
                    max = 30,
                    message = "Phone cannot exceed 30 characters"
            )
            String phone,

            @Size(
                    max = 100,
                    message = "City cannot exceed 100 characters"
            )
            String city

    ) {
    }
}
