package com.smartmove.backend.controller;

import com.smartmove.backend.document.TripCheckpoint;
import com.smartmove.backend.entity.Trip;
import com.smartmove.backend.entity.RouteStop;
import com.smartmove.backend.entity.UserRole;
import com.smartmove.backend.repository.TripRepository;
import com.smartmove.backend.repository.RouteStopRepository;
import com.smartmove.backend.repository.BookingRepository;
import com.smartmove.backend.repository.TripCheckpointRepository;
import com.smartmove.backend.service.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/trip-checkpoints")
public class TripCheckpointController {
    private final TripRepository trips;
    private final RouteStopRepository stops;
    private final BookingRepository bookings;
    private final TripCheckpointRepository checkpoints;
    private final CurrentUserService users;

    public TripCheckpointController(TripRepository trips, RouteStopRepository stops,
            BookingRepository bookings, TripCheckpointRepository checkpoints, CurrentUserService users) {
        this.trips=trips; this.stops=stops; this.bookings=bookings; this.checkpoints=checkpoints; this.users=users;
    }
    public record CheckpointProgress(Long tripId, String tripStatus, List<RouteStopInfo> stops,
                                      List<ConfirmedStop> confirmed) {}
    public record RouteStopInfo(Long id, String name, Integer order) {}
    public record ConfirmedStop(Long stopId, String name, Integer order, LocalDateTime confirmedAt) {}
    public record ConfirmRequest(Long stopId) {}

    private Trip trip(Long id) {
        return trips.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Trip not found"));
    }
    private void checkViewer(Trip t) {
        if(users.hasAnyRole(UserRole.ADMIN,UserRole.SUPER_ADMIN))return;
        if(users.hasRole(UserRole.DRIVER) && t.getDriver()!=null
                && users.getCurrentDriverId().equals(t.getDriver().getId()))return;
        if(users.hasRole(UserRole.PASSENGER) && bookings.findByTripId(t.getId()).stream().anyMatch(b ->
                b.getPassenger().getId().equals(users.getCurrentPassengerId())
                && "CONFIRMED".equalsIgnoreCase(b.getStatus())))return;
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Not authorized to view this trip");
    }
    private CheckpointProgress progress(Trip t) {
        List<RouteStopInfo> routeStops=stops.findByRouteIdOrderByStopOrderAsc(t.getRoute().getId())
                .stream().map(s -> new RouteStopInfo(s.getId(),s.getStopName(),s.getStopOrder())).toList();
        List<ConfirmedStop> reached=checkpoints.findByTripIdOrderByStopOrderAsc(t.getId())
                .stream().map(c -> new ConfirmedStop(c.getStopId(),c.getStopName(),c.getStopOrder(),c.getConfirmedAt())).toList();
        return new CheckpointProgress(t.getId(),t.getStatus(),routeStops,reached);
    }
    @GetMapping("/{tripId}")
    public CheckpointProgress get(@PathVariable Long tripId) {
        Trip t=trip(tripId);checkViewer(t);return progress(t);
    }
    @PostMapping("/{tripId}/confirm")
    public synchronized CheckpointProgress confirm(@PathVariable Long tripId,@RequestBody ConfirmRequest request) {
        users.requireDriver();
        Trip t=trip(tripId);
        if(t.getDriver()==null || !users.getCurrentDriverId().equals(t.getDriver().getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Not assigned to trip");
        if(!"IN_PROGRESS".equalsIgnoreCase(t.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Start the trip before confirming stops");
        List<RouteStop> ordered=stops.findByRouteIdOrderByStopOrderAsc(t.getRoute().getId());
        List<TripCheckpoint> reached=checkpoints.findByTripIdOrderByStopOrderAsc(tripId);
        if(reached.size()>=ordered.size())
            throw new ResponseStatusException(HttpStatus.CONFLICT,"All stops already confirmed");
        RouteStop next=ordered.get(reached.size());
        if(request==null || !next.getId().equals(request.stopId()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Confirm stops in route order");
        TripCheckpoint record=new TripCheckpoint();
        record.setTripId(tripId);record.setDriverId(users.getCurrentDriverId());
        record.setStopId(next.getId());record.setStopOrder(next.getStopOrder());
        record.setStopName(next.getStopName());record.setConfirmedAt(LocalDateTime.now());
        checkpoints.save(record);
        return progress(t);
    }
}
