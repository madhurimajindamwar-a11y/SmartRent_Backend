package com.smartrent.dto;

import java.time.LocalDateTime;

public record MaintenanceResponse(
        Long id,
        Long rentalId,
        Long propertyId,
        String propertyTitle,
        Long tenantId,
        String title,
        String description,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}