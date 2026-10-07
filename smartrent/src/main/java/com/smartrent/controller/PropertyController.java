package com.smartrent.controller;

import com.smartrent.entity.Property;
import com.smartrent.service.PropertyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/properties")
public class PropertyController {

    private final PropertyService propertyService;

    public PropertyController(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    // CREATE a property
    @PostMapping
    public ResponseEntity<Property> createProperty(
            @RequestBody Property property) {

        Property saved = propertyService.createProperty(property);

        return ResponseEntity
                .created(URI.create("/api/properties/" + saved.getId()))
                .body(saved);
    }

    // READ all properties
    @GetMapping
    public List<Property> getAllProperties() {
        return propertyService.getAllProperties();
    }

    // READ one property
    @GetMapping("/{id}")
    public Property getPropertyById(@PathVariable("id") Long id) {
        return propertyService.getPropertyById(id);
    }

    // UPDATE a property
    @PutMapping("/{id}")
    public Property updateProperty(
            @PathVariable("id") Long id,
            @RequestBody Property property) {

        return propertyService.updateProperty(id, property);
    }

    // DELETE a property
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProperty(
            @PathVariable("id") Long id) {

        propertyService.deleteProperty(id);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/search")
public List<Property> searchProperties(
        @RequestParam(name = "locality", required = false)
        String locality,

        @RequestParam(name = "maxRent", required = false)
        BigDecimal maxRent,

        @RequestParam(name = "roomType", required = false)
        String roomType) {

    return propertyService.searchProperties(
            locality, maxRent, roomType
    );
}
}
