package com.smartrent.service;

import com.smartrent.entity.Property;
import com.smartrent.entity.User;
import com.smartrent.repository.PropertyRepository;
import com.smartrent.repository.RentalRepository;
import jakarta.persistence.EntityManager;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final RentalRepository rentalRepository;
    private final CurrentUserService currentUserService;
    private final EntityManager entityManager;

    public PropertyService(
            PropertyRepository propertyRepository,
            RentalRepository rentalRepository,
            CurrentUserService currentUserService,
            EntityManager entityManager) {

        this.propertyRepository = propertyRepository;
        this.rentalRepository = rentalRepository;
        this.currentUserService = currentUserService;
        this.entityManager = entityManager;
    }

    // CREATE: clients cannot approve their own listings.
    public Property createProperty(Property input) {
        User owner = currentUserService.requireOwnerOrAdmin();

        validateProperty(input);

        Property property = new Property();
        copyDetails(input, property);
        property.setOwnerId(owner.getId());
        property.setVerificationStatus("PENDING");

        return propertyRepository.save(property);
    }

    // PUBLIC LIST: verified properties only.
    @Transactional(readOnly = true)
    public List<Property> getAllProperties() {
        return propertyRepository.findAll(verifiedOnly());
    }

    // PUBLIC DETAILS: unverified properties are hidden.
    @Transactional(readOnly = true)
    public Property getPropertyById(Long id) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> notFound());

        if (!"VERIFIED".equals(property.getVerificationStatus())) {
            throw notFound();
        }

        return property;
    }

    // UPDATE: owner/admin only.
    public Property updateProperty(Long id, Property input) {
        User user = currentUserService.requireOwnerOrAdmin();
        Property existing = lockProperty(id);

        checkOwnership(existing, user);
        validateProperty(input);

        boolean activelyRented =
                rentalRepository.existsByPropertyIdAndStatus(
                        id, "ACTIVE"
                );

        if (activelyRented && input.isAvailable()) {
            throw conflict(
                    "An actively rented property cannot be marked available"
            );
        }

        boolean needsReview = listingDetailsChanged(existing, input);

        copyDetails(input, existing);

        if (needsReview) {
            existing.setVerificationStatus("PENDING");
        }

        return propertyRepository.save(existing);
    }

    // DELETE: preserve properties with request/rental history.
    public void deleteProperty(Long id) {
        User user = currentUserService.requireOwnerOrAdmin();
        Property existing = lockProperty(id);

        checkOwnership(existing, user);

        Long requestCount = entityManager.createQuery(
                """
                SELECT COUNT(r)
                FROM RentalRequest r
                WHERE r.propertyId = :propertyId
                """,
                Long.class
        )
                .setParameter("propertyId", id)
                .getSingleResult();

        if (requestCount > 0) {
            throw conflict(
                    "This property has rental-request history "
                            + "and cannot be deleted. "
                            + "Set its availability to false instead."
            );
        }

        propertyRepository.delete(existing);
    }

    // SEARCH: verified AND available properties only.
    @Transactional(readOnly = true)
    public List<Property> searchProperties(
            String locality,
            BigDecimal maxRent,
            String roomType) {

        if (maxRent != null && maxRent.signum() < 0) {
            throw badRequest("Maximum rent cannot be negative");
        }

        Specification<Property> specification =
                verifiedOnly().and(
                        (root, query, cb) ->
                                cb.isTrue(root.<Boolean>get("available"))
                );

        if (locality != null && !locality.isBlank()) {
            String location =
                    locality.trim().toLowerCase(Locale.ROOT);

            specification = specification.and(
                    (root, query, cb) -> cb.equal(
                            cb.lower(root.<String>get("locality")),
                            location
                    )
            );
        }

        if (maxRent != null) {
            specification = specification.and(
                    (root, query, cb) -> cb.lessThanOrEqualTo(
                            root.<BigDecimal>get("rent"),
                            maxRent
                    )
            );
        }

        if (roomType != null && !roomType.isBlank()) {
            String type =
                    roomType.trim().toLowerCase(Locale.ROOT);

            specification = specification.and(
                    (root, query, cb) -> cb.equal(
                            cb.lower(root.<String>get("roomType")),
                            type
                    )
            );
        }

        return propertyRepository.findAll(specification);
    }

    private Specification<Property> verifiedOnly() {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("verificationStatus"), "VERIFIED"),
                cb.isNotNull(root.get("ownerId"))
        );
    }

    private Property lockProperty(Long id) {
        return propertyRepository.findByIdForUpdate(id)
                .orElseThrow(() -> notFound());
    }

    private void checkOwnership(Property property, User user) {
        boolean isAdmin = "ADMIN".equals(user.getRole());
        boolean isOwner = user.getId().equals(property.getOwnerId());

        if (!isAdmin && !isOwner) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can only modify your own properties"
            );
        }
    }

    private boolean listingDetailsChanged(
            Property existing,
            Property input) {

        return !Objects.equals(
                    existing.getTitle(), input.getTitle().trim())
                || !Objects.equals(
                    existing.getDescription(), input.getDescription())
                || !Objects.equals(
                    existing.getLocality(), input.getLocality().trim())
                || existing.getRent().compareTo(input.getRent()) != 0
                || !Objects.equals(
                    existing.getRoomType(), input.getRoomType().trim())
                || existing.isWifi() != input.isWifi()
                || existing.isFood() != input.isFood()
                || existing.isAc() != input.isAc();
    }

    private void validateProperty(Property property) {
        if (property.getTitle() == null
                || property.getTitle().isBlank()
                || property.getTitle().length() > 150) {
            throw badRequest(
                    "Title is required and must not exceed 150 characters"
            );
        }

        if (property.getLocality() == null
                || property.getLocality().isBlank()
                || property.getLocality().length() > 100) {
            throw badRequest(
                    "Locality is required and must not exceed 100 characters"
            );
        }

        if (property.getRoomType() == null
                || property.getRoomType().isBlank()
                || property.getRoomType().length() > 30) {
            throw badRequest(
                    "Room type is required and must not exceed 30 characters"
            );
        }

        if (property.getDescription() != null
                && property.getDescription().length() > 2000) {
            throw badRequest(
                    "Description must not exceed 2000 characters"
            );
        }

        if (property.getRent() == null
                || property.getRent().signum() < 0
                || property.getRent().compareTo(
                        new BigDecimal("99999999.99")) > 0
                || property.getRent().stripTrailingZeros().scale() > 2) {
            throw badRequest(
                    "Rent must be between 0 and 99999999.99 "
                            + "with at most 2 decimal places"
            );
        }
    }

    // Never copy ID, ownerId, or verificationStatus from input.
    private void copyDetails(Property source, Property target) {
        target.setTitle(source.getTitle().trim());
        target.setDescription(source.getDescription());
        target.setLocality(source.getLocality().trim());
        target.setRent(source.getRent());
        target.setRoomType(source.getRoomType().trim());
        target.setWifi(source.isWifi());
        target.setFood(source.isFood());
        target.setAc(source.isAc());
        target.setAvailable(source.isAvailable());
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Property not found"
        );
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                message
        );
    }
}