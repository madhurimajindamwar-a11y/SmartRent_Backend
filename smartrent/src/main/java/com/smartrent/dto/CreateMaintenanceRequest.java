package com.smartrent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateMaintenanceRequest(
        @NotNull
        @Positive
        Long rentalId,

        @NotBlank
        @Size(max = 150)
        String title,

        @NotBlank
        @Size(max = 2000)
        String description
) {
}