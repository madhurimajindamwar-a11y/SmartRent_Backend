package com.smartrent.service;

import com.smartrent.dto.ReviewRequest;
import com.smartrent.entity.Property;
import com.smartrent.entity.PropertyReview;
import com.smartrent.entity.User;
import com.smartrent.repository.PropertyRepository;
import com.smartrent.repository.PropertyReviewRepository;
import com.smartrent.repository.RentalRepository;
import com.smartrent.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

@Service
public class PropertyReviewService {

    private static final Set<String> ELIGIBLE_RENTAL_STATUSES =
            Set.of("ACTIVE", "ENDED", "COMPLETED");

    private final PropertyReviewRepository reviewRepository;
    private final RentalRepository rentalRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final PropertyService propertyService;

    public PropertyReviewService(
            PropertyReviewRepository reviewRepository,
            RentalRepository rentalRepository,
            PropertyRepository propertyRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService,
            PropertyService propertyService) {

        this.reviewRepository = reviewRepository;
        this.rentalRepository = rentalRepository;
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.propertyService = propertyService;
    }

    public record ReviewView(
            Long id,
            String reviewerName,
            Integer rating,
            String comment,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record ReviewSummary(
            double averageRating,
            long reviewCount,
            List<ReviewView> reviews
    ) {
    }

    public record MyReview(
            boolean canReview,
            ReviewView review
    ) {
    }

    @Transactional(readOnly = true)
    public ReviewSummary getReviews(Long propertyId) {
        // Uses the existing visibility rules for public property details.
        propertyService.getPropertyById(propertyId);

        Double average = reviewRepository.averageRating(propertyId);

        List<ReviewView> reviews = reviewRepository
                .findTop50ByPropertyIdOrderByUpdatedAtDescIdDesc(propertyId)
                .stream()
                .map(this::toView)
                .toList();

        return new ReviewSummary(
                average == null ? 0.0 : average,
                reviewRepository.countByPropertyId(propertyId),
                reviews
        );
    }

    @Transactional(readOnly = true)
    public MyReview getMyReview(Long propertyId) {
        User tenant = requireTenant();
        propertyService.getPropertyById(propertyId);

        PropertyReview existing = reviewRepository
                .findByPropertyIdAndTenantId(propertyId, tenant.getId())
                .orElse(null);

        return new MyReview(
                hasRentalHistory(tenant.getId(), propertyId),
                existing == null ? null : toView(existing)
        );
    }

    @Transactional
    public ReviewView saveReview(Long propertyId, ReviewRequest request) {
        User current = requireTenant();

        // Serializes submissions from the same tenant.
        User tenant = userRepository
                .findByIdForUpdate(current.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Please sign in again"
                ));

        if (!tenant.isEnabled() || !"TENANT".equals(tenant.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only active tenant accounts can write reviews"
            );
        }

        Property property = propertyRepository
                .findByIdForUpdate(propertyId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Property not found"
                ));

        if (!"VERIFIED".equals(property.getVerificationStatus())
                || property.getOwnerId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "This listing is unavailable for review"
            );
        }

        if (!hasRentalHistory(tenant.getId(), propertyId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can review only a property you have rented"
            );
        }

        PropertyReview review = reviewRepository
                .findByPropertyIdAndTenantId(propertyId, tenant.getId())
                .orElseGet(PropertyReview::new);

        review.setPropertyId(propertyId);
        review.setTenantId(tenant.getId());
        review.setRating(request.rating());
        review.setComment(request.comment().trim());

        return toView(reviewRepository.saveAndFlush(review));
    }

    private boolean hasRentalHistory(Long tenantId, Long propertyId) {
        return rentalRepository
                .findByTenantIdOrderByIdDesc(tenantId)
                .stream()
                .anyMatch(rental ->
                        propertyId.equals(rental.getPropertyId())
                                && ELIGIBLE_RENTAL_STATUSES.contains(
                                        rental.getStatus()
                                )
                );
    }

    private User requireTenant() {
        User user = currentUserService.getCurrentUser();

        if (!"TENANT".equals(user.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only tenants can write reviews"
            );
        }

        return user;
    }

    private ReviewView toView(PropertyReview review) {
        String name = userRepository.findById(review.getTenantId())
                .map(User::getName)
                .orElse("Tenant");

        return new ReviewView(
                review.getId(),
                name,
                review.getRating(),
                review.getComment(),
                review.getCreatedAt().toInstant(ZoneOffset.UTC),
                review.getUpdatedAt().toInstant(ZoneOffset.UTC)
        );
    }
}