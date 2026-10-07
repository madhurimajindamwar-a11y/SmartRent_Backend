package com.smartrent.service;

import com.smartrent.entity.Favorite;
import com.smartrent.entity.Property;
import com.smartrent.entity.User;
import com.smartrent.repository.FavoriteRepository;
import com.smartrent.repository.PropertyRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final PropertyRepository propertyRepository;
    private final CurrentUserService currentUserService;

    public FavoriteService(
            FavoriteRepository favoriteRepository,
            PropertyRepository propertyRepository,
            CurrentUserService currentUserService) {

        this.favoriteRepository = favoriteRepository;
        this.propertyRepository = propertyRepository;
        this.currentUserService = currentUserService;
    }

    public Favorite addFavorite(Long propertyId) {
        User tenant = requireTenant();

        Property property = propertyRepository
                .findByIdForUpdate(propertyId)
                .orElseThrow(() -> propertyNotFound());

        if (!"VERIFIED".equals(property.getVerificationStatus())
                || property.getOwnerId() == null) {
            throw propertyNotFound();
        }

        if (favoriteRepository.existsByTenantIdAndPropertyId(
                tenant.getId(), propertyId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Property is already in your favorites"
            );
        }

        Favorite favorite = new Favorite();
        favorite.setTenantId(tenant.getId());
        favorite.setPropertyId(propertyId);

        try {
            return favoriteRepository.saveAndFlush(favorite);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Could not save favorite. Refresh and try again."
            );
        }
    }

    @Transactional(readOnly = true)
    public List<Property> getMyFavorites() {
        User tenant = requireTenant();

        return favoriteRepository
                .findByTenantIdOrderByIdDesc(tenant.getId())
                .stream()
                .map(favorite -> propertyRepository.findById(
                        favorite.getPropertyId()
                ))
                .flatMap(java.util.Optional::stream)
                .filter(property ->
                        "VERIFIED".equals(
                                property.getVerificationStatus()
                        )
                        && property.getOwnerId() != null
                )
                .toList();
    }

    public void removeFavorite(Long propertyId) {
        User tenant = requireTenant();

        Favorite favorite = favoriteRepository
                .findByTenantIdAndPropertyId(
                        tenant.getId(), propertyId
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Property is not in your favorites"
                ));

        favoriteRepository.delete(favorite);
    }

    private User requireTenant() {
        User user = currentUserService.getCurrentUser();

        if (!"TENANT".equals(user.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only tenants can manage favorites"
            );
        }

        return user;
    }

    private ResponseStatusException propertyNotFound() {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Verified property not found"
        );
    }
}