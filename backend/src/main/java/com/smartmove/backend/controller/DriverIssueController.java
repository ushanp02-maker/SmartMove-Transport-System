package com.smartmove.backend.controller;

import com.smartmove.backend.entity.DriverIssue;
import com.smartmove.backend.repository.DriverIssueRepository;
import com.smartmove.backend.service.CurrentUserService;
import com.smartmove.backend.service.TripStatusService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/driver-issues")
public class DriverIssueController {
    private final CurrentUserService users;
    private final TripStatusService trips;
    private final DriverIssueRepository issues;

    public DriverIssueController(CurrentUserService users, TripStatusService trips, DriverIssueRepository issues) {
        this.users=users;
        this.trips=trips;
        this.issues=issues;
    }

    public record IssueRequest(@NotNull @Positive Long tripId,
                               @NotBlank @Size(max=1000) String description,
                               String priority) {}

    @PostMapping
    public ResponseEntity<DriverIssue> report(@Valid @RequestBody IssueRequest request) {
        users.requireDriver();
        var assigned = trips.getMyTrips().stream()
            .filter(t -> t.tripId().equals(request.tripId()))
            .findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Trip is not assigned to this driver"));
        if (assigned.vehicleId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assigned trip has no vehicle");
        }
        String priority = request.priority()==null ? "HIGH" : request.priority().toUpperCase();
        if (!Set.of("LOW","NORMAL","HIGH","URGENT").contains(priority)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported issue priority");
        }
        var created = issues.save(new DriverIssue(users.getCurrentDriverId(), users.getCurrentAccountId(),
                assigned.tripId(), assigned.vehicleId(), request.description().trim(), priority));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/mine")
    public List<DriverIssue> mine() {
        users.requireDriver();
        return issues.findByDriverIdOrderByCreatedAtDesc(users.getCurrentDriverId());
    }

    @GetMapping("/admin")
    public List<DriverIssue> all() {
        users.requireAdmin();
        return issues.findAllByOrderByCreatedAtDesc();
    }

    @PatchMapping("/admin/{id}/status")
    public DriverIssue update(@PathVariable String id, @RequestBody Map<String,String> input) {
        users.requireAdmin();
        String status = input.get("status");
        if (!Set.of("OPEN","IN_PROGRESS","RESOLVED").contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported issue status");
        }
        var issue=issues.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Issue not found"));
        issue.setStatus(status);
        return issues.save(issue);
    }
}
