package com.smartrent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RentalDecisionRequest(

        @NotBlank(message = "Status is required")
        @Pattern(
                regexp = "ACCEPTED|REJECTED",
                message = "Status must be ACCEPTED or REJECTED"
        )
        String status

) {
}