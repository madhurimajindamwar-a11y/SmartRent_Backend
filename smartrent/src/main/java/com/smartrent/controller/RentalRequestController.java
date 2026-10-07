package com.smartrent.controller;

import com.smartrent.dto.CreateRentalRequest;
import com.smartrent.dto.RentalDecisionRequest;
import com.smartrent.entity.Rental;
import com.smartrent.entity.RentalRequest;
import com.smartrent.service.RentalRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class RentalRequestController {

    private final RentalRequestService rentalRequestService;

    public RentalRequestController(
            RentalRequestService rentalRequestService) {

        this.rentalRequestService = rentalRequestService;
    }

    // Tenant submits a request.
    @PostMapping("/rental-requests")
    public ResponseEntity<RentalRequest> createRequest(
            @Valid @RequestBody CreateRentalRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(rentalRequestService.createRequest(request));
    }

    // Tenant views their requests.
    @GetMapping("/me/rental-requests")
    public List<RentalRequest> getMyRequests() {
        return rentalRequestService.getMyRequests();
    }

    // Owner views requests for their properties.
    @GetMapping("/owner/rental-requests")
    public List<RentalRequest> getOwnerRequests() {
        return rentalRequestService.getOwnerRequests();
    }

    // Owner accepts or rejects a request.
    @PatchMapping("/rental-requests/{id}/status")
    public RentalRequest decideRequest(
            @PathVariable("id") Long id,
            @Valid @RequestBody RentalDecisionRequest request) {

        return rentalRequestService.decideRequest(id, request);
    }

    // Tenant cancels their pending request.
    @PatchMapping("/rental-requests/{id}/cancel")
    public RentalRequest cancelRequest(
            @PathVariable("id") Long id) {

        return rentalRequestService.cancelRequest(id);
    }

    // Tenant views their rentals.
    @GetMapping("/me/rentals")
    public List<Rental> getMyRentals() {
        return rentalRequestService.getMyRentals();
    }

    // Owner views rentals for their properties.
    @GetMapping("/owner/rentals")
    public List<Rental> getOwnerRentals() {
        return rentalRequestService.getOwnerRentals();
    }
}