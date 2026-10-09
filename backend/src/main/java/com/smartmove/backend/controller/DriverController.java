package com.smartmove.backend.controller;

import com.smartmove.backend.service.DriverService;
import com.smartmove.backend.service.CurrentUserService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/drivers")
public class DriverController {
  private final DriverService drivers;
  private final CurrentUserService currentUser;
  public DriverController(DriverService drivers, CurrentUserService currentUser) {
    this.drivers = drivers;
    this.currentUser = currentUser;
  }
  @GetMapping
  public List<DriverService.DriverProfile> list() {
    currentUser.requireAdmin();
    return drivers.getAllDrivers();
  }
}
