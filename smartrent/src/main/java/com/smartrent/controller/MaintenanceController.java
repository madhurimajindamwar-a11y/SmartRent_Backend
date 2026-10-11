package com.smartrent.controller;

import com.smartrent.dto.CreateMaintenanceRequest;
import com.smartrent.dto.MaintenanceResponse;
import com.smartrent.dto.MaintenanceStatusRequest;
import com.smartrent.service.MaintenanceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/maintenance")
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    public MaintenanceController(MaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @GetMapping
    public List<MaintenanceResponse> getMine() {
        return maintenanceService.getMine();
    }

    @PostMapping
    public ResponseEntity<MaintenanceResponse> create(
            @Valid @RequestBody CreateMaintenanceRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(maintenanceService.create(request));
    }

    @PatchMapping("/{id}/status")
    public MaintenanceResponse updateStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody MaintenanceStatusRequest request) {

        return maintenanceService.updateStatus(id, request);
    }
}