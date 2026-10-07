package com.smartrent.controller;

import com.smartrent.dto.VerificationRequest;
import com.smartrent.entity.Property;
import com.smartrent.service.AdminPropertyService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/properties")
public class AdminPropertyController {

    private final AdminPropertyService adminPropertyService;

    public AdminPropertyController(
            AdminPropertyService adminPropertyService) {

        this.adminPropertyService = adminPropertyService;
    }

    @GetMapping
    public List<Property> getProperties(
            @RequestParam(name = "status", required = false)
            String status) {

        return adminPropertyService.getProperties(status);
    }

    @PatchMapping("/{id}/verification")
    public Property verifyProperty(
            @PathVariable("id") Long id,
            @Valid @RequestBody VerificationRequest request) {

        return adminPropertyService.verifyProperty(id, request);
    }
}