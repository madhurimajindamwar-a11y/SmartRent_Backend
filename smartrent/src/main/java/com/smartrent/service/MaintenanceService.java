package com.smartrent.service;

import com.smartrent.dto.CreateMaintenanceRequest;
import com.smartrent.dto.MaintenanceResponse;
import com.smartrent.dto.MaintenanceStatusRequest;
import com.smartrent.entity.MaintenanceRequest;
import com.smartrent.entity.Property;
import com.smartrent.entity.Rental;
import com.smartrent.entity.User;
import com.smartrent.repository.MaintenanceRepository;
import com.smartrent.repository.PropertyRepository;
import com.smartrent.repository.RentalRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class MaintenanceService {

    private final MaintenanceRepository maintenanceRepository;
    private final RentalRepository rentalRepository;
    private final PropertyRepository propertyRepository;
    private final CurrentUserService currentUserService;
    private final EntityManager entityManager;
    private final NotificationService notificationService;

    public MaintenanceService(
            MaintenanceRepository maintenanceRepository,
            RentalRepository rentalRepository,
            PropertyRepository propertyRepository,
            CurrentUserService currentUserService,
            EntityManager entityManager,
            NotificationService notificationService) {

        this.maintenanceRepository = maintenanceRepository;
        this.rentalRepository = rentalRepository;
        this.propertyRepository = propertyRepository;
        this.currentUserService = currentUserService;
        this.entityManager = entityManager;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<MaintenanceResponse> getMine() {
        User user = currentUserService.getCurrentUser();
        List<MaintenanceRequest> requests;

        if ("TENANT".equals(user.getRole())) {
            requests = maintenanceRepository.findForTenant(user.getId());
        } else if ("OWNER".equals(user.getRole())) {
            requests = maintenanceRepository.findForOwner(user.getId());
        } else {
            throw error(
                    HttpStatus.FORBIDDEN,
                    "Maintenance is available to tenants and owners"
            );
        }

        return requests.stream()
                .map(this::response)
                .toList();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public MaintenanceResponse create(CreateMaintenanceRequest input) {
        User user = currentUserService.getCurrentUser();

        if (!"TENANT".equals(user.getRole())) {
            throw error(
                    HttpStatus.FORBIDDEN,
                    "Only tenants can submit maintenance requests"
            );
        }

        Rental rental = entityManager.find(
                Rental.class,
                input.rentalId(),
                LockModeType.PESSIMISTIC_WRITE
        );

        if (rental == null) {
            throw error(HttpStatus.NOT_FOUND, "Rental not found");
        }

        if (!user.getId().equals(rental.getTenantId())) {
            throw error(
                    HttpStatus.FORBIDDEN,
                    "You cannot submit requests for another tenant"
            );
        }

        if (!"ACTIVE".equals(rental.getStatus())) {
            throw error(
                    HttpStatus.CONFLICT,
                    "Maintenance requires an active rental"
            );
        }

        String title = input.title().trim();
        String description = input.description().trim();

        if (title.isBlank() || description.isBlank()) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "Title and description are required"
            );
        }

        if (maintenanceRepository.existsByRentalIdAndTitleAndStatusIn(
                rental.getId(),
                title,
                List.of("OPEN", "IN_PROGRESS"))) {

            throw error(
                    HttpStatus.CONFLICT,
                    "An unresolved request with this title already exists"
            );
        }

        Property property = propertyRepository
                .findById(rental.getPropertyId())
                .orElseThrow(() -> error(
                        HttpStatus.NOT_FOUND,
                        "Property not found"
                ));

        MaintenanceRequest request = new MaintenanceRequest();
        request.setRentalId(rental.getId());
        request.setTitle(title);
        request.setDescription(description);
        request.setStatus("OPEN");

        MaintenanceRequest saved =
                maintenanceRepository.saveAndFlush(request);

        notificationService.publish(
                property.getOwnerId(),
                "New maintenance request",
                user.getName() + " reported \"" + saved.getTitle()
                        + "\" at \"" + property.getTitle() + "\".",
                "/maintenance"
        );

        return response(saved);
    }

    @Transactional
    public MaintenanceResponse updateStatus(
            Long id,
            MaintenanceStatusRequest input) {

        User owner = currentUserService.getCurrentUser();

        if (!"OWNER".equals(owner.getRole())) {
            throw error(
                    HttpStatus.FORBIDDEN,
                    "Only property owners can update maintenance status"
            );
        }

        MaintenanceRequest request =
                maintenanceRepository.findByIdForUpdate(id)
                        .orElseThrow(() -> error(
                                HttpStatus.NOT_FOUND,
                                "Request not found"
                        ));

        Rental rental = rentalRepository
                .findById(request.getRentalId())
                .orElseThrow(() -> error(
                        HttpStatus.NOT_FOUND,
                        "Rental not found"
                ));

        Property property = propertyRepository
                .findById(rental.getPropertyId())
                .orElseThrow(() -> error(
                        HttpStatus.NOT_FOUND,
                        "Property not found"
                ));

        if (!owner.getId().equals(property.getOwnerId())) {
            throw error(
                    HttpStatus.FORBIDDEN,
                    "You cannot manage another owner's request"
            );
        }

        String next = input.status();

        // Repeating the same status does not create another notification.
        if (next.equals(request.getStatus())) {
            return response(request);
        }

        boolean allowed =
                ("OPEN".equals(request.getStatus())
                        && "IN_PROGRESS".equals(next))
                || ("IN_PROGRESS".equals(request.getStatus())
                        && "RESOLVED".equals(next));

        if (!allowed) {
            throw error(
                    HttpStatus.CONFLICT,
                    "Use Open, then In Progress, then Resolved"
            );
        }

        request.setStatus(next);

        MaintenanceRequest saved =
                maintenanceRepository.saveAndFlush(request);

        notificationService.publish(
                rental.getTenantId(),
                "RESOLVED".equals(next)
                        ? "Maintenance issue resolved"
                        : "Maintenance work started",
                "\"" + saved.getTitle() + "\" at \""
                        + property.getTitle() + "\" is now "
                        + ("RESOLVED".equals(next)
                                ? "resolved."
                                : "in progress."),
                "/maintenance"
        );

        return response(saved);
    }

    private MaintenanceResponse response(MaintenanceRequest request) {
        Rental rental = rentalRepository
                .findById(request.getRentalId())
                .orElseThrow(() -> error(
                        HttpStatus.NOT_FOUND,
                        "Rental not found"
                ));

        Property property = propertyRepository
                .findById(rental.getPropertyId())
                .orElseThrow(() -> error(
                        HttpStatus.NOT_FOUND,
                        "Property not found"
                ));

        return new MaintenanceResponse(
                request.getId(),
                request.getRentalId(),
                property.getId(),
                property.getTitle(),
                rental.getTenantId(),
                request.getTitle(),
                request.getDescription(),
                request.getStatus(),
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
    }

    private ResponseStatusException error(
            HttpStatus status,
            String message) {

        return new ResponseStatusException(status, message);
    }
}