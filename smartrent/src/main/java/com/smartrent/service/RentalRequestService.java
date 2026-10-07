package com.smartrent.service;

import com.smartrent.dto.CreateRentalRequest;
import com.smartrent.dto.RentalDecisionRequest;
import com.smartrent.entity.Property;
import com.smartrent.entity.Rental;
import com.smartrent.entity.RentalRequest;
import com.smartrent.entity.User;
import com.smartrent.repository.PropertyRepository;
import com.smartrent.repository.RentalRepository;
import com.smartrent.repository.RentalRequestRepository;
import com.smartrent.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class RentalRequestService {

    private final RentalRequestRepository requestRepository;
    private final RentalRepository rentalRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final EntityManager entityManager;

    public RentalRequestService(
            RentalRequestRepository requestRepository,
            RentalRepository rentalRepository,
            PropertyRepository propertyRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService,
            EntityManager entityManager) {

        this.requestRepository = requestRepository;
        this.rentalRepository = rentalRepository;
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.entityManager = entityManager;
    }

    // TENANT: submit a request for a verified, available property.
    public RentalRequest createRequest(CreateRentalRequest input) {
        User tenant = requireTenant();

        lockTenant(tenant.getId());
        Property property = lockProperty(input.propertyId());

        requireVerified(property);

        if (!property.isAvailable()
                || rentalRepository.existsByPropertyIdAndStatus(
                        property.getId(), "ACTIVE")) {
            throw conflict("Property is not available");
        }

        if (rentalRepository.existsByTenantIdAndStatus(
                tenant.getId(), "ACTIVE")) {
            throw conflict("You already have an active rental");
        }

        if (requestRepository.existsByTenantIdAndPropertyIdAndStatus(
                tenant.getId(), property.getId(), "PENDING")) {
            throw conflict(
                    "You already have a pending request for this property"
            );
        }

        RentalRequest request = new RentalRequest();
        request.setTenantId(tenant.getId());
        request.setPropertyId(property.getId());
        request.setMessage(input.message());
        request.setStatus("PENDING");

        return requestRepository.save(request);
    }

    @Transactional(readOnly = true)
    public List<RentalRequest> getMyRequests() {
        User tenant = requireTenant();

        return requestRepository.findByTenantIdOrderByIdDesc(
                tenant.getId()
        );
    }

    @Transactional(readOnly = true)
    public List<RentalRequest> getOwnerRequests() {
        User owner = currentUserService.requireOwnerOrAdmin();

        return requestRepository.findRequestsForOwner(owner.getId());
    }

    // OWNER/ADMIN: accept or reject a pending request.
    public RentalRequest decideRequest(
            Long requestId,
            RentalDecisionRequest input) {

        User owner = currentUserService.requireOwnerOrAdmin();

        if (!"ACCEPTED".equals(input.status())
                && !"REJECTED".equals(input.status())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Status must be ACCEPTED or REJECTED"
            );
        }

        RentalRequest request = findRequest(requestId);

        // Consistent lock order: tenant, property, request.
        User tenant = lockTenant(request.getTenantId());
        Property property = lockProperty(request.getPropertyId());

        checkOwnership(property, owner);

        entityManager.refresh(
                request,
                LockModeType.PESSIMISTIC_WRITE
        );

        requirePending(request);

        // Rejection remains possible even if approval was withdrawn.
        if ("REJECTED".equals(input.status())) {
            request.setStatus("REJECTED");
            return requestRepository.save(request);
        }

        // Acceptance requires current property approval.
        requireVerified(property);

        if (!tenant.isEnabled()) {
            throw conflict("The tenant account is disabled");
        }

        if (!property.isAvailable()
                || rentalRepository.existsByPropertyIdAndStatus(
                        property.getId(), "ACTIVE")) {
            throw conflict("Property is no longer available");
        }

        if (rentalRepository.existsByTenantIdAndStatus(
                tenant.getId(), "ACTIVE")) {
            throw conflict("This tenant already has an active rental");
        }

        Rental rental = new Rental();
        rental.setRequestId(request.getId());
        rental.setTenantId(tenant.getId());
        rental.setPropertyId(property.getId());
        rental.setAgreedRent(property.getRent());
        rental.setStartDate(
                LocalDate.now(ZoneId.of("Asia/Kolkata"))
        );
        rental.setStatus("ACTIVE");

        rentalRepository.save(rental);

        property.setAvailable(false);
        propertyRepository.save(property);

        request.setStatus("ACCEPTED");
        requestRepository.saveAndFlush(request);

        List<RentalRequest> competingRequests =
                requestRepository.findByPropertyIdAndStatus(
                        property.getId(), "PENDING"
                );

        for (RentalRequest competing : competingRequests) {
            competing.setStatus("REJECTED");
        }

        requestRepository.saveAll(competingRequests);

        return request;
    }

    // TENANT: cancel their own pending request.
    public RentalRequest cancelRequest(Long requestId) {
        User tenant = requireTenant();
        RentalRequest request = findRequest(requestId);

        if (!tenant.getId().equals(request.getTenantId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can only cancel your own requests"
            );
        }

        lockTenant(tenant.getId());
        lockProperty(request.getPropertyId());

        entityManager.refresh(
                request,
                LockModeType.PESSIMISTIC_WRITE
        );

        requirePending(request);

        request.setStatus("CANCELLED");
        return requestRepository.save(request);
    }

    @Transactional(readOnly = true)
    public List<Rental> getMyRentals() {
        User tenant = requireTenant();

        return rentalRepository.findByTenantIdOrderByIdDesc(
                tenant.getId()
        );
    }

    @Transactional(readOnly = true)
    public List<Rental> getOwnerRentals() {
        User owner = currentUserService.requireOwnerOrAdmin();

        return rentalRepository.findRentalsForOwner(owner.getId());
    }

    private User requireTenant() {
        User user = currentUserService.getCurrentUser();

        if (!"TENANT".equals(user.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only tenants can perform this action"
            );
        }

        return user;
    }

    private RentalRequest findRequest(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Rental request not found"
                ));
    }

    private User lockTenant(Long id) {
        return userRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Tenant not found"
                ));
    }

    private Property lockProperty(Long id) {
        return propertyRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Property not found"
                ));
    }

    private void requireVerified(Property property) {
        if (property.getOwnerId() == null
                || !"VERIFIED".equals(
                        property.getVerificationStatus())) {
            throw conflict(
                    "Only verified properties can receive "
                            + "or accept rental requests"
            );
        }
    }

    private void checkOwnership(Property property, User user) {
        boolean isAdmin = "ADMIN".equals(user.getRole());
        boolean isOwner = user.getId().equals(property.getOwnerId());

        if (!isAdmin && !isOwner) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can only manage requests for your own properties"
            );
        }
    }

    private void requirePending(RentalRequest request) {
        if (!"PENDING".equals(request.getStatus())) {
            throw conflict("Only pending requests can be changed");
        }
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                message
        );
    }
}