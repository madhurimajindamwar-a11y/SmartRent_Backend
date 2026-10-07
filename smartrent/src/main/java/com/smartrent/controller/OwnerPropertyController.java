package com.smartrent.controller;

import com.smartrent.entity.Property;
import com.smartrent.service.OwnerPropertyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/owner/properties")
public class OwnerPropertyController {

    private final OwnerPropertyService ownerPropertyService;

    public OwnerPropertyController(
            OwnerPropertyService ownerPropertyService) {

        this.ownerPropertyService = ownerPropertyService;
    }

    @GetMapping
    public List<Property> getMyProperties() {
        return ownerPropertyService.getMyProperties();
    }
}