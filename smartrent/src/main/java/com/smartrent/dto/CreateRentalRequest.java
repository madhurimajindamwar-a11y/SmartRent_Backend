package com.smartrent.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateRentalRequest(

        @NotNull(message = "Property ID is required")
        @Positive(message = "Property ID must be positive")
        Long propertyId,

        @Size(max = 1000,
            message = "Message must not exceed 1000 characters")
        String message

) {
}