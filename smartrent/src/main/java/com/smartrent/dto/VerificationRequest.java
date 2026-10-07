package com.smartrent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerificationRequest(

        @NotBlank(message = "Verification status is required")
        @Pattern(
                regexp = "VERIFIED|REJECTED",
                message = "Status must be VERIFIED or REJECTED"
        )
        String status

) {
}