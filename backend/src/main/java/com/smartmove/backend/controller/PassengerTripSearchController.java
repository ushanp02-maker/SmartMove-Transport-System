package com.smartmove.backend.controller;

import com.smartmove.backend.entity.Route;
import com.smartmove.backend.entity.RouteStop;
import com.smartmove.backend.repository.RouteStopRepository;
import com.smartmove.backend.repository.RouteRepository;
import com.smartmove.backend.service.TripService;
import com.smartmove.backend.service.TripService.TripProfile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/trips")
public class PassengerTripSearchController {
    private final TripService tripService;
    private final RouteRepository routes;
    private final RouteStopRepository stops;

    public PassengerTripSearchController(TripService tripService, RouteRepository routes,
                                         RouteStopRepository stops) {
        this.tripService = tripService;
        this.routes = routes;
        this.stops = stops;
    }

    public record StopMatch(Long id, Long routeId, String stopName, Integer stopOrder) {}
    public record SearchResult(TripProfile trip, StopMatch board, StopMatch destination,
                               String origin, String destinationName) {}

    private static boolean matches(String actual, String requested) {
        return actual != null && actual.trim().toLowerCase(Locale.ROOT)
                .contains(requested.trim().toLowerCase(Locale.ROOT));
    }

    @GetMapping("/search")
    @Transactional(readOnly = true)
    public List<SearchResult> search(@RequestParam String from, @RequestParam String to,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "STANDARD") String type) {
        if (from == null || from.isBlank() || to == null || to.isBlank() || date == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Origin, destination and date are required");
        }
        String service = type.trim().toUpperCase(Locale.ROOT);
        if (!service.equals("STANDARD") && !service.equals("STAFF")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Service type must be STANDARD or STAFF");
        }
        LocalDateTime now = LocalDateTime.now();
        return tripService.getUpcomingTrips().stream()
                .filter(t -> t.departureTime().toLocalDate().equals(date))
                .filter(t -> !t.departureTime().isBefore(now))
                .map(t -> {
                    Route route = routes.findById(t.routeId()).orElse(null);
                    if (route == null || !service.equalsIgnoreCase(route.getServiceType())) return null;
                    List<RouteStop> ordered = stops.findByRouteIdOrderByStopOrderAsc(route.getId());
                    for (RouteStop board : ordered) {
                        if (!matches(board.getStopName(), from)) continue;
                        for (RouteStop drop : ordered) {
                            if (drop.getStopOrder() <= board.getStopOrder()
                                    || !matches(drop.getStopName(), to)) continue;
                            return new SearchResult(t,
                                    new StopMatch(board.getId(), route.getId(), board.getStopName(), board.getStopOrder()),
                                    new StopMatch(drop.getId(), route.getId(), drop.getStopName(), drop.getStopOrder()),
                                    route.getOrigin(), route.getDestination());
                        }
                    }
                    return null;
                })
                .filter(result -> result != null)
                .sorted(Comparator.comparing(result -> result.trip().departureTime()))
                .toList();
    }
}
