package com.smartrent.controller;

import com.smartrent.entity.Favorite;
import com.smartrent.entity.Property;
import com.smartrent.service.FavoriteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @PostMapping("/{propertyId}")
    public ResponseEntity<Favorite> addFavorite(
            @PathVariable("propertyId") Long propertyId) {

        Favorite favorite = favoriteService.addFavorite(propertyId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(favorite);
    }

    @GetMapping
    public List<Property> getMyFavorites() {
        return favoriteService.getMyFavorites();
    }

    @DeleteMapping("/{propertyId}")
    public ResponseEntity<Void> removeFavorite(
            @PathVariable("propertyId") Long propertyId) {

        favoriteService.removeFavorite(propertyId);

        return ResponseEntity.noContent().build();
    }
}