package com.smartrent.service;

import com.smartrent.dto.VerificationRequest;
import com.smartrent.entity.Property;
import com.smartrent.entity.User;
import com.smartrent.repository.PropertyRepository;
import com.smartrent.repository.RentalRepository;
import com.smartrent.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class AdminPropertyService {

    private final PropertyRepository propertyRepository;
    private final RentalRepository rentalRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public AdminPropertyService(
            PropertyRepository propertyRepository,
            RentalRepository rentalRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService) {

        this.propertyRepository = propertyRepository;
        this.rentalRepository = rentalRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<Property> getProperties(String status) {
        requireAdmin();

        Sort sort = Sort.by(Sort.Direction.DESC, "id");

        if (status == null || status.isBlank()) {
            return propertyRepository.findAll(sort);
        }

        String normalizedStatus =
                status.trim().toUpperCase(Locale.ROOT);

        if (!List.of("PENDING", "VERIFIED", "REJECTED")
                .contains(normalizedStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Status must be PENDING, VERIFIED or REJECTED"
            );
        }

        Specification<Property> specification =
                (root, query, cb) -> cb.equal(
                        root.get("verificationStatus"),
                        normalizedStatus
                );

        return propertyRepository.findAll(specification, sort);
    }

    public Property verifyProperty(
            Long propertyId,
            VerificationRequest input) {

        requireAdmin();

        if (!"VERIFIED".equals(input.status())
                && !"REJECTED".equals(input.status())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Status must be VERIFIED or REJECTED"
            );
        }

        Property property = propertyRepository
                .findByIdForUpdate(propertyId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Property not found"
                ));

        if ("VERIFIED".equals(input.status())) {
            if (property.getOwnerId() == null) {
                throw conflict(
                        "A property must have an owner before approval"
                );
            }

            User owner = userRepository
                    .findById(property.getOwnerId())
                    .orElseThrow(() -> conflict(
                            "Property owner no longer exists"
                    ));

            boolean validRole = "OWNER".equals(owner.getRole())
                    || "ADMIN".equals(owner.getRole());

            if (!owner.isEnabled() || !validRole) {
                throw conflict(
                        "Property must belong to an active owner or admin"
                );
            }
        }

        if ("REJECTED".equals(input.status())
                && rentalRepository.existsByPropertyIdAndStatus(
                        propertyId, "ACTIVE")) {
            throw conflict(
                    "Cannot reject a property with an active rental"
            );
        }

        property.setVerificationStatus(input.status());

        return propertyRepository.save(property);
    }

    private void requireAdmin() {
        User user = currentUserService.getCurrentUser();

        if (!"ADMIN".equals(user.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only admins can verify properties"
            );
        }
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                message
        );
    }
}