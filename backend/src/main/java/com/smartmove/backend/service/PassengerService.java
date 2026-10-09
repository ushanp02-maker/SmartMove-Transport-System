
package com.smartmove.backend.service;

import com.smartmove.backend.entity.Passenger;
import com.smartmove.backend.entity.UserAccount;
import com.smartmove.backend.repository.PassengerRepository;
import com.smartmove.backend.repository.UserAccountRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
public class PassengerService {

    private final PassengerRepository passengerRepository;
    private final UserAccountRepository accountRepository;

    public PassengerService(
            PassengerRepository passengerRepository,
            UserAccountRepository accountRepository
    ) {
        this.passengerRepository = passengerRepository;
        this.accountRepository = accountRepository;
    }

    // ==========================================
    // PASSENGER PROFILE DTO
    // ==========================================

    public record PassengerProfile(
            Long id,
            String name,
            String email,
            String phone,
            String city,
            java.time.LocalDate joinedDate
    ) {
    }

    public record PassengerUpdateRequest(
            String name,
            String phone,
            String city
    ) {
    }

    // ==========================================
    // GET PASSENGER BY ID
    // ==========================================

    @Transactional(readOnly = true)
    public Passenger getPassengerById(Long passengerId) {
        if (passengerId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
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

    // ==========================================
    // PASSENGER PROFILE
    // ==========================================

    @Transactional(readOnly = true)
    public PassengerProfile getPassengerProfile(
            Long passengerId
    ) {
        return toProfile(getPassengerById(passengerId));
    }

    // Resolve a passenger through an Oracle
    // UserAccount rather than trusting a passenger
    // ID supplied by the frontend.
    @Transactional(readOnly = true)
    public PassengerProfile getProfileByAccountId(
            Long accountId
    ) {
        UserAccount account = getAccount(accountId);

        if (account.getPassengerId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No passenger profile linked to this account"
            );
        }

        return getPassengerProfile(account.getPassengerId());
    }

    // ==========================================
    // UPDATE PASSENGER PROFILE
    // ==========================================

    @Transactional
    public PassengerProfile updatePassengerProfile(
            Long passengerId,
            PassengerUpdateRequest request
    ) {
        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Update details are required"
            );
        }

        Passenger passenger = getPassengerById(passengerId);

        if (request.name() != null) {
            passenger.setName(
                    requireText(request.name(), "Name")
            );
        }

        if (request.phone() != null) {
            passenger.setPhone(
                    normalizeOptional(request.phone())
            );
        }

        if (request.city() != null) {
            passenger.setCity(
                    normalizeOptional(request.city())
            );
        }

        Passenger saved = passengerRepository.save(passenger);

        return toProfile(saved);
    }

    // Update the profile linked to a UserAccount.
    @Transactional
    public PassengerProfile updateProfileByAccountId(
            Long accountId,
            PassengerUpdateRequest request
    ) {
        UserAccount account = getAccount(accountId);

        if (account.getPassengerId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No passenger profile linked to this account"
            );
        }

        return updatePassengerProfile(
                account.getPassengerId(),
                request
        );
    }

    // ==========================================
    // ADMIN: ALL PASSENGERS
    // ==========================================

    @Transactional(readOnly = true)
    public List<PassengerProfile> getAllPassengers() {
        return passengerRepository.findAll()
                .stream()
                .map(this::toProfile)
                .toList();
    }

    // Paginated list for large datasets.
    @Transactional(readOnly = true)
    public Page<PassengerProfile> getPassengers(
            Pageable pageable
    ) {
        return passengerRepository.findAll(pageable)
                .map(this::toProfile);
    }

    // ==========================================
    // ADMIN: PASSENGER SEARCH
    // ==========================================

    @Transactional(readOnly = true)
    public List<PassengerProfile> searchPassengers(
            String keyword
    ) {
        if (keyword == null || keyword.isBlank()) {
            return getAllPassengers();
        }

        String search = keyword.trim()
                .toLowerCase(Locale.ROOT);

        return passengerRepository.findAll()
                .stream()
                .filter(passenger ->
                        containsIgnoreCase(
                                passenger.getName(), search
                        )
                                || containsIgnoreCase(
                                passenger.getEmail(), search
                        )
                                || containsIgnoreCase(
                                passenger.getPhone(), search
                        )
                                || containsIgnoreCase(
                                passenger.getCity(), search
                        )
                )
                .map(this::toProfile)
                .toList();
    }

    // ==========================================
    // ADMIN: PASSENGER STATISTICS
    // ==========================================

    @Transactional(readOnly = true)
    public long countPassengers() {
        return passengerRepository.count();
    }

    // ==========================================
    // INTERNAL HELPERS
    // ==========================================

    private UserAccount getAccount(Long accountId) {
        if (accountId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Account ID is required"
            );
        }

        return accountRepository.findById(accountId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Account not found"
                        )
                );
    }

    private PassengerProfile toProfile(
            Passenger passenger
    ) {
        return new PassengerProfile(
                passenger.getId(),
                passenger.getName(),
                passenger.getEmail(),
                passenger.getPhone(),
                passenger.getCity(),
                passenger.getJoinedDate()
        );
    }

    private String requireText(
            String value,
            String fieldName
    ) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    fieldName + " cannot be empty"
            );
        }

        return value.trim();
    }

    private String normalizeOptional(String value) {
        String normalized = value.trim();

        return normalized.isEmpty() ? null : normalized;
    }

    private boolean containsIgnoreCase(
            String value,
            String keyword
    ) {
        return value != null &&
                value.toLowerCase(Locale.ROOT)
                        .contains(keyword);
    }
}
