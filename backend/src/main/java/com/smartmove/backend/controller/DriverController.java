package com.smartmove.backend.controller;

import com.smartmove.backend.service.DriverService;
import com.smartmove.backend.service.CurrentUserService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.time.LocalDate;
import org.springframework.http.*;

@RestController
@RequestMapping("/api/drivers")
public class DriverController {
  private final DriverService drivers;
  private final CurrentUserService currentUser;
  public DriverController(DriverService drivers, CurrentUserService currentUser) {
    this.drivers = drivers;
    this.currentUser = currentUser;
  }
  public record DriverInput(String name, String email, String phone, String licenseNumber, LocalDate licenseExpiry, Integer experienceYears) {}
  @PostMapping
  public ResponseEntity<DriverService.DriverProfile> create(@RequestBody DriverInput input) {
    currentUser.requireAdmin();
    return ResponseEntity.status(HttpStatus.CREATED).body(drivers.createDriver(
      new DriverService.CreateDriverRequest(input.name(), input.email(), input.phone(), input.licenseNumber(), input.licenseExpiry(), input.experienceYears())));
  }
  @GetMapping("/me")
  public DriverService.DriverProfile myProfile() {
    currentUser.requireDriver();
    return drivers.getDriverProfile(currentUser.getCurrentDriverId());
  }
  @PutMapping("/{id}")
  public DriverService.DriverProfile update(@PathVariable Long id, @RequestBody DriverInput input) {
    currentUser.requireAdmin();
    return drivers.updateDriver(id, new DriverService.UpdateDriverRequest(input.name(), input.phone(), input.licenseNumber(), input.licenseExpiry(), input.experienceYears()));
  }
  @GetMapping
  public List<DriverService.DriverProfile> list() {
    currentUser.requireAdmin();
    return drivers.getAllDrivers();
  }
}
