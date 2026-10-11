package com.smartrent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MaintenanceStatusRequest(
        @NotBlank
        @Pattern(regexp = "IN_PROGRESS|RESOLVED")
        String status
) {
}