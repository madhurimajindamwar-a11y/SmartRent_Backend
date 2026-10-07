package com.smartrent.service;

import com.smartrent.entity.Property;
import com.smartrent.entity.User;
import com.smartrent.repository.PropertyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OwnerPropertyService {

    private final PropertyRepository propertyRepository;
    private final CurrentUserService currentUserService;

    public OwnerPropertyService(
            PropertyRepository propertyRepository,
            CurrentUserService currentUserService) {

        this.propertyRepository = propertyRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<Property> getMyProperties() {
        User owner = currentUserService.requireOwnerOrAdmin();

        return propertyRepository.findByOwnerIdOrderByIdDesc(
                owner.getId()
        );
    }
}